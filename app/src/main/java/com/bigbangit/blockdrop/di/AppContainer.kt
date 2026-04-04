package com.bigbangit.blockdrop.di

import android.content.Context
import com.bigbangit.blockdrop.core.BoardState
import com.bigbangit.blockdrop.core.EffectBridge
import com.bigbangit.blockdrop.core.GameLoop
import com.bigbangit.blockdrop.core.PieceGenerator
import com.bigbangit.blockdrop.data.ScoreboardRepository
import com.bigbangit.blockdrop.data.SettingsRepository
import com.bigbangit.blockdrop.effects.SoundManager
import com.bigbangit.blockdrop.effects.VibrationManager
import com.bigbangit.blockdrop.music.DefaultModMusicService
import com.bigbangit.blockdrop.music.ModMusicService

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
