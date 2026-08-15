package tv.own.owntv.core.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataMatchScorerTest {

    private fun hit(
        title: String,
        original: String? = null,
        year: Int? = null,
        id: Int = 1,
    ) = MetadataSearchResult(
        tmdbId = id,
        type = MetadataType.MOVIE,
        title = title,
        originalTitle = original,
        year = year,
        overview = null,
        posterPath = null,
        popularity = 1.0,
    )

    @Test
    fun spanishCatalogTitleMatchesEnglishTmdbViaTopYearFallback() {
        // IPTV: "Dos canguros muy maduros (2009)" → TMDB English: Old Dogs
        val hits = listOf(
            hit("Old Dogs", original = "Old Dogs", year = 2009, id = 22949),
            hit("Some Other 2009 Film", year = 2009, id = 99),
        )
        val best = MetadataMatchScorer.pickBest("Dos canguros muy maduros", 2009, hits)
        assertNotNull(best)
        assertEquals(22949, best!!.result.tmdbId)
        assertTrue(best.score >= MetadataMatchScorer.ACCEPT_THRESHOLD)
    }

    @Test
    fun spanishTranslatedTmdbTitleScoresDirectly() {
        val hits = listOf(
            hit("Dos canguros muy maduros", original = "Old Dogs", year = 2009, id = 22949),
        )
        val best = MetadataMatchScorer.pickBest("Dos canguros muy maduros", 2009, hits)
        assertNotNull(best)
        assertEquals(22949, best!!.result.tmdbId)
        // Exact token match (+ year) should be high confidence.
        assertTrue(best.score >= 0.9)
    }

    @Test
    fun topFallbackRequiresYearWhenProviderYearPresent() {
        val hits = listOf(hit("Old Dogs", year = 2011, id = 22949))
        val best = MetadataMatchScorer.pickBest("Dos canguros muy maduros", 2009, hits)
        assertNull(best)
    }

    @Test
    fun englishExactMatchStillWorks() {
        val hits = listOf(hit("Old Dogs", original = "Old Dogs", year = 2009, id = 22949))
        val best = MetadataMatchScorer.pickBest("Old Dogs", 2009, hits)
        assertNotNull(best)
        assertEquals(22949, best!!.result.tmdbId)
        assertTrue(best.score >= 0.9)
    }
}
