package tv.own.owntv.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import tv.own.owntv.R

/**
 * Figtree — the brand sans (SIL OFL), a single variable file (weight axis 300–900).
 * Non-Latin scripts fall back to the platform Noto stack automatically.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun figtree(weight: FontWeight) = Font(
    R.font.figtree_variable,
    weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val FigtreeFamily = FontFamily(
    figtree(FontWeight.Normal),
    figtree(FontWeight.Medium),
    figtree(FontWeight.SemiBold),
    figtree(FontWeight.Bold),
    figtree(FontWeight.ExtraBold),
)
