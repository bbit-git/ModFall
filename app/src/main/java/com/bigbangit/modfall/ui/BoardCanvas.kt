package com.bigbangit.modfall.ui

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import com.bigbangit.modfall.core.CellOffset
import com.bigbangit.modfall.core.GameConstants
import com.bigbangit.modfall.core.RotationState
import com.bigbangit.modfall.core.TetrominoShapes
import com.bigbangit.modfall.core.TetrominoType
import com.bigbangit.modfall.R
import com.bigbangit.modfall.ui.model.CelebrationType
import com.bigbangit.modfall.ui.model.BoardCell
import com.bigbangit.modfall.ui.model.GameUiModel
import com.bigbangit.modfall.ui.model.ParticleImpulseType
import com.bigbangit.modfall.ui.model.ParticleQuality
import kotlin.math.absoluteValue

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
    val celebrationProgress = remember { Animatable(1f) }
    val particleImpulseProgress = remember { Animatable(1f) }
    val hardDropBurstProgress = remember { Animatable(1f) }
    var celebrationType by remember { mutableStateOf<CelebrationType?>(null) }
    val ambientParticles = remember {
        ParticleQuality.entries.associateWith { quality ->
            val count = if (quality == ParticleQuality.High) 48 else 28
            List(count) { index ->
                AmbientParticleSpec(
                    xFactor = stableNoise(index, 17 + quality.ordinal * 29),
                    yFactor = stableNoise(index, 53 + quality.ordinal * 31),
                    radiusFactor = 0.38f + stableNoise(index, 91 + quality.ordinal * 37) * 2.25f,
                    alphaFactor = 0.18f + stableNoise(index, 127 + quality.ordinal * 41) * 0.55f,
                    driftFactor = (stableNoise(index, 173 + quality.ordinal * 43) - 0.5f) / 180f,
                    speedFactor = 0.35f + stableNoise(index, 211 + quality.ordinal * 47) * 1.35f,
                )
            }
        }
    }
    val particleDrift by rememberInfiniteTransition(label = "board-particles").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particle-drift",
    )

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
        celebrationProgress.snapTo(0f)
        celebrationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        )
        celebrationType = null
    }

    LaunchedEffect(uiModel.particleImpulseAnimationKey) {
        if (uiModel.particleImpulseAnimationKey == 0) return@LaunchedEffect
        particleImpulseProgress.snapTo(0f)
        particleImpulseProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        )
    }

    LaunchedEffect(uiModel.hardDropBurstAnimationKey) {
        if (uiModel.hardDropBurstAnimationKey == 0) return@LaunchedEffect
        hardDropBurstProgress.snapTo(0f)
        hardDropBurstProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
        )
    }

    Canvas(
        modifier = modifier
            .aspectRatio(GameConstants.BOARD_WIDTH.toFloat() / GameConstants.BOARD_HEIGHT)
            .fillMaxSize(),
    ) {
        val cellWidth = size.width / GameConstants.BOARD_WIDTH
        val cellHeight = size.height / GameConstants.BOARD_HEIGHT

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    GameUiTokens.BackgroundCenter,
                    GameUiTokens.BackgroundDark,
                ),
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    GameUiTokens.BackgroundNebula.copy(alpha = 0.22f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.5f, size.height * 0.15f),
                radius = size.minDimension * 0.9f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    GameUiTokens.BackgroundCoolBloom.copy(alpha = 0.12f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.5f, size.height * 0.58f),
                radius = size.minDimension * 0.82f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    GameUiTokens.BackgroundCoolBloom.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.22f, size.height * 0.32f),
                radius = size.minDimension * 0.44f,
            ),
        )
        if (uiModel.particlesEnabled) {
            drawAmbientParticles(
                particles = ambientParticles.getValue(uiModel.particleQuality),
                driftProgress = particleDrift,
            )
        }

        drawHoldPreview(heldPiece, canHold, cellWidth, cellHeight)
        drawNextPreview(nextPieces, cellWidth, cellHeight)

        val gridColor = GameUiTokens.GridLine.copy(alpha = GameUiTokens.GridLineAlpha)
        for (x in 0..GameConstants.BOARD_WIDTH) {
            drawLine(
                color = gridColor,
                start = Offset(x * cellWidth, 0f),
                end = Offset(x * cellWidth, size.height),
                strokeWidth = 0.85f,
            )
        }
        for (y in 0..GameConstants.BOARD_HEIGHT) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y * cellHeight),
                end = Offset(size.width, y * cellHeight),
                strokeWidth = 0.85f,
            )
        }

        uiModel.ghostCells.forEach { cell ->
            val left = cell.x * cellWidth
            val top = (GameConstants.BOARD_HEIGHT - 1 - cell.y) * cellHeight
            val inset = 3f
            drawRoundRect(
                color = GameUiTokens.GhostStroke.copy(alpha = GameUiTokens.GhostFillAlpha),
                topLeft = Offset(left + inset, top + inset),
                size = Size(cellWidth - inset * 2, cellHeight - inset * 2),
                cornerRadius = CornerRadius(cellWidth * 0.14f),
            )
            drawRoundRect(
                color = GameUiTokens.GhostStroke.copy(alpha = GameUiTokens.GhostStrokeAlpha),
                topLeft = Offset(left + inset, top + inset),
                size = Size(cellWidth - inset * 2, cellHeight - inset * 2),
                cornerRadius = CornerRadius(cellWidth * 0.14f),
                style = Stroke(width = GameUiTokens.GhostStrokeWidth),
            )
        }

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
        if (
            uiModel.particlesEnabled &&
            particleImpulseProgress.value < 1f &&
            uiModel.activePiece != null &&
            uiModel.particleImpulseType != null
        ) {
            drawPieceImpulse(
                cells = uiModel.activePiece.cells,
                type = uiModel.particleImpulseType,
                pieceType = uiModel.activePiece.type,
                progress = particleImpulseProgress.value,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
            )
        }

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
        if (uiModel.particlesEnabled && hardDropBurstProgress.value < 1f && uiModel.hardDropBurstCells.isNotEmpty()) {
            drawHardDropBurst(
                cells = uiModel.hardDropBurstCells,
                progress = hardDropBurstProgress.value,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
            )
        }

        if (lineClearAlpha.value > 0f) {
            drawRect(
                color = Color.White.copy(alpha = lineClearAlpha.value * 0.42f),
                size = size,
            )
        }

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

        val activeCelebration = celebrationType
        if (celebrationProgress.value < 1f && activeCelebration != null) {
            drawCelebration(
                celebrationType = activeCelebration,
                progress = celebrationProgress.value,
            )
        }

        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                color = GameUiTokens.FrameGlow.copy(alpha = GameUiTokens.FrameGlowAlpha).toArgb()
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = GameUiTokens.FrameStrokeWidth * 2.6f
                maskFilter = BlurMaskFilter(GameUiTokens.FrameGlowRadius, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawRoundRect(
                3f, 3f, size.width - 3f, size.height - 3f,
                GameUiTokens.FrameCornerRadius, GameUiTokens.FrameCornerRadius,
                paint,
            )
        }
        drawElectricBorderAccent(
            start = Offset(2f, size.height * 0.14f),
            end = Offset(2f, size.height * 0.78f),
            alpha = 1f,
        )
        drawElectricBorderAccent(
            start = Offset(size.width - 2f, size.height * 0.18f),
            end = Offset(size.width - 2f, size.height * 0.72f),
            alpha = 0.88f,
        )
        drawElectricBorderAccent(
            start = Offset(size.width * 0.28f, 2f),
            end = Offset(size.width * 0.72f, 2f),
            alpha = 0.72f,
        )
        drawRoundRect(
            color = GameUiTokens.FrameStroke.copy(alpha = GameUiTokens.FrameStrokeAlpha),
            topLeft = Offset(1f, 1f),
            size = Size(size.width - 2f, size.height - 2f),
            cornerRadius = CornerRadius(GameUiTokens.FrameCornerRadius),
            style = Stroke(width = GameUiTokens.FrameStrokeWidth),
        )
    }
}

