## Issue: Grab and hard drop gestures silently cancelled on every game tick

### Summary

Both the grab gesture (touch the piece, drag horizontally) and the hard drop gesture (fast downward swipe) are unreliable or broken in active gameplay. The root cause is a single line in `ControlSurface.kt`: `uiModel` is used as a `pointerInput` key. The game updates `uiModel` on every gravity tick, which cancels and restarts the gesture coroutine mid-gesture, discarding all accumulated gesture state.

---

### Affected file

`app/src/main/java/com/bigbangit/blockdrop/ui/ControlSurface.kt`, line 47:

```kotlin
.pointerInput(uiModel, boardSize) {
```

---

### How Compose `pointerInput` keys work

When any key passed to `pointerInput(key1, key2)` changes, Compose cancels the currently running coroutine inside the block and starts a new one. The new coroutine immediately calls `awaitFirstDown()` and waits for the **next** finger-down event. Any gesture that was in progress is silently discarded.

---

### Why `uiModel` as a key is destructive

`uiModel` is a `StateFlow` that is updated by `GameLoop` on every tick: gravity falls, lock-delay decrements, animation keys increment. Tick rate ranges from 800 ms at level 1 down to 33 ms at level 20+.

Effect on gestures:

| Gesture phase | What the block holds at cancel time |
|---|---|
| User pressed down, starting to drag | `startedOnPiece`, `startTime`, `startOffset` |
| Horizontal drag in progress | `grabActive = true`, `totalDrag`, `horizontalSteps` |
| Downward swipe in progress | `totalDrag.y`, `lastVelocity` |

All of this is wiped when the block restarts. The new block waits for `awaitFirstDown()`, which does not fire until the **next** touch-down. The ongoing finger press is ignored entirely.

#### Grab broken

1. User places finger on the piece → `startedOnPiece = true`, grab slop check begins accumulating `totalDrag`.
2. Gravity tick fires → `uiModel` changes → coroutine cancelled → `startedOnPiece` and `totalDrag` lost.
3. New block waits for a new down event while the finger is still held. Drag motion produces no calls to `onMoveLeft` / `onMoveRight`.

At level 1 (800 ms tick) grab occasionally works if the user moves fast enough. At level 10+ (100–50 ms tick), grab never works.

#### Hard drop broken

The hard drop condition (line 90):

```kotlin
} else if (!startedOnPiece && totalDrag.y > cellHeight && lastVelocity >= hardDropThresholdPxPerSec) {
    onHardDrop()
}
```

This check runs on finger release. It requires `lastVelocity` accumulated across move events, and `totalDrag.y` > one cell height (~40px on a typical phone). If a tick fires during the swipe, `lastVelocity` and `totalDrag` are reset to 0. The velocity check fails. `onHardDrop()` is never called.

---

### Secondary issue: `lastVelocity` not updated when two events share the same timestamp

```kotlin
val timeDelta = (pointerChange.uptimeMillis - lastEventTime) / 1000f
if (timeDelta > 0f) {
    lastVelocity = dragAmount.y / timeDelta
}
```

If two consecutive move events carry the same `uptimeMillis` (can happen at gesture start or during a fast flick), `timeDelta` is 0 and `lastVelocity` is not updated. If this occurs on the final move event before release, `lastVelocity` remains at a previously sampled (low) value, causing the hard drop threshold to fail even for a genuine fast swipe.

This is independent of the key bug and will remain after the key fix.

---

### Fix

#### 1. Remove `uiModel` from `pointerInput` key

Change:
```kotlin
.pointerInput(uiModel, boardSize) {
```
To:
```kotlin
.pointerInput(boardSize) {
```

`boardSize` should remain a key because the gesture math (`cellWidth`, `cellHeight`, `grabSlopPx`) depends on it, and a size change (rotation, window resize) requires restart.

#### 2. Read `uiModel` through `rememberUpdatedState`

Since the block no longer restarts on `uiModel` changes, it must read the current `uiModel` at the points where fresh state matters. Use `rememberUpdatedState`:

```kotlin
val currentUiModel = rememberUpdatedState(uiModel)

Box(
    modifier = modifier
        .onSizeChanged { boardSize = it }
        .pointerInput(boardSize) {
            // ...
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown()
                    if (currentUiModel.value.state != GameState.Running) continue

                    val startOffset = down.position
                    val startTime = System.currentTimeMillis()
                    val startedOnPiece = currentUiModel.value.activePiece
                        ?.contains(startOffset, boardSize, grabSlopPx) == true
                    // ...
                    // On release, read currentUiModel.value.activePiece for rotation tap
                }
            }
        },
```

`rememberUpdatedState` returns a `State<T>` that always holds the latest value without triggering a recomposition or restarting the coroutine. Reading `.value` inside the suspend block gives the current value at that moment.

#### 3. Fix `lastVelocity` stale-zero case (secondary fix)

Carry the last valid velocity forward rather than leaving it unchanged when `timeDelta == 0`:

```kotlin
val timeDelta = (pointerChange.uptimeMillis - lastEventTime) / 1000f
if (timeDelta > 0f) {
    lastVelocity = dragAmount.y / timeDelta
}
// No else: if timeDelta == 0, keep last valid lastVelocity — already the case
lastEventTime = pointerChange.uptimeMillis
```

The current code already does this (keeps the previous value). The real improvement is to also consider accumulating velocity over a short window (e.g. last 3 events) for robustness, but that is out of scope. At minimum, document that `lastVelocity` is the last event's instantaneous velocity, not a smoothed value.

---

### Acceptance criteria

1. Dragging the active piece horizontally (grab gesture) moves the piece in real-time at all game levels, including level 20+.
2. A fast downward swipe on empty board space triggers a hard drop reliably.
3. No gesture state is lost between game ticks.
4. Overlay state checks (`uiModel.state != Running`) still correctly gate gestures when the game is paused or over.

---

### Out of scope

- DAS/ARR tuning (`DAS_MS`, `ARR_MS`).
- Velocity threshold tuning (`HARD_DROP_VELOCITY_DP_PER_SEC`).
- Soft drop via swipe — unchanged behavior.
- Rotation tap behavior — unchanged, but reads `currentUiModel.value.activePiece` at release for correct tap-side detection.
