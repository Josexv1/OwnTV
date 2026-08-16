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
    fun withoutTrailingTagStripsProviderCategoryLabels() {
        // "EN - Brave (2012) PIXAR" normalizes to "Brave PIXAR", which TMDB answers with zero
        // results while "Brave" returns hundreds — the whole category resolved to no metadata.
        assertEquals("Brave", TitleNormalizer.withoutTrailingTag("Brave PIXAR"))
        assertEquals("The Crime", TitleNormalizer.withoutTrailingTag("The Crime NF"))

        // A lower-case trailing word is ordinary title text and must survive.
        assertEquals(null, TitleNormalizer.withoutTrailingTag("Saving Grace"))

        // An all-caps title with nothing left over must not be stripped away.
        assertEquals(null, TitleNormalizer.withoutTrailingTag("UP"))
        assertEquals(null, TitleNormalizer.withoutTrailingTag("WALL-E"))

        // Digits mean a year or part number, which existing handling already covers.
        assertEquals(null, TitleNormalizer.withoutTrailingTag("Rocky IV2"))
    }
}
