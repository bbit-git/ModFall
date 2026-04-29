# Mod Fall implementation plan

## Overview
- **Name:** Mod Fall (Tetris-inspired Kotlin game)
- **Platform:** Native Android using Jetpack Compose UI with Kotlin core logic and coroutines-driven game loop.
- **Goal:** Ship a polished single-player experience with smooth animations, responsive controls, score/level systems, and visual/audio feedback before iterating on polish.
- **Distribution:** Completely free, ultra-lightweight launch footprint, no advertisements or paid upgrades—focus on fast load times and a single bundled APK.
- **Localisation:** Shipped in 18 languages covering all major mobile gaming markets (see Localisation section). All UI strings live in Android `strings.xml` resource files; no hardcoded text anywhere in code.

## Core requirements
1. **Grid + pieces** – 10×20 playfield, standard tetromino set (7-bag randomizer), spawn sequences with next-piece preview logic.
2. **Ghost piece** – Render a faded/outlined projection of where the active piece will land so the player can aim drops accurately at all speeds.
3. **Game loop** – coroutine tick per level gravity, lock delays (max 15 move/rotation resets per placement to prevent infinite stalling), soft/hard drops, and row-clear cascades. Include scoring hooks so each line clear triggers reward logic (base points per line + combo multiplier) and stats tracking for level/score display.
   - **Scoring** – Tetris Guideline scoring with level multiplier:

     | Action                  | Points                        |
     |-------------------------|-------------------------------|
     | Soft drop (per row)     | 1 (flat, no level multiplier) |
     | Hard drop (per row)     | 2 (flat, no level multiplier) |
     | Single line clear       | 100 × level                   |
     | Double line clear       | 300 × level                   |
     | Triple line clear       | 500 × level                   |
     | Tetris (4 lines)        | 800 × level                   |
     | Mini T-spin (no lines)  | 100 × level                   |
     | Mini T-spin single      | 200 × level                   |
     | T-spin (no lines)       | 400 × level                   |
     | T-spin single           | 800 × level                   |
     | T-spin double           | 1200 × level                  |
     | T-spin triple           | 1600 × level                  |
     | Back-to-back modifier       | ×1.5 on Tetris/T-spin clears                        |
     | Combo bonus                 | 50 × combo count × level                            |
     | Single + all-clear          | 800 × level (replaces normal single score)          |
     | Double + all-clear          | 1200 × level (replaces normal double score)         |
     | Triple + all-clear          | 1800 × level (replaces normal triple score)         |
     | Tetris + all-clear          | 2000 × level (replaces normal Tetris score)         |
     | B2B Tetris + all-clear      | 3200 × level (replaces B2B Tetris score)            |

     All-clear rules:
     - All-clear score **replaces** the base line-clear score for that move — it is not added on top.
     - B2B chain **continues** if the all-clear was achieved with a Tetris or T-spin; **breaks** on single/double/triple all-clears (same rule as normal clears).
     - T-spin + all-clear uses the T-spin score as the base replacement (e.g. T-spin triple AC = 1600 × level).

     - **T-spin detection rule (3-corner with front/back distinction):** after a T-piece locks via rotation, check the four diagonal cells of the T center and classify as follows:
       - **Full T-spin:** ≥ 3 corners occupied AND both "front" corners (the two diagonals facing the pointing direction of the T) are occupied.
       - **Mini T-spin:** ≥ 3 corners occupied but only 1 front corner is occupied (back-heavy 3-corner); OR exactly 2 corners occupied and both are the "back" corners (the two diagonals on the flat side of the T).
       - **No T-spin:** fewer than 2 corners occupied, or 2 corners occupied but they are not both back corners.
       The "front" corners per rotation state (origin = T center): Spawn → (−1,+1) and (+1,+1); Right → (+1,+1) and (+1,−1); Reverse → (−1,−1) and (+1,−1); Left → (−1,+1) and (−1,−1).
   - **Level progression** – 10 lines cleared advances one level; level cap at 100 (minimum tick interval of ~33 ms at level 100); combo counter resets after any piece placement that clears zero lines.
   - **Spawn position (Tetris Guideline standard):** pieces spawn centred horizontally in columns 4–5 (0-indexed), which places a standard 4-wide piece across columns 3–6. The visible playfield is rows 0–19 (row 0 = bottom); spawn occurs at rows 20–21 (above the visible area). J, L, S, Z, T spawn with their bounding box top at row 21; I spawns with its single-cell height row at row 21; O spawns across rows 20–21. If any cell of the spawn position is occupied, the game ends immediately (top-out).
   - **Top-out condition** – if a newly spawned piece overlaps occupied cells, the game ends immediately.
