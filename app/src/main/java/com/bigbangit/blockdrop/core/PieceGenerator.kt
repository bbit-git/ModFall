package com.bigbangit.blockdrop.core

import kotlin.random.Random

class PieceGenerator(
    private val random: Random = Random.Default,
    initialQueue: List<TetrominoType> = emptyList(),
) {
    private val seededQueue: List<TetrominoType> = initialQueue.toList()
    private val queue = ArrayDeque<TetrominoType>()

    init {
        reset()
    }

    fun reset() {
        queue.clear()
        seededQueue.forEach(queue::addLast)
        refillIfNeeded()
    }

    fun next(): TetrominoType {
        refillIfNeeded()
        val next = queue.removeFirst()
        refillIfNeeded()
        return next
    }

    fun preview(count: Int = GameConstants.MAX_PREVIEW_BUFFER): List<TetrominoType> {
        refillIfNeeded(count)
        return queue.take(count)
    }

    private fun refillIfNeeded(minimumSize: Int = GameConstants.MAX_PREVIEW_BUFFER) {
        while (queue.size < minimumSize) {
            TetrominoType.entries.shuffled(random).forEach(queue::addLast)
        }
    }
}
