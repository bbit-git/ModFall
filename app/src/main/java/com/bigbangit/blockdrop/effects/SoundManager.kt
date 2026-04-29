package com.bigbangit.blockdrop.effects

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.bigbangit.blockdrop.core.EffectBridge
import com.bigbangit.blockdrop.core.GameEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections

class SoundManager(
    context: Context,
    private val effectBridge: EffectBridge,
    private val isMuted: Flow<Boolean>,
    private val sfxVolume: Flow<Float>,
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeTracks = Collections.synchronizedSet(mutableSetOf<AudioTrack>())

    @Volatile
    private var muted: Boolean = false

    @Volatile
    private var focusVolumeScale: Float = 1f

    @Volatile
    private var userVolumeScale: Float = 1f

    private var muteJob: Job? = null
    private var volumeJob: Job? = null
    private var effectJob: Job? = null

    @Volatile
    private var hasAudioFocus: Boolean = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> focusVolumeScale = 1f
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> focusVolumeScale = 0.25f
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                focusVolumeScale = 0.5f
                hasAudioFocus = false
                stopActiveTracks()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                focusVolumeScale = 1f
                abandonAudioFocus()
                stopActiveTracks()
            }
        }
    }

    private val focusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
        } else {
            null
        }

    fun start() {
        if (muteJob != null || effectJob != null) return

        muteJob = playbackScope.launch {
            isMuted.collectLatest { value ->
                muted = value
                if (value) {
                    stopActiveTracks()
                    abandonAudioFocus()
                }
            }
        }

        volumeJob = playbackScope.launch {
            sfxVolume.collectLatest { value ->
                userVolumeScale = value.coerceIn(0f, 1f)
            }
        }

        effectJob = playbackScope.launch {
            effectBridge.effects.collectLatest { effect ->
                if (!muted && requestAudioFocus()) {
                    play(effect.toWaveform())
                }
            }
        }
    }

    fun stop() {
        muteJob?.cancel()
        volumeJob?.cancel()
        effectJob?.cancel()
        muteJob = null
        volumeJob = null
        effectJob = null
        stopActiveTracks()
        abandonAudioFocus()
        playbackScope.coroutineContext.cancelChildren()
    }

    private fun play(waveform: ShortArray) {
        if (waveform.isEmpty()) return

        val buffer = ByteBuffer.allocate(waveform.size * Short.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
        waveform.forEach(buffer::putShort)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(
                android.media.AudioFormat.Builder()
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(buffer.array().size)
            .build()

        activeTracks += audioTrack
        audioTrack.write(buffer.array(), 0, buffer.array().size)
        audioTrack.setVolume((BASE_VOLUME * userVolumeScale * focusVolumeScale).coerceIn(0f, 1f))
        audioTrack.notificationMarkerPosition = waveform.size
        audioTrack.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack) {
                    releaseTrack(track)
                }

                override fun onPeriodicNotification(track: AudioTrack) = Unit
            },
        )
        audioTrack.play()
    }

    private fun releaseTrack(track: AudioTrack) {
        activeTracks.remove(track)
        runCatching {
            track.stop()
        }
        runCatching {
            track.release()
        }
    }

    private fun stopActiveTracks() {
        activeTracks.toList().forEach(::releaseTrack)
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        }
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
        hasAudioFocus = false
    }

    private fun GameEffect.toWaveform(): ShortArray {
        return when (this) {
            GameEffect.Move -> synth.square(frequencyHz = 520.0, durationMs = 38, amplitude = 0.16)
            GameEffect.Rotate -> synth.square(frequencyHz = 660.0, durationMs = 52, amplitude = 0.18)
            GameEffect.Hold -> synth.triangle(frequencyHz = 420.0, durationMs = 72, amplitude = 0.18)
            GameEffect.SoftDrop -> synth.square(frequencyHz = 310.0, durationMs = 24, amplitude = 0.10)
            GameEffect.HardDrop -> synth.noise(durationMs = 90, amplitude = 0.16)
            is GameEffect.LineClear -> synth.lineClear(lines = lines)
            GameEffect.TSpinClear -> synth.chime(intArrayOf(660, 880, 990), 90, 0.20)
            GameEffect.AllClear -> synth.chime(intArrayOf(784, 988, 1175, 1568), 100, 0.22)
            GameEffect.LevelUp -> synth.sweep(440.0, 960.0, 180, 0.18)
            GameEffect.GameOver -> synth.descend(intArrayOf(392, 330, 262, 196), 110, 0.18)
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val BASE_VOLUME = 0.6f
        val synth = ChipSynth(SAMPLE_RATE)
    }
}