4. **Rotation system** – Super Rotation System (SRS) with standard wall-kick tables for all pieces (J, L, S, Z, T use 5-entry kick tables; I piece uses its own kick table). Rotation near walls and the floor must use kicks before failing.
5. **Controls** – Compose gesture surface on the board, tuned for Tetris Guideline feel with touch precision:

   | Gesture | Action |
   |---|---|
   | Tap right half of board | Rotate CW |
   | Tap left half of board | Rotate CCW |
   | Touch DOWN on active piece + horizontal drag | Grab — move piece freely left/right (bypasses DAS/ARR); piece continues falling normally during grab |
   | Touch DOWN on active piece + lift without drag | Rotate CW (right half of piece) or CCW (left half of piece) |
   | Swipe left / right (not on piece) | Move (DAS: 150 ms, ARR: 30 ms) |
   | Slow swipe down / hold | Soft drop — +1 pt/row (flat) |
   | Fast flick down (≥ 800 dp/s) | Hard drop — +2 pts/row (flat) |
   | Swipe up | Drop delay (one use per piece) |
   | Tap held piece panel | Hold — swap active piece with held piece (or store and spawn next if hold is empty); piece resets to spawn rotation; usable once per piece |

   - **Grab mode** – activated when `ACTION_DOWN` hit-tests against a cell occupied by the active piece and the finger moves horizontally before lifting. Piece follows finger column-by-column as touch crosses cell boundaries. Gravity continues unaffected during grab. If finger lifts without horizontal movement, the touch is classified as a tap (rotate). A `GRAB_CELL_SLOP` constant (dp) in `GameConstants` accounts for fingertip imprecision on the hit-test. If the finger moves downward from a piece touch without any prior horizontal movement, grab mode is not activated and the gesture is classified as a normal soft/hard drop by velocity threshold — this keeps accidental drops natural and the classifier simple.
   - **Drop delay** – swipe-up freezes the active piece's gravity for a fixed duration, giving the player time to reposition. Usable once per piece; duration scales linearly from **2 000 ms at level 1** to **200 ms at level 100**: `delay = max(200, 2000 - (level - 1) × 18)` ms (rounded to nearest 10 ms). After the delay expires the piece resumes falling at normal speed.
   - Gesture classification distinguishes slow-drag (soft drop) vs. fast-flick (hard drop) by velocity threshold (~800 dp/s); tunable constant so it can be adjusted during playtesting.
   - DAS (150 ms), ARR (30 ms), hard-drop velocity threshold, drop-delay formula, and `GRAB_CELL_SLOP` all exposed as named constants in `GameConstants`.
   - A small **`?` button** fixed in the top-left corner (top-start) opens the tutorial at any time without pausing the game (renders as a standalone full-screen help panel).
   - A small **mute/unmute button** fixed in the top-right corner (top-end) toggles all sound and vibration instantly; icon switches between speaker and muted-speaker states. Preference persisted in DataStore so it survives app restarts.
   - No other dedicated buttons.
6. **UI + feedback** – Compose board canvas, animated scoreboard, level/lines display, SoundPool/vibration events, per-block flashes on line clear and piece placement. Block visual style must match the reference artwork (see Visual style section below).
7. **Level speed scaling** – Increase fall frequency for each level using a table similar to the classic Sega/Bandai Tetris gravity curve (frames-per-cell decreases per level), with conversion to milliseconds so each level feels noticeably faster without becoming unfair. Hard floor at ~33 ms per tick at level 100.
8. **Next-piece preview queue** – Number of upcoming pieces shown shrinks as speed and cognitive load increase:

   | Level range | Next pieces shown |
   |-------------|-------------------|
   | 1 – 9       | 5                 |
   | 10 – 19     | 4                 |
   | 20 – 39     | 3                 |
   | 40 – 69     | 2                 |
   | 70 – 100    | 1                 |

   `PieceGenerator` always maintains a full look-ahead buffer; the UI simply renders only the first N entries based on the current level. Transitions between tiers animate the queue shrinking (fade-out the last slot).

