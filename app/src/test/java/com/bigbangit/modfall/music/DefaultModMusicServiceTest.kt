package com.bigbangit.modfall.music

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultModMusicServiceTest {
    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `playTrack loads emits and starts playback`() = runTest {
        val track = track("nebula.mod")
        val loadedTrack = track.copy(title = "Loaded Nebula")
        val fakePlayer = FakePlayer(loadResult = loadedTrack)
        val service = DefaultModMusicService(
            library = ModLibrary(baseDir = tempDir.newFolder("Mods")),
            player = fakePlayer,
            random = Random(0),
        )
        val emitted = mutableListOf<ModTrackInfo>()
        val collector = launch { service.trackChanges.collect { emitted += it } }

        service.start()
        fakePlayer.resetCounts()
        runCurrent()

        service.playTrack(track)
        runCurrent()

        assertEquals(listOf(loadedTrack), emitted)
        assertEquals(1, fakePlayer.loadCalls)
        assertEquals(1, fakePlayer.playCalls)
        assertTrue(service.isPlaying())
        assertEquals(loadedTrack, service.currentTrackInfo())

        collector.cancel()
    }

    @Test
    fun `playTrack works before service is started`() = runTest {
        val track = track("nebula.mod")
        val loadedTrack = track.copy(title = "Loaded Nebula")
        val fakePlayer = FakePlayer(loadResult = loadedTrack)
        val service = DefaultModMusicService(
            library = ModLibrary(baseDir = tempDir.newFolder("Mods")),
            player = fakePlayer,
            random = Random(0),
        )
        val emitted = mutableListOf<ModTrackInfo>()
        val collector = launch { service.trackChanges.collect { emitted += it } }

        runCurrent()
        service.playTrack(track)
        runCurrent()

        assertEquals(listOf(loadedTrack), emitted)
        assertEquals(1, fakePlayer.loadCalls)
        assertEquals(1, fakePlayer.playCalls)
        assertEquals(loadedTrack, service.currentTrackInfo())
        assertTrue(service.isPlaying())

        collector.cancel()
    }

    @Test
    fun `playTrack clears current track when load fails`() = runTest {
        val track = track("broken.mod")
        val fakePlayer = FakePlayer(loadResult = null)
        val service = DefaultModMusicService(
            library = ModLibrary(baseDir = tempDir.newFolder("Mods")),
            player = fakePlayer,
            random = Random(0),
        )
        val emitted = mutableListOf<ModTrackInfo>()
        val collector = launch { service.trackChanges.collect { emitted += it } }

        service.start()
        fakePlayer.resetCounts()
        runCurrent()

        service.playTrack(track)
        runCurrent()

        assertTrue(emitted.isEmpty())
        assertEquals(1, fakePlayer.loadCalls)
        assertEquals(1, fakePlayer.stopCalls)
        assertEquals(0, fakePlayer.playCalls)
        assertFalse(service.isPlaying())
        assertNull(service.currentTrackInfo())

        collector.cancel()
    }

    private fun track(name: String) = ModTrackInfo(
        title = null,
        fileName = name,
        format = "MOD",
        pathOrUri = "/test/$name",
    )

    private class FakePlayer(
        private val loadResult: ModTrackInfo?,
    ) : ModPlayerController {
        override var onTrackFinished: (() -> Unit)? = null
        var loadCalls = 0
        var playCalls = 0
        var stopCalls = 0
        private var playing = false
        private var currentTrack: ModTrackInfo? = null

        override fun load(track: ModTrackInfo): ModTrackInfo? {
            loadCalls += 1
            currentTrack = loadResult
            playing = false
            return loadResult
        }

        override fun play() {
            playCalls += 1
            playing = true
        }

        override fun pause() {
            playing = false
        }

        override fun resume() {
            if (currentTrack != null) {
                playing = true
            }
        }

        override fun stop() {
            stopCalls += 1
            playing = false
            currentTrack = null
        }

        override fun release() {
            stop()
        }

        override fun setVolume(volume: Float) = Unit

        override fun currentTrackInfo(): ModTrackInfo? = currentTrack

        override fun isPlaying(): Boolean = playing

        fun resetCounts() {
            loadCalls = 0
            playCalls = 0
            stopCalls = 0
        }
    }
}