private class ChipSynth(
    private val sampleRate: Int,
) {
    fun square(frequencyHz: Double, durationMs: Int, amplitude: Double): ShortArray {
        return tone(durationMs, amplitude) { phase ->
            if ((phase % 1.0) < 0.5) 1.0 else -1.0
        }(frequencyHz)
    }

    fun triangle(frequencyHz: Double, durationMs: Int, amplitude: Double): ShortArray {
        return tone(durationMs, amplitude) { phase ->
            val wrapped = phase % 1.0
            4.0 * kotlin.math.abs(wrapped - 0.5) - 1.0
        }(frequencyHz)
    }

    fun noise(durationMs: Int, amplitude: Double): ShortArray {
        val count = sampleCount(durationMs)
        var seed = 0x12345678
        return ShortArray(count) { index ->
            seed = seed xor (seed shl 13)
            seed = seed xor (seed ushr 17)
            seed = seed xor (seed shl 5)
            val envelope = decay(index, count)
            ((seed / Int.MAX_VALUE.toDouble()) * amplitude * envelope * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    fun sweep(startHz: Double, endHz: Double, durationMs: Int, amplitude: Double): ShortArray {
        val count = sampleCount(durationMs)
        return ShortArray(count) { index ->
            val progress = index.toDouble() / count.coerceAtLeast(1)
            val frequency = startHz + ((endHz - startHz) * progress)
            val sample = kotlin.math.sin(2.0 * Math.PI * frequency * index / sampleRate)
            (sample * amplitude * decay(index, count) * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    fun chime(notes: IntArray, durationPerNoteMs: Int, amplitude: Double): ShortArray {
        return notes.fold(ShortArray(0)) { acc, note ->
            acc + square(note.toDouble(), durationPerNoteMs, amplitude)
        }
    }

    fun descend(notes: IntArray, durationPerNoteMs: Int, amplitude: Double): ShortArray {
        return notes.fold(ShortArray(0)) { acc, note ->
            acc + triangle(note.toDouble(), durationPerNoteMs, amplitude)
        }
    }

    fun lineClear(lines: Int): ShortArray {
        val notes = when (lines.coerceIn(1, 4)) {
            1 -> intArrayOf(392, 494)
            2 -> intArrayOf(392, 494, 587)
            3 -> intArrayOf(392, 494, 587, 698)
            else -> intArrayOf(440, 554, 659, 880)
        }
        return chime(notes, durationPerNoteMs = 55, amplitude = 0.20)
    }

    private fun tone(
        durationMs: Int,
        amplitude: Double,
        generator: (Double) -> Double,
    ): (Double) -> ShortArray = { frequencyHz ->
        val count = sampleCount(durationMs)
        ShortArray(count) { index ->
            val phase = frequencyHz * index / sampleRate
            val sample = generator(phase)
            (sample * amplitude * decay(index, count) * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    private fun sampleCount(durationMs: Int): Int {
        return (durationMs * sampleRate / 1_000).coerceAtLeast(1)
    }

    private fun decay(index: Int, total: Int): Double {
        val attack = 0.08
        val release = 0.18
        val progress = index.toDouble() / total.coerceAtLeast(1)
        return when {
            progress < attack -> progress / attack
            progress > 1.0 - release -> (1.0 - progress) / release
            else -> 1.0
        }.coerceIn(0.0, 1.0)
    }
}
