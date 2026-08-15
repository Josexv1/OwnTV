package tv.own.owntv.features.movies

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.core.customize.CustomizeKeys
import tv.own.owntv.core.database.entity.ContentOrderEntity
import tv.own.owntv.core.database.entity.DownloadEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.features.customize.MoveToCategoryDialog
import tv.own.owntv.ui.components.TextInputDialog
import tv.own.owntv.core.model.DownloadStatus
import tv.own.owntv.features.live.LiveKey
import tv.own.owntv.features.live.displayLabel
import tv.own.owntv.features.settings.data.PanelSection
import tv.own.owntv.features.settings.data.computePanelWidths
import tv.own.owntv.features.settings.data.SettingsRepository
import tv.own.owntv.features.settings.rememberPanelShares
import tv.own.owntv.features.shell.components.CategoryRail
import tv.own.owntv.features.shell.components.MediaDetailsScreen
import tv.own.owntv.features.shell.components.PreviewPane
import tv.own.owntv.features.shell.components.RailCategory
import tv.own.owntv.ui.components.MoveOrderOverlay
import tv.own.owntv.ui.components.InAppToast
import tv.own.owntv.ui.components.rememberInAppToast
import tv.own.owntv.ui.components.FocusableSurface
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.components.OwnTVButtonStyle
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.components.OwnTVSpinner
import tv.own.owntv.ui.components.PosterCard
import tv.own.owntv.ui.components.ResumeDialog
import tv.own.owntv.ui.components.SetTmdbNameDialog
import tv.own.owntv.ui.components.TrailerPlayerScreen
import tv.own.owntv.ui.components.chNavPaging
import tv.own.owntv.ui.components.gridFocusTarget
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import tv.own.owntv.ui.components.SearchBar
import tv.own.owntv.ui.components.trapVerticalFocusExit
import tv.own.owntv.ui.components.SortChip
import tv.own.owntv.ui.components.formatCount
import tv.own.owntv.ui.components.ContentPanelFill
import tv.own.owntv.ui.components.PreviewPanelFill
import tv.own.owntv.ui.components.roundedPanel
import tv.own.owntv.ui.components.CategoryHeader
import tv.own.owntv.ui.components.MediaContextMenu
import tv.own.owntv.ui.components.MediaListRow
import tv.own.owntv.ui.components.MenuEntry
import tv.own.owntv.ui.theme.Dimens
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.format.localizedInteger

