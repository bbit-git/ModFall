package com.bigbangit.blockdrop.ui

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bigbangit.blockdrop.R
import com.bigbangit.blockdrop.core.GameConstants
import com.bigbangit.blockdrop.core.GameState
import com.bigbangit.blockdrop.ui.model.ActivePieceUiModel
import com.bigbangit.blockdrop.ui.model.BoardCell
import com.bigbangit.blockdrop.ui.model.GameUiModel
import kotlin.math.abs

@Composable
fun ControlSurface(
    uiModel: GameUiModel,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRotateClockwise: () -> Unit,
    onRotateCounterClockwise: () -> Unit,
    onSoftDrop: () -> Unit,
    onHardDrop: () -> Unit,
    onHold: () -> Unit,
    onDropDelay: () -> Unit,
) {
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    // Gesture constants from GameConstants
    val hardDropThresholdPxPerSec = with(density) { GameConstants.HARD_DROP_VELOCITY_DP_PER_SEC.dp.toPx() }
    val grabSlopPx = with(density) { GameConstants.GRAB_CELL_SLOP_DP.dp.toPx() }
    val dasMs = GameConstants.DAS_MS
    val arrMs = GameConstants.ARR_MS

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onHold,
            enabled = uiModel.state == GameState.Running && uiModel.canHold,
        ) {
            Text(
                text = stringResource(
                    R.string.hold_panel_label,
                    uiModel.heldPiece?.name ?: stringResource(R.string.none_label),
                ),
            )
        }

        Box(
            modifier = Modifier
                .onSizeChanged { boardSize = it }
                .pointerInput(uiModel, boardSize) {
                    val width = boardSize.width.toFloat().takeIf { it > 0f } ?: return@pointerInput
                    val height = boardSize.height.toFloat().takeIf { it > 0f } ?: return@pointerInput
                    val cellWidth = width / GameConstants.BOARD_WIDTH
                    val cellHeight = height / GameConstants.BOARD_HEIGHT

                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            if (uiModel.state != GameState.Running) continue

                            val startOffset = down.position
                            val startTime = System.currentTimeMillis()
                            val startedOnPiece = uiModel.activePiece?.contains(startOffset, boardSize, grabSlopPx) == true

                            var grabActive = false
                            var totalDrag = Offset.Zero
                            var horizontalSteps = 0
                            var downwardSteps = 0
                            var lastMoveTime = startTime
                            var isDasActive = false
                            var lastVelocity = 0f
                            var lastEventTime = down.uptimeMillis

                            var dragPointer = down
                            var pointerReleased = false

                            while (!pointerReleased) {
                                val event = awaitPointerEvent()
                                val pointerChange = event.changes.find { it.id == dragPointer.id }

                                if (pointerChange == null || !pointerChange.pressed) {
                                    // Pointer lifted
                                    if (totalDrag.getDistance() < 10f) {
                                        // Classification: Tap
                                        val piece = uiModel.activePiece
                                        if (piece != null && piece.contains(startOffset, boardSize, grabSlopPx)) {
                                            val pieceCenterX = piece.centerX(boardSize)
                                            if (startOffset.x < pieceCenterX) onRotateCounterClockwise() else onRotateClockwise()
                                        } else {
                                            if (startOffset.x < width / 2f) onRotateCounterClockwise() else onRotateClockwise()
                                        }
                                    } else {
                                        // Classification: End of drag/swipe
                                        if (totalDrag.y < -cellHeight && abs(totalDrag.y) > abs(totalDrag.x)) {
                                            onDropDelay()
                                        } else if (!startedOnPiece && totalDrag.y > cellHeight && lastVelocity >= hardDropThresholdPxPerSec) {
                                            onHardDrop()
                                        }
                                    }
                                    pointerReleased = true
                                } else {
                                    // Pointer moved
                                    val dragAmount = pointerChange.position - pointerChange.previousPosition
                                    totalDrag += dragAmount
                                    val currentTime = System.currentTimeMillis()

                                    // Velocity calculation for hard drop
                                    val timeDelta = (pointerChange.uptimeMillis - lastEventTime) / 1000f
                                    if (timeDelta > 0f) {
                                        lastVelocity = dragAmount.y / timeDelta
                                    }
                                    lastEventTime = pointerChange.uptimeMillis

                                    if (startedOnPiece && !grabActive) {
                                        // Check if we should engage grab mode
                                        if (abs(totalDrag.x) > grabSlopPx && abs(totalDrag.x) > abs(totalDrag.y)) {
                                            grabActive = true
                                        }
                                    }

                                    if (grabActive) {
                                        // Grab mode: move piece column-by-column as finger crosses boundaries
                                        val currentSteps = (totalDrag.x / cellWidth).toInt()
                                        while (horizontalSteps < currentSteps) {
                                            onMoveRight()
                                            horizontalSteps++
                                        }
                                        while (horizontalSteps > currentSteps) {
                                            onMoveLeft()
                                            horizontalSteps--
                                        }
                                    } else if (!startedOnPiece) {
                                        // Off-piece: horizontal move with DAS/ARR
                                        val dx = totalDrag.x
                                        val absDx = abs(dx)
                                        if (absDx > cellWidth * 0.5f) {
                                            val direction = if (dx > 0) 1 else -1
                                            val timeSinceLast = currentTime - lastMoveTime

                                            if (!isDasActive) {
                                                if (absDx > cellWidth) {
                                                    if (direction > 0) onMoveRight() else onMoveLeft()
                                                    lastMoveTime = currentTime
                                                    isDasActive = true
                                                }
                                            } else {
                                                val threshold = if (currentTime - startTime > dasMs) arrMs else dasMs
                                                if (timeSinceLast > threshold) {
                                                    if (direction > 0) onMoveRight() else onMoveLeft()
                                                    lastMoveTime = currentTime
                                                }
                                            }
                                        }

                                        // Off-piece: vertical soft drop
                                        if (totalDrag.y > cellHeight) {
                                            val currentDownSteps = (totalDrag.y / cellHeight).toInt()
                                            while (downwardSteps < currentDownSteps) {
                                                onSoftDrop()
                                                downwardSteps++
                                            }
                                        }
                                    }
                                    pointerChange.consume()
                                    dragPointer = pointerChange
                                }
                            }
                        }
                    }
                }
        ) {
            BoardCanvas(uiModel = uiModel)
        }

        Text(text = stringResource(R.string.gesture_hint_label))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = stringResource(R.string.debug_fallback_label))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onMoveLeft, enabled = uiModel.state == GameState.Running) { Text(stringResource(R.string.move_left)) }
                Button(onClick = onMoveRight, enabled = uiModel.state == GameState.Running) { Text(stringResource(R.string.move_right)) }
                Button(onClick = onRotateCounterClockwise, enabled = uiModel.state == GameState.Running) { Text(stringResource(R.string.rotate_ccw)) }
                Button(onClick = onRotateClockwise, enabled = uiModel.state == GameState.Running) { Text(stringResource(R.string.rotate_cw)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSoftDrop, enabled = uiModel.state == GameState.Running) { Text(stringResource(R.string.soft_drop)) }
                Button(onClick = onHardDrop, enabled = uiModel.state == GameState.Running) { Text(stringResource(R.string.hard_drop)) }
                Button(onClick = onHold, enabled = uiModel.state == GameState.Running && uiModel.canHold) { Text(stringResource(R.string.hold_action)) }
                Button(onClick = onDropDelay, enabled = uiModel.state == GameState.Running && uiModel.canUseDropDelay) { Text(stringResource(R.string.drop_delay)) }
            }
        }
    }
}

private fun ActivePieceUiModel.contains(offset: Offset, boardSize: IntSize, slopPx: Float): Boolean {
    val cellWidth = boardSize.width.toFloat() / GameConstants.BOARD_WIDTH
    val cellHeight = boardSize.height.toFloat() / GameConstants.BOARD_HEIGHT
    return cells.any { cell -> cell.contains(offset, cellWidth, cellHeight, slopPx) }
}

private fun ActivePieceUiModel.centerX(boardSize: IntSize): Float {
    val cellWidth = boardSize.width.toFloat() / GameConstants.BOARD_WIDTH
    val left = cells.minOf { it.x } * cellWidth
    val right = (cells.maxOf { it.x } + 1) * cellWidth
    return (left + right) / 2f
}

private fun BoardCell.contains(offset: Offset, cellWidth: Float, cellHeight: Float, slopPx: Float): Boolean {
    val left = x * cellWidth - slopPx
    val right = (x + 1) * cellWidth + slopPx
    val bottomAlignedRow = GameConstants.BOARD_HEIGHT - 1 - y
    val top = bottomAlignedRow * cellHeight - slopPx
    val bottom = (bottomAlignedRow + 1) * cellHeight + slopPx
    return offset.x in left..right && offset.y in top..bottom
}
