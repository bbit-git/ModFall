package com.bigbangit.modfall.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardStateTest {
    @Test
    fun outOfBoundsCellsAreTreatedAsOccupied() {
        val boardState = BoardState()

        assertTrue(boardState.isOccupied(-1, 0))
        assertTrue(boardState.isOccupied(0, -1))
        assertTrue(boardState.isOccupied(GameConstants.BOARD_WIDTH, 0))
    }

    @Test
    fun cellsAboveVisibleBoardAreTrackedInHiddenRows() {
        val boardState = BoardState()
        val piece = ActivePiece(type = TetrominoType.O, originX = 4, originY = 21)

        boardState.lock(piece)

        assertTrue(boardState.isOccupied(4, 21))
        assertFalse(boardState.canPlace(piece))
    }

    @Test
    fun canPlaceChecksExistingLockedCells() {
        val boardState = BoardState()
        val piece = ActivePiece(type = TetrominoType.O, originX = 4, originY = 2)

        boardState.lock(piece)

        assertFalse(boardState.canPlace(piece))
    }

    @Test
    fun ghostProjectionStopsAtFloor() {
        val boardState = BoardState()
        val piece = ActivePiece(type = TetrominoType.I, originX = 4, originY = 5)

        val ghost = boardState.projectGhost(piece)

        assertTrue(ghost.cells().all { it.y >= 0 })
        assertFalse(boardState.canPlace(ghost.movedBy(dx = 0, dy = -1)))
    }

    @Test
    fun clearLinesRemovesFullRowsAndShiftsDown() {
        val boardState = BoardState()
        // Fill row 0
        for (x in 0 until GameConstants.BOARD_WIDTH) {
            boardState.lock(ActivePiece(type = TetrominoType.O, originX = x, originY = 0))
        }
        // Place one block in row 1
        boardState.lock(ActivePiece(type = TetrominoType.O, originX = 5, originY = 1))

        val cleared = boardState.clearLines()

        assertEquals(1, cleared)
        assertTrue(boardState.isOccupied(5, 0)) // Block from row 1 shifted to row 0
        assertFalse(boardState.isOccupied(0, 0)) // Rest of row 0 is empty
    }

    @Test
    fun tSpinDetectionIdentifiesFullTSpin() {
        val boardState = BoardState()
        // Set up a T-slot
        boardState.lock(ActivePiece(type = TetrominoType.O, originX = 3, originY = 2))
        boardState.lock(ActivePiece(type = TetrominoType.O, originX = 5, originY = 2))
        boardState.lock(ActivePiece(type = TetrominoType.O, originX = 3, originY = 0))

        val piece = ActivePiece(type = TetrominoType.T, rotation = RotationState.Spawn, originX = 4, originY = 1)
        
        assertEquals(BoardState.TSpinType.Full, boardState.checkTSpin(piece))
    }

    @Test
    fun tSpinDetectionIdentifiesMiniTSpin() {
        val boardState = BoardState()
        // T center at 4,1. Rotation Spawn (pointing UP). Back corners are 3,0 and 5,0.
        boardState.lock(ActivePiece(type = TetrominoType.O, originX = 3, originY = 0))
        boardState.lock(ActivePiece(type = TetrominoType.O, originX = 5, originY = 0))

        val piece = ActivePiece(type = TetrominoType.T, rotation = RotationState.Spawn, originX = 4, originY = 1)
        
        assertEquals(BoardState.TSpinType.Mini, boardState.checkTSpin(piece))
    }

    @Test
    fun isEmptyReturnsTrueForNewBoard() {
        val boardState = BoardState()
        assertTrue(boardState.isEmpty())
    }
}
