package com.bigbangit.modfall.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class GameEffect {
    data object Move : GameEffect()
    data object Rotate : GameEffect()
    data object Hold : GameEffect()
    data object SoftDrop : GameEffect()
    data object HardDrop : GameEffect()
    data class LineClear(val lines: Int) : GameEffect()
    data object TSpinClear : GameEffect()
    data object AllClear : GameEffect()
    data object LevelUp : GameEffect()
    data object GameOver : GameEffect()
}

class EffectBridge {
    private val _effects = MutableSharedFlow<GameEffect>(replay = 0, extraBufferCapacity = 8)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    fun emit(effect: GameEffect) {
        _effects.tryEmit(effect)
    }
}
