## M3 Change Request Summary

### Request

This change request asked for a tighter gameplay-first layout on the main game screen:

- larger centered board with reduced outer spacing
- compact top bar with `SCORE | LEVEL | LINES`
- hold and next panels moved onto the board corners
- next preview scaled down to roughly 50%
- smaller board-centered game-over modal
- single bottom control row with a dedicated hard-drop button
- improved touch clarity by removing blocking or disconnected UI layers

Reference files:

- `m3-change-request.md`
- `change-request-scatch.png`

### Implemented

- Reworked the main Compose layout so the board is the dominant visual element and the overall spacing follows a tighter 8-16 dp rhythm.
- Replaced the previous scattered HUD with a compact top bar:
  - left: help button
  - center: score / level / lines
  - right: mute button
- Moved `Hold` and `Next` into lightweight board-side panels anchored to the top-left and top-right corners of the board.
- Reduced next-piece preview size using a consistent `0.5` scale factor while keeping the shapes centered and readable.
- Replaced the larger overlay treatment with smaller board-relative overlay cards for idle, paused, and game-over states.
- Replaced the old debug-style fallback control block with a single bottom row of gameplay buttons.
- Added a dedicated hard-drop button with:
  - larger hit area
  - emphasized styling
  - press-scale feedback

### Validation

Commands run:

- `./gradlew testDebugUnitTest`
- `./gradlew app:compileDebugAndroidTestKotlin`

Added test coverage:

- Compose UI test validating the compact stats bar and the dedicated hard-drop control wiring

### Notes

- Hard-drop landing flash / impact effect was not added. That item was optional in the request.
- Instrumented tests were compiled but not executed on a device or emulator.
- The next preview column was tightened to 3 visible items for the updated layout.

### Outcome

The delivered UI matches the intent of the change request:

- bigger board
- lighter side panels
- compact top stats bar
- direct bottom controls with working hard drop
- smaller modal treatment
