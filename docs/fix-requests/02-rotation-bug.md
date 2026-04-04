## Issue: L, J, T pieces have wrong Spawn and Reverse rotation states

### Summary

`TetrominoShapes.kt` contains incorrect cell offsets for the Spawn and Reverse rotation states of the L, J, and T pieces. The corner/tab Y-coordinate is negated in both states, causing the foot or tab to appear on the wrong side of the bar when the piece is horizontal. The Right and Left (vertical) states are correct.

The bug manifests as pieces appearing to change shape when rotated through horizontal orientations.

---

### Coordinate system

The codebase uses a Y-up convention: `y = 0` is the bottom of the board, increasing upward. `CellOffset(x, y)` is relative to the piece's origin. A positive Y offset means the cell is **above** the bar; a negative Y offset means it is **below** the bar.

---

### Affected pieces

#### L piece

| State   | Code                                    | Standard (correct)                      | Bug         |
|---------|-----------------------------------------|-----------------------------------------|-------------|
| Spawn   | `(−1,0)(0,0)(1,0)` + foot `(1,−1)`     | `(−1,0)(0,0)(1,0)` + foot `(1,+1)`     | foot Y sign |
| Right   | `(0,1)(0,0)(0,−1)` + foot `(1,−1)`     | `(0,1)(0,0)(0,−1)` + foot `(1,−1)`     | correct     |
| Reverse | `(−1,0)(0,0)(1,0)` + foot `(−1,+1)`   | `(−1,0)(0,0)(1,0)` + foot `(−1,−1)`   | foot Y sign |
| Left    | `(0,1)(0,0)(0,−1)` + foot `(−1,+1)`   | `(0,1)(0,0)(0,−1)` + foot `(−1,+1)`   | correct     |

Visual: in Spawn the L-foot should extend **up-right** (standard orientation). The code places it down-right, producing a mirror image.

#### J piece

| State   | Code                                    | Standard (correct)                      | Bug         |
|---------|-----------------------------------------|-----------------------------------------|-------------|
| Spawn   | `(−1,0)(0,0)(1,0)` + foot `(−1,−1)`   | `(−1,0)(0,0)(1,0)` + foot `(−1,+1)`   | foot Y sign |
| Right   | `(0,1)(0,0)(0,−1)` + foot `(1,+1)`    | `(0,1)(0,0)(0,−1)` + foot `(1,+1)`    | correct     |
| Reverse | `(−1,0)(0,0)(1,0)` + foot `(1,+1)`    | `(−1,0)(0,0)(1,0)` + foot `(1,−1)`    | foot Y sign |
| Left    | `(0,1)(0,0)(0,−1)` + foot `(−1,−1)`   | `(0,1)(0,0)(0,−1)` + foot `(−1,−1)`   | correct     |

Visual: in Spawn the J-foot should extend **up-left**. The code places it down-left.

#### T piece

| State   | Code                                    | Standard (correct)                      | Bug        |
|---------|-----------------------------------------|-----------------------------------------|------------|
| Spawn   | `(−1,0)(0,0)(1,0)` + tab `(0,−1)`     | `(−1,0)(0,0)(1,0)` + tab `(0,+1)`     | tab Y sign |
| Right   | `(0,1)(0,0)(0,−1)` + tab `(1,0)`      | `(0,1)(0,0)(0,−1)` + tab `(1,0)`      | correct    |
| Reverse | `(−1,0)(0,0)(1,0)` + tab `(0,+1)`     | `(−1,0)(0,0)(1,0)` + tab `(0,−1)`     | tab Y sign |
| Left    | `(0,1)(0,0)(0,−1)` + tab `(−1,0)`     | `(0,1)(0,0)(0,−1)` + tab `(−1,0)`     | correct    |

Visual: in Spawn the T-tab should point **upward**. The code points it downward.

---

### Root cause

In `TetrominoShapes.kt`, the Spawn and Reverse definitions for L, J, and T all have the Y-coordinate of their distinguishing cell (foot or tab) negated. The Right and Left definitions were authored correctly.

The likely cause is that these two horizontal states were written assuming Y-down (screen coordinates), while the board uses Y-up. The vertical states (Right, Left) happen to be symmetric in Y so the error did not affect them.

---

### Cascading impact: T-spin detection

`BoardState.checkTSpin()` and `docs/plan.md` define T-spin corner detection relative to the T-piece's rotation state. The front corners are currently defined as:

| T state | Front corners (code) | Front corners (correct after fix) |
|---------|----------------------|-----------------------------------|
| Spawn   | `(−1,−1)` `(+1,−1)` | `(−1,+1)` `(+1,+1)`              |
| Right   | `(+1,+1)` `(+1,−1)` | unchanged                         |
| Reverse | `(−1,+1)` `(+1,+1)` | `(−1,−1)` `(+1,−1)`              |
| Left    | `(−1,+1)` `(−1,−1)` | unchanged                         |

These corners were calibrated for the wrong (tab-down) Spawn orientation. After correcting the T-piece offsets, the Spawn and Reverse front corners in both `plan.md` and `BoardState.checkTSpin` must be swapped to match the new orientation. Failure to do so will cause T-spin detection to use the wrong corners for horizontal T states, misidentifying or missing T-spins.

---

### Required changes

1. **`app/src/main/java/com/bigbangit/blockdrop/core/TetrominoShapes.kt`**
   - L Spawn: change foot from `(1,−1)` → `(1,+1)`
   - L Reverse: change foot from `(−1,+1)` → `(−1,−1)`
   - J Spawn: change foot from `(−1,−1)` → `(−1,+1)`
   - J Reverse: change foot from `(1,+1)` → `(1,−1)`
   - T Spawn: change tab from `(0,−1)` → `(0,+1)`
   - T Reverse: change tab from `(0,+1)` → `(0,−1)`

2. **`app/src/main/java/com/bigbangit/blockdrop/core/BoardState.kt`** — `checkTSpin()`
   - Spawn front corners: `(−1,−1)(+1,−1)` → `(−1,+1)(+1,+1)`
   - Reverse front corners: `(−1,+1)(+1,+1)` → `(−1,−1)(+1,−1)`

3. **`docs/plan.md`**
   - Update T-spin front corner table to match the corrected T orientation (same values as above).

---

### Not affected

- I, S, Z, O pieces — no bugs found. These pieces have symmetric or single-axis shapes that are not sensitive to the Y-sign error.
- Right and Left states of L, J, T — correct in the code.
- Wall-kick offset tables — these apply on top of cell offsets and are independent of this bug.
- Scoring, line-clear, gravity, and all other game systems — out of scope.

---

### Verification approach

After applying the fix, confirm visually in the emulator:

- Spawn L: foot cell extends up-right from the bar
- Spawn J: foot cell extends up-left from the bar
- Spawn T: tab cell extends upward from the bar
- Rotate each piece through all four states and verify the shape is stable (no apparent shape change)
- Trigger a T-spin in Spawn and Reverse states and confirm scoring registers correctly