private data class AmbientParticleSpec(
    val xFactor: Float,
    val yFactor: Float,
    val radiusFactor: Float,
    val alphaFactor: Float,
    val driftFactor: Float,
    val speedFactor: Float,
)

private fun stableNoise(index: Int, salt: Int): Float {
    val value = ((index + 1) * (index + 11) * (salt + 37) + salt * 97) % 997
    return value / 997f
}

private fun DrawScope.drawAmbientParticles(
    particles: List<AmbientParticleSpec>,
    driftProgress: Float,
) {
    particles.forEachIndexed { index, particle ->
        val cycle = ((driftProgress * particle.speedFactor) + particle.yFactor) % 1f
        val x = size.width * (particle.xFactor + particle.driftFactor * ((cycle * 2f) - 1f))
        val y = size.height * (1f - cycle)
        val radius = size.minDimension * 0.0058f * particle.radiusFactor
        val alphaEnvelope = (0.5f - (cycle - 0.5f).absoluteValue) * 2f
        val glowAlpha = 0.055f * particle.alphaFactor * alphaEnvelope
        val coreAlpha = 0.13f * particle.alphaFactor * alphaEnvelope
        if (x < -radius * 4f || x > size.width + radius * 4f) return@forEachIndexed
        val tint = if (index % 3 == 0) {
            GameUiTokens.FrameElectricGlow
        } else {
            GameUiTokens.BackgroundCoolBloom
        }

        drawCircle(
            color = tint.copy(alpha = glowAlpha),
            radius = radius * 2.8f,
            center = Offset(x, y),
        )
        drawCircle(
            color = tint.copy(alpha = coreAlpha),
            radius = radius,
            center = Offset(x, y),
        )
    }
}