@Composable
fun MoviesScreen(
    onFullscreen: () -> Unit,
    onChildFocused: () -> Unit,
    restoreFocus: Boolean = false,
    onRestored: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val vm: MovieViewModel = koinViewModel()
    val alreadyDownloadedMessage = stringResource(R.string.content_already_downloaded)
    val refetchingTmdbMessage = stringResource(R.string.content_refetching_tmdb)
    val researchingTmdbMessage = stringResource(R.string.content_researching_tmdb)
    val railItems by vm.railItems.collectAsStateWithLifecycle()
    val selectedKey by vm.selectedKey.collectAsStateWithLifecycle()
    val count by vm.count.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val sortMode by vm.sortMode.collectAsStateWithLifecycle()
    val viewMode by vm.viewMode.collectAsStateWithLifecycle()
    val selectedMovie by vm.selectedMovie.collectAsStateWithLifecycle()
    val selectedMovieMeta by vm.selectedMovieMeta.collectAsStateWithLifecycle()
    val similarMovies by vm.similarMovies.collectAsStateWithLifecycle()
    val metadataMode by vm.metadataMode.collectAsStateWithLifecycle()
    val moveState by vm.moveState.collectAsStateWithLifecycle()
    var contextMovie by remember { mutableStateOf<MovieEntity?>(null) }
    // The movie the "Move to categoryâ€¦" flow is moving (issue #87), with the origin captured at
    // menu-open time (the rail can't change under the modal, but capturing is still safer).
    var moveItem by remember { mutableStateOf<MovieEntity?>(null) }
    var moveOriginKey by remember { mutableStateOf<String?>(null) }
    var moveOriginName by remember { mutableStateOf<String?>(null) }
    var creatingCategory by remember { mutableStateOf(false) }
    // Fullscreen TMDB details window (Â§11.1); null = closed.
    var detailsMovie by remember { mutableStateOf<MovieEntity?>(null) }
    // Cinematic layout: opened movie detail page (series-style), null = browsing list.
    var openedMovie by remember { mutableStateOf<MovieEntity?>(null) }
    // Genre discovery opened from a cinematic genre chip (TMDB genre name); null = closed.
    var openedGenre by remember { mutableStateOf<String?>(null) }
    // Cast discovery opened from a cinematic cast name; null = closed.
    var openedCast by remember { mutableStateOf<String?>(null) }
    // Long-press Similar → global multi-playlist search for this title; null = closed.
    var globalSearchTitle by remember { mutableStateOf<String?>(null) }
    // "Set TMDB name" dialog target (Â§11.2 U5b); null = closed.
    var setTmdbNameMovie by remember { mutableStateOf<MovieEntity?>(null) }
    // In-app trailer playback (Â§7.3 U4); non-null = fullscreen player open with this YouTube key.
    var trailerVideoKey by remember { mutableStateOf<String?>(null) }
    // Downloaded subtitles for the movie whose context menu is open (subtitle plan Â§11); drives the
    // "Delete subtitles" action + its popup. Reloaded on menu open and after each delete.
    var contextMovieSubs by remember { mutableStateOf<List<tv.own.owntv.core.database.dao.LinkedSubtitle>>(emptyList()) }
    var showDeleteSubs by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val toast = rememberInAppToast()
    // Id + list position of the movie the context menu was opened on. The id re-focuses the same item
    // when it survives (Favourite/Download/Cancel); when the item is REMOVED (Remove from history, or
    // un-Favourite while on the Favorites category), it's gone from the paged list, so we re-focus the
    // nearest surviving neighbour by position instead of escaping to the CategoryRail.
    var contextMovieId by remember { mutableStateOf<Long?>(null) }
    var contextMovieIndex by remember { mutableStateOf(-1) }
    val contextFocus = remember { FocusRequester() }
    val selectedProgress by vm.selectedProgress.collectAsStateWithLifecycle()
    val movieProgress by vm.movieProgress.collectAsStateWithLifecycle()
    val downloadStates by vm.downloadStates.collectAsStateWithLifecycle()
    val movies = vm.movies.collectAsLazyPagingItems()
    val resumeMode by vm.resumeMode.collectAsStateWithLifecycle()
    // Global external-player toggle: never mount the fullscreen in-app player (it spins up mpv)
    // when playback is handed to an external app.
    val externalPlayerOn by vm.externalPlayerOn.collectAsStateWithLifecycle()
    val goFullscreen: () -> Unit = { if (!externalPlayerOn) onFullscreen() }

    val selectedIndex = railItems.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
    val selectedItem = railItems.getOrNull(selectedIndex)
    val selectedLabel = selectedItem?.displayLabel(R.string.content_category_all_movies) ?: stringResource(R.string.content_category_all_movies)

    // Resume flow: AUTO continues silently, ASK prompts (â‰¥10s saved), NEVER starts from zero.
    val scope = rememberCoroutineScope()
    var resumePrompt by remember { mutableStateOf<Pair<MovieEntity, Long>?>(null) }
    val startMovie: (MovieEntity) -> Unit = { m ->
        scope.launch {
            val pos = vm.savedPositionMs(m)
            when {
                resumeMode == SettingsRepository.ResumeMode.ASK && pos >= 10_000 -> resumePrompt = m to pos
                resumeMode == SettingsRepository.ResumeMode.AUTO && pos > 0 -> { vm.play(m, pos); goFullscreen() }
                else -> { vm.play(m, 0); goFullscreen() }
            }
        }
    }
    // Cinematic detail exposes explicit Play / Resume, so those skip the ASK prompt.
    val playFromStart: (MovieEntity) -> Unit = { m ->
        vm.play(m, 0)
        goFullscreen()
    }
    val resumeMovie: (MovieEntity, Long) -> Unit = { m, pos ->
        vm.play(m, pos)
        goFullscreen()
    }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val selFocus = remember { FocusRequester() }
    val firstItemFocus = remember { FocusRequester() }

    // CH+- key paging: shared settings + hoisted rail state. gridPaneFocused/railPaneFocused let
    // chNavPaging consume the keys only for whichever pane is focused.
    val settingsVm: tv.own.owntv.features.settings.SettingsViewModel = koinViewModel()
    val chNavEnabled by settingsVm.chNavEnabled.collectAsStateWithLifecycle()
    val chNavUpSkip by settingsVm.chNavUpSkip.collectAsStateWithLifecycle()
    val chNavDownSkip by settingsVm.chNavDownSkip.collectAsStateWithLifecycle()
    val rememberMovies by settingsVm.rememberLastMovies.collectAsStateWithLifecycle()
    val moviesLayoutMode by settingsVm.moviesLayoutMode.collectAsStateWithLifecycle()
    val cinematic = moviesLayoutMode == SettingsRepository.MoviesLayoutMode.CINEMATIC
    val openMovie: (MovieEntity) -> Unit = { movie ->
        vm.onMovieFocused(movie)
        if (cinematic) openedMovie = movie else startMovie(movie)
    }
    // Keep the opened cinematic detail in sync if the focused/selected entity refreshes.
    LaunchedEffect(selectedMovie?.id, cinematic) {
        if (!cinematic) {
            openedMovie = null
            return@LaunchedEffect
        }
        val open = openedMovie
        val sel = selectedMovie
        if (open != null && sel != null && open.id == sel.id && open !== sel) {
            openedMovie = sel
        }
    }
    // Back priority: genre/cast/global overlays first, then cinematic detail.
    BackHandler(enabled = globalSearchTitle != null) { globalSearchTitle = null }
    BackHandler(enabled = openedCast != null) { openedCast = null }
    BackHandler(enabled = openedGenre != null) { openedGenre = null }
    BackHandler(
        enabled = openedMovie != null &&
            openedGenre == null &&
            openedCast == null &&
            globalSearchTitle == null,
    ) {
        openedMovie = null
    }

    // "Remember last item per category": ON â†’ each category keeps its own scroll position (per-category
    // grid + list states, so view-mode toggles also keep their offsets). OFF â†’ reset the shared grid/list
    // states to the top whenever the category changes (fixes the cross-category scroll-leak bug).
    val perCategoryGrid = remember { mutableStateMapOf<LiveKey, LazyGridState>() }
    val perCategoryList = remember { mutableStateMapOf<LiveKey, LazyListState>() }
    // NOTE: plain constructors, not remember*State() â€” these are created lazily inside getOrPut, so a
    // @Composable/rememberSaveable call here would register slots conditionally and corrupt the slot table.
    val effectiveGridState = if (rememberMovies) perCategoryGrid.getOrPut(selectedKey) { LazyGridState() } else gridState
    val effectiveListState = if (rememberMovies) perCategoryList.getOrPut(selectedKey) { LazyListState() } else listState
    LaunchedEffect(selectedKey, rememberMovies) {
        if (!rememberMovies) { runCatching { gridState.scrollToItem(0) }; runCatching { listState.scrollToItem(0) } }
    }
    val catListState = rememberLazyListState()
    var gridPaneFocused by remember { mutableStateOf(false) }
    var railPaneFocused by remember { mutableStateOf(false) }
    // Returning from the player: scroll to and focus the movie you just played (waits for the grid to load).
    // In cinematic detail the page stays open and owns focus, so skip the grid restore path.
    LaunchedEffect(restoreFocus, movies.itemCount, openedMovie?.id) {
        if (!restoreFocus) return@LaunchedEffect
        if (openedMovie != null) {
            onRestored()
            return@LaunchedEffect
        }
        if (movies.itemCount == 0) return@LaunchedEffect
        val sel = selectedMovie
        val idx = if (sel != null) movies.itemSnapshotList.items.indexOfFirst { it.id == sel.id } else -1
        if (idx >= 0) {
            runCatching { effectiveGridState.scrollToItem(idx) }
            delay(60)
            runCatching { selFocus.requestFocus() }
        }
        onRestored()
    }
    // Closing the long-press context menu must return focus inside this pane, never the CategoryRail.
    //   - Item still present (Favourite toggle / Download / Cancel): re-focus the same item by id.
    //   - Item removed (Remove from history, or un-Favourite on the Favorites category): the paged
    //     list no longer contains it, so focus the NEAREST surviving neighbour by position (the item
    //     that slid into the removed slot, else the new last item, else first item). Only if the whole
    //     category is now empty do we let focus leave (there's nothing here to land on).
    LaunchedEffect(contextMovie, moveItem, creatingCategory) {
        if (contextMovie != null) return@LaunchedEffect
        // Opening the TMDB Details window or the Set TMDB name dialog closes the menu; don't yank focus
        // back to the grid â€” they need it (and trap it). The grid is refocused when they close (see below).
        if (detailsMovie != null) return@LaunchedEffect
        if (setTmdbNameMovie != null) return@LaunchedEffect
        if (trailerVideoKey != null) return@LaunchedEffect
        // The context menu closes before MoveToCategoryDialog (and its nested name prompt) opens.
        // Do not focus the grid behind either modal; re-run this effect when the whole flow closes.
        if (moveItem != null || creatingCategory) return@LaunchedEffect
        val targetId = contextMovieId
        if (targetId == null) { contextMovieIndex = -1; return@LaunchedEffect }
        val items = movies.itemSnapshotList.items
        val idx = items.indexOfFirst { it.id == targetId }
        if (idx >= 0) {
            // Item survived â€” re-focus it directly.
            runCatching {
                if (viewMode == SettingsRepository.VodViewMode.LIST) effectiveListState.scrollToItem(idx)
                else effectiveGridState.scrollToItem(idx)
            }
            withFrameNanos { }
            runCatching { contextFocus.requestFocus() }
        } else {
            // Item was removed. Wait for the paged list to settle, then land on the nearest survivor.
            withFrameNanos { }
            val settled = movies.itemSnapshotList.items.filterNotNull()
            if (settled.isEmpty()) {
                runCatching { firstItemFocus.requestFocus() } // nothing left; firstItemFocus attaches to the next item that loads
            } else {
                val neighbor = settled.getOrNull(contextMovieIndex.coerceAtLeast(0)) ?: settled.last()
                val neighborIdx = items.indexOfFirst { it.id == neighbor.id }.coerceAtLeast(0)
                runCatching {
                    if (viewMode == SettingsRepository.VodViewMode.LIST) effectiveListState.scrollToItem(neighborIdx)
                    else effectiveGridState.scrollToItem(neighborIdx)
                }
                // selFocus is bound to selectedMovie; reuse the generic firstItemFocus path only if that
                // fails. Here we re-purpose contextFocus by re-binding it: re-request after a frame so the
                // neighbour row (now at contextMovieIndex) receives focus.
                contextMovieId = neighbor.id
                withFrameNanos { }
                runCatching { contextFocus.requestFocus() }
            }
        }
        contextMovieIndex = -1
    }

    // Manual panel widths (Settings â†’ Panel Width Adjustment). Null = this section is at Default, and
    // the three panels below keep their stock Dimens/weight() sizing. Cinematic mode ignores the
    // preview panel and always uses rail + full-width list (or the full-bleed detail page).
    val panelShares = rememberPanelShares(PanelSection.MOVIES, settingsVm)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val panels = panelShares?.let { computePanelWidths(it, maxWidth) }
    val opened = openedMovie
    if (cinematic && opened != null) {
        val openedMeta = selectedMovieMeta?.takeIf { it.movieId == opened.id }?.cache
        val details = buildMovieDetails(opened, openedMeta, metadataMode.tmdbWins)
        val alreadyDownloaded = downloadStates[opened.id] != null
        val resumeMs = selectedProgress
            ?.takeIf { selectedMovie?.id == opened.id && !vm.isMovieCompleted(it) }
            ?.positionMs
            ?.takeIf { it > 0 }
        val similarForOpened = similarMovies.takeIf { selectedMovie?.id == opened.id }.orEmpty()
        var subtitleLangs by remember(opened.id) { mutableStateOf<List<String>>(emptyList()) }
        LaunchedEffect(opened.id) {
            subtitleLangs = vm.downloadedSubtitleLanguages(opened)
        }
        MovieCinematicDetail(
            details = details,
            isFavorite = favoriteIds.contains(opened.id),
            resumePositionMs = resumeMs,
            trailerKey = openedMeta?.trailerKey,
            subtitleLanguages = subtitleLangs,
            similarMovies = similarForOpened,
            downloadStrip = downloadStates[opened.id]?.let { tv.own.owntv.ui.components.downloadStripFor(listOf(it)) },
            onPlay = { playFromStart(opened) },
            onResume = resumeMs?.let { pos -> { resumeMovie(opened, pos) } },
            onPlayTrailer = { key -> trailerVideoKey = key },
            onOpenGenre = { genre -> openedGenre = genre },
            onOpenCast = { person -> openedCast = person },
            onOpenSimilar = { movie ->
                openedMovie = movie
                vm.onMovieFocused(movie)
            },
            onSearchSimilarGlobal = { title -> globalSearchTitle = title },
            onToggleFavorite = { vm.toggleFavorite(opened) },
            onDownload = {
                if (alreadyDownloaded) toast.show(alreadyDownloadedMessage) else vm.download(opened)
            },
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { if (it.hasFocus) onChildFocused() },
        )
        // Genre browse overlay (from chip) — physical Back closes it.
        openedGenre?.let { genre ->
            GenreMoviesOverlay(
                genre = genre,
                loadPage = { page, exclude -> vm.moviesForGenrePage(genre, page = page, excludeIds = exclude) },
                favoriteIds = favoriteIds,
                movieProgress = movieProgress,
                onOpen = { movie ->
                    openedGenre = null
                    openedMovie = movie
                    vm.onMovieFocused(movie)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { if (it.hasFocus) onChildFocused() },
            )
        }
        // Cast browse overlay (from underlined cast name).
        openedCast?.let { person ->
            CastMoviesOverlay(
                personName = person,
                loadPage = { page, exclude -> vm.moviesForCastPage(person, page = page, excludeIds = exclude) },
                favoriteIds = favoriteIds,
                movieProgress = movieProgress,
                onOpen = { movie ->
                    openedCast = null
                    openedMovie = movie
                    vm.onMovieFocused(movie)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { if (it.hasFocus) onChildFocused() },
            )
        }
        // Global multi-playlist search from long-press on a Similar card.
        globalSearchTitle?.let { title ->
            GlobalMovieSearchOverlay(
                title = title,
                load = { vm.globalSearchMovies(title) },
                favoriteIds = favoriteIds,
                movieProgress = movieProgress,
                onOpen = { movie ->
                    globalSearchTitle = null
                    openedMovie = movie
                    vm.onMovieFocused(movie)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { if (it.hasFocus) onChildFocused() },
            )
        }
    } else {
    Row(modifier = Modifier.fillMaxSize().onFocusChanged { if (it.hasFocus) onChildFocused() }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CategoryRail(
            width = panels?.category ?: Dimens.RailWidthFixed,
            categories = railItems.map { RailCategory(it.displayLabel(R.string.content_category_all_movies), it.icon, showGenreDot = it.key is LiveKey.Folder) },
            selectedIndex = selectedIndex,
            onSelect = { idx -> railItems.getOrNull(idx)?.let { vm.select(it.key) } },
            listState = catListState,
            modifier = Modifier
                .onFocusChanged { railPaneFocused = it.hasFocus }
                .chNavPaging(
                    enabled = chNavEnabled,
                    upSkip = chNavUpSkip,
                    downSkip = chNavDownSkip,
                    isFocused = { railPaneFocused },
                    lastIndex = { railItems.size - 1 },
                    currentTargetIndex = { selectedIndex },
                    onJumpToIndex = { idx -> railItems.getOrNull(idx)?.let { vm.select(it.key) } },
                ),
        )

        Column(
            modifier = Modifier
                .then(
                    when {
                        cinematic -> Modifier.weight(1f)
                        panels != null -> Modifier.width(panels.list)
                        else -> Modifier.weight(1.8f)
                    },
                )
                .fillMaxSize()
                .roundedPanel(fillColor = ContentPanelFill)
                .onFocusChanged { gridPaneFocused = it.hasFocus }
                // CH+- key paging for this movies list/grid. currentTargetIndex falls back to the
                // visible top when the selected movie isn't in the loaded window (paged data).
                .chNavPaging(
                    enabled = chNavEnabled,
                    upSkip = chNavUpSkip,
                    downSkip = chNavDownSkip,
                    isFocused = { gridPaneFocused },
                    // On the "All" list (every movie) a long-press jump to the very last item is
                    // pointless and janks, so disable long-press there â€” short-press skipping stays.
                    longPressEnabled = { selectedKey != LiveKey.All },
                    lastIndex = { movies.itemCount - 1 },
                    currentTargetIndex = {
                        val sel = selectedMovie
                        if (sel != null) {
                            val idx = movies.itemSnapshotList.items.indexOfFirst { it.id == sel.id }
                            if (idx >= 0) idx
                            else if (viewMode == SettingsRepository.VodViewMode.GRID) effectiveGridState.firstVisibleItemIndex
                            else effectiveListState.firstVisibleItemIndex
                        } else {
                            if (viewMode == SettingsRepository.VodViewMode.GRID) effectiveGridState.firstVisibleItemIndex
                            else effectiveListState.firstVisibleItemIndex
                        }
                    },
                    onJumpToIndex = { idx ->
                        // Scroll the target into view (grid or list), then set it as the selected
                        // movie so selFocus binds to it (gridFocusTarget keys on selectedMovie.id),
                        // and request focus after one frame.
                        scope.launch {
                            val item = movies.itemSnapshotList.items.getOrNull(idx)
                            if (viewMode == SettingsRepository.VodViewMode.GRID) {
                                runCatching { effectiveGridState.scrollToItem(idx) }
                            } else {
                                runCatching { effectiveListState.scrollToItem(idx) }
                            }
                            withFrameNanos { }
                            if (item != null) {
                                vm.onMovieFocused(item)
                                runCatching { selFocus.requestFocus() }
                            } else {
                                runCatching { firstItemFocus.requestFocus() }
                            }
                        }
                    },
                )
                // Entering this pane must land on a poster, never the search bar: prefer the
                // last-focused movie, else the first one. onEnter fires only for directional entry
                // from outside (internal moves don't re-trigger it).
                .focusProperties {
                    onEnter = {
                        if (runCatching { selFocus.requestFocus() }.isFailure) {
                            runCatching { firstItemFocus.requestFocus() }
                        }
                    }
                }
                // Held Up/Down can outrun the lazy grid's composition and escape this pane
                // (landing on the top bar) â€” trap vertical exits; Left/Right/Back leave normally.
                .trapVerticalFocusExit()
                .focusGroup()
                .padding(horizontal = Dimens.ScreenPaddingH, vertical = Dimens.ScreenPaddingV),
        ) {
            // Cinematic browse: the rail already shows the active category, so the breadcrumb
            // title + "Category (N movies)" subtitle is pure redundancy and steals a full poster row.
            // Keep search/sort/view controls only so the grid can show two complete rows.
            if (!cinematic) {
                CategoryHeader(
                    title = stringResource(R.string.content_section_category, stringResource(R.string.common_nav_movies), selectedLabel),
                    subtitle = pluralStringResource(R.plurals.content_count_movies, count, selectedLabel, count),
                )
                Spacer(Modifier.height(14.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = vm::setSearchQuery,
                    placeholder = stringResource(R.string.content_search_movies, selectedLabel),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                SortChip(mode = sortMode, onToggle = vm::toggleSort, playlistLabel = stringResource(R.string.content_provider))
                Spacer(Modifier.width(10.dp))
                // View mode (#10): poster wall vs a compact list (more titles at once).
                tv.own.owntv.ui.components.OwnTVButton(
                    label = stringResource(if (viewMode == SettingsRepository.VodViewMode.GRID) R.string.settings_view_grid else R.string.settings_view_list),
                    onClick = vm::toggleViewMode,
                    icon = if (viewMode == SettingsRepository.VodViewMode.GRID) OwnTVIcon.MENU else OwnTVIcon.MOVIES,
                    style = tv.own.owntv.ui.components.OwnTVButtonStyle.SECONDARY,
                )
            }
            Spacer(Modifier.height(if (cinematic) 12.dp else 14.dp))

            if (movies.itemCount == 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isNotBlank()) stringResource(R.string.content_no_movies_found, searchQuery.trim()) else stringResource(R.string.content_no_movies_here),
                        style = MaterialTheme.typography.bodyLarge, color = OwnTVTheme.colors.onSurfaceVariant,
                    )
                }
            } else if (viewMode == SettingsRepository.VodViewMode.LIST) {
                LazyColumn(
                    state = effectiveListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        count = movies.itemCount,
                        key = movies.itemKey { it.id },
                        contentType = movies.itemContentType { "movie" },
                    ) { index ->
                        val movie = movies[index]
                        if (movie != null) {
                            val prog = movieProgress[movie.id]
                            MovieListRow(
                                movie = movie,
                                isFavorite = favoriteIds.contains(movie.id),
                                completed = prog?.let { vm.isMovieCompleted(it) } == true,
                                modifier = Modifier.gridFocusTarget(
                                    itemId = movie.id, index = index,
                                    contextId = contextMovieId, contextFocus = contextFocus,
                                    selectedId = selectedMovie?.id, selectedFocus = selFocus,
                                    firstItemFocus = firstItemFocus,
                                ),
                                onFocus = { vm.onMovieFocused(movie) },
                                onClick = { openMovie(movie) },
                                onLongClick = { contextMovie = movie; contextMovieId = movie.id; contextMovieIndex = index },
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = effectiveGridState,
                    columns = GridCells.Adaptive(minSize = 130.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        count = movies.itemCount,
                        key = movies.itemKey { it.id },
                        contentType = movies.itemContentType { "movie" },
                    ) { index ->
                        val movie = movies[index]
                        if (movie != null) {
                            val prog = movieProgress[movie.id]
                            val done = prog?.let { vm.isMovieCompleted(it) } == true
                            PosterCard(
                                posterUrl = movie.posterUrl,
                                title = movie.name,
                                rating = movie.rating,
                                completed = done,
                                progressFraction = if (done || prog == null || prog.durationMs <= 0) null
                                    else (prog.positionMs.toFloat() / prog.durationMs).takeIf { it > 0f },
                                isFavorite = favoriteIds.contains(movie.id),
                                modifier = Modifier.gridFocusTarget(
                                    itemId = movie.id, index = index,
                                    contextId = contextMovieId, contextFocus = contextFocus,
                                    selectedId = selectedMovie?.id, selectedFocus = selFocus,
                                    firstItemFocus = firstItemFocus,
                                ),
                                onFocus = { vm.onMovieFocused(movie) },
                                onClick = { openMovie(movie) },
                                onLongClick = { contextMovie = movie; contextMovieId = movie.id; contextMovieIndex = index },
                            )
                        }
                    }
                }
            }
        }

        // Classic layout keeps the right-hand preview pane (unless panel width hid it at 0%).
        // Cinematic layout is rail + full-width list only; details live on the dedicated page.
        if (!cinematic && panelShares?.preview != 0) {
            Box(
                modifier = Modifier
                    .then(if (panels != null) Modifier.width(panels.preview) else Modifier.weight(1f))
                    .fillMaxSize()
                    .roundedPanel(fillColor = PreviewPanelFill)
                    .padding(Dimens.GapLarge),
            ) {
                MovieDetailsPane(
                    movie = selectedMovie,
                    meta = selectedMovieMeta?.takeIf { it.movieId == selectedMovie?.id }?.cache,
                    tmdbWins = metadataMode.tmdbWins,
                    resumePositionMs = selectedProgress?.takeIf { !vm.isMovieCompleted(it) }?.positionMs?.takeIf { it > 0 },
                    downloadStrip = selectedMovie?.let { m -> downloadStates[m.id]?.let { tv.own.owntv.ui.components.downloadStripFor(listOf(it)) } },
                )
            }
        }
    }
    }
    }

    resumePrompt?.let { (m, pos) ->
        ResumeDialog(
            positionMs = pos,
            onResume = { resumePrompt = null; vm.play(m, pos); goFullscreen() },
            onStartOver = { resumePrompt = null; vm.play(m, 0); goFullscreen() },
            onDismiss = { resumePrompt = null },
        )
    }

    // Load the opened movie's downloaded subtitles so the menu can show "Delete subtitles" (Â§11).
    LaunchedEffect(contextMovie?.id) {
        contextMovieSubs = contextMovie?.let { runCatching { vm.downloadedSubtitles(it) }.getOrDefault(emptyList()) } ?: emptyList()
    }

    // Long-press a movie â†’ context menu.
    contextMovie?.let { m ->
        val alreadyDownloaded = downloadStates[m.id] != null
        // TMDB Details is shown only when enrichment is on AND a confident match resolved for THIS movie.
        val cacheForM = selectedMovieMeta?.takeIf { it.movieId == m.id }?.cache
        val watched = selectedProgress?.takeIf { selectedMovie?.id == m.id }?.let { vm.isMovieCompleted(it) } ?: false
        MovieContextMenu(
            title = m.name,
            isFavorite = favoriteIds.contains(m.id),
            watched = watched,
            canMove = selectedKey is LiveKey.Folder || selectedKey is LiveKey.Custom || selectedKey == LiveKey.Favorites,
            isHistory = selectedKey == LiveKey.History,
            hasTmdbDetails = metadataMode.enrich && cacheForM != null,
            trailerKey = if (metadataMode.enrich) cacheForM?.trailerKey else null,
            canRefetchTmdb = metadataMode.enrich,
            onShowDetails = { contextMovie = null; detailsMovie = m },
            onToggleFavorite = { vm.toggleFavorite(m); contextMovie = null },
            onToggleWatched = {
                if (watched) vm.markMovieUnwatched(m) else vm.markMovieWatched(m)
                contextMovie = null
            },
            onMove = { contextMovie = null; vm.enterMoveMode(m, selectedKey) },
            onMoveToCategory = {
                moveOriginKey = when (val k = selectedKey) {
                    is LiveKey.Folder -> vm.folderKey(k.id)
                    is LiveKey.Custom -> k.id
                    LiveKey.Favorites -> ContentOrderEntity.FAV_CONTEXT
                    else -> null
                }
                moveOriginName = railItems.firstOrNull { it.key == selectedKey }?.title
                moveItem = m
                contextMovie = null
            },
            onHide = { vm.hideMovie(m); contextMovie = null },
            onRemoveFromHistory = { vm.removeFromHistory(m.id); contextMovie = null },
            onDownload = {
                contextMovie = null
                // Idempotent (Â§11.1): don't re-queue an existing download â€” nudge to the Downloads menu.
                if (alreadyDownloaded) {
                    toast.show(alreadyDownloadedMessage)
                } else vm.download(m)
            },
            onPlayExternal = { contextMovie = null; vm.playExternal(m) },
            onRefetch = {
                contextMovie = null
                toast.show(refetchingTmdbMessage)
                vm.refetchMovieMeta(m)
            },
            onSetTmdbName = { contextMovie = null; setTmdbNameMovie = m },
            onPlayTrailer = { key -> contextMovie = null; trailerVideoKey = key },
            onDeleteSubtitles = if (contextMovieSubs.isNotEmpty()) ({ showDeleteSubs = true }) else null,
            onDismiss = { contextMovie = null },
        )
    }

    // Move toâ€¦ a combined category (issue #87), incl. the "ï¼‹ New categoryâ€¦" name prompt.
    val moveTargets by vm.moveTargets.collectAsStateWithLifecycle()
    if (creatingCategory) {
        TextInputDialog(
            title = stringResource(R.string.settings_customize_new_category_title),
            hint = stringResource(R.string.settings_customize_new_category_description),
            confirmLabel = stringResource(R.string.common_create),
            allowBlank = false,
            onConfirm = { vm.createCustomCategory(it); creatingCategory = false },
            onDismiss = { creatingCategory = false },
        )
    } else {
        moveItem?.let { m ->
            val originKey = moveOriginKey
            if (originKey != null) {
                MoveToCategoryDialog(
                    moveTargets = moveTargets.filterNot { it.id == originKey },
                    originName = moveOriginName ?: stringResource(R.string.settings_customize_this_category),
                    onNewCategory = { creatingCategory = true },
                    onMove = { targetId, keepInOrigin ->
                        vm.moveToCategory(CustomizeKeys.movie(m), m.id, originKey, targetId, keepInOrigin)
                        moveItem = null
                    },
                    onDismiss = { moveItem = null },
                )
            }
        }
    }

    // Per-item "Delete subtitles" popup (Â§11) â€” individual deletion; closes when none remain.
    if (showDeleteSubs) {
        val m = contextMovie
        if (m == null || contextMovieSubs.isEmpty()) {
            showDeleteSubs = false
        } else {
            tv.own.owntv.features.subtitles.SubtitleDeletePopup(
                contentTitle = m.name,
                items = contextMovieSubs,
                onDelete = { sub ->
                    vm.deleteSubtitle(sub.cacheId)
                    contextMovieSubs = contextMovieSubs.filterNot { it.cacheId == sub.cacheId }
                    // Last one deleted â†’ close the popup AND the context menu so focus returns to the
                    // movie tile (the menu's Delete action is gone anyway).
                    if (contextMovieSubs.isEmpty()) { showDeleteSubs = false; contextMovie = null }
                },
                onDismiss = { showDeleteSubs = false },
            )
        }
    }

    // When the TMDB Details window closes, return focus to the movie it was opened from (the window
    // trapped focus, so without this it would fall to the sidebar).
    LaunchedEffect(detailsMovie) {
        if (detailsMovie == null && contextMovieId != null && openedMovie == null) {
            withFrameNanos { }
            runCatching { contextFocus.requestFocus() }
        }
    }

    // Leaving the cinematic detail page: put focus back on the movie that was open.
    LaunchedEffect(openedMovie) {
        if (openedMovie != null) return@LaunchedEffect
        val targetId = selectedMovie?.id ?: return@LaunchedEffect
        if (!cinematic) return@LaunchedEffect
        val items = movies.itemSnapshotList.items
        val idx = items.indexOfFirst { it.id == targetId }
        if (idx >= 0) {
            runCatching {
                if (viewMode == SettingsRepository.VodViewMode.LIST) effectiveListState.scrollToItem(idx)
                else effectiveGridState.scrollToItem(idx)
            }
            withFrameNanos { }
            runCatching { selFocus.requestFocus() }
        }
    }

    // Windowed TMDB details popup (Â§11.1) â€” read-only, Back exits.
    detailsMovie?.let { m ->
        val cache = selectedMovieMeta?.takeIf { it.movieId == m.id }?.cache
        MediaDetailsScreen(
            details = buildMovieDetails(m, cache, metadataMode.tmdbWins),
            onExit = { detailsMovie = null },
        )
    }

    // "Set TMDB name" override dialog (Â§11.2 U5b). Prefill once per target (saved override, else cleaned title).
    LaunchedEffect(setTmdbNameMovie) {
        if (setTmdbNameMovie == null && contextMovieId != null) {
            withFrameNanos { }
            runCatching { contextFocus.requestFocus() }
        }
    }
    setTmdbNameMovie?.let { m ->
        var prefill by remember(m.id) { mutableStateOf<MovieViewModel.TmdbNamePrefill?>(null) }
        LaunchedEffect(m.id) { prefill = vm.movieTmdbNamePrefill(m) }
        prefill?.let { p ->
            SetTmdbNameDialog(
                initialTitle = p.title,
                initialYear = p.year,
                hasOverride = p.hasOverride,
                onSave = { title, year ->
                    setTmdbNameMovie = null
                    vm.setMovieTmdbName(m, title, year)
                    toast.show(researchingTmdbMessage)
                },
                onClear = {
                    setTmdbNameMovie = null
                    vm.clearMovieTmdbName(m)
                    toast.show(researchingTmdbMessage)
                },
                onDismiss = { setTmdbNameMovie = null },
            )
        }
    }

    // In-app trailer player (Â§7.3 U4) â€” fullscreen over everything; Back/Exit closes and refocuses the movie.
    LaunchedEffect(trailerVideoKey) {
        if (trailerVideoKey == null && contextMovieId != null) {
            withFrameNanos { }
            runCatching { contextFocus.requestFocus() }
        }
    }
    trailerVideoKey?.let { key ->
        TrailerPlayerScreen(videoKey = key, onExit = { trailerVideoKey = null })
    }

    // Move mode overlay.
    moveState?.let { ms ->
        MoveOrderOverlay(
            title = stringResource(R.string.content_reorder_movie),
            itemNames = ms.items.map { it.name },
            activeIndex = ms.activeIndex,
            onMoveUp = vm::moveUp,
            onMoveDown = vm::moveDown,
            onCommit = vm::commitMove,
            onCancel = vm::cancelMove,
        )
    }

    InAppToast(toast)
}

/**
 * Compact circular icon action for the cinematic detail row. Label lives in a shared focus tooltip
 * above the row (and as contentDescription for a11y) so the chrome stays small and centered.
 */
@Composable
private fun CinematicActionButton(
    icon: OwnTVIcon,
    tooltip: String,
    primary: Boolean,
    onClick: () -> Unit,
    onTooltip: (String?) -> Unit,
    modifier: Modifier = Modifier,
    iconFilled: Boolean = true,
) {
    val colors = OwnTVTheme.colors
    FocusableSurface(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = tooltip }
            .onFocusChanged { focus ->
                // Only publish on gain. Clearing on loss races the next button's focus gain and
                // briefly blanks the shared tooltip while moving across the action row.
                if (focus.isFocused || focus.hasFocus) onTooltip(tooltip)
            },
        shape = RoundedCornerShape(50),
        focusedScale = 1.08f,
        glowElevation = 10,
        surface = tv.own.owntv.ui.theme.LocalActionSurface.current,
        glassFrostScale = 0.9f,
        glassIdleRimAlpha = 0.18f,
        unfocusedContainerColor = if (primary) colors.primary else colors.card,
        focusedContainerColor = if (primary) colors.primary else colors.primaryContainer,
        selectedContainerColor = if (primary) colors.primary else colors.card,
        contentAlignment = Alignment.Center,
    ) { focused ->
        val tint = when {
            primary -> colors.onPrimary
            focused -> colors.onPrimaryContainer
            else -> Color.White.copy(alpha = 0.92f)
        }
        OwnTVIcon(
            icon = icon,
            tint = tint,
            filled = iconFilled,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Full-bleed cinematic movie detail used by [SettingsRepository.MoviesLayoutMode.CINEMATIC].
 *
 * Prime-style layout:
 * - fixed hero (no page scroll, no Back button — D-pad Back / system back exits)
 * - Similar rail peeks at the bottom and lifts over the hero when focused
 */
@Composable
private fun MovieCinematicDetail(
    details: tv.own.owntv.features.shell.components.MediaDetailsUi,
    isFavorite: Boolean,
    resumePositionMs: Long?,
    trailerKey: String?,
    subtitleLanguages: List<String>,
    similarMovies: List<MovieViewModel.SimilarMovie>,
    downloadStrip: tv.own.owntv.ui.components.DownloadStripState?,
    onPlay: () -> Unit,
    onResume: (() -> Unit)?,
    onPlayTrailer: (String) -> Unit,
    onOpenGenre: (String) -> Unit,
    onOpenCast: (String) -> Unit,
    onOpenSimilar: (MovieEntity) -> Unit,
    onSearchSimilarGlobal: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    val density = LocalDensity.current
    val playFocus = remember { FocusRequester() }
    // First similar poster — Down from any action button lands here (not the Nth poster under the Nth icon).
    // Non-lazy row on purpose: LazyRow disposed off-screen item 0 and left this requester inactive
    // after scrolling right (Up from card 8 then Down could no longer enter the rail).
    val firstSimilarFocus = remember { FocusRequester() }
    val similarScroll = rememberScrollState()
    LaunchedEffect(details.title, resumePositionMs != null) {
        runCatching { playFocus.requestFocus() }
    }

    val playableSimilar = remember(similarMovies) {
        similarMovies.mapNotNull { item -> item.movie?.let { m -> item to m } }
    }
    var similarFocused by remember { mutableStateOf(false) }
    // Rest: rail peeks under the hero. Focused: rail rises and covers the lower hero (Prime-like).
    val similarHeight by animateDpAsState(
        targetValue = when {
            playableSimilar.isEmpty() -> 0.dp
            similarFocused -> 310.dp
            else -> 168.dp
        },
        animationSpec = tween(durationMillis = 280),
        label = "similarHeight",
    )
    val heroShift by animateDpAsState(
        targetValue = if (similarFocused && playableSimilar.isNotEmpty()) (-36).dp else 0.dp,
        animationSpec = tween(durationMillis = 280),
        label = "heroShift",
    )
    val heroScale by animateFloatAsState(
        targetValue = if (similarFocused && playableSimilar.isNotEmpty()) 0.94f else 1f,
        animationSpec = tween(durationMillis = 280),
        label = "heroScale",
    )
    val heroDim by animateFloatAsState(
        targetValue = if (similarFocused && playableSimilar.isNotEmpty()) 0.72f else 1f,
        animationSpec = tween(durationMillis = 280),
        label = "heroDim",
    )
    // Keep plot short so action buttons never get squished by long overviews.
    // Cast hides while the Similar rail is lifted (more vertical room for posters).
    val plotMaxLines = if (similarFocused) 2 else 3
    val showCast = !similarFocused

    val rootModifier = modifier
        .clip(RoundedCornerShape(Dimens.CornerLarge))
        .background(colors.background)
        .focusGroup()
    val hScrim = Brush.horizontalGradient(
        listOf(
            Color.Black.copy(alpha = 0.88f),
            Color.Black.copy(alpha = 0.70f),
            Color.Black.copy(alpha = 0.30f),
            Color.Black.copy(alpha = 0.12f),
        ),
    )
    val vScrim = Brush.verticalGradient(
        listOf(
            Color.Black.copy(alpha = 0.42f),
            Color.Transparent,
            Color.Black.copy(alpha = 0.42f),
            Color.Black.copy(alpha = 0.90f),
        ),
    )
    val plotText = details.plot?.takeIf { it.isNotBlank() }
    val primaryIsResume = onResume != null && resumePositionMs != null

    Box(modifier = rootModifier) {
        // Full-bleed backdrop stays put while the similar rail lifts over it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surfaceContainerLowest),
        ) {
            val backdrop = details.backdropUrl
            if (!backdrop.isNullOrBlank()) {
                AsyncImage(
                    model = backdrop,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(modifier = Modifier.fillMaxSize().background(hScrim))
            Box(modifier = Modifier.fillMaxSize().background(vScrim))
        }

        // Fixed hero — no vertical page scroll. Back is handled by the parent BackHandler / remote.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 36.dp, end = 36.dp, top = 28.dp)
                .padding(bottom = if (playableSimilar.isEmpty()) 28.dp else similarHeight)
                .graphicsLayer {
                    translationY = with(density) { heroShift.toPx() }
                    scaleX = heroScale
                    scaleY = heroScale
                    alpha = heroDim
                    transformOrigin = TransformOrigin(0f, 0f)
                },
            verticalArrangement = Arrangement.Center,
        ) {
            // Match text-column height to the poster so plot can use weight() and actions stay pinned.
            val posterWidth = 200.dp
            val posterHeight = posterWidth * 3f / 2f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(posterHeight),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(posterWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surfaceContainerLowest),
                    contentAlignment = Alignment.Center,
                ) {
                    val poster = details.posterUrl
                    if (!poster.isNullOrBlank()) {
                        AsyncImage(
                            model = poster,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        OwnTVIcon(
                            OwnTVIcon.MOVIES,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.height(48.dp),
                        )
                    }
                }

                Column(
                    // Same height as poster: plot/cast flex in the middle; action row stays at bottom.
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Always text — TMDB "logos" are often brand marks (the Avengers A), not titles.
                    Text(
                        text = details.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (details.metaLine.isNotBlank()) {
                        Text(
                            text = details.metaLine,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (details.genres.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            details.genres.take(6).forEach { genre ->
                                // Clickable TMDB genre chip → local genre discovery.
                                FocusableSurface(
                                    onClick = { onOpenGenre(genre) },
                                    shape = RoundedCornerShape(50),
                                    focusedScale = 1.06f,
                                    glowElevation = 8,
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.14f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.28f),
                                    selectedContainerColor = Color.White.copy(alpha = 0.14f),
                                    showFocusBorder = true,
                                ) {
                                    Text(
                                        text = genre,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.92f),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                    // Downloaded OpenSubtitles only — not TMDB "original language" (that isn't the file).
                    val subLine = formatLanguageLine(
                        label = stringResource(R.string.content_subtitle_languages),
                        languages = subtitleLanguages,
                        maxVisible = 4,
                    )
                    if (subLine != null) {
                        Text(
                            text = subLine,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (resumePositionMs != null) {
                        Text(
                            text = stringResource(
                                R.string.content_resume_at,
                                tv.own.owntv.ui.components.formatTimestamp(resumePositionMs),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                        )
                    }
                    if (downloadStrip != null) {
                        tv.own.owntv.ui.components.DownloadStatusStrip(downloadStrip)
                    }
                    // Flexible middle: long plots shrink/ellipsis here so actions below never compress.
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (plotText != null) {
                            Text(
                                text = plotText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = plotMaxLines,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 720.dp),
                            )
                        }
                        if (showCast && details.cast.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.content_media_cast),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.70f),
                                )
                                // Prime-style underlined cast names → TMDB person filmography ∩ local catalog.
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    details.cast.take(6).forEach { person ->
                                        FocusableSurface(
                                            onClick = { onOpenCast(person.name) },
                                            shape = RoundedCornerShape(4.dp),
                                            focusedScale = 1.04f,
                                            glowElevation = 0,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedContainerColor = Color.White.copy(alpha = 0.12f),
                                            selectedContainerColor = Color.Transparent,
                                            showFocusBorder = false,
                                        ) {
                                            Text(
                                                text = person.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.92f),
                                                textDecoration = TextDecoration.Underline,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Compact icon-only actions pinned under the flexible plot/cast block.
                    // Down from ANY action always enters the similar rail at the first poster —
                    // geometric focus would otherwise land on the Nth poster under the Nth icon.
                    var actionTooltip by remember { mutableStateOf<String?>(null) }
                    val actionDown: Modifier.() -> Modifier = {
                        if (playableSimilar.isNotEmpty()) {
                            focusProperties { down = firstSimilarFocus }
                        } else {
                            this
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = actionTooltip.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .height(20.dp)
                                .alpha(if (actionTooltip.isNullOrBlank()) 0f else 1f),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (primaryIsResume) {
                                CinematicActionButton(
                                    icon = OwnTVIcon.PLAY,
                                    tooltip = stringResource(R.string.content_action_resume_movie),
                                    primary = true,
                                    onClick = onResume!!,
                                    onTooltip = { actionTooltip = it },
                                    modifier = Modifier.focusRequester(playFocus).actionDown(),
                                )
                                CinematicActionButton(
                                    icon = OwnTVIcon.PLAY,
                                    tooltip = stringResource(R.string.content_action_play_movie),
                                    primary = false,
                                    onClick = onPlay,
                                    onTooltip = { actionTooltip = it },
                                    modifier = Modifier.actionDown(),
                                )
                            } else {
                                CinematicActionButton(
                                    icon = OwnTVIcon.PLAY,
                                    tooltip = stringResource(R.string.content_action_play_movie),
                                    primary = true,
                                    onClick = onPlay,
                                    onTooltip = { actionTooltip = it },
                                    modifier = Modifier.focusRequester(playFocus).actionDown(),
                                )
                            }
                            if (!trailerKey.isNullOrBlank()) {
                                CinematicActionButton(
                                    icon = OwnTVIcon.VIDEO,
                                    tooltip = stringResource(R.string.content_action_watch_trailer),
                                    primary = false,
                                    onClick = { onPlayTrailer(trailerKey) },
                                    onTooltip = { actionTooltip = it },
                                    modifier = Modifier.actionDown(),
                                )
                            }
                            CinematicActionButton(
                                icon = OwnTVIcon.FAVORITE,
                                tooltip = stringResource(
                                    if (isFavorite) R.string.content_action_remove_favourite
                                    else R.string.content_action_save_favourite,
                                ),
                                primary = false,
                                onClick = onToggleFavorite,
                                onTooltip = { actionTooltip = it },
                                iconFilled = isFavorite,
                                modifier = Modifier.actionDown(),
                            )
                            CinematicActionButton(
                                icon = OwnTVIcon.DOWNLOADS,
                                tooltip = stringResource(R.string.content_action_download_movie),
                                primary = false,
                                onClick = onDownload,
                                onTooltip = { actionTooltip = it },
                                modifier = Modifier.actionDown(),
                            )
                        }
                    }
                }
            }
        }

        if (playableSimilar.isNotEmpty()) {
            val railScrim = Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.55f),
                    Color.Black.copy(alpha = 0.92f),
                ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(similarHeight)
                    .background(railScrim)
                    .onFocusChanged { similarFocused = it.hasFocus }
                    .focusGroup()
                    .padding(start = 36.dp, end = 36.dp, top = 12.dp, bottom = 18.dp),
            ) {
                Text(
                    text = stringResource(R.string.content_similar_movies),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                // Keep all ≤12 posters composed so firstSimilarFocus never detaches off-screen.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(similarScroll),
                ) {
                    playableSimilar.forEachIndexed { index, pair ->
                        val (item, movie) = pair
                        // Slightly smaller cards while peeking so the hero stays dominant.
                        val cardWidth = if (similarFocused) 148.dp else 118.dp
                        // Up from any similar poster returns to the primary action (Play/Resume),
                        // not whatever action happened to sit above that column.
                        val cardFocus = Modifier
                            .width(cardWidth)
                            .focusProperties { up = playFocus }
                            .then(if (index == 0) Modifier.focusRequester(firstSimilarFocus) else Modifier)
                        PosterCard(
                            posterUrl = item.posterUrl,
                            title = item.title,
                            rating = null,
                            showTitle = false,
                            modifier = cardFocus,
                            onClick = { onOpenSimilar(movie) },
                            // Long-press: search every playlist/folder for other language/source copies.
                            onLongClick = { onSearchSimilarGlobal(item.title) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieContextMenu(
    title: String,
    isFavorite: Boolean,
    watched: Boolean,
    canMove: Boolean,
    isHistory: Boolean,
    hasTmdbDetails: Boolean,
    trailerKey: String?,
    canRefetchTmdb: Boolean,
    onShowDetails: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatched: () -> Unit,
    onMove: () -> Unit,
    // "Move to category..." (issue #87): send this movie into a user's combined category.
    onMoveToCategory: () -> Unit,
    onHide: () -> Unit,
    onRemoveFromHistory: () -> Unit,
    onDownload: () -> Unit,
    onPlayExternal: () -> Unit,
    onRefetch: () -> Unit,
    onSetTmdbName: () -> Unit,
    onPlayTrailer: (String) -> Unit,
    // Non-null only when this movie has downloaded OpenSubtitles subtitles (subtitle plan Â§11).
    onDeleteSubtitles: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val entries = mutableListOf<MenuEntry>()
    entries += MenuEntry(
        if (isFavorite) stringResource(R.string.content_remove_favourite) else stringResource(R.string.content_add_favourite),
        onToggleFavorite, OwnTVIcon.FAVORITE,
    )
    entries += MenuEntry(
        if (watched) stringResource(R.string.content_mark_unwatched) else stringResource(R.string.content_mark_watched),
        onToggleWatched,
    )
    if (canMove) entries += MenuEntry(stringResource(R.string.content_move), onMove)
    if (canMove) entries += MenuEntry(stringResource(R.string.content_move_to_category), onMoveToCategory)
    if (isHistory) entries += MenuEntry(stringResource(R.string.content_remove_history), onRemoveFromHistory)
    entries += MenuEntry(stringResource(R.string.common_hide), onHide)
    entries += MenuEntry(stringResource(R.string.content_download), onDownload, OwnTVIcon.DOWNLOADS)
    // Delete subtitles â€” only when this movie has downloaded OpenSubtitles subs (Â§11).
    onDeleteSubtitles?.let { entries += MenuEntry(stringResource(R.string.content_delete_subtitles), it, OwnTVIcon.SUBTITLE) }
    // Phase B: one-off external playback, independent of the global "External player" toggle.
    entries += MenuEntry(stringResource(R.string.content_play_external), onPlayExternal, OwnTVIcon.PLAY)
    // TMDB Details â€” only when a confident match resolved (Â§11.1).
    if (hasTmdbDetails) entries += MenuEntry(stringResource(R.string.content_tmdb_details), onShowDetails, OwnTVIcon.MENU)
    // Play Trailer (Â§7.3 U4) â€” only when TMDB actually has a trailer for this title (Â§11.1 gating).
    trailerKey?.let { key -> entries += MenuEntry(stringResource(R.string.content_play_trailer), { onPlayTrailer(key) }) }
    // Refetch TMDB details (Â§11.2 U5a) â€” always available when enrichment is on, so a "no match"
    // (7-day negative cache) or a stale match can be cleared and re-searched immediately.
    if (canRefetchTmdb) {
        entries += MenuEntry(stringResource(R.string.content_refetch_tmdb), onRefetch)
        // Set TMDB name (Â§11.2 U5b) â€” hand-type the exact title to override the auto-match.
        entries += MenuEntry(stringResource(R.string.content_set_tmdb_name), onSetTmdbName)
    }
    MediaContextMenu(
        title = title,
        entries = entries,
        onDismiss = onDismiss,
        closeLabel = stringResource(R.string.content_close),
    )
}

@Composable
private fun MovieDetailsPane(
    movie: MovieEntity?,
    meta: tv.own.owntv.core.database.entity.MetadataCacheEntity?,
    tmdbWins: Boolean,
    resumePositionMs: Long? = null,
    downloadStrip: tv.own.owntv.ui.components.DownloadStripState? = null,
) {
    val colors = OwnTVTheme.colors
    if (movie == null) {
        PreviewPane(hint = stringResource(R.string.content_focus_movie))
        return
    }
    // Merge (Â§7.1 / Â§4.1). Provider+TMDB â†’ provider wins (provider ?: tmdb); TMDB-only â†’ tmdb wins
    // (tmdb ?: provider). TMDB fields are never written back to the content row.
    val providerPoster = movie.posterUrl?.takeIf { it.isNotBlank() }
    val tmdbPoster = tv.own.owntv.core.metadata.MetadataImages.poster(meta?.posterPath)
    val posterArt = (if (tmdbWins) tmdbPoster ?: providerPoster else providerPoster ?: tmdbPoster)
        ?: movie.backdropUrl?.takeIf { it.isNotBlank() }
        ?: tv.own.owntv.core.metadata.MetadataImages.backdrop(meta?.backdropPath)
    val providerPlot = movie.plot?.takeIf { it.isNotBlank() }
    val plot = if (tmdbWins) meta?.overview ?: providerPlot else providerPlot ?: meta?.overview
    // Outer details Box carries the rounded panel (Phase 6); no clip/background here.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.GapLarge),
    ) {
        // Non-focusable download status strip â€” only present while this movie is actually downloading.
        if (downloadStrip != null) {
            tv.own.owntv.ui.components.DownloadStatusStrip(downloadStrip)
            Spacer(Modifier.height(12.dp))
        }
        // Tall portrait poster (like the list / a phone screen), centred in the pane.
        Box(modifier = Modifier.fillMaxWidth().height(340.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.fillMaxHeight().aspectRatio(2f / 3f).clip(RoundedCornerShape(Dimens.CornerMedium)).background(colors.surfaceContainerLowest),
                contentAlignment = Alignment.Center,
            ) {
                if (!posterArt.isNullOrBlank()) {
                    AsyncImage(
                        model = posterArt,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    OwnTVIcon(OwnTVIcon.MOVIES, tint = colors.onSurfaceVariant, modifier = Modifier.height(48.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        // Always-visible, non-focusable resume label (kept above the title, not further down in the
        // pane, since movie metadata below can push a lower placement out of view once it scrolls long).
        if (resumePositionMs != null) {
            Text(
                stringResource(R.string.content_resume_at, tv.own.owntv.ui.components.formatTimestamp(resumePositionMs)),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(movie.name, style = MaterialTheme.typography.headlineMedium, color = colors.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(metaLine(movie, meta, tmdbWins), style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"), color = colors.onSurfaceVariant)
        // Genres & cast are TMDB-only (Â§7.1) â€” a whole layer the provider never had.
        val genres = jsonList(meta?.genresJson)
        if (genres.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(genres.joinToString(stringResource(R.string.content_genres_separator)), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
        if (!plot.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(plot, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 6, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 360.dp))
        }
        val cast = tv.own.owntv.core.metadata.MetadataCast.names(meta?.castJson)
        if (cast.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.content_media_cast), style = MaterialTheme.typography.labelMedium, color = colors.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(cast.take(6).joinToString(", "), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(20.dp))
        // Display-only pane (Â§11.1): actions live on the poster â€” OK plays, long-press opens the menu
        // (Favorite / Download / TMDB Details). Keeping the pane non-focusable fixes gridâ†’pane navigation.
        Text(
            stringResource(R.string.content_ok_play_options),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun metaLine(movie: MovieEntity, meta: tv.own.owntv.core.database.entity.MetadataCacheEntity? = null, tmdbWins: Boolean = false): String {
    val parts = mutableListOf<String>()
    // §7.1 / §4.1: precedence flips with the source mode. Many IPTV rows leave year null and only
    // bake it into the name ("… (2018)") — fall back to the normalizer so the cinematic page still
    // shows year before TMDB resolves.
    val nameYear = tv.own.owntv.core.metadata.TitleNormalizer.normalize(movie.name).year
    val year = if (tmdbWins) meta?.year ?: movie.year ?: nameYear else movie.year ?: meta?.year ?: nameYear
    val rating = if (tmdbWins) meta?.rating?.takeIf { it > 0 } ?: movie.rating?.takeIf { it > 0 }
        else movie.rating?.takeIf { it > 0 } ?: meta?.rating?.takeIf { it > 0 }
    year?.let { parts.add(localizedInteger(it, grouping = false)) }
    rating?.let { parts.add(stringResource(R.string.content_rating, it)) }
    movie.durationSecs?.takeIf { it > 0 }?.let { secs ->
        val h = secs / 3600
        val m = (secs % 3600) / 60
        parts.add(if (h > 0) stringResource(R.string.content_duration_hours, h, m) else stringResource(R.string.content_duration_minutes, m))
    }
    return parts.joinToString(stringResource(R.string.content_metadata_separator))
}

/** Build the fullscreen TMDB-details payload for a movie, applying the §7.1/§4.1 merge precedence. */
@Composable
private fun buildMovieDetails(
    movie: MovieEntity,
    meta: tv.own.owntv.core.database.entity.MetadataCacheEntity?,
    tmdbWins: Boolean,
): tv.own.owntv.features.shell.components.MediaDetailsUi {
    val providerPoster = movie.posterUrl?.takeIf { it.isNotBlank() }
    val tmdbPoster = tv.own.owntv.core.metadata.MetadataImages.poster(meta?.posterPath)
    val poster = if (tmdbWins) tmdbPoster ?: providerPoster else providerPoster ?: tmdbPoster
    // Backdrop is TMDB-only (providers don't carry one); fall back to the provider's if it exists.
    val backdrop = tv.own.owntv.core.metadata.MetadataImages.backdrop(meta?.backdropPath, size = "w1280")
        ?: movie.backdropUrl?.takeIf { it.isNotBlank() }
    val plot = if (tmdbWins) meta?.overview ?: movie.plot else movie.plot?.takeIf { it.isNotBlank() } ?: meta?.overview
    // Display title: cleaned provider name keeps localized catalog titles ("17 otra vez") instead of
    // always forcing TMDB English ("17 Again"). normalize() strips "NF -", "4K", "(2009)" noise.
    // TMDB title is the fallback when the provider name is empty/unusable, or when TMDB-only mode wins.
    val tmdbTitle = meta?.title?.takeIf { it.isNotBlank() && it != "?" }
    val providerTitle = tv.own.owntv.core.metadata.TitleNormalizer.normalize(movie.name).query
        .takeIf { it.isNotBlank() }
        ?: movie.name.takeIf { it.isNotBlank() }
    val title = when {
        tmdbWins -> tmdbTitle ?: providerTitle ?: movie.name
        !providerTitle.isNullOrBlank() -> providerTitle
        else -> tmdbTitle ?: movie.name
    }
    return tv.own.owntv.features.shell.components.MediaDetailsUi(
        title = title,
        backdropUrl = backdrop,
        posterUrl = poster,
        logoUrl = null, // cinematic page uses text title; brand logos are a poor title substitute
        metaLine = metaLine(movie, meta, tmdbWins),
        genres = jsonList(meta?.genresJson),
        plot = plot,
        cast = tv.own.owntv.core.metadata.MetadataCast.parse(meta?.castJson),
    )
}

/** Parse a stored JSON array of strings (genres/cast) back to a list; empty on null/blank/bad JSON. */
private fun jsonList(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
    }.getOrDefault(emptyList())
}

/** "Audio: English, Spanish" or "Subtitles: English, Spanish (3 more)". Null when empty. */
@Composable
private fun formatLanguageLine(label: String, languages: List<String>, maxVisible: Int): String? {
    if (languages.isEmpty()) return null
    val visible = languages.take(maxVisible)
    val joined = visible.joinToString(stringResource(R.string.content_genres_separator))
    val more = languages.size - visible.size
    val tail = if (more > 0) " " + pluralStringResource(R.plurals.content_languages_more, more, more) else ""
    return "$label: $joined$tail"
}

/**
 * Full-screen overlay listing local movies for a TMDB genre (from cinematic chip).
 * Progressive: first page ASAP, then more as the grid approaches the end.
 */
@Composable
private fun GenreMoviesOverlay(
    genre: String,
    loadPage: suspend (page: Int, excludeIds: Set<Long>) -> MovieViewModel.DiscoveryPage,
    favoriteIds: Set<Long>,
    movieProgress: Map<Long, tv.own.owntv.core.database.entity.PlaybackProgressEntity>,
    onOpen: (MovieEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    MovieGridOverlay(
        title = stringResource(R.string.content_genre_movies_title, genre),
        emptyText = stringResource(R.string.content_genre_movies_empty),
        loadPage = loadPage,
        favoriteIds = favoriteIds,
        movieProgress = movieProgress,
        onOpen = onOpen,
        modifier = modifier,
    )
}

/**
 * Full-screen overlay listing local movies for a cast member (TMDB person credits ∩ catalog).
 * Progressive paging so large filmographies don't block first paint.
 */
@Composable
private fun CastMoviesOverlay(
    personName: String,
    loadPage: suspend (page: Int, excludeIds: Set<Long>) -> MovieViewModel.DiscoveryPage,
    favoriteIds: Set<Long>,
    movieProgress: Map<Long, tv.own.owntv.core.database.entity.PlaybackProgressEntity>,
    onOpen: (MovieEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    MovieGridOverlay(
        title = stringResource(R.string.content_cast_movies_title, personName),
        emptyText = stringResource(R.string.content_cast_movies_empty),
        loadPage = loadPage,
        favoriteIds = favoriteIds,
        movieProgress = movieProgress,
        onOpen = onOpen,
        modifier = modifier,
    )
}

/**
 * Global multi-playlist search results for a recommended title (Similar long-press).
 * One-shot list (already bounded); no progressive TMDB paging needed.
 */
@Composable
private fun GlobalMovieSearchOverlay(
    title: String,
    load: suspend () -> List<MovieEntity>,
    favoriteIds: Set<Long>,
    movieProgress: Map<Long, tv.own.owntv.core.database.entity.PlaybackProgressEntity>,
    onOpen: (MovieEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    MovieGridOverlay(
        title = stringResource(R.string.content_global_search_title, title),
        emptyText = stringResource(R.string.content_global_search_empty),
        loadPage = { page, _ ->
            if (page > 1) MovieViewModel.DiscoveryPage(emptyList(), hasMore = false)
            else {
                val items = runCatching { load() }.getOrDefault(emptyList())
                MovieViewModel.DiscoveryPage(items, hasMore = false)
            }
        },
        favoriteIds = favoriteIds,
        movieProgress = movieProgress,
        onOpen = onOpen,
        modifier = modifier,
    )
}

@Composable
private fun MovieGridOverlay(
    title: String,
    emptyText: String,
    loadPage: suspend (page: Int, excludeIds: Set<Long>) -> MovieViewModel.DiscoveryPage,
    favoriteIds: Set<Long>,
    movieProgress: Map<Long, tv.own.owntv.core.database.entity.PlaybackProgressEntity>,
    onOpen: (MovieEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    val gridState = rememberLazyGridState()
    val firstFocus = remember { FocusRequester() }
    var items by remember(title) { mutableStateOf<List<MovieEntity>>(emptyList()) }
    var loadedPage by remember(title) { mutableIntStateOf(0) }
    var hasMore by remember(title) { mutableStateOf(true) }
    var loading by remember(title) { mutableStateOf(false) }
    var initialDone by remember(title) { mutableStateOf(false) }
    var loadGeneration by remember(title) { mutableIntStateOf(0) }

    // Progressive load: page 1 on open, then more when nearing the grid end (or while still empty).
    LaunchedEffect(title, loadGeneration) {
        if (!hasMore || loading) return@LaunchedEffect
        loading = true
        // Keep pulling pages until we have something to show, or TMDB/local is exhausted.
        var guard = 0
        while (hasMore && guard < 8) {
            guard++
            val next = loadedPage + 1
            val exclude = items.mapTo(HashSet()) { it.id }
            val page = runCatching { loadPage(next, exclude) }
                .getOrDefault(MovieViewModel.DiscoveryPage(emptyList(), hasMore = false))
            loadedPage = next
            if (page.movies.isNotEmpty()) {
                val merged = LinkedHashMap<Long, MovieEntity>()
                items.forEach { merged[it.id] = it }
                page.movies.forEach { merged[it.id] = it }
                items = merged.values.toList()
            }
            hasMore = page.hasMore
            // Stop once we have a batch on screen; further pages come from scroll.
            if (items.isNotEmpty()) break
            if (!page.hasMore) break
        }
        loading = false
        initialDone = true
        if (items.isNotEmpty() && loadedPage > 0) {
            runCatching { firstFocus.requestFocus() }
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            hasMore && !loading && items.isNotEmpty() && last >= items.lastIndex - 4
        }
    }
    LaunchedEffect(shouldLoadMore, title) {
        if (shouldLoadMore && hasMore && !loading) {
            loadGeneration += 1
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.CornerLarge))
            .background(colors.background)
            .focusGroup()
            .padding(horizontal = 28.dp, vertical = 22.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(16.dp))
            when {
                !initialDone && items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            OwnTVSpinner(sizeDp = 40)
                            Text(
                                text = stringResource(R.string.content_discovery_loading),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
                initialDone && items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = emptyText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        state = gridState,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(items, key = { it.id }) { movie ->
                            val progress = movieProgress[movie.id]
                            val isFirst = items.firstOrNull()?.id == movie.id
                            PosterCard(
                                posterUrl = movie.posterUrl,
                                title = movie.name,
                                rating = movie.rating,
                                isFavorite = favoriteIds.contains(movie.id),
                                progressFraction = progress
                                    ?.takeIf { it.durationMs > 0 && it.positionMs > 0 }
                                    ?.let { it.positionMs.toFloat() / it.durationMs.toFloat() },
                                modifier = if (isFirst) Modifier.focusRequester(firstFocus) else Modifier,
                                onClick = { onOpen(movie) },
                            )
                        }
                        // Footer spinner while paging more local matches (genre / cast).
                        if (loading && items.isNotEmpty()) {
                            item(
                                key = "discovery-loading-more",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    OwnTVSpinner(sizeDp = 28)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.content_discovery_loading_more),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Compact one-line row used by the List view mode â€” fits many titles on screen at once (#10). */
@Composable
private fun MovieListRow(
    movie: MovieEntity,
    isFavorite: Boolean,
    completed: Boolean = false,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    val metaText = metaLine(movie)
    MediaListRow(
        title = movie.name,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.onFocusChanged { if (it.hasFocus) onFocus() },
        dimmed = completed,
        leading = {
            Box(
                modifier = Modifier.size(width = 44.dp, height = 62.dp).clip(RoundedCornerShape(6.dp)).background(colors.surfaceContainerLowest),
                contentAlignment = Alignment.Center,
            ) {
                if (!movie.posterUrl.isNullOrBlank()) {
                    AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    OwnTVIcon(OwnTVIcon.MOVIES, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
            }
        },
        meta = if (metaText.isNotBlank()) {
            { Text(metaText, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else null,
        trailing = if (completed || isFavorite) {
            {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (completed) {
                        Box(
                            modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(colors.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("âœ“", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        }
                    }
                    if (isFavorite) {
                        OwnTVIcon(OwnTVIcon.FAVORITE, tint = colors.favorite, modifier = Modifier.size(18.dp))
                    }
                }
            }
        } else null,
    )
}
