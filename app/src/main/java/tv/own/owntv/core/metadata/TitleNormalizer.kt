package tv.own.owntv.core.metadata

/**
 * Cleans messy IPTV VOD titles into a searchable query + an extracted year (plan §3). Providers ship
 * titles like `EN| The Movie Name (2021) [HD] (MULTI-SUB)` — prefixes, quality tags, country flags and
 * embedded years all confuse a TMDB search. This strips the noise; the shared matcher then searches TMDB.
 *
 * Pure and stateless so it's trivially unit-testable and reused by Trakt later (cloud-backup-plan §9.4).
 */
object TitleNormalizer {

    data class Normalized(val query: String, val year: Int?)

    // Leading provider/language tag ending in a pipe or colon, e.g. "EN|", "4K|", "AR:", "VIP:".
    // Applied repeatedly to peel stacked tags ("EN| 4K| Movie" → "Movie").
    private val WHITESPACE = Regex("""\s+""")

    private val PIPE_TAG = Regex("""^\s*[A-Z0-9+]{1,8}\s*[|:]\s*""")

    // Leading provider/quality prefix that ends at a " - " separator, e.g. "4K-OSN+ - Title",
    // "VIP - 4K - Title", "OSN - Title". The captured prefix is all uppercase/digits/+/-, so a normal
    // Title-Case title ("Gangs of London", "Mission: Impossible") is never matched. To avoid eating a
    // genuinely upper-case multi-word title ("MAD MAX - Fury Road"), we only strip when the prefix looks
    // provider-ish: it contains a digit or '+', OR it is a single token (see [stripDashPrefix]).
    private val DASH_PREFIX = Regex("""^\s*([0-9A-Z][0-9A-Z+\-]*(?:\s+[0-9A-Z+\-]+)*)\s+-\s+""")

    // Bracketed / parenthesised tags: [HD], (MULTI-SUB), {1080p}. Years in parens are handled separately.
    private val BRACKET_TAG = Regex("""[\[{(][^\[\]{}()]*[\]})]""")

    // Standalone quality / release markers anywhere in the title.
    private val QUALITY_MARKER = Regex(
        "(?i)\\b(4k|uhd|fhd|hd|sd|hevc|h\\.?265|h\\.?264|x265|x264|hdr10?\\+?|dolby|atmos|" +
            "multi[- ]?sub|multisub|dual[- ]?audio|remux|web[- ]?dl|webrip|bluray|bdrip|dvdrip|hdrip|" +
            "imax|extended|uncut|remastered|vip|" +
            "vostfr|vost|vf|subbed|dubbed|dublado|legendado|castellano|truefrench|hdlight|" +
            // [257]\.1 needs the literal dot so "Area 51" / "Formula 51" are never touched.
            "10bit|8bit|60fps|50fps|aac|e?ac3|dts|ddp?|[257]\\.1|hdtc|hdcam|camrip)\\b"
    )

    // Trailing season/episode tail on a series name: "Breaking Bad S05", "Loki Season 2",
    // "Dark Staffel 1", "Casa Temporada 3 E04". The bare "s" form must sit directly against the
    // number (S05) so possessives ("Ocean's 8") and plural words are never eaten.
    private val SEASON_EPISODE_TAIL = Regex(
        "(?i)[\\s\\-–—:._]*(?:(?:season|saison|temporada|staffel)\\s*\\d{1,2}|s\\d{1,2})" +
            "(?:\\s*(?:e|ep|episode|x)\\s*\\d{1,4})?\\s*$"
    )

    // Trailing UPPERCASE language tag providers append ("Movie Name FR", "Show LAT").
    // Case-sensitive on purpose: a title-case word like "Fr"/"Sub" is never touched, and the risky
    // real-word codes (IT, US) are deliberately excluded.
    private val TRAILING_LANG_TAG = Regex("""\s+(?:FR|EN|DE|ES|PT|NL|PL|TR|AR|RU|LAT|SUB|DUB|MULTI)$""")

    // A 4-digit year, optionally in parens/brackets: (2021), [1999], 2015.
    private val YEAR = Regex("""[\[(]?\b(19\d{2}|20\d{2})\b[\])]?""")

    // Trailing junk separators/flags left after stripping.
    private val EMOJI_FLAG = Regex("""[🇦-🇿]""")
    private val MULTI_SPACE = Regex("""\s{2,}""")
    private val EDGE_JUNK = Regex("""^[\s\-–—|:._]+|[\s\-–—|:._]+$""")

