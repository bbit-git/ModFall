package com.bigbangit.modfall.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScoringTest {
    private lateinit var boardState: BoardState
    private lateinit var effectBridge: EffectBridge
    private lateinit var gameLoop: GameLoop
    private var snapshot: LoopSnapshot? = null

    @Before
    fun setup() {
        boardState = BoardState()
        effectBridge = EffectBridge()
    }

    @After
    fun tearDown() {
        if (::gameLoop.isInitialized) {
            gameLoop.stop()
        }
    }

    private fun initGame(scope: TestScope, initialQueue: List<TetrominoType>) {
        gameLoop = GameLoop(
            boardState = boardState,
            pieceGenerator = PieceGenerator(kotlin.random.Random(1), initialQueue = initialQueue),
            effectBridge = effectBridge,
            dispatcher = UnconfinedTestDispatcher(scope.testScheduler)
        )
        gameLoop.start(scope, onStateChanged = { snapshot = it })
    }

    @Test
    fun testSingleClear() = runTest {
        initGame(this, listOf(TetrominoType.I))
        for (x in 0 until 10) if (x !in 3..6) boardState.set(x, 0, 1)
        boardState.set(0, 19, 1)
        gameLoop.hardDrop()
        assertEquals(142, snapshot!!.score)
    }

    @Test
    fun testDoubleClear() = runTest {
        initGame(this, listOf(TetrominoType.O))
        for (y in 0..1) for (x in 0 until 10) if (x !in 4..5) boardState.set(x, y, 1)
        boardState.set(0, 19, 1)
        gameLoop.hardDrop()
        assertEquals(340, snapshot!!.score)
    }

    @Test
    fun testTripleClear() = runTest {
        initGame(this, listOf(TetrominoType.I))
        for (y in 0..2) for (x in 0 until 10) if (x != 5) boardState.set(x, y, 1)
        boardState.set(0, 19, 1)
        gameLoop.rotateClockwise()
        gameLoop.hardDrop()
        assertEquals(538, snapshot!!.score)
    }

    @Test
    fun testTetrisClear() = runTest {
        initGame(this, listOf(TetrominoType.I))
        for (y in 0..3) for (x in 0 until 10) if (x != 5) boardState.set(x, y, 1)
        boardState.set(0, 19, 1)
        gameLoop.rotateClockwise()
        gameLoop.hardDrop()
        assertEquals(838, snapshot!!.score)
    }

    @Test
    fun testTSpinNoLines() = runTest {
        initGame(this, listOf(TetrominoType.T))
        while (gameLoop.softDrop()) {} // drops to (4,0)
        gameLoop.rotateClockwise() // kicks to (3,1)
        boardState.set(4, 2, 1); boardState.set(4, 0, 1); boardState.set(2, 2, 1)
        boardState.set(3, 0, 1) 
        boardState.set(0, 19, 1)
        gameLoop.hardDrop()
        assertEquals(421, snapshot!!.score)
    }

    @Test
    fun testTSpinSingle() = runTest {
        initGame(this, listOf(TetrominoType.T))
        while (gameLoop.softDrop()) {} // drops to (4,0)
        gameLoop.rotateClockwise() // kicks to (3,1)
        for (x in 0 until 10) if (x != 3) boardState.set(x, 0, 1)
        boardState.set(4, 2, 1); boardState.set(2, 2, 1)
        boardState.set(0, 19, 1)
        gameLoop.hardDrop()
        assertEquals(821, snapshot!!.score)
    }

    @Test
    fun testTSpinDouble() = runTest {
        initGame(this, listOf(TetrominoType.T))
        while (gameLoop.softDrop()) {} // drops to (4,0)
        gameLoop.rotateClockwise() // kicks to (3,1)
        for (y in 0..1) for (x in 0 until 10) if (x != 3 && x != 4) boardState.set(x, y, 1)
        boardState.set(4, 2, 1); boardState.set(4, 0, 1); boardState.set(2, 2, 1)
        boardState.set(0, 19, 1)
        gameLoop.hardDrop()
        assertEquals(1221, snapshot!!.score)
    }

    @Test
    fun testMiniTSpinNoLines() = runTest {
        initGame(this, listOf(TetrominoType.T))
        while (gameLoop.softDrop()) {} // drops to (4,0)
        gameLoop.rotateClockwise() // kicks to (3,1)
        boardState.set(4, 2, 1); boardState.set(2, 2, 1); boardState.set(2, 0, 1)
        boardState.set(3, 0, 1)
        boardState.set(0, 19, 1)
        gameLoop.hardDrop()
        assertEquals(121, snapshot!!.score)
    }

    @Test
    fun testMiniTSpinSingle() = runTest {
        initGame(this, listOf(TetrominoType.T))
        while (gameLoop.softDrop()) {} // drops to (4,0)
        gameLoop.rotateClockwise() // kicks to (3,1)
        for (x in 0 until 10) if (x != 3 && x != 4) boardState.set(x, 0, 1)
        boardState.set(4, 0, 1); boardState.set(2, 2, 1); boardState.set(2, 0, 1)
        boardState.set(0, 19, 1)
        gameLoop.hardDrop()
        assertEquals(221, snapshot!!.score)
    }

    @Test
    fun testAllClearSingle() = runTest {
        initGame(this, listOf(TetrominoType.I))
        for (x in 0 until 10) if (x !in 3..6) boardState.set(x, 0, 1)
        gameLoop.hardDrop()
        assertEquals(842, snapshot!!.score)
    }

    @Test
    fun testAllClearDouble() = runTest {
        initGame(this, listOf(TetrominoType.O))
        for (y in 0..1) for (x in 0 until 10) if (x !in 4..5) boardState.set(x, y, 1)
        gameLoop.hardDrop()
        assertEquals(1240, snapshot!!.score)
    }

    @Test
    fun testAllClearTriple() = runTest {
        initGame(this, listOf(TetrominoType.I, TetrominoType.I, TetrominoType.I))
        for (x in 0 until 10) if (x !in 3..6) boardState.set(x, 0, 1)
        for (x in 0 until 10) if (x !in 3..6) boardState.set(x, 1, 1)
        for (x in 0 until 10) if (x !in 3..6) boardState.set(x, 2, 1)
        gameLoop.hardDrop()
        val s1 = snapshot!!.score
        gameLoop.hardDrop()
        val s2 = snapshot!!.score - s1
        gameLoop.hardDrop()
        assertEquals(1276, snapshot!!.score)
    }

    @Test
    fun testAllClearTetris() = runTest {
        initGame(this, listOf(TetrominoType.I))
        for (y in 0..3) for (x in 0 until 10) if (x != 5) boardState.set(x, y, 1)
        gameLoop.rotateClockwise()
        gameLoop.hardDrop()
        assertEquals(2038, snapshot!!.score)
    }

    @Test
    fun testTSpinTripleAllClear() = runTest {
        initGame(this, listOf(TetrominoType.T))
        while (gameLoop.softDrop()) {} // drops to (4,0)
        gameLoop.rotateCounterClockwise() // kicks to (5,1)
        for (y in 0..2) for (x in 0 until 10) if (x != 5 && (y != 1 || x != 4)) boardState.set(x, y, 1)
        gameLoop.hardDrop()
        assertEquals(1621, snapshot!!.score)
    }

    @Test
    fun testBackToBackBonus() = runTest {
        initGame(this, listOf(TetrominoType.I, TetrominoType.I))
        for (y in 0..3) for (x in 0 until 10) if (x != 5) boardState.set(x, y, 1)
        boardState.set(0, 19, 1)
        gameLoop.rotateClockwise()
        gameLoop.hardDrop()
        val score1 = snapshot!!.score 

        for (y in 0..3) for (x in 0 until 10) if (x != 5) boardState.set(x, y, 1)
        boardState.set(0, 19, 1)
        gameLoop.rotateClockwise()
        gameLoop.hardDrop()
        assertEquals(score1 + 1288, snapshot!!.score)
    }

    @Test
    fun testB2BTSpins() = runTest {
        initGame(this, listOf(TetrominoType.T, TetrominoType.T))
        
        while (gameLoop.softDrop()) {} // drops to (4,0)
        gameLoop.rotateClockwise() // kicks to (3,1)
        for (y in 0..1) for (x in 0 until 10) if (x != 3 && x != 4) boardState.set(x, y, 1)
        boardState.set(4, 2, 1); boardState.set(4, 0, 1); boardState.set(2, 2, 1)
        boardState.set(0, 19, 1)
        gameLoop.hardDrop()
        val score1 = snapshot!!.score 
        assertEquals(1221, score1)

        // Clear board for second piece but keep B2B
        boardState.clear()

        while (gameLoop.softDrop()) {} // drops to (4,0)
        gameLoop.rotateClockwise() // kicks to (3,1)
        for (y in 0..1) for (x in 0 until 10) if (x != 3 && x != 4) boardState.set(x, y, 1)
        boardState.set(4, 2, 1); boardState.set(4, 0, 1); boardState.set(2, 2, 1)
        boardState.set(0, 19, 1)
        gameLoop.hardDrop()
        // 1200 * 1.5 (B2B) + 50 (Combo 1) + 21 (drop) = 1871
        assertEquals(score1 + 1871, snapshot!!.score)
    }

    @Test
    fun testComboReset() = runTest {
        initGame(this, listOf(TetrominoType.I, TetrominoType.I, TetrominoType.I))
        for (x in 0 until 10) if (x !in 3..6) boardState.set(x, 0, 1)
        boardState.set(0, 19, 1)
        gameLoop.hardDrop()
        assertEquals(0, snapshot!!.comboCount)

        for (x in 0 until 10) if (x !in 3..6) boardState.set(x, 0, 1)
        boardState.set(0, 19, 1)
        gameLoop.hardDrop()
        assertEquals(1, snapshot!!.comboCount)

        boardState.clear(); boardState.set(0, 19, 1)
        gameLoop.hardDrop()
        assertEquals(-1, snapshot!!.comboCount)
    }

    @Test
    fun testLevelAdvancement() = runTest {
        initGame(this, List(15) { TetrominoType.I }.toMutableList().apply { 
            this[2] = TetrominoType.O 
        })
        
        repeat(2) {
            for (y in 0..3) for (x in 0 until 10) if (x != 5) boardState.set(x, y, 1)
            boardState.set(0, 19, 1)
            gameLoop.rotateClockwise()
            gameLoop.hardDrop()
        }
        
        for (y in 0..1) for (x in 0 until 10) if (x !in 4..5) boardState.set(x, y, 1)
        boardState.set(0, 19, 1)
        gameLoop.hardDrop()
        
        assertEquals(10, snapshot!!.lines)
        assertEquals(2, snapshot!!.level)
    }
}
