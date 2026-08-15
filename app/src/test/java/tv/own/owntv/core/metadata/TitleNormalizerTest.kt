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
        // Streaming-service short tags: "D+ - …", "A+ - …"
        assertEquals("Avengers 3 Infinity War", q("D+ - Avengers 3 Infinity War (2018)"))
        assertEquals(2018, y("D+ - Avengers 3 Infinity War (2018)"))
        // Common IPTV pattern: "[lang] - [title] [quality] (year)"
        assertEquals("Enola Holmes 1", q("NF - Enola Holmes 1 4K (2020)"))
        assertEquals(2020, y("NF - Enola Holmes 1 4K (2020)"))
        assertEquals("The Batman", q("EN - The Batman 8K (2022)"))
        assertEquals("Something", q("AR - Something HQ (2019)"))
        assertEquals("Other Title", q("ES - Other Title LQ (2018)"))
    }

    @Test
    fun searchVariantsDropFranchisePartNumbers() {
        val variants = TitleNormalizer.searchVariants("Avengers 3 Infinity War")
        assertEquals(true, variants.contains("Avengers 3 Infinity War"))
        assertEquals(true, variants.contains("Avengers Infinity War"))
        // Trailing sequel numbers: first film is often just "Enola Holmes" on TMDB.
        val enola = TitleNormalizer.searchVariants("Enola Holmes 1")
        assertEquals(true, enola.contains("Enola Holmes 1"))
        assertEquals(true, enola.contains("Enola Holmes"))
    }

    @Test
    fun matchTokensStripPunctuation() {
        assertEquals(listOf("avengers", "infinity", "war"), TitleNormalizer.matchTokens("Avengers: Infinity War"))
        assertEquals(listOf("avengers", "3", "infinity", "war"), TitleNormalizer.matchTokens("Avengers 3 Infinity War"))
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
        assertEquals("2012", q("2012"))
    }

    @Test
    fun preservesYearNumberMovieTitles() {
        // IPTV: language prefix + year-number film + release year in parens.
        val n = TitleNormalizer.normalize("ES - 1917 (2019)")
        assertEquals("1917", n.query)
        assertEquals(2019, n.year)
        assertEquals("1917", q("1917 (2019)"))
        assertEquals(2019, y("1917 (2019)"))
        assertEquals("1917", q("EN| 1917 4K (2019)"))
    }

    @Test
    fun seasonTailNeverEmptiesTheQuery() {
        assertEquals("S01E01", q("S01E01"))
    }
}