    fun normalize(raw: String): Normalized {
        if (raw.isBlank()) return Normalized("", null)
        var s = raw

        // 1. Extract a year (prefer the last 4-digit year — series often carry a leading channel number).
        val year = YEAR.findAll(s).mapNotNull { it.groupValues[1].toIntOrNull() }
            .lastOrNull()?.takeIf { it in 1900..2099 }

        // 2. Strip leading provider/language tags repeatedly: pipe/colon tags first ("EN| 4K| Movie"),
        //    then provider prefixes that end at a " - " separator ("4K-OSN+ - Gangs of London").
        var prev: String
        do { prev = s; s = s.replace(PIPE_TAG, ""); s = stripDashPrefix(s) } while (s != prev)

        // 3. Remove bracketed tags, quality markers, flags, and any remaining year token.
        s = s.replace(BRACKET_TAG, " ")
            .replace(QUALITY_MARKER, " ")
            .replace(YEAR, " ")
            .replace(EMOJI_FLAG, " ")

        // 3b. Peel trailing season/episode tails and language tags (repeatedly — "Show S01 FR"),
        //     but never down to an empty query (a title that IS just "S01E01" stays as-is).
        val beforeTails = s
        do { prev = s; s = s.trimEnd().replace(SEASON_EPISODE_TAIL, "").replace(TRAILING_LANG_TAG, "") } while (s != prev)
        if (s.isBlank()) s = beforeTails

        // 4. Collapse separators/whitespace and trim edge junk.
        s = s.replace('_', ' ').replace('.', ' ')
            .replace(MULTI_SPACE, " ")
            .replace(EDGE_JUNK, "")
            .trim()

        return Normalized(s, year)
    }

    /**
     * Strip one leading "PROVIDER - " prefix when it looks provider-ish. Only strips if the matched prefix
     * contains a digit or '+' (e.g. "4K-OSN+", "1080P VIP") or is a single token (e.g. "OSN", "EN") — so a
     * genuine upper-case multi-word title like "MAD MAX - Fury Road" is preserved.
     */
    /**
     * [query] without a trailing run of ALL-CAPS words, or null when there is nothing safe to strip.
     *
     * Panels append two different things to an item name, and both defeat a TMDB search because
     * TMDB matches on the title alone:
     *  - the category label — "EN - Brave (2012) PIXAR" lives in a "PIXAR MOVIES" category;
     *  - the star's name — "12 monkeys BRAD PITT", "21 Jump Street ICE CUBE".
     * Measured against the API, "Brave PIXAR" returns 0 results where "Brave" returns 553.
     *
     * The run is taken **whole**, however long it is. An earlier version capped it at three words and
     * that was a mistake: a cap does not decline to strip, it strips *part* of the cast list and
     * leaves the rest welded to the title. Over the reference catalog 1,178 names carry a longer run,
     * and truncating them produced "A Bronx Tale DE" from "A Bronx Tale ... DE NIRO, CHAZZ
     * PALMINTERI", "21 Grams SEAN PENN" and "Novecento DE" — each of them worse than not stripping at
     * all. Taking the run whole touches exactly the same 9,604 names and gets those three right.
     *
     * Two guards, both earned from that catalog:
     *  - **the head must contain a lower-case letter.** This is the one that matters. 12,177 titles
     *    in that catalog are written entirely in capitals, and without this guard
     *    "A WALK IN THE DARK" is stripped down to "A".
     *  - **no digits, no roman numerals, nothing from the format vocabulary**, so "Rocky II",
     *    "Transporter 2" and a bare "4K" are never mistaken for a name.
     *
     * A run is *not* allowed to cross a "&" or a comma to reach more capitals. That would rescue
     * "01-Road To Singapore BOB HOPE & BING CROSBY", but it also cuts "Simmer: THE JOB - ES GIBT NUR
     * EINE REGEL" down to "Simmer:", eating the real title. One clear regression is not worth one
     * clear win.
     *
     * Even guarded this over-reaches sometimes — "AK vs AK" loses its last word — so on its own the
     * result is only ever a *search retry*, never the title shown. A wrong strip costs one failed
     * lookup; a right one rescues a title that had no metadata at all. [displayTitle] does show it,
     * but only with TMDB's match corroborating the removal.
     */
    fun withoutTrailingTag(query: String): String? {
        val words = query.trim().split(WHITESPACE).filter { it.isNotBlank() }
        if (words.size < 2) return null
        val run = words.reversed()
            .takeWhile { w -> w.trimEnd(',').let { it.length >= 2 && it == it.uppercase() && it.none(Char::isDigit) && !ROMAN_NUMERAL.matches(it) } }
        if (run.isEmpty() || run.size >= words.size) return null
        val head = words.dropLast(run.size).joinToString(" ").trimEnd(' ', ',', '-')
        if (head.length < 2 || head.none { it.isLowerCase() }) return null
        return head
    }

    private val ROMAN_NUMERAL = Regex("""[IVXLCDM]+""")

    /**
     * The provider's name cleaned up for **display**, or the raw name when cleaning would ruin it.
     *
     * [normalize] exists to build a TMDB query, where over-trimming costs nothing — a shorter query
     * still finds the film. Shown to a user it has to be held to a higher bar, so this wraps it in a
     * guard: if what comes back is empty, or no longer starts with a letter or digit, the original
     * is shown instead.
     *
     * Measured over a 194,728-title catalog: cleaning changes 98.9% of names and the guard catches
     * the 0.23% it would otherwise mangle — "SD/CAM - Disclosure Day (2026) (CAM)" came out as
     * "/CAM - Disclosure Day" before the guard existed.
     */
    fun displayTitle(raw: String): String {
        val cleaned = normalize(raw).query
        val usable = cleaned.length >= 2 && cleaned.first().isLetterOrDigit()
        return if (usable) cleaned else raw.trim()
    }

