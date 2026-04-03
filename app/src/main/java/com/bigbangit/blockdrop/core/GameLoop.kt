package com.bigbangit.blockdrop.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameLoop(
    private val boardState: BoardState,
    private val pieceGenerator: PieceGenerator,
    private val effectBridge: EffectBridge,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private var loopScope: CoroutineScope? = null
    private var gravityJob: Job? = null
    private var lockDelayJob: Job? = null
    private var dropDelayJob: Job? = null

    private var activePiece: ActivePiece? = null
    private var heldPiece: TetrominoType? = null
    private var canHold: Boolean = true
    private var dropDelayUsed: Boolean = false
    private var lockResetCount: Int = 0
    private var state: GameState = GameState.Idle
    private var level: Int = 1
    private var lines: Int = 0
    private var score: Int = 0
    private var comboCount: Int = -1 // -1 means no combo active (resets to -1 after placement with 0 clears)
    private var isBackToBack: Boolean = false
    private var lastMoveWasRotation: Boolean = false

    private var onStateChanged: ((LoopSnapshot) -> Unit)? = null

    fun start(scope: CoroutineScope, onStateChanged: (LoopSnapshot) -> Unit) {
        stop()
        this.onStateChanged = onStateChanged
        loopScope = scope
        boardState.clear()
        pieceGenerator.reset()
        activePiece = null
        heldPiece = null
        canHold = true
        dropDelayUsed = false
        lockResetCount = 0
        state = GameState.Running
        level = 1
        lines = 0
        score = 0
        comboCount = -1
        isBackToBack = false
        lastMoveWasRotation = false

        spawnNextPiece()
        scheduleGravityLoop()
        emitState()
    }

    fun stop() {
        gravityJob?.cancel()
        lockDelayJob?.cancel()
        dropDelayJob?.cancel()
        gravityJob = null
        lockDelayJob = null
        dropDelayJob = null
        loopScope = null
        activePiece = null
        state = GameState.Idle
        comboCount = -1
        isBackToBack = false
        lastMoveWasRotation = false
        emitState()
    }

    fun pause() {
        if (state != GameState.Running) return
        state = GameState.Paused
        gravityJob?.cancel()
        lockDelayJob?.cancel()
        dropDelayJob?.cancel()
        emitState()
    }

    fun resume() {
        if (state != GameState.Paused) return
        state = GameState.Running
        scheduleGravityLoop()
        if (activePiece?.let(::isGrounded) == true) {
            beginLockDelay()
        }
        emitState()
    }

    fun moveLeft(): Boolean {
        val moved = moveHorizontal(dx = -1)
        if (moved) lastMoveWasRotation = false
        return moved
    }

    fun moveRight(): Boolean {
        val moved = moveHorizontal(dx = 1)
        if (moved) lastMoveWasRotation = false
        return moved
    }

    fun softDrop(): Boolean {
        if (state != GameState.Running) return false
        val piece = activePiece ?: return false
        val moved = piece.movedBy(dx = 0, dy = -1)
        if (!boardState.canPlace(moved)) {
            beginLockDelay()
            return false
        }
        activePiece = moved
        score += 1
        lastMoveWasRotation = false
        effectBridge.emit(GameEffect.SoftDrop)
        updateGroundingAfterMovement(resetEligible = false)
        emitState()
        return true
    }

    fun hardDrop() {
        if (state != GameState.Running) return
        var piece = activePiece ?: return
        var rowsDropped = 0
        while (boardState.canPlace(piece.movedBy(dx = 0, dy = -1))) {
            piece = piece.movedBy(dx = 0, dy = -1)
            rowsDropped += 1
        }
        activePiece = piece
        score += rowsDropped * 2
        if (rowsDropped > 0) {
            lastMoveWasRotation = false
        }
        effectBridge.emit(GameEffect.HardDrop)
        lockActivePiece()
    }

    fun rotateClockwise(): Boolean {
        val rotated = rotate(direction = RotationDirection.Clockwise)
        if (rotated) lastMoveWasRotation = true
        return rotated
    }

    fun rotateCounterClockwise(): Boolean {
        val rotated = rotate(direction = RotationDirection.CounterClockwise)
        if (rotated) lastMoveWasRotation = true
        return rotated
    }

    fun hold() {
        if (state != GameState.Running || !canHold) return
        val current = activePiece ?: return
        val currentType = current.type
        val nextType = heldPiece
        heldPiece = currentType
        canHold = false
        dropDelayUsed = false
        lockResetCount = 0
        lastMoveWasRotation = false
        lockDelayJob?.cancel()
        activePiece = createSpawnPiece(nextType ?: pieceGenerator.next())
        if (!boardState.canPlace(activePiece!!)) {
            triggerGameOver()
            return
        }
        effectBridge.emit(GameEffect.Hold)
        emitState()
    }

    fun activateDropDelay() {
        if (state != GameState.Running || dropDelayUsed) return
        dropDelayUsed = true
        gravityJob?.cancel()
        dropDelayJob?.cancel()
        lastMoveWasRotation = false
        dropDelayJob = loopScope?.launch {
            delay(GameConstants.dropDelayForLevel(level))
            if (state == GameState.Running) {
                scheduleGravityLoop()
                emitState()
            }
        }
        emitState()
    }

    private fun moveHorizontal(dx: Int): Boolean {
        if (state != GameState.Running) return false
        val piece = activePiece ?: return false
        val moved = piece.movedBy(dx = dx, dy = 0)
        if (!boardState.canPlace(moved)) return false
        activePiece = moved
        effectBridge.emit(GameEffect.Move)
        updateGroundingAfterMovement(resetEligible = true)
        emitState()
        return true
    }

    private fun rotate(direction: RotationDirection): Boolean {
        if (state != GameState.Running) return false
        val piece = activePiece ?: return false
        val targetRotation = direction.apply(piece.rotation)
        val kicks = SrsKickTables.kicksFor(piece.type, piece.rotation, targetRotation)
        val rotated = piece.rotated(targetRotation)
        val kickedPiece = kicks
            .asSequence()
            .map { offset -> rotated.movedBy(dx = offset.dx, dy = offset.dy) }
            .firstOrNull(boardState::canPlace)
            ?: return false

        activePiece = kickedPiece
        effectBridge.emit(GameEffect.Rotate)
        updateGroundingAfterMovement(resetEligible = true)
        emitState()
        return true
    }

    private fun scheduleGravityLoop() {
        gravityJob?.cancel()
        gravityJob = loopScope?.launch {
            while (isActive && state == GameState.Running) {
                delay(GameConstants.gravityForLevel(level).toLong())
                tickGravity()
            }
        }
    }

    private fun tickGravity() {
        if (state != GameState.Running) return
        val piece = activePiece ?: return
        val moved = piece.movedBy(dx = 0, dy = -1)
        if (boardState.canPlace(moved)) {
            activePiece = moved
            lockDelayJob?.cancel()
            lastMoveWasRotation = false
            emitState()
            return
        }
        beginLockDelay()
        emitState()
    }

    private fun beginLockDelay() {
        if (lockDelayJob?.isActive == true || state != GameState.Running) return
        lockDelayJob = loopScope?.launch {
            delay(GameConstants.LOCK_DELAY_MS)
            lockActivePiece()
        }
    }

    private fun lockActivePiece() {
        val piece = activePiece ?: return
        val tSpinType = if (lastMoveWasRotation) boardState.checkTSpin(piece) else BoardState.TSpinType.None

        boardState.lock(piece)
        activePiece = null
        canHold = true
        dropDelayUsed = false
        lockResetCount = 0
        lastMoveWasRotation = false
        lockDelayJob?.cancel()

        val linesCleared = boardState.clearLines()
        val allClear = boardState.isEmpty()

        calculateScore(linesCleared, tSpinType, allClear)

        if (linesCleared > 0) {
            lines += linesCleared
            val newLevel = (lines / GameConstants.LINES_PER_LEVEL) + 1
            if (newLevel > level) {
                level = newLevel.coerceAtMost(GameConstants.MAX_LEVEL)
                effectBridge.emit(GameEffect.LevelUp)
                scheduleGravityLoop()
            }
            effectBridge.emit(GameEffect.LineClear(linesCleared))
        }

        spawnNextPiece()
        emitState()
    }

    private fun calculateScore(linesCleared: Int, tSpin: BoardState.TSpinType, allClear: Boolean) {
        if (linesCleared == 0) {
            if (tSpin != BoardState.TSpinType.None) {
                val base = if (tSpin == BoardState.TSpinType.Mini) 100 else 400
                score += base * level
                effectBridge.emit(GameEffect.TSpinClear)
            }
            comboCount = -1
            return
        }

        comboCount++
        val comboBonus = 50 * comboCount * level

        val isDifficultClear = tSpin != BoardState.TSpinType.None || linesCleared == 4
        val b2bMultiplier = if (isDifficultClear && isBackToBack) 1.5 else 1.0

        var baseScore = when {
            tSpin == BoardState.TSpinType.Mini -> when (linesCleared) {
                1 -> 200
                else -> 100
            }
            tSpin == BoardState.TSpinType.Full -> when (linesCleared) {
                1 -> 800
                2 -> 1200
                3 -> 1600
                else -> 400
            }
            else -> when (linesCleared) {
                1 -> 100
                2 -> 300
                3 -> 500
                4 -> 800
                else -> 0
            }
        }

        if (allClear) {
            // T-spin + all-clear uses the T-spin score as the base replacement.
            // Regular clears use the all-clear replacement table.
            if (tSpin == BoardState.TSpinType.None) {
                baseScore = when (linesCleared) {
                    1 -> 800
                    2 -> 1200
                    3 -> 1800
                    4 -> if (isDifficultClear && isBackToBack) 3200 else 2000
                    else -> baseScore
                }
            }
            effectBridge.emit(GameEffect.AllClear)
        }

        score += (baseScore * b2bMultiplier * level).toInt() + comboBonus

        if (tSpin != BoardState.TSpinType.None) {
            effectBridge.emit(GameEffect.TSpinClear)
        }

        // B2B chain continues on Tetris or T-spin clears; breaks on others.
        // For All-Clear: breaks on single/double/triple unless it was a T-spin.
        isBackToBack = isDifficultClear
    }

    private fun spawnNextPiece() {
        val nextPiece = createSpawnPiece(pieceGenerator.next())
        if (!boardState.canPlace(nextPiece)) {
            activePiece = nextPiece
            triggerGameOver()
            return
        }
        activePiece = nextPiece
    }

    private fun createSpawnPiece(type: TetrominoType): ActivePiece {
        // Spec: J, L, S, Z, T, I spawn with bounding box top at row 21 (visible 0-19).
        // O spawns across rows 20-21.
        // With originY = 21, all pieces spawn at the correct height per spec.
        return ActivePiece(
            type = type,
            rotation = RotationState.Spawn,
            originX = GameConstants.SPAWN_COLUMN_CENTER,
            originY = GameConstants.SPAWN_ROW_JLSTZ,
        )
    }

    private fun updateGroundingAfterMovement(resetEligible: Boolean) {
        val piece = activePiece ?: return
        if (!isGrounded(piece)) {
            lockDelayJob?.cancel()
            return
        }

        if (resetEligible) {
            if (lockResetCount >= GameConstants.LOCK_DELAY_MAX_RESETS) {
                lockActivePiece()
                return
            }
            lockResetCount += 1
            lockDelayJob?.cancel()
            beginLockDelay()
            return
        }

        if (lockDelayJob?.isActive != true) {
            beginLockDelay()
        }
    }

    private fun isGrounded(piece: ActivePiece): Boolean {
        return !boardState.canPlace(piece.movedBy(dx = 0, dy = -1))
    }

    private fun triggerGameOver() {
        gravityJob?.cancel()
        lockDelayJob?.cancel()
        dropDelayJob?.cancel()
        state = GameState.GameOver
        effectBridge.emit(GameEffect.GameOver)
        emitState()
    }

    private fun emitState() {
        val piece = activePiece
        val ghost = piece?.let(boardState::projectGhost)
        onStateChanged?.invoke(
            LoopSnapshot(
                state = state,
                board = boardState.snapshot(),
                activePiece = piece,
                ghostPiece = ghost,
                nextPieces = pieceGenerator.preview(),
                heldPiece = heldPiece,
                canHold = canHold,
                level = level,
                lines = lines,
                score = score,
                isDropDelayAvailable = !dropDelayUsed,
                lockResetCount = lockResetCount,
                isGrounded = piece?.let(::isGrounded) == true,
                comboCount = comboCount,
                isBackToBack = isBackToBack,
            ),
        )
    }
}
