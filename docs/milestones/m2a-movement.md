# M2a — Movement, Collision & Gravity

**Status:** Completed
**Depends on:** M1

## Scope

- `PieceGenerator` (7-bag randomizer, 5-piece look-ahead buffer)
- Piece spawning at Guideline positions (columns 3–6, rows 20–21)
- Collision detection against board walls + placed cells
- SRS rotation with wall-kick tables (J/L/S/Z/T 5-entry, I separate table)
- Gravity ticks (level-scaled speed curve from `GameConstants`)
- Lock delay (500 ms timer, reset on move/rotation, 15-reset cap then force-lock)
- Soft drop / hard drop (movement only, flat point rewards: 1 pt/row soft, 2 pt/row hard)
- Ghost piece computation (drop projection exposed in `GameUiModel`)
- Hold mechanic (swap/store, reset to spawn rotation, once-per-piece flag, resets on lock)
- Drop delay (swipe-up gravity freeze, `max(200, 2000 - (level-1) × 18)` ms, once-per-piece)
- Top-out: game ends immediately if spawned piece overlaps occupied cells
- `EffectBridge` (`SharedFlow<GameEffect>` with `replay=0`, `extraBufferCapacity=8`)
- Wire `GameLoop` into `GameViewModel` → `StateFlow<GameUiModel>`

## Exit criteria

Pieces spawn, fall, rotate with kicks, lock, and stack. Hold and drop delay work. Game ends on top-out. Unit tests pass for `BoardState` (collision, placement), `PieceGenerator` (7-bag distribution), `GameLoop` (lock-delay cap, drop-delay timer, gravity ticks via `kotlinx-coroutines-test`).

## Current progress

- Core spawn / movement / rotation / gravity loop is wired into `GameViewModel`.
- Hold, hard drop, soft drop, ghost projection, and drop delay have first-pass implementations.
- The control path now includes a dedicated `ControlSurface` with board-area gesture handling plus safe fallback buttons.
- The board preview renderer is sufficient to inspect active piece, locked stack, and ghost projection during manual testing.
- `./gradlew testDebugUnitTest assembleDebug` passes on the current branch.

## Transfer notes

- This milestone is transferable to the next stage because the core movement/collision/gravity contract is in place and verified by direct tests.
- The remaining gaps are interaction fidelity and presentation polish, not missing core movement mechanics.
- M3 can build on the current `BoardCanvas` / `ControlSurface` split without reworking the loop or board-state model.

## Verified coverage

- `BoardState`: occupancy rules, hidden spawn rows, placement collision, ghost projection
- `PieceGenerator`: seeded queue behavior, look-ahead buffer, 7-piece coverage
- `GameLoop`: gravity ticks, drop delay timing, hold restrictions/reset, wall kicks, floor kick, top-out, lock-delay reset postponement, 15-reset cap with immediate force-lock on the 16th reset attempt

## Residual gaps

- Gesture classification is still an interim implementation and does not yet fully match the final product spec.
- The current renderer is intentionally simple and exists for observability; final visual treatment still belongs to later work.
