package tv.own.owntv.features.movies

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import tv.own.owntv.R
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
 * Phase 1: presentation and actions. The similar-titles rail, and making the genre chips and cast
 * names open in-library discovery, come next — the layout already reserves the room for both.
 */
@Composable
fun MovieCinematicDetail(
    details: MediaDetailsUi,
    resumeLabel: String?,
    isFavorite: Boolean,
    canDownload: Boolean,
    trailerKey: String?,
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
    // One frame before grabbing focus: the browse grid behind is still settling its own focus when
    // this composes, and a request made in the same frame is overwritten by it — the page opened
    // with nothing focused, so the D-pad drove the invisible grid instead of the buttons.
    LaunchedEffect(details.title) {
        withFrameNanos { }
        runCatching { primaryAction.requestFocus() }
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
            modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            details.posterUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(POSTER_WIDTH).height(POSTER_HEIGHT).clip(RoundedCornerShape(12.dp)),
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
                if (details.metaLine.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(details.metaLine, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
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
                        maxLines = PLOT_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (details.cast.isNotEmpty()) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
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

private val POSTER_WIDTH = 240.dp
private val POSTER_HEIGHT = 360.dp
private const val MAX_GENRES = 4
private const val MAX_CAST = 6
private const val PLOT_MAX_LINES = 4
private const val CAST_SEPARATOR = "   "
