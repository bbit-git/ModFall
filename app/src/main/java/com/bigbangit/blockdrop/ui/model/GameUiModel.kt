package com.bigbangit.blockdrop.ui.model

import com.bigbangit.blockdrop.core.BoardSnapshot
import com.bigbangit.blockdrop.core.GameConstants
import com.bigbangit.blockdrop.core.GameState
import com.bigbangit.blockdrop.core.TetrominoType
import com.bigbangit.blockdrop.data.RankedScoreboardEntry

data class GameUiModel(
    val state: GameState = GameState.Idle,
    val pauseReason: PauseReason? = null,
    val score: Int = 0,
    val level: Int = 1,
    val lines: Int = 0,
    val board: BoardSnapshot = BoardSnapshot(
        cells = List(GameConstants.BOARD_HEIGHT) { List(GameConstants.BOARD_WIDTH) { 0 } },
    ),
    val activePiece: ActivePieceUiModel? = null,
    val ghostCells: List<BoardCell> = emptyList(),
    val nextPieces: List<TetrominoType> = emptyList(),
    val heldPiece: TetrominoType? = null,
    val canHold: Boolean = true,
    val canUseDropDelay: Boolean = true,
    val lockResetCount: Int = 0,
    val isGrounded: Boolean = false,
    val isMuted: Boolean = false,
    val showTutorial: Boolean = false,
    val scoreboardEntries: List<RankedScoreboardEntry> = emptyList(),
    val isScoreboardVisible: Boolean = false,
    val pendingNickname: String = "",
    val isSubmittingScore: Boolean = false,
    val hasSubmittedScore: Boolean = false,
    val didQualifyForScoreboard: Boolean? = null,
    val qualifiedRank: Int? = null,
    val nicknameError: String? = null,
    val lineClearLines: Int? = null,
    val lineClearAnimationKey: Int = 0,
    val placementFlashCells: List<BoardCell> = emptyList(),
    val placementFlashAnimationKey: Int = 0,
    val levelUpAnimationKey: Int = 0,
    val celebrationType: CelebrationType? = null,
    val celebrationAnimationKey: Int = 0,
)
