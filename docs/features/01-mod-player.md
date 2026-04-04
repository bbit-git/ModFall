## Task: Implement Android MOD music playback from `Downloads/Mods`

### Objective

Add background music playback for user-provided tracker module files on Android. The game should read module files from the device `Downloads/Mods` directory, randomly choose a track to play, continue with another random track when one ends, and expose small current-track info for UI display.

### Constraints

* Do not bundle any music assets with the game.
* Do not implement any music download/import feature.
* Do not add editor functionality.
* Keep the implementation headless except for exposing minimal track info needed by the existing UI layer.
* Target Android first.
* Prefer an implementation that is safe under modern Android storage restrictions.

### Required behavior

#### Music source

* Default source directory: `Downloads/Mods`
* Supported user-provided formats:

  * required: `MOD`, `XM`
  * preferred: `S3M`, `IT`
* Ignore non-files, unreadable files, and unsupported files.
* Corrupt files must not crash the app.

#### Playback

* On start, scan the music source.
* Build a library of valid module files.
* If at least one valid file exists, pick one at random and start playback.
* When the current track finishes, pick another random track and continue playback automatically.
* If more than one valid file exists, avoid immediate repeat of the same file.
* If exactly one valid file exists, replay it.
* If no valid files exist, do not crash. Expose an empty state and log a useful message.

#### Track info

Expose enough metadata for a small in-game overlay:

* title from module metadata when available
* otherwise filename
* optional format label such as `MOD`, `XM`, `S3M`, `IT`

Example display strings:

* `♫ Space Drift [XM]`
* `♫ acid_loop_02.mod`

#### UI expectations

Do not build a full UI. Only expose state that an existing UI layer can render.

**Decided overlay spec:**

* Placed at the top level of `BlockDropApp`, floating above all other content.
* Centered text only — no border, no background box.
* Text has a drop shadow for legibility against any background.
* Overflow hidden (text clipped to its bounds, no wrapping beyond a single line if possible).
* Show on track change, remain visible for 3–5 seconds, then fade out.
* Respects the global mute flag — hide overlay when muted (no music playing).

### Android storage requirements

**Decided: direct `File` access to `Downloads/Mods` with `READ_EXTERNAL_STORAGE` permission. SAF fallback deferred.**

* Declare `READ_EXTERNAL_STORAGE` in the manifest (effective on API < 33; on API 33+ no permission is needed for `Downloads` via `MediaStore` if scoped access suffices — handle at runtime).
* Keep playback logic separate from storage access logic so SAF can be added later without touching the playback layer.

### Lifecycle ownership

**Decided: `GameViewModel` owns `ModMusicService`.**

* `GameViewModel` calls `ModMusicService.start()` when a new game begins and `stop()` on game restart — music restarts with each new game.
* `GameViewModel` pauses/resumes music on `ON_STOP`/`ON_START` lifecycle events (same hooks used for the game loop).
* `GameViewModel.onCleared()` calls `ModMusicService.close()`.
* Music volume and enabled state respect the existing global mute `StateFlow` from `SettingsRepository` — no separate mute control needed.

### Architecture requirements

Split the implementation into two layers:

#### 1. Storage/library layer

Responsible for:

* resolving access to `Downloads/Mods`
* scanning the directory or granted folder
* returning readable file handles or URIs
* filtering candidates
* validating candidates enough to avoid obvious bad entries

#### 2. Playback layer

Responsible for:

* loading a module file
* decoding/rendering PCM through the selected backend
* playing audio off the main thread
* detecting track completion
* triggering next random track
* exposing current track info and playback state
* releasing resources correctly

### Suggested public API

```kotlin
interface ModMusicService : AutoCloseable {
    fun start()
    fun stop()
    fun pause()
    fun resume()

    fun setEnabled(enabled: Boolean)
    fun setVolume(volume: Float)

    fun rescanLibrary()
    fun currentTrackInfo(): ModTrackInfo?
    fun hasTracks(): Boolean
}
```

```kotlin
data class ModTrackInfo(
    val title: String?,
    val fileName: String,
    val format: String?,
    val pathOrUri: String
)
```

### Implementation guidance

* Use a dedicated worker thread or coroutine context for decode/playback work.
* Do not block the main/game thread.
* Make `stop()` and `close()` safe to call repeatedly.
* Skip invalid tracks and continue if others are available.
* Keep selection logic deterministic enough to test.
* Track the last played item and avoid replaying it immediately when alternatives exist.

### Backend choice

**Decided: libopenmpt via JNI.**

Rationale: pure-JVM alternatives (javamod, Micromod/IBXM) either depend on `javax.sound`/AWT (not available on Android) or lack IT/S3M support. libopenmpt provides the broadest format coverage, correct playback of real-world files, and active maintenance. A minimal JNI wrapper will be built for the project.

### Acceptance criteria

* App scans `Downloads/Mods` or a fallback user-granted equivalent
* Valid `MOD` and `XM` files are playable
* Random playback starts when tracks are available
* On track end, another random track starts automatically
* Immediate repeat is avoided when more than one valid track exists
* Current track metadata is exposed for a small overlay
* Empty folder, unreadable folder, corrupt file, and unsupported file cases do not crash the app
* No bundled music assets
* No music downloading/import logic

### Non-goals

* playlist screen
* file browser beyond minimal storage-access fallback
* tracker editor
* visualizer
* pattern/row sync callbacks
* per-channel controls
* online music discovery

### Deliverables

* production implementation
* minimal integration notes
* tests covering:

  * empty library
  * single valid track
  * multiple valid tracks
  * corrupt/unsupported file handling
  * no immediate repeat logic
  * resource cleanup

### Output expectations for Codex

1. Inspect the existing project structure and identify the correct integration points.
2. Implement the feature with minimal disruption to current architecture.
3. Add or update tests.
4. Summarize:

   * files changed
   * backend chosen
   * Android storage approach used
   * any limitations or follow-up work

Here is an even tighter prompt version you can paste directly into Codex:

```text
Implement Android background music playback for user-provided tracker module files.

Requirements:
- Source folder is Downloads/Mods by default
- Play user-provided MOD and XM files at minimum; S3M and IT if supported
- Randomly choose a valid track on start
- When a track ends, automatically play another random track
- Avoid immediate repeat when more than one valid track exists
- Expose current track info for a small UI overlay: title if available, otherwise filename, plus optional format
- Do not bundle any music assets
- Do not implement downloading/importing music
- Handle empty, unreadable, unsupported, and corrupt files without crashing
- Keep playback off the main thread
- Separate storage access from playback logic
- Add tests for empty library, invalid files, multiple tracks, and no-immediate-repeat behavior

Please:
1. inspect the codebase and choose the best integration points,
2. implement the feature,
3. add tests,
4. summarize changed files, design choices, and limitations.
