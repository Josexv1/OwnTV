package tv.own.owntv.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import tv.own.owntv.R
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * In-app YouTube trailer player (plan §7.3 / U4): a WebView-backed IFrame player
 * (`android-youtube-player`) under a Compose overlay carrying the four controls a trailer needs —
 * back 5s, play/pause, forward 5s, exit — plus a progress bar driven by the player's
 * currentSecond/duration callbacks. Focus stays entirely in the Compose overlay; the WebView is
 * treated as a pure video surface, which sidesteps the classic "iframe steals D-pad focus" problem.
 *
 * Buttons rather than a hidden key handler: on a 10-foot screen a control you cannot see is a
 * control that does not exist. Left/Right move between them; the remote's media keys still work.
 *
 * Graceful fallback (required by the plan): if the WebView is missing/ancient or the video errors,
 * we hand off to an external "Open in YouTube" intent and exit — the button always does *something*.
 */
@Composable
fun TrailerPlayerScreen(videoKey: String, onExit: () -> Unit) {
    val context = LocalContext.current
    val colors = OwnTVTheme.colors

    var currentSec by remember { mutableFloatStateOf(0f) }
    var durationSec by remember { mutableFloatStateOf(0f) }
    var failed by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }

    val playPauseFocus = remember { FocusRequester() }
    // A frame late, or the row is not attached yet and focus settles on Exit — under the first OK.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { playPauseFocus.requestFocus() }
    }
    BackHandler { onExit() }

    fun seekBy(delta: Float) {
        val p = player ?: return
        val target = currentSec + delta
        p.seekTo(
            when {
                target < 0f -> 0f
                durationSec > 0f -> target.coerceAtMost(durationSec - 1f)
                else -> target
            },
        )
    }

    fun togglePlay() {
        val p = player ?: return
        if (playing) p.pause() else p.play()
    }

    // Some no-name boxes ship without a usable System WebView — constructing the player view itself
    // can throw there, so treat construction failure like a playback error (external fallback).
    val playerView = remember {
        runCatching {
            YouTubePlayerView(context).apply {
                enableAutomaticInitialization = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                // Pure video surface: the Compose overlay owns all D-pad focus.
                isFocusable = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            }
        }.getOrNull()
    }

    if (playerView == null) {
        LaunchedEffect(Unit) { openInYouTube(context, videoKey); onExit() }
        return
    }

    LaunchedEffect(failed) {
        if (failed) { openInYouTube(context, videoKey); onExit() }
    }

    DisposableEffect(Unit) {
        val listener = object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                player = youTubePlayer
                youTubePlayer.loadVideo(videoKey, 0f)
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                currentSec = second
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                durationSec = duration
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                failed = true
            }

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                // The player's own state, not a local guess: a buffering stall must not read as paused.
                when (state) {
                    PlayerConstants.PlayerState.PLAYING -> playing = true
                    PlayerConstants.PlayerState.PAUSED -> playing = false
                    PlayerConstants.PlayerState.ENDED -> onExit()
                    else -> Unit
                }
            }
        }
        // Default IFrame options already hide YouTube's own chrome (controls=0) — our overlay is the only UI.
        runCatching { playerView.initialize(listener) }.onFailure { failed = true }
        onDispose { player = null; runCatching { playerView.release() } }
    }

    // Shortcut past the buttons. Left/Right are deliberately absent: they move between the controls.
    val onTransportKey: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = onKey@{ e ->
        if (e.type != KeyEventType.KeyDown || player == null) return@onKey false
        when (e.key) {
            Key.MediaPlayPause -> { togglePlay(); true }
            Key.MediaPlay -> { player?.play(); true }
            Key.MediaPause -> { player?.pause(); true }
            Key.MediaRewind -> { seekBy(-SEEK_STEP_SECONDS.toFloat()); true }
            Key.MediaFastForward -> { seekBy(SEEK_STEP_SECONDS.toFloat()); true }
            else -> false
        }
    }

    // Fullscreen and unclipped, unlike the windowed TMDB details popup this used to copy.
    //
    // The WebView only renders video on a hardware overlay when nothing forces it to be composited
    // into the app's own layer. A rounded clip, a fractional size and a scrim behind it all did, and
    // the result was that every decoded frame was copied through the GPU instead: playback filled the
    // log with "no buffers currently available in the reader queue" / "CopySharedImage: Source shared
    // image is not accessable" and dropped frames continuously. Decoding was never the problem — the
    // Realtek AV1 hardware decoder kept up throughout.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent(onTransportKey)
            .trapAllFocusExit()
            .focusGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(factory = { playerView }, modifier = Modifier.fillMaxSize())

            // Transport + progress + exit, bottom-aligned over the video.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
                    .focusGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // "-5 s" rather than ⏪: the arrow says "seek", the label says how far — which is the
                // only thing being decided.
                SeekButton(
                    pluralStringResource(R.plurals.player_trailer_seek_back, SEEK_STEP_SECONDS, SEEK_STEP_SECONDS),
                ) { seekBy(-SEEK_STEP_SECONDS.toFloat()) }
                TransportButton(
                    icon = if (playing) OwnTVIcon.PAUSE else OwnTVIcon.PLAY,
                    modifier = Modifier.focusRequester(playPauseFocus),
                    onClick = { togglePlay() },
                )
                SeekButton(
                    pluralStringResource(R.plurals.player_trailer_seek_forward, SEEK_STEP_SECONDS, SEEK_STEP_SECONDS),
                ) { seekBy(SEEK_STEP_SECONDS.toFloat()) }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                    ) {
                        val fraction = if (durationSec > 0f) (currentSec / durationSec).coerceIn(0f, 1f) else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .background(colors.primary),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.player_trailer_progress, formatSec(currentSec), formatSec(durationSec)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.width(120.dp),
                )
                OwnTVButton(
                    stringResource(R.string.player_trailer_exit),
                    onClick = onExit,
                    style = OwnTVButtonStyle.SECONDARY,
                )
            }
        }
    }
}

/** One round transport control. Sized for a 10-foot read, not for a mouse. */
@Composable
private fun TransportButton(icon: OwnTVIcon, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FocusableSurface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        unfocusedContainerColor = Color.White.copy(alpha = 0.16f),
        focusedContainerColor = Color.White.copy(alpha = 0.34f),
        focusedScale = 1.10f,
    ) { _ ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            OwnTVIcon(icon = icon, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

/** A labelled jump control. Wider than the round buttons because it carries its own distance. */
@Composable
private fun SeekButton(label: String, onClick: () -> Unit) {
    FocusableSurface(
        onClick = onClick,
        modifier = Modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        unfocusedContainerColor = Color.White.copy(alpha = 0.16f),
        focusedContainerColor = Color.White.copy(alpha = 0.34f),
        focusedScale = 1.10f,
    ) { _ ->
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

/** Seek step for one press of back/forward, in seconds — and the number those buttons show. */
private const val SEEK_STEP_SECONDS = 5

/** External fallback: YouTube app if installed, else any browser. Never throws. */
private fun openInYouTube(context: Context, videoKey: String) {
    val app = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoKey"))
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoKey"))
    runCatching { context.startActivity(app) }
        .recoverCatching { context.startActivity(web) }
}

@Composable
private fun formatSec(s: Float): String {
    val total = s.toInt().coerceAtLeast(0)
    val m = total / 60
    val sec = total % 60
    return stringResource(R.string.common_timestamp_minutes, m, sec)
}
