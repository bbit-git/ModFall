package com.bigbangit.blockdrop.di

import android.content.Context
import com.bigbangit.blockdrop.core.BoardState
import com.bigbangit.blockdrop.core.EffectBridge
import com.bigbangit.blockdrop.core.GameLoop
import com.bigbangit.blockdrop.core.PieceGenerator
import com.bigbangit.blockdrop.data.SettingsRepository

class AppContainer(
    context: Context,
) {
    val boardState: BoardState = BoardState()
    val pieceGenerator: PieceGenerator = PieceGenerator()
    val effectBridge: EffectBridge = EffectBridge()
    val settingsRepository: SettingsRepository = SettingsRepository(context.applicationContext)
    val gameLoop: GameLoop = GameLoop(
        boardState = boardState,
        pieceGenerator = pieceGenerator,
        effectBridge = effectBridge,
    )
}
