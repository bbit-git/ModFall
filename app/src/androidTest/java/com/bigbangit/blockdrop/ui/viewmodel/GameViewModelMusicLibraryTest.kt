package com.bigbangit.blockdrop.ui.viewmodel

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bigbangit.blockdrop.core.BoardState
import com.bigbangit.blockdrop.core.EffectBridge
import com.bigbangit.blockdrop.core.GameLoop
import com.bigbangit.blockdrop.core.GameState
import com.bigbangit.blockdrop.core.PieceGenerator
import com.bigbangit.blockdrop.core.TetrominoType
import com.bigbangit.blockdrop.data.ScoreboardRepository
import com.bigbangit.blockdrop.data.SettingsRepository
import com.bigbangit.blockdrop.music.ModMusicService
import com.bigbangit.blockdrop.music.ModTrackInfo
import com.bigbangit.blockdrop.ui.BlockDropApp
import com.bigbangit.blockdrop.ui.theme.BlockDropTheme
import com.bigbangit.blockdrop.ui.model.PauseReason
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class GameViewModelMusicLibraryTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun musicLibraryStateUpdatesTrackPlaybackAndHidesTrackOverlay() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val currentTrack = track("nebula.mod", title = "Nebula")
        val libraryTrack = track("orbit.xm", title = "Orbit")
        val musicService = FakeModMusicService(
            initialCurrentTrack = currentTrack,
            availableTracks = listOf(libraryTrack),
            initialPlaying = true,
        )
        val viewModel = createViewModel(context, musicService)

        composeRule.setContent {
            BlockDropTheme {
                BlockDropApp(viewModel, splashDurationMs = 0L)
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiModel.value.trackDisplay != null
        }
        composeRule.onNodeWithText(currentTrack.displayString()).assertIsDisplayed()

        composeRule.runOnIdle {
            viewModel.startGame()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiModel.value.state == GameState.Running
        }

        composeRule.runOnIdle {
            viewModel.openMusicLibrary()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiModel.value.showMusicLibrary
        }

        assertEquals(GameState.Paused, viewModel.uiModel.value.state)
        assertEquals(PauseReason.MusicLibrary, viewModel.uiModel.value.pauseReason)
        composeRule.onAllNodes(hasText(currentTrack.displayString())).assertCountEquals(0)
        composeRule.onNodeWithText(libraryTrack.displayString()).assertIsDisplayed()

        composeRule.runOnIdle {
            viewModel.selectTrack(libraryTrack)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiModel.value.currentTrack?.pathOrUri == libraryTrack.pathOrUri
        }
        assertTrue(viewModel.uiModel.value.isMusicPlaying)

        composeRule.runOnIdle {
            viewModel.pauseMusic()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiModel.value.isMusicPlaying
        }

        composeRule.runOnIdle {
            viewModel.resumeMusic()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiModel.value.isMusicPlaying
        }

        composeRule.runOnIdle {
            viewModel.stopMusic()
            viewModel.closeMusicLibrary()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiModel.value.showMusicLibrary
        }
        assertFalse(viewModel.uiModel.value.isMusicPlaying)
        assertEquals(GameState.Paused, viewModel.uiModel.value.state)
    }

    @Test
    fun refreshMusicLibraryRescansAndUpdatesAvailableTracks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firstTrack = track("nebula.mod", title = "Nebula")
        val secondTrack = track("orbit.xm", title = "Orbit")
        val musicService = FakeModMusicService(
            initialCurrentTrack = null,
            availableTracks = listOf(firstTrack),
            initialPlaying = false,
        )
        val viewModel = createViewModel(context, musicService)

        composeRule.runOnIdle {
            viewModel.openMusicLibrary()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiModel.value.availableTracks.size == 1
        }

        musicService.availableTracks = listOf(firstTrack, secondTrack)

        composeRule.runOnIdle {
            viewModel.refreshMusicLibrary()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiModel.value.availableTracks.size == 2
        }

        assertEquals(2, musicService.rescanCalls)
        assertEquals(listOf(firstTrack, secondTrack), viewModel.uiModel.value.availableTracks)
    }

    @Test
    fun selectTrackSurfacesLoadFailureInsteadOfBlockingUi() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val brokenTrack = track("broken.mod", title = "Broken")
        val musicService = FakeModMusicService(
            initialCurrentTrack = null,
            availableTracks = listOf(brokenTrack),
            initialPlaying = false,
            failingTracks = setOf(brokenTrack.pathOrUri),
        )
        val viewModel = createViewModel(context, musicService)

        composeRule.runOnIdle {
            viewModel.openMusicLibrary()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiModel.value.showMusicLibrary
        }

        composeRule.runOnIdle {
            viewModel.selectTrack(brokenTrack)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiModel.value.trackLoadError != null
        }

        assertEquals(brokenTrack.fileName, viewModel.uiModel.value.trackLoadError)
        assertNull(viewModel.uiModel.value.currentTrack)
        assertFalse(viewModel.uiModel.value.isMusicPlaying)
    }

    @Test
    fun setMusicFolderUriPersistsUriAndPassesItToMusicService() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val musicService = FakeModMusicService(
            initialCurrentTrack = null,
            availableTracks = emptyList(),
            initialPlaying = false,
        )
        val viewModel = createViewModel(context, musicService)
        val treeUri = "content://mods/tree/primary%3ADownload%2FMods"

        composeRule.runOnIdle {
            viewModel.setMusicFolderUri(treeUri)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiModel.value.musicFolderUri == treeUri
        }

        assertEquals(treeUri, musicService.configuredLibraryTreeUri)
    }

    @Test
    fun startGamePrefersSelectedMainTrackWhenMusicIsEnabled() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firstTrack = track("nebula.mod", title = "Nebula")
        val secondTrack = track("orbit.xm", title = "Orbit")
        val musicService = FakeModMusicService(
            initialCurrentTrack = null,
            availableTracks = listOf(firstTrack, secondTrack),
            initialPlaying = false,
        )
        val viewModel = createViewModel(context, musicService)

        composeRule.runOnIdle {
            viewModel.setMainTrack(secondTrack)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiModel.value.mainTrackPathOrUri == secondTrack.pathOrUri
        }

        composeRule.runOnIdle {
            viewModel.startGame()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiModel.value.currentTrack?.pathOrUri == secondTrack.pathOrUri
        }

        assertEquals(secondTrack.pathOrUri, musicService.lastPlayedTrackPath)
        assertTrue(viewModel.uiModel.value.isMusicPlaying)
    }

    @Test
    fun musicDisabledBlocksManualPlaybackButKeepsMainTrackSelectionAvailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firstTrack = track("nebula.mod", title = "Nebula")
        val secondTrack = track("orbit.xm", title = "Orbit")
        val musicService = FakeModMusicService(
            initialCurrentTrack = null,
            availableTracks = listOf(firstTrack, secondTrack),
            initialPlaying = false,
        )
        val viewModel = createViewModel(context, musicService)

        composeRule.runOnIdle {
            viewModel.toggleMusicEnabled()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiModel.value.musicEnabled
        }

        composeRule.runOnIdle {
            viewModel.setMainTrack(secondTrack)
            viewModel.selectTrack(firstTrack)
            viewModel.startGame()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiModel.value.mainTrackPathOrUri == secondTrack.pathOrUri
        }

        assertEquals(secondTrack.pathOrUri, viewModel.uiModel.value.mainTrackPathOrUri)
        assertNull(viewModel.uiModel.value.currentTrack)
        assertFalse(viewModel.uiModel.value.isMusicPlaying)
        assertEquals(0, musicService.playTrackCalls)
    }

    private fun createViewModel(
        context: Context,
        musicService: FakeModMusicService,
    ): GameViewModel {
        val boardState = BoardState()
        val effectBridge = EffectBridge()
        val gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(Random(1), initialQueue = listOf(TetrominoType.T, TetrominoType.O)),
            effectBridge = effectBridge,
        )
        val settingsRepository = SettingsRepository(context)
        runBlocking {
            settingsRepository.setMuted(false)
            settingsRepository.setButtonsEnabled(true)
            settingsRepository.setGesturesEnabled(true)
            settingsRepository.setMusicEnabled(true)
            settingsRepository.setMainTrackPathOrUri(null)
            settingsRepository.setMusicFolderUri(null)
        }
        val scoreboardRepository = ScoreboardRepository(context)
        return GameViewModel(
            gameLoop = gameLoop,
            effectBridge = effectBridge,
            settingsRepository = settingsRepository,
            scoreboardRepository = scoreboardRepository,
            modMusicService = musicService,
        )
    }

    private fun track(name: String, title: String? = null) = ModTrackInfo(
        title = title,
        fileName = name,
        format = name.substringAfterLast('.').uppercase(),
        pathOrUri = "/music/$name",
    )

    private class FakeModMusicService(
        initialCurrentTrack: ModTrackInfo?,
        var availableTracks: List<ModTrackInfo>,
        initialPlaying: Boolean,
        private val failingTracks: Set<String> = emptySet(),
    ) : ModMusicService {
        private val trackEvents = MutableSharedFlow<ModTrackInfo>(replay = 1, extraBufferCapacity = 1)
        private var enabled = true
        private var playing = initialPlaying
        private var currentTrack: ModTrackInfo? = initialCurrentTrack
        var playTrackCalls: Int = 0
        var lastPlayedTrackPath: String? = null
        var rescanCalls: Int = 0
        var configuredLibraryTreeUri: String? = null

        override val trackChanges: Flow<ModTrackInfo> = trackEvents

        init {
            currentTrack?.let { trackEvents.tryEmit(it) }
        }

        override fun start() {
            if (enabled && currentTrack != null) {
                playing = true
            }
        }

        override fun stop() {
            playing = false
        }

        override fun stopPlayback() {
            playing = false
        }

        override fun pause() {
            playing = false
        }

        override fun resume() {
            if (enabled && currentTrack != null) {
                playing = true
            }
        }

        override fun playTrack(track: ModTrackInfo) {
            playTrackCalls += 1
            if (track.pathOrUri in failingTracks) {
                currentTrack = null
                playing = false
                return
            }
            currentTrack = track
            lastPlayedTrackPath = track.pathOrUri
            playing = true
            trackEvents.tryEmit(track)
        }

        override fun setEnabled(enabled: Boolean) {
            this.enabled = enabled
            if (!enabled) {
                playing = false
            }
        }

        override fun setVolume(volume: Float) = Unit

        override fun setLibraryTreeUri(treeUri: String?) {
            configuredLibraryTreeUri = treeUri
        }

        override fun rescanLibrary() {
            rescanCalls += 1
        }

        override fun tracks(): List<ModTrackInfo> = availableTracks

        override fun currentTrackInfo(): ModTrackInfo? = currentTrack

        override fun isPlaying(): Boolean = playing

        override fun hasTracks(): Boolean = availableTracks.isNotEmpty()

        override fun close() = Unit
    }
}
