package com.bigbangit.modfall.music

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.util.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Streams decoded PCM from libopenmpt to an [AudioTrack] on a dedicated thread.
 *
 * Lifecycle: [load] → [play] → … → [stop] → [release].
 * [onTrackFinished] fires when a track reaches its natural end (not on [stop]).
 */
class ModPlayer(
    private val context: Context? = null,
    override var onTrackFinished: (() -> Unit)? = null,
) : ModPlayerController {
    private val nativeLock = Any()
    private val playing = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val currentInfo = AtomicReference<ModTrackInfo?>(null)

    @Volatile
    private var moduleHandle: Long = 0L

    @Volatile
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var playbackThread: Thread? = null

    @Volatile
    private var volume: Float = 1f

    /**
     * Load a module file and prepare for playback.
     * Returns the enriched [ModTrackInfo] with title from metadata, or null on failure.
     */
    override fun load(track: ModTrackInfo): ModTrackInfo? {
        release()

        val bytes = try {
            readTrackBytes(track)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read file: ${track.pathOrUri}", e)
            return null
        }

        val handle = try {
            synchronized(nativeLock) {
                OpenMptJni.nativeOpen(bytes, bytes.size)
            }
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "libopenmpt_jni not available", e)
            return null
        }

        if (handle == 0L) {
            Log.w(TAG, "Failed to open module: ${track.fileName}")
            return null
        }

        synchronized(nativeLock) {
            moduleHandle = handle
        }

        val metadataTitle = try {
            synchronized(nativeLock) {
                OpenMptJni.nativeGetTitle(handle)
            }
        } catch (_: Exception) {
            null
        }

        val enriched = if (!metadataTitle.isNullOrBlank()) {
            track.copy(title = metadataTitle)
        } else {
            track
        }

        currentInfo.set(enriched)
        return enriched
    }

    /** Start streaming audio on a background thread. */
    override fun play() {
        if (moduleHandle == 0L) return
        if (playing.getAndSet(true)) return
        paused.set(false)

        val at = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(BUFFER_FRAMES * 2 * Short.SIZE_BYTES * 2)
            .build()

        at.setVolume(volume)
        audioTrack = at

        playbackThread = Thread({
            at.play()
            val buf = ShortArray(BUFFER_FRAMES * 2) // interleaved stereo
            var reachedEnd = false

            while (playing.get()) {
                if (paused.get()) {
                    try {
                        Thread.sleep(50)
                    } catch (_: InterruptedException) {
                        break
                    }
                    continue
                }

                val frames = try {
                    synchronized(nativeLock) {
                        val handle = moduleHandle
                        if (handle == 0L) {
                            0
                        } else {
                            OpenMptJni.nativeReadStereo(handle, SAMPLE_RATE, buf, BUFFER_FRAMES)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Read error", e)
                    0
                }

                if (frames <= 0) {
                    reachedEnd = true
                    break
                }

                at.write(buf, 0, frames * 2)
            }

            at.stop()
            playbackThread = null
            if (reachedEnd) {
                onTrackFinished?.invoke()
            }
        }, "ModPlayer-stream").also { it.start() }
    }

    override fun pause() {
        paused.set(true)
        audioTrack?.pause()
    }

    override fun resume() {
        if (!playing.get()) return
        paused.set(false)
        audioTrack?.play()
    }

    override fun stop() {
        playing.set(false)
        paused.set(false)
        val thread = playbackThread
        if (thread != null && thread !== Thread.currentThread()) {
            thread.interrupt()
            runCatching { thread.join() }
        }
        playbackThread = null
        audioTrack?.let { at ->
            runCatching { at.stop() }
            runCatching { at.release() }
        }
        audioTrack = null
        currentInfo.set(null)
    }

    override fun release() {
        stop()
        synchronized(nativeLock) {
            val handle = moduleHandle
            moduleHandle = 0L
            if (handle != 0L) {
                try {
                    OpenMptJni.nativeClose(handle)
                } catch (_: UnsatisfiedLinkError) {
                    // library not loaded — nothing to free
                }
            }
        }
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        audioTrack?.setVolume(volume)
    }

    override fun currentTrackInfo(): ModTrackInfo? = currentInfo.get()

    override fun isPlaying(): Boolean = playing.get() && !paused.get()

    private fun readTrackBytes(track: ModTrackInfo): ByteArray {
        val parsedUri = runCatching { Uri.parse(track.pathOrUri) }.getOrNull()
        if (parsedUri?.scheme == ContentResolver.SCHEME_CONTENT) {
            val resolver = context?.contentResolver
                ?: throw IllegalStateException("Cannot load content URI without a context")
            return resolver.openInputStream(parsedUri)?.use { input ->
                input.readBytes()
            } ?: throw IllegalStateException("Cannot open content URI: ${track.pathOrUri}")
        }

        return File(track.pathOrUri).readBytes()
    }

    companion object {
        private const val TAG = "ModPlayer"
        private const val SAMPLE_RATE = 48_000
        private const val BUFFER_FRAMES = 2048
    }
}
