package tv.own.owntv.features.movies

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import tv.own.owntv.R
import tv.own.owntv.core.metadata.TitleNormalizer
import tv.own.owntv.features.shell.components.MediaDetailsUi
import androidx.compose.foundation.focusGroup
import tv.own.owntv.ui.components.FocusableSurface
import tv.own.owntv.ui.components.trapAllFocusExit
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.components.OwnTVIcon as OwnTVIconGraphic
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * Full-bleed movie detail page — the [SettingsRepository.MoviesLayout.CINEMATIC] presentation.
 *
 * Reads the same [MediaDetailsUi] the windowed [MediaDetailsScreen] does, so both modes show
 * identical content and neither can drift from the other. What differs is the frame: the backdrop
 * fills the screen instead of sitting in a card, and the page is **fixed height with no vertical
 * scroll**, because a 10-foot reader should never have to scroll to reach the Play button.
 *
 * That constraint drives the layout. The text column is pinned to the poster's height; the plot
 * takes the flexible middle and ellipsises; the action row is anchored to the bottom and therefore
 * always on screen, whatever the overview's length.
 *
 * The "more like this" rail sits at the bottom. It only peeks while the actions hold focus, and
 * rises over the lower half of the hero when focus enters it — the hero stays the subject until you
 * ask for something else. Making the genre chips and cast names open in-library discovery is next.
 */
