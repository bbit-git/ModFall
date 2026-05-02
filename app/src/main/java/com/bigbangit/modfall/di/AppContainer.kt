package com.bigbangit.modfall.di

import android.content.Context
import com.bigbangit.modfall.core.BoardState
import com.bigbangit.modfall.core.EffectBridge
import com.bigbangit.modfall.core.GameLoop
import com.bigbangit.modfall.core.PieceGenerator
import com.bigbangit.modfall.data.ScoreboardRepository
import com.bigbangit.modfall.data.SettingsRepository
import com.bigbangit.modfall.effects.SoundManager
import com.bigbangit.modfall.effects.VibrationManager
import com.bigbangit.modfall.music.DefaultModMusicService
import com.bigbangit.modfall.music.ModMusicService

class AppContainer(
    context: Context,
) {
    private val applicationContext = context.applicationContext

    val boardState: BoardState = BoardState()
    val pieceGenerator: PieceGenerator = PieceGenerator()
    val effectBridge: EffectBridge = EffectBridge()
    val settingsRepository: SettingsRepository = SettingsRepository(applicationContext)
    val scoreboardRepository: ScoreboardRepository = ScoreboardRepository(applicationContext)
    val soundManager: SoundManager = SoundManager(
        context = applicationContext,
        effectBridge = effectBridge,
        isMuted = settingsRepository.isMuted,
        sfxVolume = settingsRepository.sfxVolume,
    )
    val vibrationManager: VibrationManager = VibrationManager(
        context = applicationContext,
        effectBridge = effectBridge,
        isMuted = settingsRepository.isMuted,
    )
    val modMusicService: ModMusicService = DefaultModMusicService(context = applicationContext)
    val gameLoop: GameLoop = GameLoop(
        boardState = boardState,
        pieceGenerator = pieceGenerator,
        effectBridge = effectBridge,
    )
}
