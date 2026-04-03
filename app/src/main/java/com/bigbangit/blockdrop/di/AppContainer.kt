package com.bigbangit.blockdrop.di

import com.bigbangit.blockdrop.core.BoardState
import com.bigbangit.blockdrop.core.EffectBridge
import com.bigbangit.blockdrop.core.GameLoop
import com.bigbangit.blockdrop.core.PieceGenerator

class AppContainer {
    val boardState: BoardState = BoardState()
    val pieceGenerator: PieceGenerator = PieceGenerator()
    val effectBridge: EffectBridge = EffectBridge()
    val gameLoop: GameLoop = GameLoop(
        boardState = boardState,
        pieceGenerator = pieceGenerator,
        effectBridge = effectBridge,
    )
}
