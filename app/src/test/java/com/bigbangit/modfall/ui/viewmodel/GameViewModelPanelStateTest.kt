package com.bigbangit.modfall.ui.viewmodel

import com.bigbangit.modfall.core.GameState
import com.bigbangit.modfall.ui.model.GameUiModel
import com.bigbangit.modfall.ui.model.PauseReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewModelPanelStateTest {
    @Test
    fun `opening music library from settings records return path`() {
        val current = GameUiModel(
            state = GameState.Paused,
            showSettings = true,
        )

        val updated = openMusicLibraryPanel(current)

        assertTrue(updated.showMusicLibrary)
        assertFalse(updated.showSettings)
        assertTrue(updated.returnToSettingsFromMusicLibrary)
        assertEquals(current.pauseReason, updated.pauseReason)
    }

    @Test
    fun `closing music library returns to settings when launched there`() {
        val current = GameUiModel(
            showMusicLibrary = true,
            returnToSettingsFromMusicLibrary = true,
        )

        val updated = closeMusicLibraryPanel(current)

        assertFalse(updated.showMusicLibrary)
        assertTrue(updated.showSettings)
        assertFalse(updated.returnToSettingsFromMusicLibrary)
    }

    @Test
    fun `opening music library from running game pauses into music library state`() {
        val current = GameUiModel(state = GameState.Running)

        val updated = openMusicLibraryPanel(current)

        assertTrue(updated.showMusicLibrary)
        assertFalse(updated.showSettings)
        assertFalse(updated.returnToSettingsFromMusicLibrary)
        assertEquals(PauseReason.MusicLibrary, updated.pauseReason)
    }
}
