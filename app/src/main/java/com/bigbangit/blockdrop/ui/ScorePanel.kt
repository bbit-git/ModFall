package com.bigbangit.blockdrop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bigbangit.blockdrop.R
import com.bigbangit.blockdrop.core.GameConstants
import com.bigbangit.blockdrop.core.TetrominoShapes
import com.bigbangit.blockdrop.core.TetrominoType
import com.bigbangit.blockdrop.ui.model.GameUiModel
import com.bigbangit.blockdrop.ui.theme.BoardBackground
import com.bigbangit.blockdrop.ui.theme.TextWhite

@Composable
fun ScorePanel(
    uiModel: GameUiModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(100.dp)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Held Piece
        InfoBox(label = stringResource(R.string.hold_label)) {
            PiecePreview(
                type = uiModel.heldPiece,
                modifier = Modifier.alpha(if (uiModel.canHold) 1f else 0.4f)
            )
        }

        // Stats
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatItem(label = stringResource(R.string.score_label_caps), value = uiModel.score.toString())
            StatItem(label = stringResource(R.string.level_label_caps), value = uiModel.level.toString())
            StatItem(label = stringResource(R.string.lines_label), value = uiModel.lines.toString())
        }

        Spacer(modifier = Modifier.weight(1f))

        // Next Pieces
        InfoBox(label = stringResource(R.string.next_label_caps)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val visibleCount = getVisibleNextCount(uiModel.level)
                uiModel.nextPieces.take(visibleCount).forEach { type ->
                    PiecePreview(type = type, modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoBox(
    label: String,
    content: @Composable () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = TextWhite.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BoardBackground.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = TextWhite.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp
        )
        Text(
            text = value,
            color = TextWhite,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun PiecePreview(
    type: TetrominoType?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(50.dp)) {
        if (type == null) return@Canvas

        val definition = TetrominoShapes.definitionFor(type)
        val cells = definition.rotations.getValue(com.bigbangit.blockdrop.core.RotationState.Spawn).cells
        
        // Find bounds to center the piece
        val minX = cells.minOf { it.x }
        val maxX = cells.maxOf { it.x }
        val minY = cells.minOf { it.y }
        val maxY = cells.maxOf { it.y }
        
        val pieceWidth = maxX - minX + 1
        val pieceHeight = maxY - minY + 1
        
        val cellSize = size.minDimension / 4f
        val colors = CandyPalette.colorsFor(type)
        
        val offsetX = (size.width - pieceWidth * cellSize) / 2f - minX * cellSize
        val offsetY = (size.height - pieceHeight * cellSize) / 2f - minY * cellSize

        cells.forEach { cell ->
            val left = offsetX + cell.x * cellSize
            val top = size.height - (offsetY + (cell.y + 1) * cellSize) // Invert Y for preview too
            
            drawPreviewBlock(left, top, cellSize, colors)
        }
    }
}

private fun DrawScope.drawPreviewBlock(left: Float, top: Float, size: Float, colors: BlockColors) {
    val cornerRadius = CornerRadius(size * 0.15f)
    
    // Simplified candy block for preview
    drawRoundRect(
        color = colors.base,
        topLeft = Offset(left + 1f, top + 1f),
        size = Size(size - 2f, size - 2f),
        cornerRadius = cornerRadius
    )
    
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(colors.highlight, colors.shadow),
            startY = top + 1f,
            endY = top + size - 1f
        ),
        topLeft = Offset(left + 1f, top + 1f),
        size = Size(size - 2f, size - 2f),
        cornerRadius = cornerRadius,
        alpha = 0.5f
    )
    
    drawOval(
        color = Color.White.copy(alpha = 0.3f),
        topLeft = Offset(left + size * 0.15f, top + size * 0.15f),
        size = Size(size * 0.3f, size * 0.2f)
    )
}

private fun getVisibleNextCount(level: Int): Int {
    return GameConstants.nextQueueTiers.find { level in it.levelStart..it.levelEnd }?.visiblePieces ?: 1
}
