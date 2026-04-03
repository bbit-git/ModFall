package com.bigbangit.blockdrop.ui

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
    onDropDelay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val currentUiModel = rememberUpdatedState(uiModel)
    val density = LocalDensity.current

    val hardDropThresholdPxPerSec = with(density) { GameConstants.HARD_DROP_VELOCITY_DP_PER_SEC.dp.toPx() }
    val grabSlopPx = with(density) { GameConstants.GRAB_CELL_SLOP_DP.dp.toPx() }
    val dasMs = GameConstants.DAS_MS
    val arrMs = GameConstants.ARR_MS

    Box(
        modifier = modifier
            .onSizeChanged { boardSize = it }
            .pointerInput(boardSize) {
                val width = boardSize.width.toFloat().takeIf { it > 0f } ?: return@pointerInput
                val height = boardSize.height.toFloat().takeIf { it > 0f } ?: return@pointerInput
                val cellWidth = width / GameConstants.BOARD_WIDTH
                val cellHeight = height / GameConstants.BOARD_HEIGHT

                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        if (currentUiModel.value.state != GameState.Running) continue

                        val startOffset = down.position
                        val startTime = System.currentTimeMillis()
                        val startedOnPiece = currentUiModel.value.activePiece?.contains(startOffset, boardSize, grabSlopPx) == true

                        var grabActive = false
                        var totalDrag = Offset.Zero
                        var horizontalSteps = 0
                        var downwardSteps = 0
                        var lastMoveTime = startTime
                        var isDasActive = false
                        var dasServed = false
                        var lastVelocity = 0f
                        var lastEventTime = down.uptimeMillis

                        var dragPointer = down
                        var pointerReleased = false

                        while (!pointerReleased) {
                            val event = awaitPointerEvent()
                            val pointerChange = event.changes.find { it.id == dragPointer.id }

                            if (pointerChange == null || !pointerChange.pressed) {
                                if (totalDrag.getDistance() < 10f) {
                                    val piece = currentUiModel.value.activePiece
                                    if (piece != null && piece.contains(startOffset, boardSize, grabSlopPx)) {
                                        val pieceCenterX = piece.centerX(boardSize)
                                        if (startOffset.x < pieceCenterX) onRotateCounterClockwise() else onRotateClockwise()
                                    } else {
                                        if (startOffset.x < width / 2f) onRotateCounterClockwise() else onRotateClockwise()
                                    }
                                } else {
                                    if (totalDrag.y < -cellHeight && abs(totalDrag.y) > abs(totalDrag.x)) {
                                        onDropDelay()
                                    } else if (!startedOnPiece && totalDrag.y > cellHeight && lastVelocity >= hardDropThresholdPxPerSec) {
                                        onHardDrop()
                                    }
                                }
                                pointerReleased = true
                            } else {
                                val dragAmount = pointerChange.position - pointerChange.previousPosition
                                totalDrag += dragAmount
                                val currentTime = System.currentTimeMillis()

                                val timeDelta = (pointerChange.uptimeMillis - lastEventTime) / 1000f
                                if (timeDelta > 0f) {
                                    lastVelocity = dragAmount.y / timeDelta
                                }
                                lastEventTime = pointerChange.uptimeMillis

                                if (startedOnPiece && !grabActive) {
                                    if (abs(totalDrag.x) > grabSlopPx && abs(totalDrag.x) > abs(totalDrag.y)) {
                                        grabActive = true
                                    }
                                }

                                if (grabActive) {
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
                                            val threshold = if (dasServed) arrMs else dasMs
                                            if (timeSinceLast > threshold) {
                                                if (direction > 0) onMoveRight() else onMoveLeft()
                                                lastMoveTime = currentTime
                                                dasServed = true
                                            }
                                        }
                                    }

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
            },
    ) {
        BoardCanvas(uiModel = uiModel)
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
