package tv.own.owntv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.ui.theme.OwnTVTheme

/** One option row in a [MediaContextMenu]. The label arrives resolved (stringResource at the caller). */
data class MenuEntry(
    val label: String,
    val onClick: () -> Unit,
    val icon: OwnTVIcon? = null,
)

/**
 * The shared long-press context menu (Movies + Series browse). Owns the frame — scrim, focus trap,
 * long-press guard, auto-focus, Back-dismiss, dialog panel — while callers own the policy (which
 * entries appear, in what order). Per the dialog ruling, entries render SECONDARY and the single
 * close action renders PRIMARY.
 */
@Composable
fun MediaContextMenu(
    title: String,
    entries: List<MenuEntry>,
    onDismiss: () -> Unit,
    closeLabel: String,
) {
    val colors = OwnTVTheme.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BackHandler { onDismiss() }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))
            .trapAllFocusExit().focusGroup()
            .longPressMenuGuard(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.dialogPanel(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            entries.forEachIndexed { index, entry ->
                OwnTVButton(
                    entry.label,
                    onClick = entry.onClick,
                    style = OwnTVButtonStyle.SECONDARY,
                    icon = entry.icon,
                    modifier = if (index == 0) Modifier.fillMaxWidth().focusRequester(focus) else Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(4.dp))
            OwnTVButton(closeLabel, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