## Visual style

The target look is derived directly from the splash screen and icon reference images. Every rendering decision should aim to reproduce this aesthetic.

### Block anatomy (single cell)
Each block is a **slightly rounded square** (corner radius ≈ 12–16 % of cell size, so on a 32 dp cell that is ~4–5 dp). The face has three distinct painted layers stacked:

1. **Base fill** – a solid, saturated colour specific to the tetromino (see palette below). Fills the full rounded square.
2. **Inner gradient** – a radial or top-to-bottom linear gradient that brightens toward the top-centre and darkens toward the bottom edge, giving the block a convex, inflated look. The highlight peak is roughly 40 % brighter than the base; the bottom shadow is roughly 30 % darker.
3. **Top-left shine** – a small, soft white oval highlight in the upper-left quadrant of the face (~25 % of cell width), semi-transparent (~60 % opacity). This is what makes the block read as glossy / candy-like rather than flat.
4. **Edge bevel** – a 1–2 dp border around the perimeter, slightly lighter than the base colour on the top/left edges and slightly darker on the bottom/right edges, reinforcing the 3-D raised look.

The overall impression is a **glossy, inflated cube face** — not flat, not fully 3-D isometric, but somewhere between a sticker and a gemstone.

### Tetromino colour palette
Matched to the splash screen artwork:

| Piece | Base colour | Approximate hex |
|-------|-------------|-----------------|
| I     | Cyan / sky blue | `#29BFFF` |
| O     | Yellow | `#FFD633` |
| T     | Magenta / pink-purple | `#CC44FF` |
| S     | Bright green | `#44DD44` |
| Z     | Red-orange | `#FF3333` |
| J     | Royal blue | `#3366FF` |
| L     | Orange | `#FF8800` |

All colours are vivid and saturated — no pastels. They should glow slightly against the dark board background.

### Board background
Dark blue-grey, similar to `#1A1A2E` or `#12122A`. The board grid lines are barely visible — a subtle `#FFFFFF0A` (4 % white) so the grid is hinted at without competing with the blocks. The board itself sits inside a faint neon border (thin, ~1 dp, matching the dominant glow colour of the active piece or a neutral blue-white).

### Glow / bloom effect
Each block emits a soft outer glow in its base colour — a blurred shadow drawn outside the block bounds at roughly 4–8 dp spread and 40–60 % opacity. This is the feature most visible in the icon (the cyan stack glows blue, the orange block glows amber). Implement as a `BlurMaskFilter` on the `Canvas` shadow layer or as a repeated semi-transparent rounded rect drawn at increasing radius before the main block.

### Ghost piece
Same shape as the active piece but rendered as an outline only — the edge bevel colour at ~30 % opacity with no fill and no glow. Clearly visible against the dark board but does not compete visually with the live piece.

### App background (outside the board)
Deep space-like dark purple-to-black radial gradient (`#0D0D1A` center → `#000000` edges), matching the splash screen background. Subtle particle sparkles (small white dots, 1 dp, low opacity, static or very slow drift) can be added in phase 4 as a polish pass.

### Splash screen
- Background: same deep purple-black radial gradient as above.
- Centred logo: the stacked-blocks icon (three cyan + one orange, isometric-style, glowing) with the "MOD FALL" wordmark beneath it in the same bold rounded font visible in the splash image — thick white letters with a dark outline and a subtle colour gradient fill (yellow → green → blue across the letters, matching the splash artwork).
- No animation required; static display for 2 seconds.

### Title / wordmark font style
Bold, rounded, slightly inflated letterforms. Each letter has: a thick dark outline (2–3 dp), a coloured fill with a top-to-bottom gradient (yellow at top, transitioning through green to blue at the bottom), and a subtle white highlight stroke on the top edge. Reproduce this with a custom `Canvas` text draw or a pre-rendered image asset — do not use a generic system font.

### `CandyPalette` implementation notes
- `CandyPalette` provides for each tetromino type: `baseColor`, `highlightColor` (+40 % brightness), `shadowColor` (−30 % brightness), `glowColor` (baseColor at 50 % alpha), and `borderLight` / `borderDark` for the bevel.
- All colours derived programmatically from the base hex values using HSL manipulation so they stay consistent if the palette is adjusted.

