package com.bigbangit.modfall.effects

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.bigbangit.modfall.core.EffectBridge
import com.bigbangit.modfall.core.GameEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VibrationManager(
    context: Context,
    private val effectBridge: EffectBridge,
    private val isMuted: Flow<Boolean>,
) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    private val vibrationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var muted: Boolean = false

    private var muteJob: Job? = null
    private var effectJob: Job? = null

    fun start() {
        if (muteJob != null || effectJob != null) return

        muteJob = vibrationScope.launch {
            isMuted.collectLatest { value ->
                muted = value
                if (value) vibrator?.cancel()
            }
        }

        effectJob = vibrationScope.launch {
            effectBridge.effects.collectLatest { effect ->
                if (!muted) {
                    vibrate(effect.toPattern())
                }
            }
        }
    }

    fun stop() {
        muteJob?.cancel()
        effectJob?.cancel()
        muteJob = null
        effectJob = null
        vibrator?.cancel()
        vibrationScope.coroutineContext.cancelChildren()
    }

    private fun vibrate(effect: VibrationEffect?) {
        val target = vibrator ?: return
        if (effect == null || !target.hasVibrator()) return
        target.vibrate(effect)
    }

    private fun GameEffect.toPattern(): VibrationEffect? {
        return when (this) {
            GameEffect.Move -> oneShot(10L, 20)
            GameEffect.Rotate -> oneShot(14L, 40)
            GameEffect.Hold -> waveform(longArrayOf(0, 16, 12, 20), intArrayOf(0, 35, 0, 45))
            GameEffect.SoftDrop -> oneShot(8L, 16)
            GameEffect.HardDrop -> oneShot(24L, 100)
            is GameEffect.LineClear -> waveform(
                timings = longArrayOf(0, 20, 18, 26 + (lines * 4L)),
                amplitudes = intArrayOf(0, 70, 0, 110),
            )
            GameEffect.TSpinClear -> waveform(longArrayOf(0, 20, 12, 28, 16, 36), intArrayOf(0, 90, 0, 120, 0, 150))
            GameEffect.AllClear -> waveform(longArrayOf(0, 18, 12, 24, 12, 32, 12, 40), intArrayOf(0, 80, 0, 100, 0, 120, 0, 160))
            GameEffect.LevelUp -> waveform(longArrayOf(0, 18, 10, 28), intArrayOf(0, 70, 0, 140))
            GameEffect.GameOver -> waveform(longArrayOf(0, 40, 18, 60), intArrayOf(0, 120, 0, 70))
        }
    }

    private fun oneShot(durationMs: Long, amplitude: Int): VibrationEffect {
        return VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
    }

    private fun waveform(timings: LongArray, amplitudes: IntArray): VibrationEffect {
        return VibrationEffect.createWaveform(timings, amplitudes, -1)
    }
}
