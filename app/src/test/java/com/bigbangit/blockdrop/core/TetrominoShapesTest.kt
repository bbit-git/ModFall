package com.bigbangit.blockdrop.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TetrominoShapesTest {
    @Test
    fun everyTetrominoHasFourRotationsWithFourCells() {
        TetrominoType.entries.forEach { type ->
            val definition = TetrominoShapes.definitionFor(type)

            assertEquals(4, definition.rotations.size)
            RotationState.entries.forEach { rotation ->
                assertEquals(4, definition.rotations.getValue(rotation).cells.size)
            }
        }
    }

    @Test
    fun everyKickTransitionHasFiveOffsetsForRotatablePieces() {
        assertEquals(8, SrsKickTables.standard.size)
        assertEquals(8, SrsKickTables.iPiece.size)
        assertTrue(SrsKickTables.standard.values.all { it.size == 5 })
        assertTrue(SrsKickTables.iPiece.values.all { it.size == 5 })
    }
}