## Architecture
- **Core modules**
  - `BoardState` (grid matrix, line-clear detection, all-clear detection, placement, top-out detection). Internally uses a mutable `Array<IntArray>` for fast in-place mutation on the game loop coroutine (single writer — no locks needed). Cell encoding: `0` = empty, `1–7` = tetromino type (I=1, O=2, T=3, S=4, Z=5, J=6, L=7); `CandyPalette` maps type int → colour at render time, keeping core free of UI concerns. Exposes a `snapshot(): BoardSnapshot` method that copies current cells into an immutable `data class BoardSnapshot(val cells: List<List<Int>>)`. `GameViewModel` calls `snapshot()` after each tick and emits it into `GameUiModel`; the UI only ever reads `BoardSnapshot`.
  - `PieceGenerator` (7-bag randomizer, next-queue logic; maintains full 5-piece buffer regardless of displayed count).
  - `GameLoop` (tick coroutine, level scaling, lock-delay counter with 15-reset cap, drop-delay timer, scoring updates, event stream). Guards tick execution against `GameState` — only ticks when state is `Running`. Launched on `Dispatchers.Default` — never on `Dispatchers.Main` to avoid UI jank.
  - `Scoring` (line clears, T-spin/mini T-spin detection, all-clear bonus, combos, back-to-back multiplier, soft/hard drop bonuses, level thresholds).
  - `EffectBridge` (events for sound/vibration).
  - `GameState` — enum used by core: `Idle`, `Running`, `Paused`, `GameOver`. Lives in the `core` package so `GameLoop` and `GameViewModel` share the same type without depending on UI models.
- **UI modules**
  - `BlockDropApp` (Scaffold, menus, dialogs, screen-orientation locked to portrait).
  - `BoardCanvas` (render grid, active tetromino, ghost piece projection via `Canvas`; use `Animatable` for fades).
  - `ControlSurface` (gesture detector covering the board area, tied to `ViewModel` actions).
  - `ScorePanel` (animated score/level/lines display).
  - `TutorialScreen` (full-screen plain-text tutorial/help panel triggered by `?` button; shown automatically on first launch, then on demand; does not pause the game).
- **Infrastructure**
  - `GameViewModel` exposes `StateFlow<GameUiModel>`; handles pause/start/resume/reset; observes `Lifecycle` to auto-pause when the app backgrounds. `GameLoop` receives `viewModelScope` rather than owning its own `CoroutineScope` — the ViewModel holds the returned `Job` and cancels/relaunches it on pause/restart/`onCleared()`.
  - `GameUiModel` includes: `state: GameState`, `pauseReason: PauseReason?` (`User` or `Lifecycle`; null when not paused), `score: Int`, `level: Int`, `lines: Int`, `board: BoardSnapshot`, `activePiece: ActivePieceUiModel` (type, cells as board coordinates — needed by `ControlSurface` for grab hit-testing and by `BoardCanvas` for rendering), `ghostCells: List<BoardCell>` (precomputed drop position, exposed for `BoardCanvas`), `nextPieces: List<TetrominoType>` (first N entries per level tier), `heldPiece: TetrominoType?` (null if hold slot is empty), `canHold: Boolean` (false after hold is used; resets to true on piece lock). `PauseReason` lives in the UI layer since core has no need for it.
  - `EffectBridge` exposes a `SharedFlow<GameEffect>` with `replay=0` and `extraBufferCapacity=8` (fire-and-forget; dropping an effect under pressure is preferable to suspending the game loop). `GameEffect` is a sealed class covering: `Move`, `Rotate`, `Hold`, `SoftDrop`, `HardDrop`, `LineClear(lines: Int)`, `TSpinClear`, `AllClear`, `LevelUp`, `GameOver`.
  - `SoundManager` + `VibrationManager` subscribe to `EffectBridge.effects: SharedFlow<GameEffect>`; `SoundManager` requests/releases Android audio focus and ducks on interruption (calls, notifications). Both respect a `isMuted: StateFlow<Boolean>` sourced from DataStore; the top-right mute button writes to this flow and the change takes effect immediately.
  - `CandyPalette` helper maps each tetromino to its vivid saturated base colour and derives highlight, shadow, glow, and bevel values (see Visual style section for full spec).
  - `SoundManager` should use lightweight bundled samples (short PCM/WAV or Synthesized SFX) so the APK stays small and load to memory quickly.
  - Sound effects should lean on simple synth/retro tones reminiscent of the original arcade Tetris—short blips for moves, a deeper pulse for clears, and a celebratory chime for level-up.
  - `GameLoop` events (line clears, hard drop, level up, game over) flow into a shared `GameUiModel` `StateFlow`; Compose UI, effects (sound/vibration), and the scoreboard screen react to that single stream so we avoid divergent state logic.
  - Local scoreboard persisted with **DataStore** (not SharedPreferences); store up to 10 entries sorted by score descending; when full, drop the lowest entry if the new score qualifies.
  - Keep the dependency list minimal (lean Compose modules, no analytics/ads) and note to profile cold start on early builds so we hit the "fast load" goal before shipping.
  - `GameConstants` — a single object holding all tunable numeric constants: DAS (150 ms), ARR (30 ms), hard-drop velocity threshold (800 dp/s), lock-delay duration (500 ms), lock-delay max resets (15), drop-delay formula coefficients, gravity speed table, next-queue tier table, `GRAB_CELL_SLOP` (touch tolerance in dp for piece hit-testing).
  - **DI strategy:** manual constructor injection (no Hilt/Koin); `GameViewModel` receives `BoardState`, `PieceGenerator`, `GameLoop`, `Scoring`, and `EffectBridge` via a `ViewModelFactory` so components remain testable without a DI framework dependency.

