package tv.own.owntv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * The browse middle-pane header: breadcrumb title + neutral count subtitle. One rhythm for
 * Live/Movies/Series; the subtitle is ALWAYS onSurfaceVariant (spec §3 — counts are information,
 * not state, so they never take accent).
 */
@Composable
fun CategoryHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
