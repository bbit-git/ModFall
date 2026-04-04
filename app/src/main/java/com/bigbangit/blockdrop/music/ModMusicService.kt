package com.bigbangit.blockdrop.music

import kotlinx.coroutines.flow.Flow

interface ModMusicService : AutoCloseable {
    val trackChanges: Flow<ModTrackInfo>

    fun start()
    fun stop()
    fun stopPlayback()
    fun pause()
    /**
     * Resume playback if the underlying player is paused.
     *
     * If the track has already been stopped, callers should reissue [playTrack]
     * for the current track instead of expecting a stopped player to resume.
     */
    fun resume()
    fun playTrack(track: ModTrackInfo)

    fun setEnabled(enabled: Boolean)
    fun setVolume(volume: Float)
    fun setLibraryTreeUri(treeUri: String?)

    fun rescanLibrary()
    fun tracks(): List<ModTrackInfo>
    fun currentTrackInfo(): ModTrackInfo?
    fun isPlaying(): Boolean
    fun hasTracks(): Boolean
}
