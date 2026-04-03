package com.bigbangit.blockdrop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigbangit.blockdrop.core.ActivePiece
import com.bigbangit.blockdrop.core.GameLoop
import com.bigbangit.blockdrop.core.GameState
import com.bigbangit.blockdrop.core.LoopSnapshot
import com.bigbangit.blockdrop.data.SettingsRepository
import com.bigbangit.blockdrop.ui.model.ActivePieceUiModel
import com.bigbangit.blockdrop.ui.model.BoardCell
import com.bigbangit.blockdrop.ui.model.GameUiModel
import com.bigbangit.blockdrop.ui.model.PauseReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
    private val gameLoop: GameLoop,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiModel = MutableStateFlow(GameUiModel())
    val uiModel: StateFlow<GameUiModel> = _uiModel.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.isMuted.collect { isMuted ->
                _uiModel.update { current -> current.copy(isMuted = isMuted) }
            }
        }
        viewModelScope.launch {
            settingsRepository.shouldShowTutorialOnLaunch.collect { shouldShowTutorial ->
                _uiModel.update { current ->
                    if (!current.showTutorial && shouldShowTutorial) {
                        current.copy(showTutorial = true)
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun toggleMute() {
        val nextValue = !_uiModel.value.isMuted
        viewModelScope.launch {
            settingsRepository.setMuted(nextValue)
        }
    }

    fun showTutorial() {
        _uiModel.update { current -> current.copy(showTutorial = true) }
    }

    fun dismissTutorial() {
        _uiModel.update { current -> current.copy(showTutorial = false) }
        viewModelScope.launch {
            settingsRepository.markTutorialSeen()
        }
    }

    fun onForegrounded() {
        var shouldResume = false
        _uiModel.update { current ->
            if (current.state == GameState.Paused && current.pauseReason == PauseReason.Lifecycle) {
                shouldResume = true
                current.copy(pauseReason = null)
            } else {
                current
            }
        }
        if (shouldResume) gameLoop.resume()
    }

    fun onBackgrounded() {
        var shouldPause = false
        _uiModel.update { current ->
            if (current.state == GameState.Running) {
                shouldPause = true
                current.copy(pauseReason = PauseReason.Lifecycle)
            } else {
                current
            }
        }
        if (shouldPause) gameLoop.pause()
    }

    fun pauseGame() {
        var shouldPause = false
        _uiModel.update { current ->
            if (current.state == GameState.Running) {
                shouldPause = true
                current.copy(pauseReason = PauseReason.User)
            } else {
                current
            }
        }
        if (shouldPause) gameLoop.pause()
    }

    fun resumeGame() {
        var shouldResume = false
        _uiModel.update { current ->
            if (current.state == GameState.Paused) {
                shouldResume = true
                current.copy(pauseReason = null)
            } else {
                current
            }
        }
        if (shouldResume) gameLoop.resume()
    }

    fun startGame() {
        gameLoop.start(scope = viewModelScope, onStateChanged = ::consumeSnapshot)
    }

    fun quitGame() {
        gameLoop.stop()
    }

    fun moveLeft() = gameLoop.moveLeft()
    fun moveRight() = gameLoop.moveRight()
    fun rotateClockwise() = gameLoop.rotateClockwise()
    fun rotateCounterClockwise() = gameLoop.rotateCounterClockwise()
    fun softDrop() = gameLoop.softDrop()
    fun hardDrop() = gameLoop.hardDrop()
    fun hold() = gameLoop.hold()
    fun activateDropDelay() = gameLoop.activateDropDelay()

    override fun onCleared() {
        gameLoop.stop()
        super.onCleared()
    }

    private fun consumeSnapshot(snapshot: LoopSnapshot) {
        _uiModel.update { current ->
            current.copy(
                state = snapshot.state,
                score = snapshot.score,
                level = snapshot.level,
                lines = snapshot.lines,
                board = snapshot.board,
                activePiece = snapshot.activePiece?.toUiModel(),
                ghostCells = snapshot.ghostPiece?.cells()?.map(::toBoardCell).orEmpty(),
                nextPieces = snapshot.nextPieces,
                heldPiece = snapshot.heldPiece,
                canHold = snapshot.canHold,
                canUseDropDelay = snapshot.isDropDelayAvailable,
                lockResetCount = snapshot.lockResetCount,
                isGrounded = snapshot.isGrounded,
                pauseReason = if (snapshot.state == GameState.Paused) (current.pauseReason ?: PauseReason.User) else null,
            )
        }
    }

    private fun ActivePiece.toUiModel(): ActivePieceUiModel {
        return ActivePieceUiModel(
            type = type,
            cells = cells().map(::toBoardCell),
        )
    }

    private fun toBoardCell(cell: com.bigbangit.blockdrop.core.PieceCell): BoardCell {
        return BoardCell(x = cell.x, y = cell.y)
    }
}
