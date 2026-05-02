package com.bigbangit.modfall.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GameConstantsTest {
    @Test
    fun gravityForLevelClampsToSupportedTableRange() {
        assertEquals(800, GameConstants.gravityForLevel(0))
        assertEquals(800, GameConstants.gravityForLevel(1))
        assertEquals(33, GameConstants.gravityForLevel(20))
        assertEquals(33, GameConstants.gravityForLevel(100))
    }

    @Test
    fun dropDelayForLevelUsesStepFormulaAndMinimumClamp() {
        assertEquals(2_000L, GameConstants.dropDelayForLevel(0))
        assertEquals(2_000L, GameConstants.dropDelayForLevel(1))
        assertEquals(1_838L, GameConstants.dropDelayForLevel(10))
        assertEquals(218L, GameConstants.dropDelayForLevel(100))
    }
}
