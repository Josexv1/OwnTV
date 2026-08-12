package tv.own.owntv.features.setup

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.ProvideTextStyle
import tv.own.owntv.ui.components.BrandLockup
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.theme.OwnTVTypography

/**
 * The animated ambient backdrop shared by every first-run setup page: a soft radial glow plus two
 * concentric rings (the outer one gently breathing) behind the page content. Moved here from
 * SetupWizard.kt so [SetupScaffold] and the legacy `MainSetupPage` frame can both use it; visibility
 * is `internal` (not `private`) because Kotlin top-level `private` is file-scoped and `MainSetupPage`
 * still calls this from SetupWizard.kt until it is retired.
 */
@Composable
internal fun SetupAmbientBackdrop() {
    val primary = OwnTVTheme.colors.primary
    val transition = rememberInfiniteTransition()
    val ringScale by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.5f, size.height * 0.48f)
        val glowRadius = size.minDimension * 0.46f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = 0.14f),
                    primary.copy(alpha = 0.05f),
                    Color.Transparent,
                ),
                center = center,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = center,
        )
        // The ambient rings are the screen's one atmospheric signature, so give them enough alpha to
        // actually read on a TV — the old 0.075 was invisible on-panel. Two concentric strokes (the
        // outer one breathing) add depth without pulling focus from the wordmark.
        drawCircle(
            color = primary.copy(alpha = 0.20f),
            radius = size.minDimension * 0.37f * ringScale,
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
        )
        drawCircle(
            color = primary.copy(alpha = 0.09f),
            radius = size.minDimension * 0.28f,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

/**
 * The shared frame for every first-run setup page: ambient backdrop, an optional small OwnTV badge,
 * a hero title, an optional subtitle, and the page's controls — all at true dp/sp size (no pixel
 * scaling; long pages scroll). Title/subtitle are slots that inherit a canonical style provided here,
 * so call sites carry no `style =` and cannot drift.
 */
@Composable
fun SetupScaffold(
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    showLogoBadge: Boolean = true,
    showBackdrop: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OwnTVTheme.colors
    Box(Modifier.fillMaxSize()) {
        if (showBackdrop) {
            SetupAmbientBackdrop()
        }
        Box(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showLogoBadge) {
                    BrandLockup(markSize = 40, textSize = 30)
                    Spacer(Modifier.height(22.dp))
                }
                // Title slot: provide the canonical hero style + on-surface content colour so a bare
                // Text() in the slot needs no styling.
                ProvideTextStyle(OwnTVTypography.headlineLarge) {
                    CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
                        title()
                    }
                }
                if (subtitle != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.widthIn(max = 620.dp)) {
                        ProvideTextStyle(
                            OwnTVTypography.bodyLarge.copy(textAlign = TextAlign.Center),
                        ) {
                            CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant) {
                                subtitle()
                            }
                        }
                    }
                }
                Spacer(Modifier.height(44.dp))
                content()
            }
        }
    }
}
