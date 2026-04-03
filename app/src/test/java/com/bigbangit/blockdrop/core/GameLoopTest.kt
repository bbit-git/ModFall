package com.bigbangit.blockdrop.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameLoopTest {
    @Test
    fun gravityMovesThePieceDownOverTime() = runTest {
        val boardState = BoardState()
        val gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(kotlin.random.Random(1), initialQueue = listOf(TetrominoType.T)),
            effectBridge = EffectBridge(),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        var snapshot: LoopSnapshot? = null

        gameLoop.start(backgroundScope, onStateChanged = { snapshot = it })
        val startingY = snapshot!!.activePiece!!.originY

        advanceTimeBy(GameConstants.gravityForLevel(1).toLong() + 1)

        assertTrue(snapshot!!.activePiece!!.originY < startingY)
    }

    @Test
    fun groundedMoveResetsLockDelayTimer() = runTest {
        val boardState = BoardState()
        val gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(kotlin.random.Random(11), initialQueue = listOf(TetrominoType.T, TetrominoType.O)),
            effectBridge = EffectBridge(),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        var snapshot: LoopSnapshot? = null

        gameLoop.start(backgroundScope, onStateChanged = { snapshot = it })
        while (gameLoop.softDrop()) {
        }
        runCurrent()

        advanceTimeBy(GameConstants.LOCK_DELAY_MS - 1)
        assertEquals(TetrominoType.T, snapshot!!.activePiece!!.type)

        assertTrue(gameLoop.moveLeft())
        runCurrent()

        advanceTimeBy(GameConstants.LOCK_DELAY_MS - 1)
        assertEquals(TetrominoType.T, snapshot!!.activePiece!!.type)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(TetrominoType.O, snapshot!!.activePiece!!.type)
    }

    @Test
    fun dropDelaySuspendsGravityTemporarily() = runTest {
        val boardState = BoardState()
        val gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(kotlin.random.Random(2), initialQueue = listOf(TetrominoType.T)),
            effectBridge = EffectBridge(),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        var snapshot: LoopSnapshot? = null

        gameLoop.start(backgroundScope, onStateChanged = { snapshot = it })
        val startingY = snapshot!!.activePiece!!.originY
        gameLoop.activateDropDelay()

        advanceTimeBy(GameConstants.gravityForLevel(1).toLong() + 1)
        assertEquals(startingY, snapshot!!.activePiece!!.originY)

        advanceTimeBy(GameConstants.dropDelayForLevel(1) + GameConstants.gravityForLevel(1).toLong() + 1)
        assertTrue(snapshot!!.activePiece!!.originY < startingY)
    }

    @Test
    fun holdIsLimitedToOncePerPieceUntilLock() = runTest {
        val boardState = BoardState()
        val gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(kotlin.random.Random(3), initialQueue = listOf(TetrominoType.I, TetrominoType.O, TetrominoType.T)),
            effectBridge = EffectBridge(),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        var snapshot: LoopSnapshot? = null

        gameLoop.start(backgroundScope, onStateChanged = { snapshot = it })
        gameLoop.hold()

        assertNotNull(snapshot!!.heldPiece)
        assertFalse(snapshot!!.canHold)

        val heldAfterFirst = snapshot!!.heldPiece
        gameLoop.hold()

        assertEquals(heldAfterFirst, snapshot!!.heldPiece)
    }

    @Test
    fun holdResetsAfterLockingNextPiece() = runTest {
        val boardState = BoardState()
        val gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(kotlin.random.Random(5), initialQueue = listOf(TetrominoType.I, TetrominoType.O, TetrominoType.T)),
            effectBridge = EffectBridge(),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        var snapshot: LoopSnapshot? = null

        gameLoop.start(backgroundScope, onStateChanged = { snapshot = it })
        gameLoop.hold()
        gameLoop.hardDrop()

        assertTrue(snapshot!!.canHold)
        assertEquals(TetrominoType.T, snapshot!!.activePiece!!.type)
    }

    @Test
    fun wallKickAllowsRotationAgainstLeftWall() = runTest {
        val boardState = BoardState()
        val gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(kotlin.random.Random(6), initialQueue = listOf(TetrominoType.J)),
            effectBridge = EffectBridge(),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        var snapshot: LoopSnapshot? = null

        gameLoop.start(backgroundScope, onStateChanged = { snapshot = it })
        while (gameLoop.moveLeft()) {
        }

        val rotated = gameLoop.rotateClockwise()

        assertTrue(rotated)
        assertTrue(snapshot!!.activePiece!!.cells().all { it.x >= 0 })
    }

    @Test
    fun wallKickAllowsRotationAgainstRightWall() = runTest {
        val boardState = BoardState()
        val gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(kotlin.random.Random(8), initialQueue = listOf(TetrominoType.L)),
            effectBridge = EffectBridge(),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        var snapshot: LoopSnapshot? = null

        gameLoop.start(backgroundScope, onStateChanged = { snapshot = it })
        while (gameLoop.moveRight()) {
        }

        val rotated = gameLoop.rotateCounterClockwise()

        assertTrue(rotated)
        assertTrue(snapshot!!.activePiece!!.cells().all { it.x < GameConstants.BOARD_WIDTH })
    }

    @Test
    fun floorKickAllowsIRotationAtTheFloor() = runTest {
        val boardState = BoardState()
        val gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(kotlin.random.Random(9), initialQueue = listOf(TetrominoType.I)),
            effectBridge = EffectBridge(),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        var snapshot: LoopSnapshot? = null

        gameLoop.start(backgroundScope, onStateChanged = { snapshot = it })
        while (gameLoop.softDrop()) {
        }

        val rotated = gameLoop.rotateClockwise()

        assertTrue(rotated)
        assertTrue(snapshot!!.activePiece!!.cells().all { it.y >= 0 })
        assertTrue(snapshot!!.activePiece!!.cells().maxOf { it.y } >= 2)
    }

    @Test
    fun topOutTriggersGameOverWhenSpawnSpaceIsOccupied() = runTest {
        val boardState = BoardState()
        val gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(kotlin.random.Random(7), initialQueue = listOf(TetrominoType.I, TetrominoType.O)),
            effectBridge = EffectBridge(),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        var snapshot: LoopSnapshot? = null

        gameLoop.start(backgroundScope, onStateChanged = { snapshot = it })
        boardState.lock(ActivePiece(type = TetrominoType.O, originX = 4, originY = 21))
        gameLoop.hardDrop()

        assertEquals(GameState.GameOver, snapshot!!.state)
    }

    @Test
    fun sixteenthGroundedResetAttemptForceLocksImmediately() = runTest {
        val boardState = BoardState()
        val gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(kotlin.random.Random(12), initialQueue = listOf(TetrominoType.T, TetrominoType.O)),
            effectBridge = EffectBridge(),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        var snapshot: LoopSnapshot? = null

        gameLoop.start(backgroundScope, onStateChanged = { snapshot = it })
        while (gameLoop.softDrop()) {
        }
        runCurrent()

        repeat(GameConstants.LOCK_DELAY_MAX_RESETS) {
            val moved = if (it % 2 == 0) gameLoop.moveLeft() else gameLoop.moveRight()
            assertTrue(moved)
            runCurrent()
            assertEquals(TetrominoType.T, snapshot!!.activePiece!!.type)
        }

        val forceLocked = gameLoop.moveLeft()
        runCurrent()

        assertTrue(forceLocked)
        assertEquals(TetrominoType.O, snapshot!!.activePiece!!.type)
        assertTrue(boardState.snapshot().cells.flatten().any { it != 0 })
    }

    @Test
    fun hardDropImmediatelyLocksAndSpawnsNextPiece() = runTest {
        val boardState = BoardState()
        val gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(kotlin.random.Random(4), initialQueue = listOf(TetrominoType.I, TetrominoType.O)),
            effectBridge = EffectBridge(),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        var snapshot: LoopSnapshot? = null

        gameLoop.start(backgroundScope, onStateChanged = { snapshot = it })
        gameLoop.hardDrop()

        assertNotNull(snapshot!!.activePiece)
        assertTrue(boardState.snapshot().cells.flatten().any { it != 0 })
    }

    @Test
    fun dropDelayLimitedToOncePerPiece() = runTest {
        val boardState = BoardState()
        val gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(kotlin.random.Random(4), initialQueue = listOf(TetrominoType.I, TetrominoType.O)),
            effectBridge = EffectBridge(),
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        var snapshot: LoopSnapshot? = null

        gameLoop.start(backgroundScope, onStateChanged = { snapshot = it })
        assertTrue(snapshot!!.isDropDelayAvailable)

        gameLoop.activateDropDelay()
        assertFalse(snapshot!!.isDropDelayAvailable)

        val currentPiece = snapshot!!.activePiece

        // Call again
        gameLoop.activateDropDelay()
        assertFalse(snapshot!!.isDropDelayAvailable)
        assertEquals(currentPiece, snapshot!!.activePiece)
    }
}
