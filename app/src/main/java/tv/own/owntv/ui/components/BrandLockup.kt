package tv.own.owntv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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

/**
 * Theme-adaptive "OwnTV" wordmark. The provided logo asset has a near-white "Own" that vanishes on
 * AMOLED black, so the in-app lockup is drawn from brand tokens instead and stays legible on both
 * themes. The cyan play-mark and the "TV" accent are constant brand colors.
 *
 * Horizontal (default, [vertical] = false): play-mark and wordmark side by side, mark size and text
 * size as given. Callers that place this in a wide container (full-screen empty states, dialogs,
 * setup pages) should use this path. The wordmark is single-line with an ellipsis safety net — it
 * doesn't wrap or shrink, so give it a container wide enough for [markSize] + [textSize] at your locale.
 *
 * Vertical ([vertical] = true): play-mark centered above the wordmark, for narrow panes (e.g. the
 * shell's idle preview pane) where the horizontal layout doesn't have room for both side by side.
 */
@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    markSize: Int = 36,
    textSize: Int = 26,
    vertical: Boolean = false,
) {
    val colors = OwnTVTheme.colors
    val own = stringResource(R.string.brand_own)
    val tv = stringResource(R.string.brand_tv)

    val mark: @Composable () -> Unit = {
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
    }
    val wordmark: @Composable () -> Unit = {
        Text(
            text = buildAnnotatedString {
                withStyle(androidx.compose.ui.text.SpanStyle(color = colors.textPrimary, fontWeight = if (vertical) FontWeight.SemiBold else FontWeight.Bold)) {
                    append(own)
                }
                withStyle(androidx.compose.ui.text.SpanStyle(color = AccentCyan, fontWeight = if (vertical) FontWeight.SemiBold else FontWeight.Bold)) {
                    append(tv)
                }
            },
            fontSize = textSize.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }

    if (vertical) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            mark()
            wordmark()
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            mark()
            wordmark()
        }
    }
}
