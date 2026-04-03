package com.bigbangit.blockdrop.core

enum class RotationDirection {
    Clockwise,
    CounterClockwise,
    ;

    fun apply(rotationState: RotationState): RotationState {
        val rotations = RotationState.entries
        val currentIndex = rotations.indexOf(rotationState)
        val delta = if (this == Clockwise) 1 else -1
        return rotations[(currentIndex + delta + rotations.size) % rotations.size]
    }
}