private fun DrawScope.drawPieceImpulse(
    cells: List<BoardCell>,
    type: ParticleImpulseType,
    pieceType: TetrominoType,
    progress: Float,
    cellWidth: Float,
    cellHeight: Float,
) {
    val fadeOut = 1f - progress
    val colors = CandyPalette.colorsFor(pieceType)
    val centerX = cells.map { (it.x + 0.5f) * cellWidth }.average().toFloat()
    val centerY = cells.map { (GameConstants.BOARD_HEIGHT - it.y - 0.5f) * cellHeight }.average().toFloat()
    val center = Offset(centerX, centerY)

    when (type) {
        ParticleImpulseType.Move -> {
            val spread = cellWidth * (0.24f + progress * 0.82f)
            repeat(6) { idx ->
                val sign = if (idx % 2 == 0) -1f else 1f
                drawCircle(
                    color = colors.glow.copy(alpha = 0.28f * fadeOut),
                    radius = cellWidth * (0.06f + idx * 0.018f),
                    center = Offset(
                        center.x + sign * spread * (0.55f + idx * 0.08f),
                        center.y - idx * cellHeight * 0.05f,
                    ),
                )
            }
        }
        ParticleImpulseType.Rotate -> {
            repeat(8) { idx ->
                val angleFactor = idx / 6f
                val dx = cellWidth * (0.2f + progress * 0.74f) * listOf(-0.95f, -0.55f, -0.15f, 0.2f, 0.65f, 0.95f, 0.45f, -0.45f)[idx]
                val dy = cellHeight * (0.14f + progress * 0.52f) * listOf(-0.9f, -1f, -0.45f, 0.35f, 0.8f, 0.35f, -0.2f, 0.75f)[idx]
                drawCircle(
                    color = colors.highlight.copy(alpha = (0.22f + angleFactor * 0.05f) * fadeOut),
                    radius = cellWidth * (0.055f + idx * 0.008f),
                    center = Offset(center.x + dx, center.y + dy),
                )
            }
        }
        ParticleImpulseType.SoftDrop -> {
            repeat(7) { idx ->
                val drop = cellHeight * (0.16f + progress * (0.45f + idx * 0.08f))
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colors.base.copy(alpha = 0.28f * fadeOut),
                            Color.Transparent,
                        ),
                        start = Offset(center.x, center.y),
                        end = Offset(center.x, center.y + drop),
                    ),
                    start = Offset(center.x + (idx - 3) * cellWidth * 0.09f, center.y - cellHeight * 0.18f),
                    end = Offset(center.x + (idx - 3) * cellWidth * 0.07f, center.y + drop),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round,
                )
            }
        }
        ParticleImpulseType.Hold -> {
            repeat(8) { idx ->
                val rise = cellHeight * (0.08f + progress * (0.34f + idx * 0.04f))
                drawCircle(
                    color = colors.borderLight.copy(alpha = 0.22f * fadeOut),
                    radius = cellWidth * (0.055f + idx * 0.012f),
                    center = Offset(
                        center.x + (idx - 3.5f) * cellWidth * 0.09f,
                        center.y - rise,
                    ),
                )
            }
        }
    }
}

