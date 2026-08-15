package tv.own.owntv.core.metadata

import android.util.Log
import org.json.JSONArray
import tv.own.owntv.core.database.dao.MetadataDao
import tv.own.owntv.core.database.entity.MetadataCacheEntity
import tv.own.owntv.core.database.entity.MetadataMatchEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.features.settings.data.SettingsRepository

/**
 * On-demand TMDB enrichment orchestrator (plan §3, §7). Resolves a local content item → TMDB metadata,
 * caching both the resolution (match table) and the metadata (cache table) so a second view is instant
 * and offline. NEVER bulk — callers invoke this lazily when a detail screen opens.
 *
 * Merge rule (§7.1) is applied by the UI at render time (`providerField ?: tmdbField`); this layer only
 * fetches and caches TMDB fields, never mutating the provider content tables.
 */
class MetadataRepository(
    private val provider: MetadataProvider,
    private val dao: MetadataDao,
    private val settings: SettingsRepository,
    private val overrideStore: MetadataOverrideStore,
) {
    /** Guards [healNegativeMatchesOnce] so the DataStore read happens once per process, not per resolve. */
    private val healNeeded = java.util.concurrent.atomic.AtomicBoolean(true)

    /**
     * Resolve TMDB metadata for a movie. Returns the cached row (fresh or freshly fetched), or null when
     * enrichment is off, no confident match exists, or the network failed. Cheap on repeat calls.
     */
    suspend fun resolveMovie(movie: MovieEntity): MetadataCacheEntity? {
        if (!settings.metadataConfig().enabled) return null
        healNegativeMatchesOnce()

        val localKey = movieLocalKey(movie)
        val now = System.currentTimeMillis()

        // 1. Consult the local→tmdb mapping (incl. negative cache) before hitting the network.
        dao.getMatch(localKey)?.let { match ->
            val ttl = if (match.tmdbId == null) NEGATIVE_TTL_MS else POSITIVE_TTL_MS
            if (now - match.updatedAt < ttl) {
                val tmdbId = match.tmdbId ?: return null // fresh negative cache
                dao.getCache(cacheKey(tmdbId))?.let { return it } // fresh positive cache
                // Match known but cache row missing/evicted → re-fetch details below.
                return fetchAndCache(tmdbId, localKey, match.confidence)
            }
        }

        // 2. Build the search query: a user override (plan §11.2 U5b) wins over the auto-normalizer.
        val q = resolveQuery(localKey, movie.name, movie.year)
        if (q.query.isBlank()) return null

        // An override is the user telling us the exact name → trust TMDB's top relevance hit directly
        // (no fuzzy threshold) so a hand-typed title isn't rejected over punctuation/formatting differences.
        val best: Scored? = if (q.isOverride) {
            val hits = searchMovieHits(q.query, q.year) ?: return null
            hits.firstOrNull()?.let { Scored(it, 1.0) }
        } else {
            findBestMovieMatch(q.query, q.year)
        }
        if (best == null) {
            // Negative cache: remember "searched, no confident match" so we don't re-hammer on scroll.
            // Only write this after every query variant was tried (or TMDB answered empty).
            dao.upsertMatch(MetadataMatchEntity(localKey, TYPE_MOVIE, tmdbId = null, confidence = 0.0, updatedAt = now))
            return null
        }

        dao.upsertMatch(MetadataMatchEntity(localKey, TYPE_MOVIE, tmdbId = best.result.tmdbId, confidence = best.score, updatedAt = now))
        return fetchAndCache(best.result.tmdbId, localKey, best.score, fallback = best.result)
    }

    /**
     * Resolve TMDB metadata for a series (show-level). Same lazy resolve + cache + negative-cache as
     * [resolveMovie], but against TMDB's TV endpoints. Cache/match keyed with the "tv" type.
     */
    suspend fun resolveSeries(series: tv.own.owntv.core.database.entity.SeriesEntity): MetadataCacheEntity? {
        if (!settings.metadataConfig().enabled) return null
        healNegativeMatchesOnce()

        val localKey = seriesLocalKey(series)
        val now = System.currentTimeMillis()

        dao.getMatch(localKey)?.let { match ->
            val ttl = if (match.tmdbId == null) NEGATIVE_TTL_MS else POSITIVE_TTL_MS
            if (now - match.updatedAt < ttl) {
                val tmdbId = match.tmdbId ?: return null
                dao.getCache(tvCacheKey(tmdbId))?.let { return it }
                return fetchAndCacheTv(tmdbId, null)
            }
        }

        val q = resolveQuery(localKey, series.name, series.year)
        if (q.query.isBlank()) return null

        val best: Scored? = if (q.isOverride) {
            val hits = searchTvHits(q.query, q.year) ?: return null
            hits.firstOrNull()?.let { Scored(it, 1.0) }
        } else {
            findBestTvMatch(q.query, q.year)
        }
        if (best == null) {
            dao.upsertMatch(MetadataMatchEntity(localKey, TYPE_TV, tmdbId = null, confidence = 0.0, updatedAt = now))
            return null
        }
        dao.upsertMatch(MetadataMatchEntity(localKey, TYPE_TV, tmdbId = best.result.tmdbId, confidence = best.score, updatedAt = now))
        return fetchAndCacheTv(best.result.tmdbId, best.result)
    }

    private suspend fun fetchAndCacheTv(tmdbId: Int, fallback: MetadataSearchResult?): MetadataCacheEntity? {
        val now = System.currentTimeMillis()
        val details = provider.tvDetails(tmdbId)
        val entity = when {
            details != null -> MetadataCacheEntity(
                key = tvCacheKey(tmdbId), tmdbId = tmdbId, imdbId = details.imdbId, type = TYPE_TV,
                title = details.title, year = details.year ?: fallback?.year,
                overview = details.overview ?: fallback?.overview,
                posterPath = details.posterPath ?: fallback?.posterPath,
                backdropPath = details.backdropPath, rating = details.rating,
                genresJson = details.genres.takeIf { it.isNotEmpty() }?.let { JSONArray(it).toString() },
                castJson = details.cast.takeIf { it.isNotEmpty() }?.let { JSONArray(it).toString() },
                trailerKey = details.trailerKey,
                logoPath = details.logoPath,
                updatedAt = now,
            )
            fallback != null -> MetadataCacheEntity(
                key = tvCacheKey(tmdbId), tmdbId = tmdbId, imdbId = null, type = TYPE_TV,
                title = fallback.title, year = fallback.year, overview = fallback.overview,
                posterPath = fallback.posterPath, backdropPath = null, rating = null,
                genresJson = null, castJson = null, trailerKey = null, logoPath = null, updatedAt = now,
            )
            else -> return dao.getCache(tvCacheKey(tmdbId))
        }
        dao.upsertCache(entity)
        return entity
    }

    /**
     * Resolve per-episode TMDB metadata (still, plot, air date, rating). First resolves the show (cached)
     * to get its TMDB id, then fetches the episode lazily and caches it under `tv:<id>:s<n>e<m>`. Returns
     * null when enrichment is off, the show has no match, or that episode isn't on TMDB.
     */
    suspend fun resolveEpisode(
        series: tv.own.owntv.core.database.entity.SeriesEntity,
        episode: tv.own.owntv.core.database.entity.EpisodeEntity,
    ): MetadataCacheEntity? {
        if (!settings.metadataConfig().enabled) return null
        val show = resolveSeries(series) ?: return null // no confident show match → no episode lookup
        val tvId = show.tmdbId
        val season = episode.seasonNumber
        val ep = episode.episodeNumber
        val key = episodeCacheKey(tvId, season, ep)
        val now = System.currentTimeMillis()

        dao.getCache(key)?.let { if (now - it.updatedAt < POSITIVE_TTL_MS) return it }

        val d = provider.tvEpisodeDetails(tvId, season, ep) ?: return dao.getCache(key)
        val entity = MetadataCacheEntity(
            key = key, tmdbId = tvId, imdbId = null, type = TYPE_EPISODE,
            title = d.name?.takeIf { it.isNotBlank() } ?: episode.name,
            year = d.airDate?.take(4)?.toIntOrNull(),
            overview = d.overview,
            posterPath = d.stillPath, // 16:9 still, rendered via MetadataImages.backdrop sizing
            backdropPath = d.stillPath,
            rating = d.rating,
            genresJson = null, castJson = null, trailerKey = null, updatedAt = now,
            logoPath = null,
        )
        dao.upsertCache(entity)
        return entity
    }

    /**
     * Drop every cached TMDB detail row so the next resolve re-fetches. Used when the metadata language
     * changes: cached rows hold language-specific text (overview, genres, title) but the cache key is only
     * `<type>:<tmdbId>`, so without this users would keep seeing the old language until the 60-day TTL.
     *
     * Deliberately leaves POSITIVE `metadata_match` rows intact — a title→tmdbId match is
     * language-independent, and keeping it avoids re-running a search for every item in a ~220k catalog.
     * Negative rows do go: a miss can be an artefact of the language the search ran under, and leaving it
     * meant a bad language choice kept metadata (and the OpenSubtitles tmdb_id lookup) dead for 7 days
     * even after the user switched back.
     */
    /**
     * One-shot drop of the "no match" rows written by an older matcher generation. Installs that ran
     * with a non-English metadata language cached a miss for every title they opened (the search hit's
     * title came back translated and scored ~0), and those rows outlive both the language change and the
     * app upgrade — so without this the fix wouldn't reach the affected users for 7 days.
     *
     * Deliberately lazy: it rides the first detail-screen resolve, never cold start, and only the cheap
     * negative rows go. Failures are swallowed and simply re-tried on the next resolve.
     */
    private suspend fun healNegativeMatchesOnce() {
        if (!healNeeded.get()) return
        runCatching {
            if (settings.metadataMatchHealVersion() < MATCH_HEURISTICS_VERSION) {
                dao.clearNegativeMatches()
                settings.setMetadataMatchHealVersion(MATCH_HEURISTICS_VERSION)
            }
        }.onSuccess { healNeeded.set(false) }
            .onFailure { Log.w(TAG, "negative-match heal failed: ${it.message}") }
    }

    suspend fun clearCacheForLanguageChange() {
        dao.clearCache()
        dao.clearNegativeMatches()
    }

    /**
     * Related TMDB movies for the cinematic "Similar" rail. Returns empty when enrichment is off, the
     * movie has no confident match, or TMDB has nothing useful. Transport failures also return empty so
     * the UI simply hides the row.
     */
    suspend fun relatedMovies(movie: MovieEntity, limit: Int = 16): List<MetadataSearchResult> {
        if (!settings.metadataConfig().enabled) return emptyList()
        val match = dao.getMatch(movieLocalKey(movie))?.tmdbId
            ?: resolveMovie(movie)?.tmdbId
            ?: return emptyList()
        val hits = runCatching { provider.relatedMovies(match) }
            .onFailure { Log.w(TAG, "relatedMovies failed: ${it.message}") }
            .getOrNull()
            ?: return emptyList()
        return hits.filter { it.tmdbId != match }.take(limit)
    }

    /**
     * Reverse-map already-resolved local movies for a set of TMDB ids. Used so the similar rail can
     * open playable catalog titles when the user has already browsed/matched them.
     */
    suspend fun localKeysForTmdbIds(tmdbIds: List<Int>): Map<Int, String> {
        if (tmdbIds.isEmpty()) return emptyMap()
        return dao.matchesForTmdbIds(tmdbIds, TYPE_MOVIE)
            .mapNotNull { m -> m.tmdbId?.let { id -> id to m.localKey } }
            .toMap()
    }

    /**
     * Clear a movie's TMDB match (negative OR positive) and its cached details so the next [resolveMovie]
     * re-searches from scratch (plan §11.2 U5a — manual "Refetch TMDB details"). Does NOT resolve; the caller
     * re-triggers [resolveMovie] afterwards.
     */
    suspend fun clearMovie(movie: MovieEntity) {
        val localKey = movieLocalKey(movie)
        dao.getMatch(localKey)?.tmdbId?.let { dao.deleteCache(cacheKey(it)) }
        dao.deleteMatch(localKey)
    }

    /**
     * Clear a series' match + cached show details (plan §11.2 U5a). Per-episode cache rows for the old tmdbId
     * are left in place — they're orphaned but harmless (episode resolve looks them up by tmdbId, so stale
     * rows under an old id are simply never read). Caller re-triggers [resolveSeries].
     */
    suspend fun clearSeries(series: tv.own.owntv.core.database.entity.SeriesEntity) {
        val localKey = seriesLocalKey(series)
        dao.getMatch(localKey)?.tmdbId?.let { dao.deleteCache(tvCacheKey(it)) }
        dao.deleteMatch(localKey)
    }

    /**
     * Clear an episode's cache AND its show's match + show cache (plan §11.2 U5a). Episodes inherit the show's
     * match, so an episode whose show was negative-cached can only recover by clearing the show match too.
     * Caller re-triggers [resolveEpisode].
     */
    suspend fun clearEpisode(
        series: tv.own.owntv.core.database.entity.SeriesEntity,
        episode: tv.own.owntv.core.database.entity.EpisodeEntity,
    ) {
        val localKey = seriesLocalKey(series)
        dao.getMatch(localKey)?.tmdbId?.let { tid ->
            dao.deleteCache(tvCacheKey(tid)) // show details
            dao.deleteCache(episodeCacheKey(tid, episode.seasonNumber, episode.episodeNumber)) // this episode
        }
        dao.deleteMatch(localKey) // show match (negative OR positive)
    }

    // --- TMDB name overrides (plan §11.2 U5b) ---
    // Stored in DataStore (no Room schema change) and keyed by the same stable local key as matching, so
    // they survive re-sync. Setting/clearing also drops the cached match+details so the next resolve
    // re-searches under the new query (caller bumps the meta-refresh tick to trigger it).

    /** The saved override for this movie, if any (used to prefill the dialog). */
    suspend fun movieOverride(movie: MovieEntity): TmdbOverride? = overrideStore.get(movieLocalKey(movie))

    /** The saved override for this series, if any. */
    suspend fun seriesOverride(series: tv.own.owntv.core.database.entity.SeriesEntity): TmdbOverride? =
        overrideStore.get(seriesLocalKey(series))

    /** Save a movie's override and drop its cached match so the next resolve uses the new query. */
    suspend fun setMovieOverride(movie: MovieEntity, title: String, year: Int?) {
        overrideStore.set(movieLocalKey(movie), title, year)
        clearMovie(movie)
    }

    /** Save a series' override and drop its cached match so the next resolve uses the new query. */
    suspend fun setSeriesOverride(series: tv.own.owntv.core.database.entity.SeriesEntity, title: String, year: Int?) {
        overrideStore.set(seriesLocalKey(series), title, year)
        clearSeries(series)
    }

    /** Remove a movie's override and drop its cached match so the next resolve re-normalizes the provider title. */
    suspend fun clearMovieOverride(movie: MovieEntity) {
        overrideStore.clear(movieLocalKey(movie))
        clearMovie(movie)
    }

    /** Remove a series' override and drop its cached match so the next resolve re-normalizes the provider title. */
    suspend fun clearSeriesOverride(series: tv.own.owntv.core.database.entity.SeriesEntity) {
        overrideStore.clear(seriesLocalKey(series))
        clearSeries(series)
    }

    /**
     * Build the TMDB search query + year for [localKey]: a user override (§11.2 U5b) wins over the
     * auto-normalized provider title. [ResolvedQuery.isOverride] lets the caller bypass the fuzzy
     * threshold and trust TMDB's top relevance hit when the user hand-typed the name.
     */
    private suspend fun resolveQuery(localKey: String, rawName: String, providerYear: Int?): ResolvedQuery {
        overrideStore.get(localKey)?.let { return ResolvedQuery(it.title, it.year ?: providerYear, isOverride = true) }
        val norm = TitleNormalizer.normalize(rawName)
        return ResolvedQuery(norm.query, providerYear ?: norm.year, isOverride = false)
    }

    private data class ResolvedQuery(val query: String, val year: Int?, val isOverride: Boolean)

    /** Fetch full details for [tmdbId] and cache them; falls back to the search hit if details fail. */
    private suspend fun fetchAndCache(
        tmdbId: Int,
        localKey: String,
        confidence: Double,
        fallback: MetadataSearchResult? = null,
    ): MetadataCacheEntity? {
        val now = System.currentTimeMillis()
        val details = provider.movieDetails(tmdbId)
        val entity = when {
            details != null -> MetadataCacheEntity(
                key = cacheKey(tmdbId),
                tmdbId = tmdbId,
                imdbId = details.imdbId,
                type = TYPE_MOVIE,
                title = details.title,
                year = details.year ?: fallback?.year,
                overview = details.overview ?: fallback?.overview,
                posterPath = details.posterPath ?: fallback?.posterPath,
                backdropPath = details.backdropPath,
                rating = details.rating,
                genresJson = details.genres.takeIf { it.isNotEmpty() }?.let { JSONArray(it).toString() },
                castJson = details.cast.takeIf { it.isNotEmpty() }?.let { JSONArray(it).toString() },
                trailerKey = details.trailerKey,
                logoPath = details.logoPath,
                updatedAt = now,
            )
            fallback != null -> MetadataCacheEntity(
                key = cacheKey(tmdbId), tmdbId = tmdbId, imdbId = null, type = TYPE_MOVIE,
                title = fallback.title, year = fallback.year, overview = fallback.overview,
                posterPath = fallback.posterPath, backdropPath = null, rating = null,
                genresJson = null, castJson = null, trailerKey = null, logoPath = null, updatedAt = now,
            )
            else -> return dao.getCache(cacheKey(tmdbId)) // nothing to write; return existing if any
        }
        dao.upsertCache(entity)
        return entity
    }

    /**
     * Try cleaned-title variants (+ optional year-less retry) until a confident TMDB movie hit lands.
     * null = every attempt was a transport failure (caller must NOT negative-cache).
     * empty-best = TMDB answered but nothing passed the threshold (caller MAY negative-cache).
     */
    private suspend fun findBestMovieMatch(query: String, year: Int?): Scored? {
        var anyAnswer = false
        var best: Scored? = null
        val variants = TitleNormalizer.searchVariants(query)
        // Year first (tighter), then year-less — some IPTV years are wrong/off-by-one.
        val years = listOfNotNull(year, null).distinct()
        for (variant in variants) {
            for (y in years) {
                val hits = searchMovieHits(variant, y) ?: continue
                anyAnswer = true
                val scored = pickBest(query, year ?: y, hits) ?: continue
                if (best == null || scored.score > best.score) best = scored
                if (scored.score >= EARLY_ACCEPT) return scored
            }
        }
        // anyAnswer=false means every call failed transport → treat like null search (no negative cache)
        if (!anyAnswer) return null
        return best
    }

    private suspend fun findBestTvMatch(query: String, year: Int?): Scored? {
        var anyAnswer = false
        var best: Scored? = null
        val variants = TitleNormalizer.searchVariants(query)
        val years = listOfNotNull(year, null).distinct()
        for (variant in variants) {
            for (y in years) {
                val hits = searchTvHits(variant, y) ?: continue
                anyAnswer = true
                val scored = pickBest(query, year ?: y, hits) ?: continue
                if (best == null || scored.score > best.score) best = scored
                if (scored.score >= EARLY_ACCEPT) return scored
            }
        }
        if (!anyAnswer) return null
        return best
    }

    /** null = transport failure; empty list = TMDB answered with no results. */
    private suspend fun searchMovieHits(query: String, year: Int?): List<MetadataSearchResult>? =
        runCatching { provider.searchMovie(query, year) }
            .onFailure { Log.w(TAG, "resolveMovie search failed: ${it.message}") }
            .getOrNull()

    private suspend fun searchTvHits(query: String, year: Int?): List<MetadataSearchResult>? =
        runCatching { provider.searchTv(query, year) }
            .onFailure { Log.w(TAG, "resolveSeries search failed: ${it.message}") }
            .getOrNull()

    /** Best confident match, or null (plan §12: "no art beats wrong art"). */
    private fun pickBest(query: String, year: Int?, hits: List<MetadataSearchResult>): Scored? {
        if (hits.isEmpty()) return null
        return hits.asSequence()
            .map { Scored(it, score(query, year, it)) }
            .filter { it.score >= ACCEPT_THRESHOLD }
            .maxByOrNull { it.score }
    }

    private data class Scored(val result: MetadataSearchResult, val score: Double)

    /**
     * 0..1 confidence from title similarity + year agreement.
     *
     * Similarity takes the BEST of the localized and the original title. TMDB translates `title`/`name`
     * when `&language=` is set, so a user on e.g. Greek metadata got Greek titles scored against Latin
     * provider names — zero overlap, every match rejected, and the negative cache then hid metadata AND
     * broke the OpenSubtitles tmdb_id lookup for 7 days. `original_title` is language-independent.
     *
     * Tokens are punctuation-stripped ("Avengers:" → "avengers") and franchise part-numbers are ignored
     * for overlap ("Avengers 3 Infinity War" ≈ "Avengers: Infinity War").
     */
    private fun score(query: String, year: Int?, r: MetadataSearchResult): Double {
        var s = maxOf(
            titleSimilarity(query, r.title),
            r.originalTitle?.let { titleSimilarity(query, it) } ?: 0.0,
        )
        if (year != null && r.year != null) {
            val diff = kotlin.math.abs(year - r.year)
            s += when {
                diff == 0 -> 0.15
                diff == 1 -> 0.05
                else -> -0.35
            }
        }
        return s.coerceIn(0.0, 1.0)
    }

    /** Similarity of a cleaned provider query against one TMDB candidate title. */
    private fun titleSimilarity(query: String, candidate: String): Double {
        val qTokens = significantTokens(query)
        val tTokens = significantTokens(candidate)
        if (qTokens.isEmpty() || tTokens.isEmpty()) return 0.0
        if (qTokens == tTokens) return 1.0

        val qSet = qTokens.toSet()
        val tSet = tTokens.toSet()
        // One title fully covers the other (order-insensitive) — common when IPTV drops a subtitle
        // or TMDB adds a franchise colon form.
        if (qSet.containsAll(tSet) || tSet.containsAll(qSet)) return 0.88

        val inter = qSet.intersect(tSet).size.toDouble()
        if (inter == 0.0) return 0.0
        val jaccard = inter / (qSet.size + tSet.size - inter)
        // Soft boost when most of the shorter title is present (e.g. 3 of 4 tokens).
        val coverage = inter / minOf(qSet.size, tSet.size).toDouble()
        return maxOf(jaccard, coverage * 0.85)
    }

    /**
     * Meaningful match tokens: punctuation stripped, tiny stop-ish words dropped, and bare 1–2 digit
     * franchise numbers ignored so "Avengers 3 Infinity War" aligns with "Avengers: Infinity War".
     */
    private fun significantTokens(title: String): List<String> =
        TitleNormalizer.matchTokens(title).filter { token ->
            if (token.length <= 2 && token.all { it.isDigit() }) return@filter false
            token !in MATCH_STOPWORDS
        }

    companion object {
        private const val TAG = "MetadataRepository"
        private const val TYPE_MOVIE = "movie"
        private const val TYPE_TV = "tv"
        private const val TYPE_EPISODE = "episode"

        /** Accept a match at/above this confidence; below it, prefer no metadata over a wrong one. */
        private const val ACCEPT_THRESHOLD = 0.58

        /** High-confidence enough to stop trying further query variants. */
        private const val EARLY_ACCEPT = 0.86

        /**
         * Bump when a matcher change makes previously cached misses wrong — existing installs then drop
         * their negative rows once ([healNegativeMatchesOnce]).
         * 1 = scoring against `original_title`.
         * 2 = punctuation-aware tokens + part-number query variants.
         */
        private const val MATCH_HEURISTICS_VERSION = 2

        private const val POSITIVE_TTL_MS = 60L * 24 * 3600 * 1000  // 60 days
        private const val NEGATIVE_TTL_MS = 7L * 24 * 3600 * 1000   // 7 days

        private val MATCH_STOPWORDS = setOf("the", "a", "an", "and", "or", "of", "la", "le", "el", "los", "las", "de", "da", "der", "die", "das")

        /** Stable, re-sync-proof local key (mirrors CustomizeKeys): sourceId + remoteId, or name fallback. */
        fun movieLocalKey(movie: MovieEntity): String = "$TYPE_MOVIE:${movie.sourceId}:${movie.remoteId ?: movie.name}"

        fun cacheKey(tmdbId: Int): String = "$TYPE_MOVIE:$tmdbId"

        fun seriesLocalKey(series: tv.own.owntv.core.database.entity.SeriesEntity): String =
            "$TYPE_TV:${series.sourceId}:${series.remoteId ?: series.name}"

        fun tvCacheKey(tmdbId: Int): String = "$TYPE_TV:$tmdbId"

        fun episodeCacheKey(tvId: Int, season: Int, episode: Int): String = "$TYPE_TV:$tvId:s${season}e$episode"
    }
}
