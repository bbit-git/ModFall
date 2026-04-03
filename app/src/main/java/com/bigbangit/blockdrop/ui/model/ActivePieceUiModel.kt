package com.bigbangit.blockdrop.ui.model

import com.bigbangit.blockdrop.core.TetrominoType

data class ActivePieceUiModel(
    val type: TetrominoType,
    val cells: List<BoardCell>,
)
