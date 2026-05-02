package com.bigbangit.modfall.core

data class CellOffset(val x: Int, val y: Int)

data class RotationDefinition(val cells: List<CellOffset>)

data class TetrominoDefinition(
    val type: TetrominoType,
    val rotations: Map<RotationState, RotationDefinition>,
)

object TetrominoShapes {
    val definitions: Map<TetrominoType, TetrominoDefinition> = mapOf(
        TetrominoType.I to tetromino(
            TetrominoType.I,
            spawn = listOf(CellOffset(-1, 0), CellOffset(0, 0), CellOffset(1, 0), CellOffset(2, 0)),
            right = listOf(CellOffset(1, 1), CellOffset(1, 0), CellOffset(1, -1), CellOffset(1, -2)),
            reverse = listOf(CellOffset(-1, -1), CellOffset(0, -1), CellOffset(1, -1), CellOffset(2, -1)),
            left = listOf(CellOffset(0, 1), CellOffset(0, 0), CellOffset(0, -1), CellOffset(0, -2)),
        ),
        TetrominoType.O to tetromino(
            TetrominoType.O,
            spawn = listOf(CellOffset(0, 0), CellOffset(1, 0), CellOffset(0, -1), CellOffset(1, -1)),
            right = listOf(CellOffset(0, 0), CellOffset(1, 0), CellOffset(0, -1), CellOffset(1, -1)),
            reverse = listOf(CellOffset(0, 0), CellOffset(1, 0), CellOffset(0, -1), CellOffset(1, -1)),
            left = listOf(CellOffset(0, 0), CellOffset(1, 0), CellOffset(0, -1), CellOffset(1, -1)),
        ),
        TetrominoType.T to tetromino(
            TetrominoType.T,
            spawn = listOf(CellOffset(-1, 0), CellOffset(0, 0), CellOffset(1, 0), CellOffset(0, 1)),
            right = listOf(CellOffset(0, 1), CellOffset(0, 0), CellOffset(0, -1), CellOffset(1, 0)),
            reverse = listOf(CellOffset(-1, 0), CellOffset(0, 0), CellOffset(1, 0), CellOffset(0, -1)),
            left = listOf(CellOffset(0, 1), CellOffset(0, 0), CellOffset(0, -1), CellOffset(-1, 0)),
        ),
        TetrominoType.S to tetromino(
            TetrominoType.S,
            spawn = listOf(CellOffset(0, 0), CellOffset(1, 0), CellOffset(-1, -1), CellOffset(0, -1)),
            right = listOf(CellOffset(0, 1), CellOffset(0, 0), CellOffset(1, 0), CellOffset(1, -1)),
            reverse = listOf(CellOffset(0, 0), CellOffset(1, 0), CellOffset(-1, -1), CellOffset(0, -1)),
            left = listOf(CellOffset(0, 1), CellOffset(0, 0), CellOffset(1, 0), CellOffset(1, -1)),
        ),
        TetrominoType.Z to tetromino(
            TetrominoType.Z,
            spawn = listOf(CellOffset(-1, 0), CellOffset(0, 0), CellOffset(0, -1), CellOffset(1, -1)),
            right = listOf(CellOffset(1, 1), CellOffset(1, 0), CellOffset(0, 0), CellOffset(0, -1)),
            reverse = listOf(CellOffset(-1, 0), CellOffset(0, 0), CellOffset(0, -1), CellOffset(1, -1)),
            left = listOf(CellOffset(1, 1), CellOffset(1, 0), CellOffset(0, 0), CellOffset(0, -1)),
        ),
        TetrominoType.J to tetromino(
            TetrominoType.J,
            spawn = listOf(CellOffset(-1, 0), CellOffset(0, 0), CellOffset(1, 0), CellOffset(-1, 1)),
            right = listOf(CellOffset(0, 1), CellOffset(0, 0), CellOffset(0, -1), CellOffset(1, 1)),
            reverse = listOf(CellOffset(-1, 0), CellOffset(0, 0), CellOffset(1, 0), CellOffset(1, -1)),
            left = listOf(CellOffset(0, 1), CellOffset(0, 0), CellOffset(0, -1), CellOffset(-1, -1)),
        ),
        TetrominoType.L to tetromino(
            TetrominoType.L,
            spawn = listOf(CellOffset(-1, 0), CellOffset(0, 0), CellOffset(1, 0), CellOffset(1, 1)),
            right = listOf(CellOffset(0, 1), CellOffset(0, 0), CellOffset(0, -1), CellOffset(1, -1)),
            reverse = listOf(CellOffset(-1, 0), CellOffset(0, 0), CellOffset(1, 0), CellOffset(-1, -1)),
            left = listOf(CellOffset(0, 1), CellOffset(0, 0), CellOffset(0, -1), CellOffset(-1, 1)),
        ),
    )

    fun definitionFor(type: TetrominoType): TetrominoDefinition = definitions.getValue(type)

    private fun tetromino(
        type: TetrominoType,
        spawn: List<CellOffset>,
        right: List<CellOffset>,
        reverse: List<CellOffset>,
        left: List<CellOffset>,
    ): TetrominoDefinition {
        return TetrominoDefinition(
            type = type,
            rotations = mapOf(
                RotationState.Spawn to RotationDefinition(spawn),
                RotationState.Right to RotationDefinition(right),
                RotationState.Reverse to RotationDefinition(reverse),
                RotationState.Left to RotationDefinition(left),
            ),
        )
    }
}