private fun DrawScope.drawHardDropBurst(
    cells: List<BoardCell>,
    progress: Float,
    cellWidth: Float,
    cellHeight: Float,
) {
    val fadeOut = 1f - progress
    cells.forEachIndexed { index, cell ->
        val center = Offset(
            x = (cell.x + 0.5f) * cellWidth,
            y = (GameConstants.BOARD_HEIGHT - cell.y - 0.5f) * cellHeight,
        )
        val rayLength = cellWidth * (0.22f + progress * 0.82f)
        val upwardLift = cellHeight * progress * 0.42f
        repeat(3) { ray ->
            val direction = when ((index + ray) % 3) {
                0 -> -1f
                1 -> 0f
                else -> 1f
            }
            val start = center.copy(y = center.y - upwardLift * 0.35f)
            val end = Offset(
                x = center.x + direction * rayLength,
                y = center.y - upwardLift - rayLength * 0.3f,
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        GameUiTokens.FrameElectricGlow.copy(alpha = 0.55f * fadeOut),
                        Color.Transparent,
                    ),
                    start = start,
                    end = end,
                ),
                start = start,
                end = end,
                strokeWidth = 2.2f,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(
            color = GameUiTokens.BackgroundCoolBloom.copy(alpha = 0.22f * fadeOut),
            radius = cellWidth * (0.24f + progress * 0.28f),
            center = center.copy(y = center.y - upwardLift * 0.2f),
        )
    }
}

