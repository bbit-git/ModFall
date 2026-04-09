package com.bigbangit.blockdrop.ui

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
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
import com.bigbangit.blockdrop.core.CellOffset
import com.bigbangit.blockdrop.core.GameConstants
import com.bigbangit.blockdrop.core.RotationState
import com.bigbangit.blockdrop.core.TetrominoShapes
import com.bigbangit.blockdrop.core.TetrominoType
import com.bigbangit.blockdrop.ui.model.CelebrationType
import com.bigbangit.blockdrop.ui.model.GameUiModel

@Composable
fun BoardCanvas(
    uiModel: GameUiModel,
    heldPiece: TetrominoType?,
    canHold: Boolean,
    nextPieces: List<TetrominoType>,
    modifier: Modifier = Modifier,
) {
    val lineClearAlpha = remember { Animatable(0f) }
    val placementFlashAlpha = remember { Animatable(0f) }
    val levelUpFlashAlpha = remember { Animatable(0f) }
    val celebrationAlpha = remember { Animatable(0f) }
    var celebrationType by remember { mutableStateOf<CelebrationType?>(null) }

    LaunchedEffect(uiModel.lineClearAnimationKey) {
        if (uiModel.lineClearAnimationKey == 0) return@LaunchedEffect
        lineClearAlpha.snapTo(0.78f)
        lineClearAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 280, easing = LinearEasing),
        )
    }

    LaunchedEffect(uiModel.placementFlashAnimationKey) {
        if (uiModel.placementFlashAnimationKey == 0) return@LaunchedEffect
        placementFlashAlpha.snapTo(0.9f)
        placementFlashAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        )
    }

    LaunchedEffect(uiModel.levelUpAnimationKey) {
        if (uiModel.levelUpAnimationKey == 0) return@LaunchedEffect
        levelUpFlashAlpha.snapTo(0.55f)
        levelUpFlashAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 420, easing = LinearEasing),
        )
    }

    LaunchedEffect(uiModel.celebrationAnimationKey) {
        if (uiModel.celebrationAnimationKey == 0) return@LaunchedEffect
        celebrationType = uiModel.celebrationType
        celebrationAlpha.snapTo(0f)
        celebrationAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        )
        celebrationAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 650, easing = LinearEasing),
        )
        celebrationType = null
    }

    Canvas(
        modifier = modifier
            .aspectRatio(GameConstants.BOARD_WIDTH.toFloat() / GameConstants.BOARD_HEIGHT)
            .fillMaxSize(),
    ) {
        val cellWidth = size.width / GameConstants.BOARD_WIDTH
        val cellHeight = size.height / GameConstants.BOARD_HEIGHT

        // 1. Board Background
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    GameUiTokens.BackgroundCenter,
                    GameUiTokens.BackgroundDark,
                ),
                center = center,
                radius = size.maxDimension / 2f,
            ),
        )

        // 2. Preview pieces drawn UNDER the grid (part of background layer)
        drawHoldPreview(heldPiece, canHold, cellWidth, cellHeight)
        drawNextPreview(nextPieces, cellWidth, cellHeight)

        // 3. Subtle Grid (drawn ON TOP of previews)
        val gridColor = GameUiTokens.GridLine.copy(alpha = GameUiTokens.GridLineAlpha)
        for (x in 0..GameConstants.BOARD_WIDTH) {
            drawLine(
                color = gridColor,
                start = Offset(x * cellWidth, 0f),
                end = Offset(x * cellWidth, size.height),
                strokeWidth = 0.5f,
            )
        }
        for (y in 0..GameConstants.BOARD_HEIGHT) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y * cellHeight),
                end = Offset(size.width, y * cellHeight),
                strokeWidth = 0.5f,
            )
        }

        // 4. Ghost Piece
        uiModel.ghostCells.forEach { cell ->
            val left = cell.x * cellWidth
            val top = (GameConstants.BOARD_HEIGHT - 1 - cell.y) * cellHeight
            val inset = 2f
            drawRoundRect(
                color = Color.White.copy(alpha = GameUiTokens.GhostFillAlpha),
                topLeft = Offset(left + inset, top + inset),
                size = Size(cellWidth - inset * 2, cellHeight - inset * 2),
                cornerRadius = CornerRadius(cellWidth * 0.15f),
            )
            drawRoundRect(
                color = GameUiTokens.GhostStroke.copy(alpha = GameUiTokens.GhostStrokeAlpha),
                topLeft = Offset(left + inset, top + inset),
                size = Size(cellWidth - inset * 2, cellHeight - inset * 2),
                cornerRadius = CornerRadius(cellWidth * 0.15f),
                style = Stroke(width = GameUiTokens.GhostStrokeWidth),
            )
        }

        // 5. Locked Blocks (settled — weaker glow)
        for (y in 0 until GameConstants.BOARD_HEIGHT) {
            for (x in 0 until GameConstants.BOARD_WIDTH) {
                val cellValue = uiModel.board.cells[y][x]
                if (cellValue != 0) {
                    val colors = CandyPalette.colorsFor(cellValue)
                    if (colors != null) {
                        drawCandyBlock(
                            x, y, cellWidth, cellHeight, colors,
                            glowAlpha = GameUiTokens.SettledBlockGlowAlpha,
                            blurFraction = GameUiTokens.SettledBlockBlurRadius,
                        )
                    }
                }
            }
        }

        // 6. Active Piece (stronger glow)
        uiModel.activePiece?.let { piece ->
            val colors = CandyPalette.colorsFor(piece.type)
            piece.cells.forEach { cell ->
                if (cell.y < GameConstants.BOARD_HEIGHT) {
                    drawCandyBlock(
                        cell.x, cell.y, cellWidth, cellHeight, colors,
                        glowAlpha = GameUiTokens.ActiveBlockGlowAlpha,
                        blurFraction = GameUiTokens.ActiveBlockBlurRadius,
                    )
                }
            }
        }

        // 7. Piece placement flash
        if (placementFlashAlpha.value > 0f) {
            uiModel.placementFlashCells.forEach { cell ->
                val left = cell.x * cellWidth
                val top = (GameConstants.BOARD_HEIGHT - 1 - cell.y) * cellHeight
                drawRoundRect(
                    color = Color.White.copy(alpha = placementFlashAlpha.value),
                    topLeft = Offset(left + 1f, top + 1f),
                    size = Size(cellWidth - 2f, cellHeight - 2f),
                    cornerRadius = CornerRadius(cellWidth * 0.15f),
                )
            }
        }

        // 8. Line clear flash
        if (lineClearAlpha.value > 0f) {
            drawRect(
                color = Color.White.copy(alpha = lineClearAlpha.value * 0.42f),
                size = size,
            )
        }

        // 9. Level-up flash
        if (levelUpFlashAlpha.value > 0f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFEBD6FF).copy(alpha = levelUpFlashAlpha.value),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.maxDimension * 0.6f,
                ),
            )
        }

        // 10. Celebration overlay
        if (celebrationAlpha.value > 0f && celebrationType != null) {
            drawCelebration(
                celebrationType = celebrationType!!,
                alpha = celebrationAlpha.value,
            )
        }

        // 11. Glowing frame
        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                color = GameUiTokens.FrameGlow.copy(alpha = GameUiTokens.FrameGlowAlpha).toArgb()
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = GameUiTokens.FrameStrokeWidth * 3f
                maskFilter = BlurMaskFilter(GameUiTokens.FrameGlowRadius, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                GameUiTokens.FrameCornerRadius, GameUiTokens.FrameCornerRadius,
                paint,
            )
        }
        drawRoundRect(
            color = GameUiTokens.FrameStroke.copy(alpha = GameUiTokens.FrameStrokeAlpha),
            size = size,
            cornerRadius = CornerRadius(GameUiTokens.FrameCornerRadius),
            style = Stroke(width = GameUiTokens.FrameStrokeWidth),
        )
    }
}

