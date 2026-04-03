# M4 — Effects, Polish, Tutorial, Scoreboard & Game Over

**Status:** Transferable
**Depends on:** Playtest Gate

## Scope

### Sound & vibration
- `SoundManager`: **programmatic synthesis only** (no bundled samples)
  - Old-school chip-style synthesis via `AudioTrack` — square waves, triangle waves, noise channel
  - Short blips for move/rotate, deeper pulse for line clears, rising tone for level-up, noise burst for hard drop, celebratory chime for Tetris/T-spin, descending tone for game over
  - Audio focus request/release, duck on interruption
  - Respects `isMuted: StateFlow<Boolean>` from DataStore
- `VibrationManager`: haptic feedback per `GameEffect` event, respects mute state
- Both subscribe to `EffectBridge.effects: SharedFlow<GameEffect>`

### Animations
- Line-clear: row flash then fade-out
- Piece placement: per-block flash
- Level-up: screen flash
- T-spin / Tetris: celebration overlay

### Tutorial & menus
- `TutorialOverlay`: scrollable plain-text reference (all gestures + scoring rules), auto-shown on first launch (DataStore flag), dismissable by tap outside, triggered by `?` button on demand, renders as overlay without pausing game
- Pause menu: Resume, Restart, Exit to main menu

### Scoreboard & game over
- Game-over overlay with final score display
- Nickname input (max 12 characters, alphanumeric + spaces)
- DataStore scoreboard storage (max 10 entries, sorted by score descending)
- Tie-breaking: score desc → level asc → lines asc → shared rank (next entry skips)
- Eviction: drop lowest entry if full and new score qualifies
- "Not in top 10" message when score doesn't qualify
- Scoreboard modal overlay (rank, nickname, score, level, lines)
- Each entry stores: nickname, score, level reached, total lines cleared

## Progress

- DataStore-backed `SettingsRepository` added for persistent mute state and first-launch tutorial visibility
- DataStore-backed `ScoreboardRepository` added with top-10 storage, sort/eviction, shared-rank calculation, and nickname sanitisation
- `GameViewModel` now owns mute/tutorial persistence and the game-over score submission flow
- Game-over UI now prompts for nickname, saves local scores, and opens a scoreboard modal overlay
- `SoundManager` now subscribes to `EffectBridge`, synthesises chip-style PCM with `AudioTrack`, and applies mute plus audio-focus handling
- `VibrationManager` now subscribes to `EffectBridge` and maps gameplay events to haptic patterns while respecting mute state
- Board rendering now reacts to gameplay effects with placement flash, line-clear flash, level-up flash, and Tetris/T-spin/all-clear celebration overlays
- Ranking/eviction logic covered by direct unit tests

## Remaining

- Manual device tuning for effect intensity and animation feel

## Transfer Notes

- Core M4 scope is implemented: persistent mute/tutorial state, local scoreboard flow, effect audio, haptics, and board reaction animations
- Pause menu now supports Resume, Restart, and Exit to menu
- Remaining work is fidelity tuning on hardware, not structural implementation

## Exit criteria

Sound synthesis plays correctly for all events. Vibration fires. Animations trigger on the right events. Tutorial displays and dismisses. Mute toggle persists across restarts. No audio glitches or leaked `AudioTrack` instances. Full game-over → nickname → scoreboard flow works. Entries persist across app restarts. Tie-breaking and eviction logic verified.
