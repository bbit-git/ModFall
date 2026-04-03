package com.bigbangit.blockdrop.core

data class KickOffset(val dx: Int, val dy: Int)

data class RotationTransition(val from: RotationState, val to: RotationState)

object SrsKickTables {
    val standard: Map<RotationTransition, List<KickOffset>> = mapOf(
        transition(RotationState.Spawn, RotationState.Right) to kicks(0 to 0, -1 to 0, -1 to 1, 0 to -2, -1 to -2),
        transition(RotationState.Right, RotationState.Spawn) to kicks(0 to 0, 1 to 0, 1 to -1, 0 to 2, 1 to 2),
        transition(RotationState.Right, RotationState.Reverse) to kicks(0 to 0, 1 to 0, 1 to -1, 0 to 2, 1 to 2),
        transition(RotationState.Reverse, RotationState.Right) to kicks(0 to 0, -1 to 0, -1 to 1, 0 to -2, -1 to -2),
        transition(RotationState.Reverse, RotationState.Left) to kicks(0 to 0, 1 to 0, 1 to 1, 0 to -2, 1 to -2),
        transition(RotationState.Left, RotationState.Reverse) to kicks(0 to 0, -1 to 0, -1 to -1, 0 to 2, -1 to 2),
        transition(RotationState.Left, RotationState.Spawn) to kicks(0 to 0, -1 to 0, -1 to -1, 0 to 2, -1 to 2),
        transition(RotationState.Spawn, RotationState.Left) to kicks(0 to 0, 1 to 0, 1 to 1, 0 to -2, 1 to -2),
    )

    val iPiece: Map<RotationTransition, List<KickOffset>> = mapOf(
        transition(RotationState.Spawn, RotationState.Right) to kicks(0 to 0, -2 to 0, 1 to 0, -2 to -1, 1 to 2),
        transition(RotationState.Right, RotationState.Spawn) to kicks(0 to 0, 2 to 0, -1 to 0, 2 to 1, -1 to -2),
        transition(RotationState.Right, RotationState.Reverse) to kicks(0 to 0, -1 to 0, 2 to 0, -1 to 2, 2 to -1),
        transition(RotationState.Reverse, RotationState.Right) to kicks(0 to 0, 1 to 0, -2 to 0, 1 to -2, -2 to 1),
        transition(RotationState.Reverse, RotationState.Left) to kicks(0 to 0, 2 to 0, -1 to 0, 2 to 1, -1 to -2),
        transition(RotationState.Left, RotationState.Reverse) to kicks(0 to 0, -2 to 0, 1 to 0, -2 to -1, 1 to 2),
        transition(RotationState.Left, RotationState.Spawn) to kicks(0 to 0, 1 to 0, -2 to 0, 1 to -2, -2 to 1),
        transition(RotationState.Spawn, RotationState.Left) to kicks(0 to 0, -1 to 0, 2 to 0, -1 to 2, 2 to -1),
    )

    fun kicksFor(type: TetrominoType, from: RotationState, to: RotationState): List<KickOffset> {
        if (type == TetrominoType.O) {
            return listOf(KickOffset(0, 0))
        }
        val table = if (type == TetrominoType.I) iPiece else standard
        return table.getValue(transition(from, to))
    }

    private fun transition(from: RotationState, to: RotationState): RotationTransition {
        return RotationTransition(from = from, to = to)
    }

    private fun kicks(vararg offsets: Pair<Int, Int>): List<KickOffset> {
        return offsets.map { (dx, dy) -> KickOffset(dx = dx, dy = dy) }
    }
}
