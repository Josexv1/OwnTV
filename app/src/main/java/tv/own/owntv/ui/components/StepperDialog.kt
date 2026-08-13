package tv.own.owntv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.ui.theme.GlassSurface
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.theme.PopupFontTheme

/** A +/- stepper dialog for an integer value. */
@Composable
fun StepperDialog(
    title: String,
    value: Int,
    step: Int,
    min: Int,
    max: Int,
    format: @Composable (Int) -> String,
    onSet: (Int) -> Unit,
    onDismiss: () -> Unit,
    onReset: (() -> Unit)? = null,
) {
    val colors = OwnTVTheme.colors
    val frPlus = remember { FocusRequester() }
    val frMinus = remember { FocusRequester() }
    val plusEnabled = value < max
    val minusEnabled = value > min
    // "+" is the natural landing spot, but at [max] it is disabled and so cannot take focus. Focus is
    // trapped inside this dialog, so silently failing to focus anything left the D-pad dead with only
    // Back working — the reported "+/- unreachable" at the top of the range. Land on whichever stepper
    // is usable, and hand focus over if the one holding it becomes disabled mid-adjustment.
    // A caller can remount this dialog within the same frame another composable unmounts (e.g. a
    // confirmation gate closing back into it) — requesting focus before the new node has completed a
    // layout pass silently fails and falls through the trap, so wait a frame first.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { (if (plusEnabled) frPlus else frMinus).requestFocus() }
    }
    LaunchedEffect(plusEnabled) { if (!plusEnabled && minusEnabled) runCatching { frMinus.requestFocus() } }
    LaunchedEffect(minusEnabled) { if (!minusEnabled && plusEnabled) runCatching { frPlus.requestFocus() } }
    BackHandler { onDismiss() }
    PopupFontTheme {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).trapAllFocusExit().focusGroup(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.dialogPanel(width = 360.dp, corner = 16.dp, padding = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StepBtn("–", enabled = minusEnabled, modifier = Modifier.focusRequester(frMinus)) { onSet((value - step).coerceAtLeast(min)) }
                Text(
                    format(value),
                    style = MaterialTheme.typography.titleMedium,
                    // Design contract: the value is the sole readout here, not an accent/action — neutral onSurface, not primary.
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                StepBtn("+", enabled = plusEnabled, modifier = Modifier.focusRequester(frPlus)) { onSet((value + step).coerceAtMost(max)) }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onReset != null) {
                    OwnTVButton(stringResource(R.string.common_reset), onClick = onReset, style = OwnTVButtonStyle.SECONDARY)
                }
                Spacer(Modifier.weight(1f))
                OwnTVButton(stringResource(R.string.common_done), onClick = onDismiss)
            }
        }
    }
    }
}

@Composable
private fun StepBtn(label: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = OwnTVTheme.colors
    FocusableSurface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        contentAlignment = Alignment.Center,
        surface = GlassSurface.DIALOGS,
    ) { _ -> Text(label, style = MaterialTheme.typography.titleMedium, color = if (enabled) colors.onSurface else colors.outline) }
}
