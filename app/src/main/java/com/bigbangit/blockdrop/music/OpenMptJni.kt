package com.bigbangit.blockdrop.music

/**
 * Thin JNI bridge to libopenmpt.
 *
 * Each [nativeOpen] call returns an opaque handle that must eventually be
 * passed to [nativeClose]. A handle must only be used from one thread at a
 * time (the caller serialises via the playback thread).
 */
object OpenMptJni {

    init {
        runCatching {
            System.loadLibrary("openmpt_jni")
        }
    }

    /**
     * Create a module from raw file bytes.
     * @return native handle, or 0 on failure.
     */
    @JvmStatic
    external fun nativeOpen(data: ByteArray, length: Int): Long

    /** Destroy a module. Safe to call with 0. */
    @JvmStatic
    external fun nativeClose(handle: Long)

    /**
     * Render interleaved stereo 16-bit PCM.
     * @param outBuf must have capacity >= frames * 2.
     * @return number of frames rendered (0 = end of track).
     */
    @JvmStatic
    external fun nativeReadStereo(handle: Long, sampleRate: Int, outBuf: ShortArray, frames: Int): Int

    /** Module title from metadata, or null. */
    @JvmStatic
    external fun nativeGetTitle(handle: Long): String?

    /** Duration in seconds. */
    @JvmStatic
    external fun nativeGetDuration(handle: Long): Double
}