@Composable
fun MovieCinematicDetail(
    details: MediaDetailsUi,
    resumeLabel: String?,
    isFavorite: Boolean,
    canDownload: Boolean,
    trailerKey: String?,
    similar: MovieViewModel.SimilarRail,
    onNeedMoreSimilar: () -> Unit,
    onSimilarClick: (MovieViewModel.SimilarItem) -> Unit,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onPlayTrailer: (String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    val primaryAction = remember { FocusRequester() }
    val railEntry = remember { FocusRequester() }
    var railFocused by remember { mutableStateOf(false) }
    val hasRail = similar.items.isNotEmpty()

    // The hero is padded by exactly this height, so growing the rail lifts the hero rather than
    // overlapping it.
    val railHeight by animateDpAsState(
        targetValue = when {
            !hasRail -> 0.dp
            railFocused -> RAIL_HEIGHT_FOCUSED
            else -> RAIL_HEIGHT_PEEK
        },
        label = "similarRailHeight",
    )
    val railPoster by animateDpAsState(
        targetValue = if (railFocused) RAIL_POSTER_WIDTH_FOCUSED else RAIL_POSTER_WIDTH_PEEK,
        label = "similarRailPoster",
    )
    // Not in the same frame: the browse grid behind is still settling its own focus and would
    // overwrite the request, leaving the D-pad driving the invisible grid. Repeated over a few frames
    // because a rail poster can swap the page mid-recomposition, where one request lands on nothing.
    LaunchedEffect(details.title) {
        railFocused = false
        repeat(FOCUS_CLAIM_FRAMES) {
            withFrameNanos { }
            runCatching { primaryAction.requestFocus() }
        }
    }
    BackHandler { onExit() }

    // trapAllFocusExit + focusGroup are what make this a modal rather than a picture laid over the
    // grid. Without them focus escapes to the still-composed browse behind: the action buttons
    // never take focus, and moving the D-pad drives the hidden list.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .trapAllFocusExit()
            .focusGroup(),
    ) {
        details.backdropUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Two axes, not one. The vertical pass seats the action row; the horizontal pass is what
        // keeps the text column readable over a bright backdrop without dimming the whole image.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.92f))),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.85f), 0.7f to Color.Transparent),
            ),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 40.dp)
                .padding(bottom = railHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            details.posterUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    // Sized from the height it is given, not a fixed 240x360: a 1080p TV is 540dp
                    // tall, so once the rail takes its share a fixed poster loses its bottom edge.
                    modifier = Modifier
                        .heightIn(max = POSTER_MAX_HEIGHT)
                        .fillMaxHeight()
                        .aspectRatio(1f / POSTER_ASPECT)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }

            // Same height as the poster so the plot can flex in the middle and the actions stay put.
            Column(Modifier.fillMaxHeight().weight(1f), verticalArrangement = Arrangement.Center) {
                // Always text. TMDB "logos" are brand marks as often as titles — the Avengers "A"
                // tells a 10-foot viewer nothing — so the title is never rendered as artwork.
                Text(
                    details.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (details.metaLine.isNotBlank() || details.qualityTags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (details.metaLine.isNotBlank()) {
                            Text(details.metaLine, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
                        }
                        // Quality/source markers lifted out of the provider's name. They sit with the
                        // rating rather than in the title, which is what lets the title read clean
                        // without throwing the information away.
                        details.qualityTags.forEach { tag -> QualityChip(tag) }
                    }
                }
                if (details.genres.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        details.genres.take(MAX_GENRES).forEach { genre ->
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(genre, style = MaterialTheme.typography.labelMedium, color = colors.onSurface)
                            }
                        }
                    }
                }
                resumeLabel?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, style = MaterialTheme.typography.labelLarge, color = colors.primary)
                }
                details.plot?.takeIf { it.isNotBlank() }?.let { plot ->
                    Spacer(Modifier.height(16.dp))
                    Text(
                        plot,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurface.copy(alpha = 0.92f),
                        // The one elastic element. With everything fixed, the rail's share overflowed
                        // the column and Compose took it out of the last child — the round action
                        // buttons flattened into slivers.
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = if (railFocused) PLOT_MAX_LINES_RAIL_UP else PLOT_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Cast is the first thing to go when the rail rises — it is the least load-bearing
                // line here, and dropping it is what buys the vertical room.
                if (details.cast.isNotEmpty() && !railFocused) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.content_media_cast),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        details.cast.take(MAX_CAST).joinToString(CAST_SEPARATOR) { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface.copy(alpha = 0.86f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(24.dp))
                Row(
                    // Down from *any* action enters the rail, at its first poster.
                    modifier = Modifier
                        .focusGroup()
                        .then(if (hasRail) Modifier.focusProperties { down = railEntry } else Modifier),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Resume and Play are one slot, never two: offering both as peers made the
                    // common case ("carry on") compete with the rare one for the first focus.
                    CircleAction(
                        icon = OwnTVIcon.PLAY,
                        primary = true,
                        modifier = Modifier.focusRequester(primaryAction),
                        onClick = { if (resumeLabel != null) onResume() else onPlay() },
                    )
                    trailerKey?.let { key ->
                        CircleAction(icon = OwnTVIcon.VIDEO, onClick = { onPlayTrailer(key) })
                    }
                    CircleAction(
                        icon = if (isFavorite) OwnTVIcon.FAVORITE else OwnTVIcon.STAR,
                        onClick = onToggleFavorite,
                    )
                    if (canDownload) CircleAction(icon = OwnTVIcon.DOWNLOADS, onClick = onDownload)
                }
            }
        }

        if (hasRail) {
            SimilarRail(
                items = similar.items,
                posterWidth = railPoster,
                entry = railEntry,
                onFocusedChange = { railFocused = it },
                onNeedMore = onNeedMoreSimilar,
                onClick = onSimilarClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(railHeight)
                    // Up from any poster returns to Play/Resume, never to whichever action happens
                    // to sit above that column.
                    .focusProperties { up = primaryAction },
            )
        }
    }
}

/**
 * Posters only, no captions — a title under each one turns a glance into a reading exercise, and the
 * poster already is the title.
 *
 * LazyRow, not the fixed row this replaced: the list pages in from TMDB as it is scrolled, so its
 * length is not known when it is composed.
 *
 * Both obvious answers to entry focus are wrong: aiming `down` at the row's focus group lets the
 * framework pick geometrically (the tenth poster, from the Download button), and pinning [entry] to
 * item 0 dies the moment LazyRow disposes it. So the row rewinds whenever focus leaves it — item 0 is
 * then always composed while unfocused, and every entry lands on the first poster.
 */