// ─── Preview rendering (drawn under the grid) ──────────────────────────────────

private fun DrawScope.drawHoldPreview(
    type: TetrominoType?,
    canHold: Boolean,
    cellWidth: Float,
    cellHeight: Float,
) {
    val previewCellSize = cellWidth * 0.48f
    val boxPadding = cellWidth * 0.3f
    val boxInnerW = previewCellSize * 4f
    val boxInnerH = previewCellSize * 2f
    val boxWidth = boxInnerW + boxPadding * 2f
    val boxHeight = boxInnerH + boxPadding * 2f
    val boxLeft = cellWidth * 0.2f
    val boxTop = cellHeight * 0.2f

    // Proper rectangle border around hold area
    val borderAlpha = if (canHold) 0.12f else 0.06f
    drawRect(
        color = Color.White.copy(alpha = borderAlpha),
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxWidth, boxHeight),
        style = Stroke(width = 1f),
    )

    if (type == null) return
    val alpha = if (canHold) 0.28f else 0.12f
    val cells = spawnCells(type)

    val pieceWidth = (cells.maxOf { it.x } - cells.minOf { it.x } + 1)
    val pieceHeight = (cells.maxOf { it.y } - cells.minOf { it.y } + 1)
    val originX = boxLeft + (boxWidth - pieceWidth * previewCellSize) / 2f
    val originY = boxTop + (boxHeight - pieceHeight * previewCellSize) / 2f

    drawPreviewPiece(cells, originX, originY, previewCellSize, type, alpha)
}

