## Issue: DAS threshold measured from gesture start instead of first move — swipe outside piece moves multiple cells

### Summary

When dragging outside the active piece, the piece should move one cell on the first threshold crossing, then repeat after a DAS delay (150 ms), then at ARR rate (30 ms). Instead, if the first cell-width threshold is crossed after the gesture has already been running for more than `dasMs`, the DAS period is skipped entirely and the second move fires at ARR rate (~30 ms) almost immediately after the first. A slow deliberate swipe can fire three or more cells where one was intended.

---

### Affected file and line

`app/src/main/java/com/bigbangit/blockdrop/ui/ControlSurface.kt`, line 138:

```kotlin
val threshold = if (currentTime - startTime > dasMs) arrMs else dasMs
```

---

### How DAS/ARR should work

1. **First move** — fires once when `absDx > cellWidth` (distance-triggered, no timer).
2. **DAS delay** — after the first move, wait `dasMs` (150 ms) before firing the second move.
3. **ARR repeat** — after the DAS delay has been served once, subsequent moves fire every `arrMs` (30 ms).

---

### What the code does instead

The threshold between DAS and ARR is decided by `currentTime - startTime > dasMs` — elapsed time since the gesture began, not since the first move.

**Fast swipe (first move fires before 150 ms):** the condition happens to work. Example: first move at t = 80 ms. At t = 231 ms, `startTime` elapsed crosses 150 ms and threshold switches to ARR, but `timeSinceLast` is ~151 ms which also exceeds the DAS threshold that was in effect until that moment. The second move fires roughly 151 ms after the first — close to correct DAS behavior by coincidence.

**Slow swipe (first move fires at or after 150 ms):** the condition is already true the moment `isDasActive` is set. Threshold immediately becomes `arrMs` (30 ms), and the DAS delay is never served.

Concrete trace — first move fires at t = 160 ms:

| Time | `timeSinceLast` | `startTime` elapsed | Threshold | Move fires? |
|------|-----------------|---------------------|-----------|-------------|
| 160 ms | — | 160 ms | — | **first move** |
| 176 ms | 16 ms | 176 ms > 150 ms | ARR (30 ms) | no |
| 192 ms | 32 ms | 192 ms > 150 ms | ARR (30 ms) | **yes** — 32 ms after first move |
| 208 ms | 16 ms | — | ARR | no |
| 224 ms | 32 ms | — | ARR | **yes** |

The second move fires 32 ms after the first instead of 150 ms. A deliberate slow-drag fires 3–5 cells instead of 1.

---

### Fix

Add a `dasServed` flag that flips to `true` after the second move fires (i.e., after the DAS period from the first move has been consumed). Replace the `startTime`-based threshold with a check on that flag.

```kotlin
var isDasActive = false
var dasServed = false          // new flag

// ... inside the pointer-move branch, non-piece path:

if (!isDasActive) {
    if (absDx > cellWidth) {
        if (direction > 0) onMoveRight() else onMoveLeft()
        lastMoveTime = currentTime
        isDasActive = true
        // dasServed stays false — DAS delay has not been served yet
    }
} else {
    val threshold = if (dasServed) arrMs else dasMs   // replaces startTime condition
    if (timeSinceLast > threshold) {
        if (direction > 0) onMoveRight() else onMoveLeft()
        lastMoveTime = currentTime
        dasServed = true                               // DAS served after second move
    }
}
```

With this fix:
- First move: immediate, distance-triggered.
- Second move: fires after `dasMs` (150 ms) from the first, regardless of how long the gesture has been running.
- Third move onward: fires every `arrMs` (30 ms).

The `startTime` variable is no longer needed by this branch and can be removed if it is not used elsewhere in the gesture handler.

---

### Acceptance criteria

1. A quick swipe (gesture < 150 ms) moves the piece exactly 1 cell.
2. A held drag (finger down for > 150 ms before moving) moves 1 cell on the first threshold, then waits ~150 ms before the second cell, then repeats at ~30 ms intervals.
3. Grab (drag on piece) is unaffected — it uses a separate code path.
4. Soft drop swipe-down is unaffected — it uses `downwardSteps` distance accumulation, not DAS/ARR.

---

### Out of scope

- Tuning `DAS_MS` or `ARR_MS` constants — these are already correct by spec.
- Direction-reversal behaviour during a held drag — pre-existing, not introduced by this bug or fix.
