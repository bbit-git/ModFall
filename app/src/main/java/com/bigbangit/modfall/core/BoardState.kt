package com.bigbangit.modfall.core

class BoardState(
    val width: Int = GameConstants.BOARD_WIDTH,
    val height: Int = GameConstants.BOARD_HEIGHT,
    private val hiddenRows: Int = GameConstants.HIDDEN_ROWS,
) {
    enum class TSpinType { None, Mini, Full }
    private val totalHeight: Int = height + hiddenRows

    // Mutable board storage is intentionally unsynchronized; keep all access on one game-loop thread.
    private val cells: Array<IntArray> = Array(totalHeight) { IntArray(width) }

    fun clear() {
        cells.forEach { row -> row.fill(0) }
    }

    fun snapshot(): BoardSnapshot {
        return BoardSnapshot(cells = cells.take(height).map { it.toList() })
    }

    fun isOccupied(x: Int, y: Int): Boolean {
        if (x !in 0 until width) return true
        if (y < 0) return true
        if (y >= totalHeight) return false
        return cells[y][x] != 0
    }

    fun canPlace(piece: ActivePiece): Boolean {
        return piece.cells().none { cell -> isOccupied(cell.x, cell.y) }
    }

    fun lock(piece: ActivePiece) {
        piece.cells().forEach { cell ->
            if (cell.y in 0 until totalHeight && cell.x in 0 until width) {
                cells[cell.y][cell.x] = cell.type.cellValue
            }
        }
    }

    fun clearLines(): Int {
        var linesCleared = 0
        var row = 0
        while (row < totalHeight) {
            if (cells[row].all { it != 0 }) {
                linesCleared++
                // Shift everything down
                for (i in row until totalHeight - 1) {
                    cells[i] = cells[i + 1]
                }
                // Last row is now empty
                cells[totalHeight - 1] = IntArray(width) { 0 }
            } else {
                row++
            }
        }
        return linesCleared
    }

    fun isEmpty(): Boolean {
        return cells.all { row -> row.all { it == 0 } }
    }

    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
    fun set(x: Int, y: Int, value: Int) {
        if (x in 0 until width && y in 0 until totalHeight) {
            cells[y][x] = value
        }
    }

    fun checkTSpin(piece: ActivePiece): TSpinType {
        if (piece.type != TetrominoType.T) return TSpinType.None

        // 3-corner rule: check the four diagonal cells of the T center.
        val corners = listOf(
            CellOffset(-1, 1), CellOffset(1, 1),
            CellOffset(-1, -1), CellOffset(1, -1)
        )
        val occupiedCorners = corners.count { offset ->
            isOccupied(piece.originX + offset.x, piece.originY + offset.y)
        }

        if (occupiedCorners < 2) return TSpinType.None

        val frontCorners = when (piece.rotation) {
            RotationState.Spawn -> listOf(CellOffset(-1, 1), CellOffset(1, 1))
            RotationState.Right -> listOf(CellOffset(1, 1), CellOffset(1, -1))
            RotationState.Reverse -> listOf(CellOffset(-1, -1), CellOffset(1, -1))
            RotationState.Left -> listOf(CellOffset(-1, 1), CellOffset(-1, -1))
        }

        val backCorners = when (piece.rotation) {
            RotationState.Spawn -> listOf(CellOffset(-1, -1), CellOffset(1, -1))
            RotationState.Right -> listOf(CellOffset(-1, 1), CellOffset(-1, -1))
            RotationState.Reverse -> listOf(CellOffset(-1, 1), CellOffset(1, 1))
            RotationState.Left -> listOf(CellOffset(1, 1), CellOffset(1, -1))
        }

        return when {
            occupiedCorners >= 3 && frontCorners.all { offset -> isOccupied(piece.originX + offset.x, piece.originY + offset.y) } -> TSpinType.Full
            occupiedCorners >= 3 -> TSpinType.Mini
            backCorners.all { offset -> isOccupied(piece.originX + offset.x, piece.originY + offset.y) } -> TSpinType.Mini
            else -> TSpinType.None
        }
    }

    fun projectGhost(piece: ActivePiece): ActivePiece {
        var projected = piece
        while (canPlace(projected.movedBy(dx = 0, dy = -1))) {
            projected = projected.movedBy(dx = 0, dy = -1)
        }
        return projected
    }
}

data class BoardSnapshot(val cells: List<List<Int>>)
