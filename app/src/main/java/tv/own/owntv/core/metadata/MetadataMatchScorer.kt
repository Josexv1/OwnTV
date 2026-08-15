package tv.own.owntv.core.metadata

import kotlin.math.abs
import kotlin.math.max

/**
 * Pure title/year confidence scoring for TMDB search hits.
 *
 * IPTV catalogs often carry localized display names ("Dos canguros muy maduros") while TMDB's default
 * `title`/`original_title` stay English ("Old Dogs"). Token overlap alone then scores ~0 even when
 * TMDB search already resolved the translation — [score] + [pickBest] handle both cases.
 */
internal object MetadataMatchScorer {

    /** Accept a match at/above this confidence; below it, prefer no metadata over a wrong one. */
    const val ACCEPT_THRESHOLD = 0.58

    /** High-confidence enough to stop trying further query variants. */
    const val EARLY_ACCEPT = 0.86

    /**
     * Confidence assigned when token overlap is weak but TMDB ranked a hit #1 for our exact query and
     * the year agrees. Multilingual catalog titles rely on this path.
     */
    const val TMDB_TOP_FALLBACK_SCORE = 0.72

    data class Scored(val result: MetadataSearchResult, val score: Double)

    fun pickBest(query: String, year: Int?, hits: List<MetadataSearchResult>): Scored? {
        if (hits.isEmpty()) return null
        val byTokens = hits.asSequence()
            .map { Scored(it, score(query, year, it)) }
            .filter { it.score >= ACCEPT_THRESHOLD }
            .maxByOrNull { it.score }
        if (byTokens != null) return byTokens
        return pickTmdbTopFallback(query, year, hits)
    }

    /**
     * When localized IPTV names share no tokens with English TMDB titles, trust TMDB's own ranking:
     * top hit + exact year + enough query substance.
     */
    fun pickTmdbTopFallback(query: String, year: Int?, hits: List<MetadataSearchResult>): Scored? {
        val top = hits.firstOrNull() ?: return null
        if (significantTokens(query).size < 2) return null
        if (year != null) {
            val hy = top.year ?: return null
            if (hy != year) return null
        }
        // If tokens already score something non-zero but below threshold, still allow the fallback
        // only when year locks the match (above). Zero-overlap localized titles are the main case.
        return Scored(top, TMDB_TOP_FALLBACK_SCORE)
    }

    fun score(query: String, year: Int?, r: MetadataSearchResult): Double {
        var s = max(
            titleSimilarity(query, r.title),
            r.originalTitle?.let { titleSimilarity(query, it) } ?: 0.0,
        )
        if (year != null && r.year != null) {
            val diff = abs(year - r.year)
            s += when {
                diff == 0 -> 0.15
                diff == 1 -> 0.05
                else -> -0.35
            }
        }
        return s.coerceIn(0.0, 1.0)
    }

    fun titleSimilarity(query: String, candidate: String): Double {
        val qTokens = significantTokens(query)
        val tTokens = significantTokens(candidate)
        if (qTokens.isEmpty() || tTokens.isEmpty()) return 0.0
        if (qTokens == tTokens) return 1.0

        val qSet = qTokens.toSet()
        val tSet = tTokens.toSet()
        if (qSet.containsAll(tSet) || tSet.containsAll(qSet)) return 0.88

        val inter = qSet.intersect(tSet).size.toDouble()
        if (inter == 0.0) return 0.0
        val jaccard = inter / (qSet.size + tSet.size - inter)
        val coverage = inter / minOf(qSet.size, tSet.size).toDouble()
        return max(jaccard, coverage * 0.85)
    }

    fun significantTokens(title: String): List<String> =
        TitleNormalizer.matchTokens(title).filter { token ->
            if (token.length <= 2 && token.all { it.isDigit() }) return@filter false
            token !in MATCH_STOPWORDS
        }

    /**
     * English + common Romance/Germanic function words. Dropping these keeps "Dos canguros muy maduros"
     * focused on content tokens when the localized TMDB title is available.
     */
    private val MATCH_STOPWORDS = setOf(
        // English
        "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "with",
        // Spanish
        "el", "la", "los", "las", "un", "una", "unos", "unas", "del", "al", "y", "o", "de", "en",
        "con", "por", "para", "muy", "mas", "más", "que", "su", "sus", "lo", "le", "les", "se",
        "es", "al", "del",
        // Portuguese
        "os", "as", "um", "uma", "uns", "umas", "do", "da", "dos", "das", "no", "na", "nos", "nas",
        "em", "com", "por", "para", "ao", "à", "às",
        // French
        "le", "la", "les", "un", "une", "des", "du", "de", "et", "ou", "en", "au", "aux", "avec",
        // Italian
        "il", "lo", "la", "i", "gli", "le", "un", "uno", "una", "del", "della", "dei", "delle",
        "di", "e", "o", "con", "per",
        // German
        "der", "die", "das", "ein", "eine", "und", "oder", "von", "im", "den", "dem", "des",
    )
}
