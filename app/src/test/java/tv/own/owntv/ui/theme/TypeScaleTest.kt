package tv.own.owntv.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Locks the spec's scale: every style defined, Figtree, correct size/weight. */
class TypeScaleTest {
    @Test
    fun `all fifteen styles use Figtree`() {
        val t = OwnTVTypography
        listOf(
            t.displayLarge, t.displayMedium, t.displaySmall,
            t.headlineLarge, t.headlineMedium, t.headlineSmall,
            t.titleLarge, t.titleMedium, t.titleSmall,
            t.bodyLarge, t.bodyMedium, t.bodySmall,
            t.labelLarge, t.labelMedium, t.labelSmall,
        ).forEachIndexed { i, style ->
            assertEquals("style[$i] fontFamily", FigtreeFamily, style.fontFamily)
        }
    }

    @Test
    fun `key sizes and weights match the spec`() {
        val t = OwnTVTypography
        assertEquals(44.sp, t.displayLarge.fontSize)
        assertEquals(FontWeight.ExtraBold, t.displayLarge.fontWeight)
        assertEquals(28.sp, t.headlineLarge.fontSize)
        assertEquals(17.sp, t.titleMedium.fontSize)
        assertEquals(FontWeight.Normal, t.bodyLarge.fontWeight)
        assertEquals(11.sp, t.labelSmall.fontSize)
        assertEquals(FontWeight.Medium, t.labelSmall.fontWeight)
    }
}
