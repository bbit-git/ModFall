package com.bigbangit.blockdrop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigbangit.blockdrop.core.ActivePiece
import com.bigbangit.blockdrop.core.BoardSnapshot
import com.bigbangit.blockdrop.core.EffectBridge
import com.bigbangit.blockdrop.core.GameEffect
import com.bigbangit.blockdrop.core.GameLoop
import com.bigbangit.blockdrop.core.GameState
import com.bigbangit.blockdrop.core.LoopSnapshot
import com.bigbangit.blockdrop.data.ScoreSubmissionResult
import com.bigbangit.blockdrop.data.ScoreboardEntry
import com.bigbangit.blockdrop.data.ScoreboardRepository
import com.bigbangit.blockdrop.data.SettingsRepository
import com.bigbangit.blockdrop.music.DefaultModMusicService
import com.bigbangit.blockdrop.music.ModTrackInfo
import com.bigbangit.blockdrop.music.ModMusicService
import com.bigbangit.blockdrop.ui.model.ActivePieceUiModel
import com.bigbangit.blockdrop.ui.model.BoardCell
import com.bigbangit.blockdrop.ui.model.CelebrationType
import com.bigbangit.blockdrop.ui.model.GameUiModel
import com.bigbangit.blockdrop.ui.model.PauseReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameViewModel(
    private val gameLoop: GameLoop,
    private val effectBridge: EffectBridge,
    private val settingsRepository: SettingsRepository,
    private val scoreboardRepository: ScoreboardRepository,
    private val modMusicService: ModMusicService = DefaultModMusicService(),
) : ViewModel() {
    private val _uiModel = MutableStateFlow(GameUiModel())
    val uiModel: StateFlow<GameUiModel> = _uiModel.asStateFlow()
    private var musicPausedForBackground = false

    init {
        viewModelScope.launch {
            modMusicService.trackChanges.collect { trackInfo ->
                _uiModel.update { current ->
                    current.copy(
                        trackDisplay = trackInfo.displayString(),
                        trackDisplayKey = current.trackDisplayKey + 1,
                    )
                }
                refreshMusicState()
            }
        }
        viewModelScope.launch {
            settingsRepository.isMuted.collect { isMuted ->
                modMusicService.setEnabled(!isMuted)
                refreshMusicState(isMutedOverride = isMuted)
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
        viewModelScope.launch {
            settingsRepository.musicFolderUri.collect { musicFolderUri ->
                modMusicService.setLibraryTreeUri(musicFolderUri)
                _uiModel.update { current -> current.copy(musicFolderUri = musicFolderUri) }
                refreshMusicState()
            }
        }
        viewModelScope.launch {
            scoreboardRepository.entries.collect { entries ->
                _uiModel.update { current -> current.copy(scoreboardEntries = entries) }
            }
        }
        viewModelScope.launch {
            effectBridge.effects.collect { effect ->
                handleEffect(effect)
            }
        }
    }

    fun onForegrounded() {
        modMusicService.rescanLibrary()
        var shouldResume = false
        _uiModel.update { current ->
            if (current.state == GameState.Paused && current.pauseReason == PauseReason.Lifecycle) {
                shouldResume = true
                current.copy(pauseReason = null)
            } else {
                current
            }
        }
        if (shouldResume) {
            gameLoop.resume()
        }
        resumeMusicIfNeededAfterBackground()
        refreshMusicState()
    }

    fun onBackgrounded() {
        musicPausedForBackground = modMusicService.isPlaying()
        if (musicPausedForBackground) {
            modMusicService.pause()
        }
        var shouldPause = false
        _uiModel.update { current ->
            if (current.state == GameState.Running) {
                shouldPause = true
                current.copy(pauseReason = PauseReason.Lifecycle)
            } else {
                current
            }
        }
        if (shouldPause) {
            gameLoop.pause()
        }
        refreshMusicState()
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
        if (shouldPause) {
            gameLoop.pause()
            modMusicService.pause()
        }
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
        if (shouldResume) {
            gameLoop.resume()
            modMusicService.resume()
        }
    }

    fun startGame() {
        modMusicService.stop()
        gameLoop.start(scope = viewModelScope, onStateChanged = ::consumeSnapshot)
        modMusicService.start()
    }

    fun quitGame() {
        gameLoop.stop()
        modMusicService.stop()
    }

    fun moveLeft() = gameLoop.moveLeft()
    fun moveRight() = gameLoop.moveRight()
    fun rotateClockwise() = gameLoop.rotateClockwise()
    fun rotateCounterClockwise() = gameLoop.rotateCounterClockwise()
    fun softDrop() = gameLoop.softDrop()
    fun hardDrop() = gameLoop.hardDrop()
    fun hold() = gameLoop.hold()
    fun activateDropDelay() = gameLoop.activateDropDelay()

    fun toggleMute() {
        val nextValue = !_uiModel.value.isMuted
        viewModelScope.launch {
            settingsRepository.setMuted(nextValue)
        }
    }

    fun refreshMusicLibrary() {
        modMusicService.rescanLibrary()
        refreshMusicState()
    }

    fun openMusicLibrary() {
        modMusicService.rescanLibrary()
        var shouldPause = false
        _uiModel.update { current ->
            current.copy(
                showMusicLibrary = true,
                pauseReason = if (current.state == GameState.Running) {
                    shouldPause = true
                    PauseReason.MusicLibrary
                } else {
                    current.pauseReason
                },
            )
        }
        if (shouldPause) {
            gameLoop.pause()
        }
        refreshMusicState()
    }

    fun closeMusicLibrary() {
        _uiModel.update { current -> current.copy(showMusicLibrary = false) }
    }

    fun selectTrack(track: ModTrackInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            modMusicService.playTrack(track)
            val trackLoadError = if (modMusicService.currentTrackInfo()?.pathOrUri == track.pathOrUri) {
                null
            } else {
                track.fileName
            }
            withContext(Dispatchers.Main) {
                refreshMusicState(trackLoadError = trackLoadError)
            }
        }
    }

    fun pauseMusic() {
        modMusicService.pause()
        refreshMusicState()
    }

    fun resumeMusic() {
        modMusicService.resume()
        if (!modMusicService.isPlaying()) {
            modMusicService.currentTrackInfo()?.let(modMusicService::playTrack)
        }
        refreshMusicState()
    }

    fun stopMusic() {
        modMusicService.stopPlayback()
        refreshMusicState()
    }

    fun setMusicFolderUri(treeUri: String?) {
        viewModelScope.launch {
            settingsRepository.setMusicFolderUri(treeUri)
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

    fun updateNickname(input: String) {
        val sanitizedInput = ScoreboardRepository.sanitizeNickname(input)
        _uiModel.update { current ->
            current.copy(
                pendingNickname = sanitizedInput,
                nicknameError = null,
            )
        }
    }

    fun showScoreboard() {
        _uiModel.update { current -> current.copy(isScoreboardVisible = true) }
    }

    fun dismissScoreboard() {
        _uiModel.update { current -> current.copy(isScoreboardVisible = false) }
    }

    fun submitScore() {
        val currentState = _uiModel.value
        if (currentState.state != GameState.GameOver || currentState.hasSubmittedScore || currentState.isSubmittingScore) {
            return
        }

        val nickname = ScoreboardRepository.sanitizeNickname(currentState.pendingNickname)
        if (nickname.isBlank()) {
            _uiModel.update { current ->
                current.copy(nicknameError = "nickname_required")
            }
            return
        }

        _uiModel.update { current ->
            current.copy(
                isSubmittingScore = true,
                nicknameError = null,
                qualifiedRank = null,
            )
        }

        viewModelScope.launch {
            val result = scoreboardRepository.submit(
                ScoreboardEntry(
                    nickname = nickname,
                    score = _uiModel.value.score,
                    level = _uiModel.value.level,
                    lines = _uiModel.value.lines,
                ),
            )
            applyScoreSubmissionResult(result)
        }
    }

    override fun onCleared() {
        gameLoop.stop()
        modMusicService.close()
        super.onCleared()
    }

    private fun applyScoreSubmissionResult(result: ScoreSubmissionResult) {
        _uiModel.update { current ->
            val qualifiedRank = result.rankedEntries.firstOrNull {
                it.entry.nickname == current.pendingNickname &&
                    it.entry.score == current.score &&
                    it.entry.level == current.level &&
                    it.entry.lines == current.lines
            }?.rank
            current.copy(
                scoreboardEntries = result.rankedEntries,
                isScoreboardVisible = true,
                isSubmittingScore = false,
                hasSubmittedScore = true,
                didQualifyForScoreboard = result.accepted,
                qualifiedRank = qualifiedRank,
            )
        }
    }

    private fun consumeSnapshot(snapshot: LoopSnapshot) {
        _uiModel.update { current ->
            val enteringGameOver = snapshot.state == GameState.GameOver && current.state != GameState.GameOver
            val placementFlashCells = detectNewLockedCells(current.board, snapshot.board)
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
                isScoreboardVisible = if (snapshot.state == GameState.GameOver) current.isScoreboardVisible else false,
                pendingNickname = if (enteringGameOver) "" else if (snapshot.state == GameState.GameOver) current.pendingNickname else "",
                isSubmittingScore = if (snapshot.state == GameState.GameOver) current.isSubmittingScore else false,
                hasSubmittedScore = if (snapshot.state == GameState.GameOver) current.hasSubmittedScore else false,
                didQualifyForScoreboard = if (snapshot.state == GameState.GameOver) current.didQualifyForScoreboard else null,
                qualifiedRank = if (snapshot.state == GameState.GameOver) current.qualifiedRank else null,
                nicknameError = if (snapshot.state == GameState.GameOver) current.nicknameError else null,
                placementFlashCells = placementFlashCells,
                placementFlashAnimationKey = if (placementFlashCells.isNotEmpty()) current.placementFlashAnimationKey + 1 else current.placementFlashAnimationKey,
            )
        }
    }

    private fun handleEffect(effect: GameEffect) {
        _uiModel.update { current ->
            when (effect) {
                is GameEffect.LineClear -> current.copy(
                    lineClearLines = effect.lines,
                    lineClearAnimationKey = current.lineClearAnimationKey + 1,
                    celebrationType = if (effect.lines >= 4) CelebrationType.Tetris else current.celebrationType,
                    celebrationAnimationKey = if (effect.lines >= 4) current.celebrationAnimationKey + 1 else current.celebrationAnimationKey,
                )
                GameEffect.TSpinClear -> current.copy(
                    celebrationType = CelebrationType.TSpin,
                    celebrationAnimationKey = current.celebrationAnimationKey + 1,
                )
                GameEffect.AllClear -> current.copy(
                    celebrationType = CelebrationType.AllClear,
                    celebrationAnimationKey = current.celebrationAnimationKey + 1,
                )
                GameEffect.LevelUp -> current.copy(
                    levelUpAnimationKey = current.levelUpAnimationKey + 1,
                )
                else -> current
            }
        }
    }

    private fun detectNewLockedCells(
        previous: BoardSnapshot,
        next: BoardSnapshot,
    ): List<BoardCell> {
        val cells = mutableListOf<BoardCell>()
        for (y in next.cells.indices) {
            for (x in next.cells[y].indices) {
                val previousValue = previous.cells[y][x]
                val nextValue = next.cells[y][x]
                if (previousValue == 0 && nextValue != 0) {
                    cells += BoardCell(x = x, y = y)
                }
            }
        }
        return cells
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

    private fun refreshMusicState(
        isMutedOverride: Boolean? = null,
        trackLoadError: String? = null,
    ) {
        val tracks = modMusicService.tracks()
        val currentTrack = modMusicService.currentTrackInfo()
        val isPlaying = modMusicService.isPlaying()
        _uiModel.update { current ->
            current.copy(
                isMuted = isMutedOverride ?: current.isMuted,
                availableTracks = tracks,
                currentTrack = currentTrack,
                isMusicPlaying = isPlaying,
                trackLoadError = trackLoadError,
            )
        }
    }

    private fun resumeMusicIfNeededAfterBackground() {
        if (!musicPausedForBackground) return
        musicPausedForBackground = false
        modMusicService.resume()
        if (!modMusicService.isPlaying()) {
            modMusicService.currentTrackInfo()?.let(modMusicService::playTrack)
        }
    }
}
