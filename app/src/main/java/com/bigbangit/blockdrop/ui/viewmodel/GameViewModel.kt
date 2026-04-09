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
import com.bigbangit.blockdrop.ui.model.ParticleImpulseType
import com.bigbangit.blockdrop.ui.model.ParticleQuality
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
    private var pendingHardDropBurst = false

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
                modMusicService.setEnabled(!isMuted && _uiModel.value.musicEnabled)
                refreshMusicState(isMutedOverride = isMuted)
            }
        }
        viewModelScope.launch {
            settingsRepository.buttonsEnabled.collect { buttonsEnabled ->
                _uiModel.update { current -> current.copy(buttonsEnabled = buttonsEnabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.gesturesEnabled.collect { gesturesEnabled ->
                _uiModel.update { current -> current.copy(gesturesEnabled = gesturesEnabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.musicEnabled.collect { musicEnabled ->
                _uiModel.update { current -> current.copy(musicEnabled = musicEnabled) }
                modMusicService.setEnabled(musicEnabled && !_uiModel.value.isMuted)
                if (!musicEnabled) modMusicService.stop()
            }
        }
        viewModelScope.launch {
            settingsRepository.particlesEnabled.collect { particlesEnabled ->
                _uiModel.update { current -> current.copy(particlesEnabled = particlesEnabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.particleQuality.collect { particleQuality ->
                _uiModel.update { current -> current.copy(particleQuality = particleQuality) }
            }
        }
        viewModelScope.launch {
            settingsRepository.mainTrackPathOrUri.collect { pathOrUri ->
                _uiModel.update { current -> current.copy(mainTrackPathOrUri = pathOrUri) }
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
        if (_uiModel.value.musicEnabled && !_uiModel.value.isMuted) {
            modMusicService.start()
            playPreferredMainTrack()
        }
        refreshMusicState()
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

    fun toggleButtonsEnabled() {
        val current = _uiModel.value
        viewModelScope.launch {
            settingsRepository.setButtonsEnabled(!current.buttonsEnabled)
        }
    }

    fun toggleGesturesEnabled() {
        val current = _uiModel.value
        viewModelScope.launch {
            settingsRepository.setGesturesEnabled(!current.gesturesEnabled)
        }
    }

    fun toggleMusicEnabled() {
        val current = _uiModel.value
        viewModelScope.launch {
            settingsRepository.setMusicEnabled(!current.musicEnabled)
        }
    }

    fun toggleParticlesEnabled() {
        val current = _uiModel.value
        viewModelScope.launch {
            settingsRepository.setParticlesEnabled(!current.particlesEnabled)
        }
    }

    fun cycleParticleQuality() {
        val next = when (_uiModel.value.particleQuality) {
            ParticleQuality.Low -> ParticleQuality.High
            ParticleQuality.High -> ParticleQuality.Low
        }
        viewModelScope.launch {
            settingsRepository.setParticleQuality(next)
        }
    }

    fun openSettings() {
        _uiModel.update { current ->
            current.copy(
                showSettings = true,
                returnToSettingsFromMusicLibrary = false,
            )
        }
        if (_uiModel.value.state == GameState.Running) {
            pauseGame()
        }
    }

    fun closeSettings() {
        _uiModel.update { current ->
            current.copy(
                showSettings = false,
                returnToSettingsFromMusicLibrary = false,
            )
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
            openMusicLibraryPanel(current).also { updated ->
                if (updated.pauseReason == PauseReason.MusicLibrary) {
                    shouldPause = true
                }
            }
        }
        if (shouldPause) {
            gameLoop.pause()
        }
        refreshMusicState()
    }

    fun closeMusicLibrary() {
        _uiModel.update(::closeMusicLibraryPanel)
    }

    fun setMainTrack(track: ModTrackInfo) {
        viewModelScope.launch {
            settingsRepository.setMainTrackPathOrUri(track.pathOrUri)
        }
    }

    fun selectTrack(track: ModTrackInfo) {
        if (!_uiModel.value.musicEnabled || _uiModel.value.isMuted) {
            refreshMusicState(trackLoadError = null)
            return
        }
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
        if (!_uiModel.value.musicEnabled || _uiModel.value.isMuted) {
            refreshMusicState()
            return
        }
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
        val placementFlashCells = detectNewLockedCells(_uiModel.value.board, snapshot.board)
        _uiModel.update { current ->
            val enteringGameOver = snapshot.state == GameState.GameOver && current.state != GameState.GameOver
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
                hardDropBurstCells = if (placementFlashCells.isNotEmpty() && pendingHardDropBurst) placementFlashCells else current.hardDropBurstCells,
                hardDropBurstAnimationKey = if (placementFlashCells.isNotEmpty() && pendingHardDropBurst) {
                    current.hardDropBurstAnimationKey + 1
                } else {
                    current.hardDropBurstAnimationKey
                },
            )
        }
        if (placementFlashCells.isNotEmpty() && pendingHardDropBurst) {
            pendingHardDropBurst = false
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
                GameEffect.Move -> current.copy(
                    particleImpulseType = ParticleImpulseType.Move,
                    particleImpulseAnimationKey = current.particleImpulseAnimationKey + 1,
                )
                GameEffect.Rotate -> current.copy(
                    particleImpulseType = ParticleImpulseType.Rotate,
                    particleImpulseAnimationKey = current.particleImpulseAnimationKey + 1,
                )
                GameEffect.SoftDrop -> current.copy(
                    particleImpulseType = ParticleImpulseType.SoftDrop,
                    particleImpulseAnimationKey = current.particleImpulseAnimationKey + 1,
                )
                GameEffect.Hold -> current.copy(
                    particleImpulseType = ParticleImpulseType.Hold,
                    particleImpulseAnimationKey = current.particleImpulseAnimationKey + 1,
                )
                GameEffect.HardDrop -> {
                    pendingHardDropBurst = true
                    current
                }
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

    private fun playPreferredMainTrack() {
        val preferred = _uiModel.value.mainTrackPathOrUri
        val tracks = modMusicService.tracks()
        val candidate = tracks.firstOrNull { it.pathOrUri == preferred } ?: tracks.firstOrNull()
        if (candidate != null && _uiModel.value.musicEnabled && !_uiModel.value.isMuted) {
            modMusicService.playTrack(candidate)
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

internal fun openMusicLibraryPanel(current: GameUiModel): GameUiModel {
    val shouldPauseForLibrary = current.state == GameState.Running
    return current.copy(
        showMusicLibrary = true,
        showSettings = false,
        returnToSettingsFromMusicLibrary = current.showSettings,
        pauseReason = if (shouldPauseForLibrary) PauseReason.MusicLibrary else current.pauseReason,
    )
}

internal fun closeMusicLibraryPanel(current: GameUiModel): GameUiModel {
    val restoreSettings = current.returnToSettingsFromMusicLibrary
    return current.copy(
        showMusicLibrary = false,
        showSettings = restoreSettings,
        returnToSettingsFromMusicLibrary = false,
    )
}
