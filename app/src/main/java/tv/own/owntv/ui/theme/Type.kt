package tv.own.owntv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

/**
 * Figtree scale tuned for the 10-foot experience (design spec 2026-08-12):
 * tracking tightens as size grows, weight caps at ExtraBold for the hero display,
 * and hierarchy is carried by size — components must not stack fontWeight overrides on top.
 */
private fun figtreeStyle(size: Int, line: Int, weight: FontWeight, trackingPercent: Float) = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = (trackingPercent / 100f).em,
)

val OwnTVTypography = Typography(
    displayLarge = figtreeStyle(44, 52, FontWeight.ExtraBold, -2f),
    displayMedium = figtreeStyle(36, 44, FontWeight.Bold, -1.5f),
    displaySmall = figtreeStyle(30, 38, FontWeight.Bold, -1f),
    headlineLarge = figtreeStyle(28, 36, FontWeight.Bold, -1f),
    headlineMedium = figtreeStyle(24, 32, FontWeight.SemiBold, -0.5f),
    headlineSmall = figtreeStyle(20, 28, FontWeight.SemiBold, 0f),
    titleLarge = figtreeStyle(22, 28, FontWeight.SemiBold, -0.5f),
    titleMedium = figtreeStyle(17, 24, FontWeight.SemiBold, 0f),
    titleSmall = figtreeStyle(15, 20, FontWeight.SemiBold, 0.1f),
    bodyLarge = figtreeStyle(16, 24, FontWeight.Normal, 0.15f),
    bodyMedium = figtreeStyle(14, 20, FontWeight.Normal, 0.15f),
    bodySmall = figtreeStyle(12, 16, FontWeight.Normal, 0.2f),
    labelLarge = figtreeStyle(14, 20, FontWeight.SemiBold, 0.2f),
    labelMedium = figtreeStyle(12, 16, FontWeight.SemiBold, 0.4f),
    labelSmall = figtreeStyle(11, 16, FontWeight.Medium, 0.5f),
)
