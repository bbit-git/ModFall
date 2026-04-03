package com.bigbangit.blockdrop.ui

import androidx.compose.ui.graphics.Color
import com.bigbangit.blockdrop.core.TetrominoType
import com.bigbangit.blockdrop.ui.theme.TetrominoI
import com.bigbangit.blockdrop.ui.theme.TetrominoJ
import com.bigbangit.blockdrop.ui.theme.TetrominoL
import com.bigbangit.blockdrop.ui.theme.TetrominoO
import com.bigbangit.blockdrop.ui.theme.TetrominoS
import com.bigbangit.blockdrop.ui.theme.TetrominoT
import com.bigbangit.blockdrop.ui.theme.TetrominoZ

data class BlockColors(
    val base: Color,
    val highlight: Color,
    val shadow: Color,
    val glow: Color,
    val borderLight: Color,
    val borderDark: Color,
)

object CandyPalette {
    private val colorsByType = mapOf(
        TetrominoType.I to derive(TetrominoI),
        TetrominoType.O to derive(TetrominoO),
        TetrominoType.T to derive(TetrominoT),
        TetrominoType.S to derive(TetrominoS),
        TetrominoType.Z to derive(TetrominoZ),
        TetrominoType.J to derive(TetrominoJ),
        TetrominoType.L to derive(TetrominoL),
    )

    fun colorsFor(type: TetrominoType): BlockColors = colorsByType.getValue(type)

    fun colorsFor(typeInt: Int): BlockColors? {
        val type = TetrominoType.entries.find { it.cellValue == typeInt } ?: return null
        return colorsFor(type)
    }

    private fun derive(base: Color): BlockColors {
        // Use simple RGB scaling instead of Android's ColorUtils to stay testable in local JVM
        fun Color.scale(factor: Float): Color {
            return Color(
                red = (red * factor).coerceIn(0f, 1f),
                green = (green * factor).coerceIn(0f, 1f),
                blue = (blue * factor).coerceIn(0f, 1f),
                alpha = alpha
            )
        }

        return BlockColors(
            base = base,
            highlight = base.scale(1.4f),
            shadow = base.scale(0.7f),
            glow = base.copy(alpha = 0.5f),
            borderLight = base.scale(1.2f),
            borderDark = base.scale(0.8f),
        )
    }
}