@Composable
private fun SimilarRail(
    items: List<MovieViewModel.SimilarItem>,
    posterWidth: androidx.compose.ui.unit.Dp,
    entry: FocusRequester,
    onFocusedChange: (Boolean) -> Unit,
    onNeedMore: () -> Unit,
    onClick: (MovieViewModel.SimilarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Ask for the next page before the last poster is reached, so scrolling never stops on a gap.
    LaunchedEffect(listState, items.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last -> if (last >= 0 && last >= items.size - RAIL_PREFETCH_DISTANCE) onNeedMore() }
    }

    Box(
        modifier = modifier
            .onFocusChanged { state ->
                onFocusedChange(state.hasFocus)
                if (!state.hasFocus && listState.firstVisibleItemIndex != 0) {
                    scope.launch { listState.scrollToItem(0) }
                }
            }
            .focusGroup()
            .background(
                // Reads as rising over the hero: the scrim deepens downward and fades into the artwork.
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))),
            ),
        contentAlignment = Alignment.BottomStart,
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(items, key = { _, item -> item.tmdbId }) { index, item ->
                FocusableSurface(
                    onClick = { onClick(item) },
                    modifier = Modifier
                        .then(if (index == 0) Modifier.focusRequester(entry) else Modifier)
                        .width(posterWidth)
                        .height(posterWidth * POSTER_ASPECT),
                    shape = RoundedCornerShape(8.dp),
                    focusedScale = 1.10f,
                ) { _ ->
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    )
                }
            }
        }
    }
}

/**
 * A quality marker beside the rating.
 *
 * Premium resolutions get the treatment a disc case gives them: black on yellow, bold. Everything
 * else stays a quiet grey chip — badging "WEB-DL" as loudly would make the loud one meaningless.
 */
@Composable
private fun QualityChip(tag: String) {
    val colors = OwnTVTheme.colors
    val headline = TitleNormalizer.isHeadlineTag(tag)
    Box(
        Modifier
            .clip(RoundedCornerShape(if (headline) 4.dp else 6.dp))
            .background(if (headline) HEADLINE_TAG_BACKGROUND else Color.White.copy(alpha = 0.18f))
            .padding(horizontal = if (headline) 7.dp else 8.dp, vertical = 3.dp),
    ) {
        Text(
            tag,
            style = MaterialTheme.typography.labelSmall,
            color = if (headline) Color.Black else colors.onSurface,
            fontWeight = if (headline) FontWeight.Black else FontWeight.Normal,
        )
    }
}

@Composable
private fun CircleAction(
    icon: OwnTVIcon,
    onClick: () -> Unit,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    FocusableSurface(
        onClick = onClick,
        modifier = modifier.size(if (primary) 62.dp else 54.dp),
        shape = CircleShape,
        unfocusedContainerColor = if (primary) colors.primary else Color.White.copy(alpha = 0.16f),
        focusedContainerColor = if (primary) colors.primary else Color.White.copy(alpha = 0.30f),
        focusedScale = 1.08f,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            OwnTVIconGraphic(
                icon = icon,
                tint = if (primary) colors.onPrimary else colors.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Cap only — the poster is sized from the height the row actually has. */
private val POSTER_MAX_HEIGHT = 360.dp
private const val MAX_GENRES = 4
private const val MAX_CAST = 6
private const val PLOT_MAX_LINES = 4
private const val PLOT_MAX_LINES_RAIL_UP = 2
private const val CAST_SEPARATOR = "   "

/** Standard 2:3 poster. */
private const val POSTER_ASPECT = 1.5f

/** Rail heights. Modest on purpose: a 1080p TV is only 540dp tall, so these are already a third of it. */
private val RAIL_HEIGHT_PEEK = 138.dp
private val RAIL_HEIGHT_FOCUSED = 196.dp
private val RAIL_POSTER_WIDTH_PEEK = 76.dp
private val RAIL_POSTER_WIDTH_FOCUSED = 114.dp

/** Posters left ahead of the last visible one before the next page is requested. */
private const val RAIL_PREFETCH_DISTANCE = 6

/** Frames the page keeps re-claiming focus for its primary action after the film it shows changes. */
private const val FOCUS_CLAIM_FRAMES = 3

/** Fixed, not themed: this mimics the badge printed on a case, which is this yellow everywhere. */
private val HEADLINE_TAG_BACKGROUND = Color(0xFFFFC107)