## Localisation

All UI strings are defined in `res/values/strings.xml` (English baseline). Each locale gets its own `res/values-<code>/strings.xml`. No text is hardcoded in Kotlin or Compose files.

### Supported languages

| Tier | Language | Locale code | Notes |
|------|----------|-------------|-------|
| 1 | English | `en` | Baseline |
| 1 | Chinese Simplified | `zh-rCN` | Largest Android install base |
| 1 | Japanese | `ja` | Tetris home market; high mobile spend |
| 1 | Korean | `ko` | Top mobile gaming market per capita |
| 1 | Portuguese (Brazil) | `pt-rBR` | Largest Android market in Latin America |
| 1 | Spanish | `es` | 500 M+ speakers; Latin America + Spain |
| 2 | Russian | `ru` | Large CIS gaming market |
| 2 | German | `de` | |
| 2 | French | `fr` | France, Quebec, West Africa |
| 2 | Italian | `it` | |
| 2 | Turkish | `tr` | Top-10 download market |
| 2 | Polish | `pl` | |
| 3 | Hindi | `hi` | World's largest Android market by volume |
| 3 | Indonesian | `in` | 4th largest country, Android-dominant |
| 3 | Arabic | `ar` | **RTL** — requires layout mirroring |
| 3 | Vietnamese | `vi` | Fast-growing mobile market |
| 3 | Thai | `th` | |
| 3 | Chinese Traditional | `zh-rTW` | Taiwan / Hong Kong |

### RTL support (Arabic)
- Set `android:supportsRtl="true"` in the manifest.
- All Compose layouts use `start`/`end` rather than `left`/`right` so they mirror automatically under `LayoutDirection.Rtl`.
- The `?` button stays top-**start** (mirrors to top-right in RTL).
- Board canvas (`Canvas`) is direction-neutral (grid is symmetric); no special mirroring needed.
- Test RTL in the emulator with Arabic locale before release.

### Strings to localise
Every visible string must have a key: game title, all score action labels (Single, Double, Tetris, T-spin, All Clear, etc.), tutorial text, pause menu items, scoreboard labels, game-over prompt, nickname input hint, "not in top 10" message, and the `?` button content description for accessibility.

## Implementation phases
1. **Foundation**
   - Scaffold `MainActivity` locked to portrait, Compose entry point, theme, and ViewModel.
   - Set `android:supportsRtl="true"` and `minSdk = 28` in manifest/`build.gradle`. API 28 (Android 9 Pie) covers ~95 % of active Android devices and enables `VibrationEffect`, `AudioFocusRequest`, and all Compose/DataStore APIs used in this project without compatibility shims.
   - **Adaptive icon:** created manually by the developer in Android Studio (Image Asset Studio); agent does not generate icon assets. Agent only ensures `res/mipmap-anydpi-v26/ic_launcher.xml` is referenced correctly in the manifest.
   - **Splash screen:** implement using the `androidx.core:core-splashscreen` API (compatible with minSdk 28). Show the app logo centred on the deep purple-black background for 2 seconds, then transition to `MainActivity`. No animation required — static logo is sufficient. Style matches the Visual style splash screen spec.
   - Define core data models (tetromino shapes, SRS rotation tables with wall-kick data, board state).
   - Create `res/values/strings.xml` with all string keys in English; create placeholder `strings.xml` files for all 17 other locales (empty or English fallback) so the resource structure is in place from day one.
   - Wire `GameViewModel` with lifecycle observer so the game auto-pauses on `ON_STOP` and resumes on `ON_START`.
