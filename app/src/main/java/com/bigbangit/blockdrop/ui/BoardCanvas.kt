package com.bigbangit.blockdrop.ui

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.bigbangit.blockdrop.core.GameConstants
import com.bigbangit.blockdrop.core.TetrominoType
import com.bigbangit.blockdrop.ui.model.GameUiModel
import com.bigbangit.blockdrop.ui.theme.AppBackgroundCenter
import com.bigbangit.blockdrop.ui.theme.AppBackgroundEdge
import com.bigbangit.blockdrop.ui.theme.BoardBackground
import com.bigbangit.blockdrop.ui.theme.GridLineColor

@Composable
fun BoardCanvas(
    uiModel: GameUiModel,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .aspectRatio(GameConstants.BOARD_WIDTH.toFloat() / GameConstants.BOARD_HEIGHT)
            .fillMaxSize(),
    ) {
        val cellWidth = size.width / GameConstants.BOARD_WIDTH
        val cellHeight = size.height / GameConstants.BOARD_HEIGHT

        // 1. Draw Board Background
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(AppBackgroundCenter, BoardBackground),
                center = center,
                radius = size.maxDimension / 2f
            )
        )

        // 2. Draw Subtle Grid
        for (x in 0..GameConstants.BOARD_WIDTH) {
            drawLine(
                color = GridLineColor,
                start = Offset(x * cellWidth, 0f),
                end = Offset(x * cellWidth, size.height),
                strokeWidth = 1f
            )
        }
        for (y in 0..GameConstants.BOARD_HEIGHT) {
            drawLine(
                color = GridLineColor,
                start = Offset(0f, y * cellHeight),
                end = Offset(size.width, y * cellHeight),
                strokeWidth = 1f
            )
        }

        // 3. Draw Ghost Piece (outline only)
        uiModel.ghostCells.forEach { cell ->
            val left = cell.x * cellWidth
            val top = (GameConstants.BOARD_HEIGHT - 1 - cell.y) * cellHeight
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(left + 2f, top + 2f),
                size = Size(cellWidth - 4f, cellHeight - 4f),
                cornerRadius = CornerRadius(cellWidth * 0.15f),
                style = Stroke(width = 2f)
            )
        }

        // 4. Draw Locked Blocks
        for (y in 0 until GameConstants.BOARD_HEIGHT) {
            for (x in 0 until GameConstants.BOARD_WIDTH) {
                val cellValue = uiModel.board.cells[y][x]
                if (cellValue != 0) {
                    val colors = CandyPalette.colorsFor(cellValue)
                    if (colors != null) {
                        drawCandyBlock(x, y, cellWidth, cellHeight, colors)
                    }
                }
            }
        }

        // 5. Draw Active Piece
        uiModel.activePiece?.let { piece ->
            val colors = CandyPalette.colorsFor(piece.type)
            piece.cells.forEach { cell ->
                // Only draw if within visible board
                if (cell.y < GameConstants.BOARD_HEIGHT) {
                    drawCandyBlock(cell.x, cell.y, cellWidth, cellHeight, colors)
                }
            }
        }
        
        // 6. Board Border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.1f),
            size = size,
            cornerRadius = CornerRadius(8f),
            style = Stroke(width = 4f)
        )
    }
}

private fun DrawScope.drawCandyBlock(
    x: Int,
    y: Int,
    cellWidth: Float,
    cellHeight: Float,
    colors: BlockColors
) {
    val left = x * cellWidth
    val top = (GameConstants.BOARD_HEIGHT - 1 - y) * cellHeight
    val cornerRadius = CornerRadius(cellWidth * 0.15f)

    // A. Outer Glow (Bloom)
    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            color = colors.glow.toArgb()
            maskFilter = BlurMaskFilter(cellWidth * 0.2f, BlurMaskFilter.Blur.OUTER)
        }
        canvas.nativeCanvas.drawRoundRect(
            left + 2f, top + 2f, left + cellWidth - 2f, top + cellHeight - 2f,
            cornerRadius.x, cornerRadius.y,
            paint
        )
    }

    // B. Base Fill
    drawRoundRect(
        color = colors.base,
        topLeft = Offset(left + 2f, top + 2f),
        size = Size(cellWidth - 4f, cellHeight - 4f),
        cornerRadius = cornerRadius
    )

    // C. Inner Gradient (Convex effect)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(colors.highlight, colors.shadow),
            startY = top + 2f,
            endY = top + cellHeight - 2f
        ),
        topLeft = Offset(left + 2f, top + 2f),
        size = Size(cellWidth - 4f, cellHeight - 4f),
        cornerRadius = cornerRadius,
        alpha = 0.6f
    )

    // D. Top-Left Shine
    drawOval(
        color = Color.White.copy(alpha = 0.4f),
        topLeft = Offset(left + cellWidth * 0.15f, top + cellHeight * 0.15f),
        size = Size(cellWidth * 0.3f, cellHeight * 0.2f)
    )

    // E. Edge Bevel (Top-Left Light)
    drawRoundRect(
        color = colors.borderLight,
        topLeft = Offset(left + 2f, top + 2f),
        size = Size(cellWidth - 4f, cellHeight - 4f),
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.5f),
        alpha = 0.8f
    )

    // F. Edge Bevel (Bottom-Right Dark)
    drawRoundRect(
        color = colors.borderDark,
        topLeft = Offset(left + 3.5f, top + 3.5f),
        size = Size(cellWidth - 5.5f, cellHeight - 5.5f),
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.5f),
        alpha = 0.6f
    )
}
