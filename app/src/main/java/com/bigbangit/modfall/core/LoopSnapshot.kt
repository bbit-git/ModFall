package com.bigbangit.modfall.core

data class LoopSnapshot(
    val state: GameState,
    val board: BoardSnapshot,
    val activePiece: ActivePiece?,
    val ghostPiece: ActivePiece?,
    val nextPieces: List<TetrominoType>,
    val heldPiece: TetrominoType?,
    val canHold: Boolean,
    val level: Int,
    val lines: Int,
    val score: Int,
    val isDropDelayAvailable: Boolean,
    val lockResetCount: Int,
    val isGrounded: Boolean,
    val comboCount: Int,
    val isBackToBack: Boolean,
)
