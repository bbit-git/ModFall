package com.bigbangit.modfall.ui.model

import com.bigbangit.modfall.core.TetrominoType

data class ActivePieceUiModel(
    val type: TetrominoType,
    val cells: List<BoardCell>,
)
