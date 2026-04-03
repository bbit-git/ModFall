package com.bigbangit.blockdrop.core

object GameConstants {
    const val BOARD_WIDTH = 10
    const val BOARD_HEIGHT = 20
    const val HIDDEN_ROWS = 2
    const val TOTAL_BOARD_HEIGHT = BOARD_HEIGHT + HIDDEN_ROWS
    const val SPAWN_COLUMN_CENTER = 4
    const val SPAWN_ROW_JLSTZ = 21

    const val LOCK_DELAY_MS = 500L
    const val LOCK_DELAY_MAX_RESETS = 15
    const val DAS_MS = 150L
    const val ARR_MS = 30L
    const val HARD_DROP_VELOCITY_DP_PER_SEC = 800f
    const val GRAB_CELL_SLOP_DP = 12f

    const val DROP_DELAY_MAX_MS = 2_000L
    const val DROP_DELAY_MIN_MS = 200L
    const val DROP_DELAY_STEP_MS = 18L

    const val MAX_LEVEL = 100
    const val LINES_PER_LEVEL = 10
    const val MAX_PREVIEW_BUFFER = 5

    val gravityTableMs: IntArray = intArrayOf(
        800, 716, 633, 550, 466, 383, 300, 216, 166, 133,
        116, 100, 83, 83, 66, 66, 50, 50, 50, 33,
    )

    val nextQueueTiers: List<NextQueueTier> = listOf(
        NextQueueTier(levelStart = 1, levelEnd = 9, visiblePieces = 5),
        NextQueueTier(levelStart = 10, levelEnd = 19, visiblePieces = 4),
        NextQueueTier(levelStart = 20, levelEnd = 39, visiblePieces = 3),
        NextQueueTier(levelStart = 40, levelEnd = 69, visiblePieces = 2),
        NextQueueTier(levelStart = 70, levelEnd = 100, visiblePieces = 1),
    )

    fun gravityForLevel(level: Int): Int {
        val clampedLevel = level.coerceIn(1, MAX_LEVEL)
        return gravityTableMs.getOrElse(clampedLevel - 1) { gravityTableMs.last() }
    }

    fun dropDelayForLevel(level: Int): Long {
        val clampedLevel = level.coerceIn(1, MAX_LEVEL)
        val delay = DROP_DELAY_MAX_MS - (clampedLevel - 1L) * DROP_DELAY_STEP_MS
        return delay.coerceAtLeast(DROP_DELAY_MIN_MS)
    }
}

data class NextQueueTier(
    val levelStart: Int,
    val levelEnd: Int,
    val visiblePieces: Int,
)
