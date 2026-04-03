package com.bigbangit.blockdrop.ui

import androidx.compose.ui.graphics.Color
import com.bigbangit.blockdrop.core.TetrominoType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CandyPaletteTest {
    @Test
    fun colorsForReturnsExpectedBasePalette() {
        val expectedBaseColors = mapOf(
            TetrominoType.I to Color(0xFF29BFFF),
            TetrominoType.O to Color(0xFFFFD633),
            TetrominoType.T to Color(0xFFCC44FF),
            TetrominoType.S to Color(0xFF44DD44),
            TetrominoType.Z to Color(0xFFFF3333),
            TetrominoType.J to Color(0xFF3366FF),
            TetrominoType.L to Color(0xFFFF8800),
        )

        expectedBaseColors.forEach { (type, expected) ->
            assertEquals(expected, CandyPalette.colorsFor(type).base)
        }
    }

    @Test
    fun colorsForProducesConsistentDerivedShades() {
        TetrominoType.entries.forEach { type ->
            val colors = CandyPalette.colorsFor(type)

            assertEquals(0.5f, colors.glow.alpha, 0.01f)
            assertTrue(luminance(colors.highlight) > luminance(colors.base))
            assertTrue(luminance(colors.shadow) < luminance(colors.base))
            assertTrue(luminance(colors.borderLight) > luminance(colors.base))
            assertTrue(luminance(colors.borderDark) < luminance(colors.base))
        }
    }

    private fun luminance(color: Color): Float {
        return (0.2126f * color.red) + (0.7152f * color.green) + (0.0722f * color.blue)
    }
}
