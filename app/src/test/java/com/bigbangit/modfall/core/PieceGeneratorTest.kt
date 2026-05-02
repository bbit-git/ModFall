package com.bigbangit.modfall.core

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PieceGeneratorTest {
    @Test
    fun previewMaintainsFivePieces() {
        val generator = PieceGenerator(random = Random(1234))

        assertEquals(5, generator.preview().size)
        generator.next()
        assertEquals(5, generator.preview().size)
    }

    @Test
    fun firstBagContainsAllSevenPieces() {
        val generator = PieceGenerator(random = Random(42))
        val firstBag = (0 until 7).map { generator.next() }.toSet()

        assertEquals(TetrominoType.entries.toSet(), firstBag)
    }

    @Test
    fun previewContainsOnlyTetrominoTypes() {
        val generator = PieceGenerator(random = Random(5))

        assertTrue(generator.preview().all { it in TetrominoType.entries })
    }
}