private fun DrawScope.drawNextPreview(
    pieces: List<TetrominoType>,
    cellWidth: Float,
    cellHeight: Float,
) {
    if (pieces.isEmpty()) return
    val previewCellSize = cellWidth * 0.30f

    // Fixed-width container: 4 mini-cells wide, right-aligned in board
    val containerWidth = previewCellSize * 4f
    val containerRight = size.width - cellWidth * 0.25f
    val containerLeft = containerRight - containerWidth
    val containerCenterX = containerLeft + containerWidth / 2f

    // Each slot is a fixed 3-cell height regardless of piece shape
    val slotHeight = previewCellSize * 3f
    val slotGap = previewCellSize * 1.6f
    var slotTop = cellHeight * 0.4f

    pieces.forEachIndexed { index, type ->
        val cells = spawnCells(type)
        val minX = cells.minOf { it.x }
        val maxX = cells.maxOf { it.x }
        val minY = cells.minOf { it.y }
        val maxY = cells.maxOf { it.y }
        val pieceWidth = (maxX - minX + 1) * previewCellSize
        val pieceHeight = (maxY - minY + 1) * previewCellSize

        // Center piece in the fixed-width slot
        val originX = containerCenterX - pieceWidth / 2f
        val originY = slotTop + (slotHeight - pieceHeight) / 2f
        val alpha = if (index == 0) 0.28f else 0.15f

        drawPreviewPiece(cells, originX, originY, previewCellSize, type, alpha)

        slotTop += slotHeight + slotGap
    }
}

private fun DrawScope.drawPreviewPiece(
    cells: List<CellOffset>,
    originX: Float,
    originY: Float,
    cellSize: Float,
    type: TetrominoType,
    alpha: Float,
) {
    val colors = CandyPalette.colorsFor(type)
    val minX = cells.minOf { it.x }
    val minY = cells.minOf { it.y }

    cells.forEach { cell ->
        val px = originX + (cell.x - minX) * cellSize
        // Invert Y so higher Y values go upward visually
        val maxY = cells.maxOf { it.y }
        val py = originY + (maxY - cell.y) * cellSize
        val cornerRadius = CornerRadius(cellSize * 0.15f)
        val inset = 1f

        // Subtle base fill
        drawRoundRect(
            color = colors.base.copy(alpha = alpha),
            topLeft = Offset(px + inset, py + inset),
            size = Size(cellSize - inset * 2, cellSize - inset * 2),
            cornerRadius = cornerRadius,
        )
        // Light top highlight
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    colors.highlight.copy(alpha = alpha * 0.5f),
                    colors.shadow.copy(alpha = alpha * 0.3f),
                ),
                startY = py + inset,
                endY = py + cellSize - inset,
            ),
            topLeft = Offset(px + inset, py + inset),
            size = Size(cellSize - inset * 2, cellSize - inset * 2),
            cornerRadius = cornerRadius,
        )
    }
}

