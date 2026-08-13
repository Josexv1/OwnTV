package tv.own.owntv.features.home

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.ui.theme.Dimens
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * Shared shelf-title header for Home rows. The white focus ring from [tv.own.owntv.ui.components.FocusableSurface]
 * is the sole focus signal for cards in these rows — this title never recolors on focus.
 */
@Composable
internal fun HomeRowHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = OwnTVTheme.colors.onSurface,
        modifier = modifier.padding(start = Dimens.HomeRowPaddingH),
    )
}
