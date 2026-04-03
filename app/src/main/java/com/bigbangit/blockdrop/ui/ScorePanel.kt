package com.bigbangit.blockdrop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bigbangit.blockdrop.R
import com.bigbangit.blockdrop.core.TetrominoShapes
import com.bigbangit.blockdrop.core.TetrominoType
import com.bigbangit.blockdrop.ui.theme.BoardBackground
import com.bigbangit.blockdrop.ui.theme.TextWhite

@Composable
fun CompactStatsBar(
    score: Int,
    level: Int,
    lines: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(999.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactStat(text = stringResource(R.string.score_stat_compact, score))
        StatDivider()
        CompactStat(text = stringResource(R.string.level_stat_compact, level))
        StatDivider()
        CompactStat(text = stringResource(R.string.lines_stat_compact, lines))
    }
}

@Composable
fun BoardSidePanel(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = TextWhite.copy(alpha = 0.78f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .background(BoardBackground.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
fun HoldPreview(
    type: TetrominoType?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    PiecePreview(
        type = type,
        modifier = modifier.alpha(if (enabled) 1f else 0.4f),
        previewSize = 56.dp,
        scaleFactor = 0.78f,
    )
}

@Composable
fun NextPreviewColumn(
    pieces: List<TetrominoType>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        pieces.forEach { type ->
            PiecePreview(
                type = type,
                previewSize = 36.dp,
                scaleFactor = 0.5f,
            )
        }
    }
}

@Composable
fun PiecePreview(
    type: TetrominoType?,
    modifier: Modifier = Modifier,
    previewSize: Dp = 50.dp,
    scaleFactor: Float = 1f,
) {
    Canvas(modifier = modifier.size(previewSize)) {
        if (type == null) return@Canvas

        val definition = TetrominoShapes.definitionFor(type)
        val cells = definition.rotations.getValue(com.bigbangit.blockdrop.core.RotationState.Spawn).cells

        val minX = cells.minOf { it.x }
        val maxX = cells.maxOf { it.x }
        val minY = cells.minOf { it.y }
        val maxY = cells.maxOf { it.y }

        val pieceWidth = maxX - minX + 1
        val pieceHeight = maxY - minY + 1

        val baseCellSize = size.minDimension / 4f
        val cellSize = baseCellSize * scaleFactor.coerceIn(0.3f, 1f)
        val colors = CandyPalette.colorsFor(type)

        val offsetX = (size.width - pieceWidth * cellSize) / 2f - minX * cellSize
        val offsetY = (size.height - pieceHeight * cellSize) / 2f - minY * cellSize

        cells.forEach { cell ->
            val left = offsetX + cell.x * cellSize
            val top = size.height - (offsetY + (cell.y + 1) * cellSize)
            drawPreviewBlock(left, top, cellSize, colors)
        }
    }
}

@Composable
private fun CompactStat(text: String) {
    Text(
        text = text,
        color = TextWhite,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun StatDivider() {
    Text(
        text = "|",
        color = Color.White.copy(alpha = 0.45f),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.defaultMinSize(minWidth = 6.dp),
        textAlign = TextAlign.Center,
    )
}

private fun DrawScope.drawPreviewBlock(left: Float, top: Float, size: Float, colors: BlockColors) {
    val cornerRadius = CornerRadius(size * 0.15f)

    drawRoundRect(
        color = colors.base,
        topLeft = Offset(left + 1f, top + 1f),
        size = Size(size - 2f, size - 2f),
        cornerRadius = cornerRadius,
    )

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(colors.highlight, colors.shadow),
            startY = top + 1f,
            endY = top + size - 1f,
        ),
        topLeft = Offset(left + 1f, top + 1f),
        size = Size(size - 2f, size - 2f),
        cornerRadius = cornerRadius,
        alpha = 0.5f,
    )

    drawOval(
        color = Color.White.copy(alpha = 0.3f),
        topLeft = Offset(left + size * 0.15f, top + size * 0.15f),
        size = Size(size * 0.3f, size * 0.2f),
    )
}
