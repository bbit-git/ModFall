package com.bigbangit.blockdrop.music

interface ModPlayerController {
    var onTrackFinished: (() -> Unit)?

    fun load(track: ModTrackInfo): ModTrackInfo?
    fun play()
    fun pause()
    fun resume()
    fun stop()
    fun release()
    fun setVolume(volume: Float)
    fun currentTrackInfo(): ModTrackInfo?
    fun isPlaying(): Boolean
}