    /**
     * [displayTitle] for a name whose TMDB match is known, which is what lets the trailing star name
     * go: "EN - 12 Monkeys 4K (1995) BRAD PITT" is shown as "12 Monkeys".
     *
     * [withoutTrailingTag] alone is too blunt for display — it also cuts "AK vs AK" down to "AK vs".
     * TMDB's own title settles it, but not by equality: a catalog and TMDB routinely spell the same
     * film differently ("12 Monkeys" is filed on TMDB as "Twelve Monkeys"), so comparing the kept
     * half would reject the very case this exists for. The words being *removed* are the reliable
     * signal instead — drop them only when TMDB's title uses none of them. "BRAD"/"PITT" appear
     * nowhere in "Twelve Monkeys", so they go; the "AK" in "AK vs AK" and the "K." in "Dossier K."
     * do appear, so those names are left exactly as the provider wrote them.
     *
     * With no match ([tmdbTitle] null) there is no evidence, and the provider's name stands.
     */
    fun displayTitle(raw: String, tmdbTitle: String?): String {
        val cleaned = displayTitle(raw)
        if (tmdbTitle.isNullOrBlank()) return cleaned
        val head = withoutTrailingTag(cleaned) ?: return cleaned
        val tmdbWords = tmdbTitle.split(WHITESPACE).mapTo(mutableSetOf()) { it.foldForCompare() }
        val dropped = cleaned.removePrefix(head).split(WHITESPACE).filter { it.isNotBlank() }
        return if (dropped.any { it.foldForCompare() in tmdbWords }) cleaned else head
    }

    /** Lower-cased and stripped of edge punctuation, so "K." and "K" compare equal. */
    private fun String.foldForCompare(): String = trim { !it.isLetterOrDigit() }.lowercase()

    /**
     * Whether a [qualityTags] entry is the kind of marker a box treats as a badge rather than as small
     * print. Only the premium resolutions qualify: "4K" on a cover is a logo, "WEB-DL" is a footnote,
     * and "HD" is on so much of the catalog that badging it would badge everything.
     */
    fun isHeadlineTag(tag: String): Boolean = tag in HEADLINE_TAGS

    private val HEADLINE_TAGS = setOf("8K", "4K", "UHD")

    /**
     * Quality and source markers pulled out of the provider's name, in display order.
     *
     * Panels encode these in the title because there is nowhere else to put them
     * ("4K - A Big Bold Beautiful Journey (2025)"), and [normalize] then throws them away. They are
     * real information about the file, so this returns them for the UI to show as chips beside the
     * rating: the title reads clean AND nothing is lost.
     *
     * Only the top of the resolution ladder survives — a name carrying both "4K" and "HD" shows 4K
     * alone — because listing every rung says nothing a viewer can act on. 5.9% of that same catalog
     * carries at least one marker, 4K being most of them.
     */
    fun qualityTags(raw: String): List<String> {
        val upper = raw.uppercase()
        val found = QUALITY_TAGS.filter { (_, pattern) -> pattern.containsMatchIn(upper) }.map { it.first }
        val ladderHit = RESOLUTION_LADDER.filter { it in found }
        // Keep the first ladder rung present (the highest), drop the rest; non-ladder tags all stay.
        val drop = ladderHit.drop(1).toSet()
        return found.filterNot { it in drop }
    }

    /** Resolution rungs, highest first — only the highest present one is shown. */
    private val RESOLUTION_LADDER = listOf("8K", "4K", "UHD", "FHD", "HD", "SD")

    /**
     * Marker -> pattern. Each is fenced by "not a letter or digit" rather than  so "CAM" cannot
     * match inside Camelot, Camp, Scam or camarade — 71 real hits in that catalog against thousands
     * of substring collisions.
     */
    private val QUALITY_TAGS: List<Pair<String, Regex>> = listOf(
        "8K" to "8K", "4K" to "4K", "UHD" to "UHD", "FHD" to "FHD",
        "HDR" to """HDR10\+?|HDR""", "Dolby Vision" to """DOLBY\s*VISION""", "IMAX" to "IMAX", "3D" to "3D",
        "CAM" to "SD/CAM|HD/CAM|HDCAM|CAMRIP|CAM|TS",
        "Blu-ray" to "BLU-?RAY|BDRIP|REMUX", "WEB-DL" to "WEB-?DL|WEBRIP",
        "HQ" to "HQ", "LQ" to "LQ", "SD" to "SD", "HD" to "HD",
        "Multi-sub" to "MULTI[- ]?SUBS?|MULTISUB", "Multi-audio" to "MULTI[- ]?AUDIO|DUAL[- ]?AUDIO",
    ).map { (tag, pattern) -> tag to Regex("(?<![A-Z0-9])(?:" + pattern + ")(?![A-Z0-9])") }

    private fun stripDashPrefix(s: String): String {
        val m = DASH_PREFIX.find(s) ?: return s
        val prefix = m.groupValues[1].trim()
        val provider = prefix.any { it.isDigit() || it == '+' } || !prefix.contains(' ')
        return if (provider) s.substring(m.value.length) else s
    }
}
