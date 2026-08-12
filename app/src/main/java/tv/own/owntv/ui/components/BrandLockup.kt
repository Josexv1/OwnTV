package tv.own.owntv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.ui.theme.AccentCyan
import tv.own.owntv.ui.theme.OwnTVTheme

/** Floor for [BrandLockup]'s shrink-to-fit wordmark; below this, ellipsis takes over instead. */
private const val BRAND_LOCKUP_MIN_TEXT_SP = 16f

/** Step the wordmark shrinks by on each overflow retry. */
private const val BRAND_LOCKUP_SHRINK_STEP_SP = 2f

/**
 * Theme-adaptive "OwnTV" wordmark. The provided logo asset has a near-white "Own" that vanishes on
 * AMOLED black, so the in-app lockup is drawn from brand tokens instead and stays legible on both
 * themes. The cyan play-mark and the "TV" accent are constant brand colors.
 *
 * [textSize] is a *maximum*, not a fixed size: the wordmark measures itself against its available
 * width and shrinks in [BRAND_LOCKUP_SHRINK_STEP_SP] steps until it fits on one line, flooring at
 * [BRAND_LOCKUP_MIN_TEXT_SP] with an ellipsis as the last-resort safety net. This makes the fit
 * guaranteed by measurement rather than tuned by eye per call site (wider Figtree metrics than the
 * previous Roboto broke a couple of fixed sizes).
 */
@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    markSize: Int = 36,
    textSize: Int = 26,
) {
    val colors = OwnTVTheme.colors
    val own = stringResource(R.string.brand_own)
    val tv = stringResource(R.string.brand_tv)
    var fittedTextSize by remember(own, tv, textSize) { mutableStateOf(textSize.sp) }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Rounded-square play mark
        val markShape = RoundedCornerShape(percent = 28)
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(markSize.dp)
                .clip(markShape)
                .background(colors.card)
                .border(2.dp, AccentCyan, markShape),
            contentAlignment = Alignment.Center,
        ) {
            OwnTVIcon(
                icon = OwnTVIcon.PLAY,
                tint = AccentCyan,
                filled = true,
                modifier = Modifier
                    .padding(start = (markSize * 0.06f).dp)
                    .size((markSize * 0.5f).dp),
            )
        }
        Text(
            text = buildAnnotatedString {
                withStyle(androidx.compose.ui.text.SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.Bold)) {
                    append(own)
                }
                withStyle(androidx.compose.ui.text.SpanStyle(color = AccentCyan, fontWeight = FontWeight.Bold)) {
                    append(tv)
                }
            },
            fontSize = fittedTextSize,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (result.didOverflowWidth && fittedTextSize.value > BRAND_LOCKUP_MIN_TEXT_SP) {
                    fittedTextSize = (fittedTextSize.value - BRAND_LOCKUP_SHRINK_STEP_SP)
                        .coerceAtLeast(BRAND_LOCKUP_MIN_TEXT_SP).sp
                }
            },
        )
    }
}