2. **Game loop + logic**
   - Implement piece spawning (7-bag), collision, gravity ticks, top-out detection (game-over on spawn collision).
   - Lock delay: 500 ms timer, reset on each move/rotation up to a max of 15 resets, then lock unconditionally.
   - Line-clear detection, T-spin detection (3-corner rule: lock T via rotation, count occupied diagonal neighbors of T center; ≥3 = T-spin, 2 back-facing = mini T-spin), combo counter (reset on zero-clear placement), back-to-back tracking.
   - All-clear detection: after line-clear processing, if board is fully empty emit all-clear event; replace the line-clear score with the appropriate all-clear value (800/1200/1800/2000/3200 × level based on line count and B2B status); B2B chain continues for Tetris/T-spin all-clears, breaks for single/double/triple.
   - Scoring: base per-line values, T-spin/mini T-spin bonuses, back-to-back 1.5× multiplier, all-clear bonus, combo linear bonus (50 × combo count × level), level multiplier, soft/hard drop rewards.
   - Level advancement: every 10 lines, cap at 100; update gravity tick interval and next-piece queue depth from their respective tables.
   - Drop delay: on swipe-up, suspend gravity tick for `max(200, 2000 - (level - 1) × 18)` ms; mark used-flag per piece so it can only fire once; resume normal gravity after timer expires or piece locks.
   - Ghost piece: compute drop position from active piece state; expose in `GameUiModel`.
   - Hold: on tap of held piece panel, if `canHold` is true — swap active piece with `heldPiece` (or store and pull next from queue if hold is empty); reset swapped-in piece to spawn rotation; set `canHold = false`; reset on piece lock.
   - Hook coroutine game loop to ViewModel; expose state updates for the UI.
3. **UI rendering**
   - Build board renderer with Compose `Canvas`; draw ghost piece as outlined/translucent blocks beneath the active piece.
   - Animate block placement/clear flashes (row flash then fade).
   - Implement gesture recognizer: tap right-half = rotate CW, tap left-half = rotate CCW, swipe-left/right with DAS/ARR = move, slow-swipe-down (< 800 dp/s) = soft drop, fast-flick-down (≥ 800 dp/s) = hard drop, swipe-up = drop delay. Board is divided at the horizontal midpoint for tap-zone detection. All tunable values in `GameConstants`.
   - Implement grab mode in `ControlSurface`: on `ACTION_DOWN`, hit-test touch position against active piece cells from `GameUiModel` (using `GRAB_CELL_SLOP` tolerance); if hit, enter grab mode — horizontal drag moves piece column-by-column, lift without drag = rotate, downward drag without prior horizontal = normal soft/hard drop classification.
   - Build `TutorialScreen`: plain-text scrollable help screen listing all gestures and scoring rules; triggered by the `?` button (top-left, always visible); shown automatically on first launch (DataStore flag), dismissable via the dedicated bottom action button or back navigation.
   - Render each block according to the Visual style spec: base fill → inner gradient → top-left shine → edge bevel → outer glow (`BlurMaskFilter`). Use `CandyPalette` for all colour values.
   - Create responsive scoreboard and next-piece preview panel; next-queue panel animates slot count change on level-tier transition.
   - Render held piece panel (tappable); dim it with reduced opacity when `canHold` is false to signal unavailability.
   - Handle Android back button: if game is running → pause and show pause dialog; if already paused → show exit confirmation.
4. **Effects & polish**
   - Add SoundPool/SoundEffect playback and vibration hooks; implement audio focus request/release in `SoundManager`.
   - Implement animations (level-up flash, T-spin/Tetris celebration overlay, soft/hard drop streaks).
   - Pause menu: Resume, Restart, Exit to main menu. No separate mute toggle in pause menu — the persistent mute button in the top-right corner covers this.
   - Add accessibility tweaks (content descriptions for score/level elements) and basic testing (unit tests for core logic, UI previews for Compose screens).
