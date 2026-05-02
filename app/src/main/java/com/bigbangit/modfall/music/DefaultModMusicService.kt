package com.bigbangit.modfall.music

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.random.Random

/**
 * Default implementation of [ModMusicService].
 *
 * Owns a [ModLibrary] for scanning and a [ModPlayer] for playback.
 * Track selection avoids immediate repeats when more than one track is available.
 *
 * @param library   storage/library layer
 * @param player    playback layer
 * @param random    injectable RNG for deterministic testing
 */
class DefaultModMusicService(
    private val context: Context? = null,
    private val library: ModLibrary = ModLibrary(context = context),
    private val player: ModPlayerController = ModPlayer(context = context),
    private val random: Random = Random.Default,
) : ModMusicService {
    private val trackEvents = MutableSharedFlow<ModTrackInfo>(extraBufferCapacity = 1)

    override val trackChanges: Flow<ModTrackInfo> = trackEvents.asSharedFlow()

    @Volatile
    private var enabled = true

    @Volatile
    private var lastPlayedPath: String? = null

    @Volatile
    private var started = false

    @Volatile
    private var currentTrack: ModTrackInfo? = null

    @Volatile
    private var libraryTreeUri: String? = null

    init {
        player.onTrackFinished = { playNextTrack() }
    }

    override fun start() {
        started = true
        player.stop()
        rescanLibrary()
        if (!enabled) return
        val selectedTrack = currentTrack
        if (selectedTrack != null) {
            playTrack(selectedTrack)
        } else if (library.tracks().isNotEmpty()) {
            playNextTrack()
        }
    }

    override fun stop() {
        started = false
        player.stop()
    }

    override fun stopPlayback() {
        player.stop()
    }

    override fun pause() {
        player.pause()
    }

    override fun resume() {
        if (enabled && started) {
            player.resume()
        }
    }

    override fun playTrack(track: ModTrackInfo) {
        lastPlayedPath = track.pathOrUri
        currentTrack = track
        if (!enabled) return

        player.stop()
        val loaded = player.load(track)
        if (loaded == null) {
            Log.w(TAG, "Failed to load requested track: ${track.fileName}")
            currentTrack = null
            return
        }

        currentTrack = loaded
        trackEvents.tryEmit(loaded)
        player.play()
    }

    override fun setEnabled(enabled: Boolean) {
        val wasEnabled = this.enabled
        this.enabled = enabled
        if (!enabled) {
            player.stop()
        } else if (!wasEnabled && started) {
            val selectedTrack = currentTrack
            if (selectedTrack != null) {
                playTrack(selectedTrack)
            } else if (library.tracks().isNotEmpty()) {
                playNextTrack()
            }
        }
    }

    override fun setVolume(volume: Float) {
        player.setVolume(volume)
    }

    override fun setLibraryTreeUri(treeUri: String?) {
        libraryTreeUri = treeUri
    }

    override fun rescanLibrary() {
        library.scan(treeUri = libraryTreeUri)
        val currentPath = currentTrack?.pathOrUri ?: return
        currentTrack = library.tracks().firstOrNull { it.pathOrUri == currentPath }
    }

    override fun tracks(): List<ModTrackInfo> = library.tracks()

    override fun currentTrackInfo(): ModTrackInfo? = currentTrack

    override fun isPlaying(): Boolean = player.isPlaying()

    override fun hasTracks(): Boolean = library.tracks().isNotEmpty()

    override fun close() {
        started = false
        player.release()
    }

    /** Select and play the next random track. */
    internal fun playNextTrack() {
        if (!enabled || !started) return

        val tracks = library.tracks()
        if (tracks.isEmpty()) {
            Log.i(TAG, "No tracks available for playback")
            return
        }

        player.stop()

        val enriched = findNextPlayableTrack(
            tracks = tracks,
            lastPlayedPath = lastPlayedPath,
            random = random,
        ) { candidate ->
            val loaded = player.load(candidate)
            if (loaded == null) {
                Log.w(TAG, "Skipping unplayable track: ${candidate.fileName}")
            }
            loaded
        }
        if (enriched == null) {
            Log.w(TAG, "No playable tracks remaining")
            return
        }

        lastPlayedPath = enriched.pathOrUri
        currentTrack = enriched
        trackEvents.tryEmit(enriched)
        player.play()
    }

    companion object {
        private const val TAG = "ModMusicService"

        /**
         * Pick the next track to play, avoiding immediate repeat when possible.
         * Visible for testing.
         */
        fun pickNext(
            tracks: List<ModTrackInfo>,
            lastPlayedPath: String?,
            random: Random,
        ): ModTrackInfo? {
            if (tracks.isEmpty()) return null
            if (tracks.size == 1) return tracks[0]

            val candidates = tracks.filter { it.pathOrUri != lastPlayedPath }
            val pool = candidates.ifEmpty { tracks }
            return pool[random.nextInt(pool.size)]
        }

        fun findNextPlayableTrack(
            tracks: List<ModTrackInfo>,
            lastPlayedPath: String?,
            random: Random,
            loadTrack: (ModTrackInfo) -> ModTrackInfo?,
        ): ModTrackInfo? {
            val remaining = tracks.toMutableList()
            while (remaining.isNotEmpty()) {
                val next = pickNext(remaining, lastPlayedPath, random) ?: return null
                val loaded = loadTrack(next)
                if (loaded != null) {
                    return loaded
                }
                remaining.removeAll { it.pathOrUri == next.pathOrUri }
            }
            return null
        }
    }
}