private fun spawnCells(type: TetrominoType): List<CellOffset> {
    val definition = TetrominoShapes.definitionFor(type)
    return definition.rotations.getValue(RotationState.Spawn).cells
}

// ─── Block rendering ────────────────────────────────────────────────────────────

private fun DrawScope.drawCandyBlock(
    x: Int,
    y: Int,
    cellWidth: Float,
    cellHeight: Float,
    colors: BlockColors,
    glowAlpha: Float = 0.5f,
    blurFraction: Float = 0.2f,
) {
    val left = x * cellWidth
    val top = (GameConstants.BOARD_HEIGHT - 1 - y) * cellHeight
    val cornerRadius = CornerRadius(cellWidth * 0.15f)

    // A. Outer Glow
    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            color = colors.glow.copy(alpha = glowAlpha).toArgb()
            maskFilter = BlurMaskFilter(cellWidth * blurFraction, BlurMaskFilter.Blur.OUTER)
        }
        canvas.nativeCanvas.drawRoundRect(
            left + 2f, top + 2f, left + cellWidth - 2f, top + cellHeight - 2f,
            cornerRadius.x, cornerRadius.y,
            paint,
        )
    }

    // B. Base Fill
    drawRoundRect(
        color = colors.base,
        topLeft = Offset(left + 2f, top + 2f),
        size = Size(cellWidth - 4f, cellHeight - 4f),
        cornerRadius = cornerRadius,
    )

    // C. Inner Gradient (Convex effect)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(colors.highlight, colors.shadow),
            startY = top + 2f,
            endY = top + cellHeight - 2f,
        ),
        topLeft = Offset(left + 2f, top + 2f),
        size = Size(cellWidth - 4f, cellHeight - 4f),
        cornerRadius = cornerRadius,
        alpha = 0.6f,
    )

    // D. Top-Left Shine
    drawOval(
        color = Color.White.copy(alpha = 0.35f),
        topLeft = Offset(left + cellWidth * 0.15f, top + cellHeight * 0.12f),
        size = Size(cellWidth * 0.28f, cellHeight * 0.18f),
    )

    // E. Edge Bevel (Light)
    drawRoundRect(
        color = colors.borderLight,
        topLeft = Offset(left + 2f, top + 2f),
        size = Size(cellWidth - 4f, cellHeight - 4f),
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.2f),
        alpha = 0.7f,
    )
}

private fun DrawScope.drawCelebration(
    celebrationType: CelebrationType,
    alpha: Float,
) {
    val (overlayColor, label) = when (celebrationType) {
        CelebrationType.Tetris -> Color(0xFF7A8DFF) to "TETRIS"
        CelebrationType.TSpin -> Color(0xFFFF6FB5) to "T-SPIN"
        CelebrationType.AllClear -> Color(0xFF7BFFE1) to "ALL CLEAR"
    }

    drawRoundRect(
        color = overlayColor.copy(alpha = alpha * 0.22f),
        topLeft = Offset(size.width * 0.14f, size.height * 0.36f),
        size = Size(size.width * 0.72f, size.height * 0.16f),
        cornerRadius = CornerRadius(24f, 24f),
    )

    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = size.minDimension * 0.11f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
            color = Color.White.copy(alpha = alpha).toArgb()
            setShadowLayer(18f, 0f, 0f, overlayColor.copy(alpha = alpha).toArgb())
        }
        canvas.nativeCanvas.drawText(
            label,
            center.x,
            size.height * 0.47f,
            paint,
        )
    }
}
