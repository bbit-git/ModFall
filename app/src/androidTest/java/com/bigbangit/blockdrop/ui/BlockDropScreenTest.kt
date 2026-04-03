package com.bigbangit.blockdrop.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bigbangit.blockdrop.core.GameState
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
                    isMuted = false,
                    showTutorial = false,
                    onStartGame = {},
                    onPause = {},
                    onResume = {},
                    onQuit = {},
                    onMuteToggle = {},
                    onTutorialToggle = {},
                    onMoveLeft = {},
                    onMoveRight = {},
                    onRotateClockwise = {},
                    onRotateCounterClockwise = {},
                    onSoftDrop = {},
                    onHardDrop = { hardDropClicks += 1 },
                    onHold = {},
                    onDropDelay = {},
                )
            }
        }

        composeRule.onNodeWithText("SCORE 10119").assertIsDisplayed()
        composeRule.onNodeWithText("LEVEL 4").assertIsDisplayed()
        composeRule.onNodeWithText("LINES 34").assertIsDisplayed()
        composeRule.onNodeWithText("HARD DROP").assertIsDisplayed().performClick()

        assertEquals(1, hardDropClicks)
    }
}
