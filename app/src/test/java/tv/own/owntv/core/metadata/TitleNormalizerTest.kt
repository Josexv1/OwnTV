package tv.own.owntv.core.metadata

import org.junit.Assert.assertEquals
import org.junit.Test

class TitleNormalizerTest {

    private fun q(raw: String) = TitleNormalizer.normalize(raw).query
    private fun y(raw: String) = TitleNormalizer.normalize(raw).year

    @Test
    fun stripsProviderTagsBracketsAndYear() {
        val n = TitleNormalizer.normalize("EN| The Movie Name (2021) [HD] (MULTI-SUB)")
        assertEquals("The Movie Name", n.query)
        assertEquals(2021, n.year)
    }

    @Test
    fun stripsStackedPrefixesAndQualityMarkers() {
        assertEquals("Gangs of London", q("4K-OSN+ - Gangs of London"))
        assertEquals("The Batman", q("EN| 4K| The Batman WEB-DL x265"))
    }

    @Test
    fun stripsAudioAndReleaseNoise() {
        assertEquals("Lupin", q("Lupin VOSTFR"))
        assertEquals("Intouchables", q("Intouchables TRUEFRENCH HDLight"))
        assertEquals("The Raid", q("The Raid DUBBED AC3 5.1"))
        assertEquals("Old Movie", q("Old Movie HDCAM"))
    }

    @Test
    fun stripsTrailingSeasonEpisodeTails() {
        assertEquals("Breaking Bad", q("Breaking Bad S05"))
        assertEquals("Loki", q("Loki Season 2"))
        assertEquals("Dark", q("Dark Staffel 1"))
        assertEquals("La Casa de Papel", q("La Casa de Papel Temporada 3"))
        assertEquals("The Office", q("The Office S02E04"))
    }

    @Test
    fun stripsTrailingUppercaseLanguageTags() {
        assertEquals("The Crown", q("The Crown FR"))
        assertEquals("Peaky Blinders", q("Peaky Blinders S01 LAT"))
    }

    @Test
    fun preservesRealTitles() {
        assertEquals("MAD MAX - Fury Road", q("MAD MAX - Fury Road"))
        assertEquals("Ocean's 8", q("Ocean's 8"))
        assertEquals("Se7en", q("Se7en"))
        assertEquals("Mission: Impossible", q("Mission: Impossible"))
        // Title-case words that look like tags stay intact.
        assertEquals("Sub Rosa", q("Sub Rosa"))
        // A pure year title keeps its year but never empties the query.
        assertEquals(2012, y("2012"))
    }

    @Test
    fun seasonTailNeverEmptiesTheQuery() {
        assertEquals("S01E01", q("S01E01"))
    }

    @Test
    fun withoutTrailingTagStripsCategoryLabelsAndStarNames() {
        // Category label appended by the panel: TMDB answers "Brave PIXAR" with zero results.
        assertEquals("Brave", TitleNormalizer.withoutTrailingTag("Brave PIXAR"))
        // The star's name, one or two words.
        assertEquals("12 monkeys", TitleNormalizer.withoutTrailingTag("12 monkeys BRAD PITT"))
        assertEquals("21 Jump Street", TitleNormalizer.withoutTrailingTag("21 Jump Street ICE CUBE"))

        // The load-bearing guard: a title written entirely in capitals is left alone. Without it
        // "A WALK IN THE DARK" is stripped to "A" — 12,177 such titles in the reference catalog.
        assertEquals(null, TitleNormalizer.withoutTrailingTag("A WALK IN THE DARK"))
        assertEquals(null, TitleNormalizer.withoutTrailingTag("MAD MAX"))

        // A lower-case trailing word is ordinary title text.
        assertEquals(null, TitleNormalizer.withoutTrailingTag("Saving Grace"))
        // Nothing would be left over.
        assertEquals(null, TitleNormalizer.withoutTrailingTag("UP"))
        // Roman numerals and digits are sequel markers, not tags.
        assertEquals(null, TitleNormalizer.withoutTrailingTag("Rocky II"))
        assertEquals(null, TitleNormalizer.withoutTrailingTag("Transporter 2"))
    }

    @Test
    fun withoutTrailingTagTakesTheWholeCastListNotJustPartOfIt() {
        // A cap on the run length does not decline to strip, it strips part of the cast list and
        // welds the rest to the title: at three words these came out as "A Bronx Tale DE",
        // "21 Grams SEAN PENN" and "Novecento DE".
        assertEquals("A Bronx Tale", TitleNormalizer.withoutTrailingTag("A Bronx Tale DE NIRO, CHAZZ PALMINTERI"))
        assertEquals("21 Grams", TitleNormalizer.withoutTrailingTag("21 Grams SEAN PENN, BENICIO DEL TORO"))
        assertEquals("Novecento", TitleNormalizer.withoutTrailingTag("Novecento DE NIRO, BERNARDO BERTOLUCCI"))

        // Taking the run whole must not weaken the all-caps guard: there is no head left to keep.
        assertEquals(null, TitleNormalizer.withoutTrailingTag("A WALK IN THE DARK"))
        // Nor may a run swallow the entire name.
        assertEquals(null, TitleNormalizer.withoutTrailingTag("THE THREE STOOGES COLLECTION"))
    }

    @Test
    fun displayTitleDropsAStarNameTheTmdbMatchDisproves() {
        // The case this exists for. Note TMDB files the film as "Twelve Monkeys" while the catalog
        // writes "12 Monkeys": comparing the kept half would reject it, so only the removed words
        // are tested — neither "BRAD" nor "PITT" appears in TMDB's title.
        assertEquals(
            "12 Monkeys",
            TitleNormalizer.displayTitle("EN - 12 Monkeys 4K (1995) BRAD PITT", "Twelve Monkeys"),
        )
        assertEquals(
            "21 Jump Street",
            TitleNormalizer.displayTitle("EN - 21 Jump Street (2012) ICE CUBE", "21 Jump Street"),
        )
        // A full cast list goes the same way — no part of it may survive as "A Bronx Tale DE".
        assertEquals(
            "A Bronx Tale",
            TitleNormalizer.displayTitle("EN - A Bronx Tale 4K (1993) DE NIRO, CHAZZ PALMINTERI", "A Bronx Tale"),
        )

        // A trailing capital that really is part of the title appears in TMDB's title too, so it stays.
        assertEquals("AK vs AK", TitleNormalizer.displayTitle("AK vs AK", "AK vs AK"))
        // A single letter never reads as a star name, so the length guard keeps it before TMDB is
        // consulted at all. (The trailing period is dropped by normalize(), as it always has been.)
        assertEquals("Dossier K", TitleNormalizer.displayTitle("Dossier K.", "Dossier K."))

        // No match, no evidence: the provider's name stands, star name and all.
        assertEquals("12 Monkeys BRAD PITT", TitleNormalizer.displayTitle("EN - 12 Monkeys 4K (1995) BRAD PITT", null))

        // A localized name TMDB spells differently is not a star name — nothing to remove, nothing lost.
        assertEquals("17 otra vez", TitleNormalizer.displayTitle("ES - 17 otra vez (2009)", "17 Again"))
    }

}