5. **Scoreboard & game over**
   - On game over: show final score overlay, prompt for nickname (max 12 characters, alphanumeric + spaces only). Each scoreboard entry stores: nickname, score, level reached, and total lines cleared.
   - Append to DataStore scoreboard (max 10 entries; evict lowest-ranking entry if full and new score qualifies; otherwise show "not in top 10" message).
   - **Sort order and tie-breaking:**
     1. Score descending (higher score = better rank).
     2. Tie on score → lower level ranks higher (same score at a lower level is more impressive).
     3. Tie on score + level → lower lines-cleared ranks higher (same score with fewer rows cleared).
     4. Tie on score + level + lines → entries share the same rank number. The next distinct entry skips rank(s) accordingly (e.g. two entries at rank 3 → next entry is rank 5).
   - Display scoreboard in a standalone full-screen screen/panel with entry rank, nickname, score, level reached, and lines cleared.
6. **Localisation & testing**
   - Fill all 17 non-English `strings.xml` files with translated content (machine-translate as a base, then review).
   - Verify Arabic RTL layout in emulator: `?` button top-right, score panel mirrored, no clipped text.
   - Check for text overflow in German and Russian (longest average word length among the set); adjust font size or line wrapping as needed.
   - Unit tests for `BoardState` (collision, line-clear, top-out), `Scoring` (all clear types, T-spin, back-to-back, combo), `PieceGenerator` (7-bag distribution), `GameLoop` (lock-delay reset cap, drop-delay timer, drop-delay one-use-per-piece flag).
   - `GameLoop` tests use `kotlinx-coroutines-test`: pass `TestScope` as the `CoroutineScope` to `GameLoop.start()`, use `advanceTimeBy` to control virtual time for lock-delay expiry, drop-delay timer, and gravity ticks without real waiting. Use `UnconfinedTestDispatcher` for tests that only need coroutines to run eagerly without time control. Add `kotlinx-coroutines-test` to `testImplementation` dependencies.
   - Compose previews for board (with ghost piece), tutorial screen, scoreboard screen, and pause menu in at least English, Arabic (RTL), and Chinese Simplified.
   - Profile cold start; target < 1 s to first frame on a mid-range device.

## Package structure

```
com.blockdrop/
├── core/
│   ├── GameState.kt          (enum: Idle, Running, Paused, GameOver)
│   ├── GameConstants.kt      (DAS, ARR, gravity table, drop-delay, etc.)
│   ├── BoardState.kt         (mutable Array<IntArray> internals + snapshot(); BoardSnapshot data class in same file)
│   ├── PieceGenerator.kt     (7-bag randomizer, next-queue buffer)
│   ├── GameLoop.kt           (tick coroutine, receives CoroutineScope from ViewModel)
│   ├── Scoring.kt            (line clears, T-spin, all-clear, combo, B2B, soft/hard drop)
│   └── EffectBridge.kt       (SharedFlow<GameEffect> replay=0 extraBufferCapacity=8; GameEffect sealed class)
├── ui/
│   ├── model/
│   │   ├── GameUiModel.kt    (state: GameState, pauseReason: PauseReason?, score, level, lines, BoardSnapshot, etc.)
│   │   └── PauseReason.kt    (enum: User, Lifecycle)
│   ├── viewmodel/
│   │   ├── GameViewModel.kt  (viewModelScope owner, Job handle, StateFlow<GameUiModel>)
│   │   └── ViewModelFactory.kt
│   ├── BlockDropApp.kt
│   ├── BoardCanvas.kt
│   ├── ControlSurface.kt
│   ├── ScorePanel.kt
│   ├── TutorialOverlay.kt
│   └── CandyPalette.kt
├── effects/
│   ├── SoundManager.kt
│   └── VibrationManager.kt
└── data/
    └── ScoreboardRepository.kt
```

## Next steps
1. Generate project scaffolding: packages per structure above and placeholder files.
2. Begin implementing the core modules from phase 1.
3. Playtesting pass after phase 2: validate DAS/ARR values and hard-drop velocity threshold on physical devices; adjust constants before building the full UI.