private fun DrawScope.drawElectricBorderAccent(
    start: Offset,
    end: Offset,
    alpha: Float,
) {
    drawIntoCanvas { canvas ->
        val glowPaint = Paint().asFrameworkPaint().apply {
            color = GameUiTokens.FrameElectricGlow.copy(
                alpha = GameUiTokens.FrameElectricGlowAlpha * alpha,
            ).toArgb()
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = GameUiTokens.FrameElectricStrokeWidth
            strokeCap = android.graphics.Paint.Cap.ROUND
            maskFilter = BlurMaskFilter(
                GameUiTokens.FrameElectricGlowRadius,
                BlurMaskFilter.Blur.NORMAL,
            )
        }
        canvas.nativeCanvas.drawLine(start.x, start.y, end.x, end.y, glowPaint)
    }
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                GameUiTokens.FrameElectricGlow.copy(alpha = 0.95f * alpha),
                Color.Transparent,
            ),
            start = start,
            end = end,
        ),
        start = start,
        end = end,
        strokeWidth = GameUiTokens.FrameElectricStrokeWidth,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawHoldPreview(
    type: TetrominoType?,
    canHold: Boolean,
    cellWidth: Float,
    cellHeight: Float,
) {
    val previewCellSize = cellWidth * 0.44f
    val boxPadding = cellWidth * 0.28f
    val boxInnerW = previewCellSize * 4f
    val boxInnerH = previewCellSize * 4f
    val boxWidth = boxInnerW + boxPadding * 2f
    val boxHeight = boxInnerH + boxPadding * 2f
    val boxLeft = cellWidth * 0.42f
    val boxTop = cellHeight * 0.72f
    val cornerRadius = CornerRadius(previewCellSize * 0.18f)

    drawRoundRect(
        color = GameUiTokens.PreviewBorderColor.copy(
            alpha = if (canHold) GameUiTokens.PreviewBorderAlpha else GameUiTokens.PreviewBorderAlpha * 0.55f,
        ),
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxWidth, boxHeight),
        cornerRadius = cornerRadius,
        style = Stroke(width = 1f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = GameUiTokens.PreviewBgAlpha),
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxWidth, boxHeight),
        cornerRadius = cornerRadius,
    )

    if (type == null) return
    val alpha = if (canHold) 0.16f else 0.08f
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
    val previewCellSize = cellWidth * 0.28f

    val containerWidth = previewCellSize * 4f
    val containerRight = size.width - cellWidth * 0.42f
    val containerLeft = containerRight - containerWidth
    val containerCenterX = containerLeft + containerWidth / 2f

    val slotHeight = previewCellSize * 3f
    val slotGap = previewCellSize * 1.25f
    var slotTop = cellHeight * 0.78f

    pieces.forEachIndexed { index, type ->
        val cells = spawnCells(type)
        val minX = cells.minOf { it.x }
        val maxX = cells.maxOf { it.x }
        val minY = cells.minOf { it.y }
        val maxY = cells.maxOf { it.y }
        val pieceWidth = (maxX - minX + 1) * previewCellSize
        val pieceHeight = (maxY - minY + 1) * previewCellSize

        val originX = containerCenterX - pieceWidth / 2f
        val originY = slotTop + (slotHeight - pieceHeight) / 2f
        val alpha = if (index == 0) 0.34f else 0.22f

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
        val maxY = cells.maxOf { it.y }
        val py = originY + (maxY - cell.y) * cellSize
        val cornerRadius = CornerRadius(cellSize * 0.16f)
        val inset = 1f

        drawRoundRect(
            color = colors.base.copy(alpha = alpha),
            topLeft = Offset(px + inset, py + inset),
            size = Size(cellSize - inset * 2, cellSize - inset * 2),
            cornerRadius = cornerRadius,
        )
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
    val cornerRadius = CornerRadius(cellWidth * 0.13f)

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

    drawRoundRect(
        color = colors.base,
        topLeft = Offset(left + 2f, top + 2f),
        size = Size(cellWidth - 4f, cellHeight - 4f),
        cornerRadius = cornerRadius,
    )

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

    drawOval(
        color = Color.White.copy(alpha = 0.35f),
        topLeft = Offset(left + cellWidth * 0.15f, top + cellHeight * 0.12f),
        size = Size(cellWidth * 0.28f, cellHeight * 0.18f),
    )

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
    progress: Float,
) {
    val (overlayColor, label) = when (celebrationType) {
        CelebrationType.Tetris -> Color(0xFF7A8DFF) to "4 ROW" // TETRIS
        CelebrationType.TSpin -> Color(0xFFFF6FB5) to "T-SPIN"
        CelebrationType.AllClear -> Color(0xFF7BFFE1) to "ALL CLEAR"
    }
    val textAlpha = 1f - progress
    val textScale = 0.82f + (progress * 0.42f)

    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = size.minDimension * 0.11f * textScale
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
            color = Color.White.copy(alpha = textAlpha).toArgb()
            setShadowLayer(24f, 0f, 0f, overlayColor.copy(alpha = textAlpha).toArgb())
        }
        val baseline = center.y - ((paint.descent() + paint.ascent()) / 2f)
        canvas.nativeCanvas.drawText(
            label,
            center.x,
            baseline,
            paint,
        )
    }
}
