package com.bigbangit.blockdrop.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bigbangit.blockdrop.core.GameState
import com.bigbangit.blockdrop.music.ModTrackInfo
import com.bigbangit.blockdrop.ui.model.GameUiModel
import com.bigbangit.blockdrop.ui.theme.BlockDropTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockDropScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun runningLayoutShowsCompactStatsAndHardDropControl() {
        var hardDropClicks = 0

        composeRule.setContent {
            BlockDropTheme {
                BlockDropScreen(
                    uiModel = GameUiModel(
                        state = GameState.Running,
                        score = 10119,
                        level = 4,
                        lines = 34,
                    ),
                    onStartGame = {},
                    onPause = {},
                    onResume = {},
                    onQuit = {},
                    onMuteToggle = {},
                    onOpenMusicLibrary = {},
                    onCloseMusicLibrary = {},
                    onRefreshMusicLibrary = {},
                    onPickMusicFolder = {},
                    onSelectTrack = {},
                    onPauseMusic = {},
                    onResumeMusic = {},
                    onStopMusic = {},
                    onShowTutorial = {},
                    onDismissTutorial = {},
                    onMoveLeft = {},
                    onMoveRight = {},
                    onRotateClockwise = {},
                    onRotateCounterClockwise = {},
                    onSoftDrop = {},
                    onHardDrop = { hardDropClicks += 1 },
                    onHold = {},
                    onDropDelay = {},
                    onNicknameChanged = {},
                    onSubmitScore = {},
                    onShowScoreboard = {},
                    onDismissScoreboard = {},
                    onOpenSettings = {},
                    onCloseSettings = {},
                    onToggleButtonsEnabled = {},
                    onToggleGesturesEnabled = {},
                    onToggleMusicEnabled = {},
                    onSetMainTrack = {},
                )
            }
        }

        composeRule.onNodeWithText("SCORE 10119").assertIsDisplayed()
        composeRule.onNodeWithText("LEVEL 4").assertIsDisplayed()
        composeRule.onNodeWithText("LINES 34").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Left").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Right").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Rotate CW").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Hard drop").assertIsDisplayed().performClick()

        assertEquals(1, hardDropClicks)
    }

    @Test
    fun musicLibraryShowsCurrentTrackAndAvailableTracks() {
        val currentTrack = ModTrackInfo(
            title = "Nebula",
            fileName = "nebula.mod",
            format = "MOD",
            pathOrUri = "/tmp/nebula.mod",
        )
        val otherTrack = ModTrackInfo(
            title = "Orbit",
            fileName = "orbit.xm",
            format = "XM",
            pathOrUri = "/tmp/orbit.xm",
        )

        composeRule.setContent {
            BlockDropTheme {
                BlockDropScreen(
                    uiModel = GameUiModel(
                        state = GameState.Paused,
                        showMusicLibrary = true,
                        availableTracks = listOf(currentTrack, otherTrack),
                        currentTrack = currentTrack,
                        isMusicPlaying = true,
                    ),
                    onStartGame = {},
                    onPause = {},
                    onResume = {},
                    onQuit = {},
                    onMuteToggle = {},
                    onOpenMusicLibrary = {},
                    onCloseMusicLibrary = {},
                    onRefreshMusicLibrary = {},
                    onPickMusicFolder = {},
                    onSelectTrack = {},
                    onPauseMusic = {},
                    onResumeMusic = {},
                    onStopMusic = {},
                    onShowTutorial = {},
                    onDismissTutorial = {},
                    onMoveLeft = {},
                    onMoveRight = {},
                    onRotateClockwise = {},
                    onRotateCounterClockwise = {},
                    onSoftDrop = {},
                    onHardDrop = {},
                    onHold = {},
                    onDropDelay = {},
                    onNicknameChanged = {},
                    onSubmitScore = {},
                    onShowScoreboard = {},
                    onDismissScoreboard = {},
                    onOpenSettings = {},
                    onCloseSettings = {},
                    onToggleButtonsEnabled = {},
                    onToggleGesturesEnabled = {},
                    onToggleMusicEnabled = {},
                    onSetMainTrack = {},
                )
            }
        }

        composeRule.onNodeWithText("Music Library").assertIsDisplayed()
        composeRule.onNodeWithText(currentTrack.displayString()).assertIsDisplayed()
        composeRule.onNodeWithText(otherTrack.displayString()).assertIsDisplayed()
    }

    @Test
    fun musicLibraryShowsEmptyStateWhenNoTracksExist() {
        composeRule.setContent {
            BlockDropTheme {
                BlockDropScreen(
                    uiModel = GameUiModel(
                        state = GameState.Paused,
                        showMusicLibrary = true,
                    ),
                    onStartGame = {},
                    onPause = {},
                    onResume = {},
                    onQuit = {},
                    onMuteToggle = {},
                    onOpenMusicLibrary = {},
                    onCloseMusicLibrary = {},
                    onRefreshMusicLibrary = {},
                    onPickMusicFolder = {},
                    onSelectTrack = {},
                    onPauseMusic = {},
                    onResumeMusic = {},
                    onStopMusic = {},
                    onShowTutorial = {},
                    onDismissTutorial = {},
                    onMoveLeft = {},
                    onMoveRight = {},
                    onRotateClockwise = {},
                    onRotateCounterClockwise = {},
                    onSoftDrop = {},
                    onHardDrop = {},
                    onHold = {},
                    onDropDelay = {},
                    onNicknameChanged = {},
                    onSubmitScore = {},
                    onShowScoreboard = {},
                    onDismissScoreboard = {},
                    onOpenSettings = {},
                    onCloseSettings = {},
                    onToggleButtonsEnabled = {},
                    onToggleGesturesEnabled = {},
                    onToggleMusicEnabled = {},
                    onSetMainTrack = {},
                )
            }
        }

        composeRule.onNodeWithText("No music files found").assertIsDisplayed()
        composeRule.onNodeWithText("Place .mod / .xm / .s3m / .it files in Download/Mods/").assertIsDisplayed()
    }

    @Test
    fun musicLibraryRefreshButtonInvokesCallback() {
        var refreshClicks = 0

        composeRule.setContent {
            BlockDropTheme {
                BlockDropScreen(
                    uiModel = GameUiModel(
                        state = GameState.Paused,
                        showMusicLibrary = true,
                    ),
                    onStartGame = {},
                    onPause = {},
                    onResume = {},
                    onQuit = {},
                    onMuteToggle = {},
                    onOpenMusicLibrary = {},
                    onCloseMusicLibrary = {},
                    onRefreshMusicLibrary = { refreshClicks += 1 },
                    onPickMusicFolder = {},
                    onSelectTrack = {},
                    onPauseMusic = {},
                    onResumeMusic = {},
                    onStopMusic = {},
                    onShowTutorial = {},
                    onDismissTutorial = {},
                    onMoveLeft = {},
                    onMoveRight = {},
                    onRotateClockwise = {},
                    onRotateCounterClockwise = {},
                    onSoftDrop = {},
                    onHardDrop = {},
                    onHold = {},
                    onDropDelay = {},
                    onNicknameChanged = {},
                    onSubmitScore = {},
                    onShowScoreboard = {},
                    onDismissScoreboard = {},
                    onOpenSettings = {},
                    onCloseSettings = {},
                    onToggleButtonsEnabled = {},
                    onToggleGesturesEnabled = {},
                    onToggleMusicEnabled = {},
                    onSetMainTrack = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Refresh music library").assertIsDisplayed().performClick()

        assertEquals(1, refreshClicks)
    }

    @Test
    fun musicLibrarySetFolderButtonInvokesCallback() {
        var pickFolderClicks = 0

        composeRule.setContent {
            BlockDropTheme {
                BlockDropScreen(
                    uiModel = GameUiModel(
                        state = GameState.Paused,
                        showMusicLibrary = true,
                    ),
                    onStartGame = {},
                    onPause = {},
                    onResume = {},
                    onQuit = {},
                    onMuteToggle = {},
                    onOpenMusicLibrary = {},
                    onCloseMusicLibrary = {},
                    onRefreshMusicLibrary = {},
                    onPickMusicFolder = { pickFolderClicks += 1 },
                    onSelectTrack = {},
                    onPauseMusic = {},
                    onResumeMusic = {},
                    onStopMusic = {},
                    onShowTutorial = {},
                    onDismissTutorial = {},
                    onMoveLeft = {},
                    onMoveRight = {},
                    onRotateClockwise = {},
                    onRotateCounterClockwise = {},
                    onSoftDrop = {},
                    onHardDrop = {},
                    onHold = {},
                    onDropDelay = {},
                    onNicknameChanged = {},
                    onSubmitScore = {},
                    onShowScoreboard = {},
                    onDismissScoreboard = {},
                    onOpenSettings = {},
                    onCloseSettings = {},
                    onToggleButtonsEnabled = {},
                    onToggleGesturesEnabled = {},
                    onToggleMusicEnabled = {},
                    onSetMainTrack = {},
                )
            }
        }

        composeRule.onNodeWithText("Set music folder").assertIsDisplayed().performClick()
        assertEquals(1, pickFolderClicks)
    }
}
