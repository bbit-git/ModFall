package com.bigbangit.modfall.core

data class ActivePiece(
    val type: TetrominoType,
    val rotation: RotationState = RotationState.Spawn,
    val originX: Int = GameConstants.SPAWN_COLUMN_CENTER,
    val originY: Int = GameConstants.SPAWN_ROW_JLSTZ,
) {
    fun cells(): List<PieceCell> {
        val definition = TetrominoShapes.definitionFor(type)
        return definition.rotations.getValue(rotation).cells.map { offset ->
            PieceCell(
                x = originX + offset.x,
                y = originY + offset.y,
                type = type,
            )
        }
    }

    fun movedBy(dx: Int, dy: Int): ActivePiece {
        return copy(originX = originX + dx, originY = originY + dy)
    }

    fun rotated(to: RotationState): ActivePiece {
        return copy(rotation = to)
    }
}

data class PieceCell(
    val x: Int,
    val y: Int,
    val type: TetrominoType,
)
