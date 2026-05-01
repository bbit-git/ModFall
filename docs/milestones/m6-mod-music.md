# M6 — MOD Music Playback

**Status:** Completed
**Depends on:** M4

## Scope

Optional background music via tracker module files (MOD, XM, S3M, IT) decoded with libopenmpt through a native JNI bridge.

### Architecture
- `ModMusicService` interface with `DefaultModMusicService` implementation
- `ModLibrary` — scans `Downloads/Mods` for supported module files
- `ModPlayer` — streams decoded PCM from libopenmpt to `AudioTrack` on a dedicated thread
- `OpenMptJni` — thin JNI bridge to libopenmpt native library
- `openmpt_jni.c` — C wrapper exposing open/close/readStereo/getTitle/getDuration
- CMake build compiles libopenmpt from source for arm64-v8a, armeabi-v7a, x86_64

### Playback behaviour
- Random track selection with immediate-repeat avoidance
- Auto-advances to next track on completion
- Respects mute toggle (pauses/resumes with game, stops when muted)
- Track metadata (title, format) extracted from module at load time

### UI integration
- `TrackInfoOverlay` — transient bottom overlay showing current track name for ~4 seconds on track change, with fade-in/fade-out animation
- Track display fields (`trackDisplay`, `trackDisplayKey`) added to `GameUiModel`
- Music lifecycle tied to game lifecycle (start/stop/pause/resume) in `GameViewModel`

### Permissions
- `READ_EXTERNAL_STORAGE` (maxSdkVersion 32) declared in manifest

## Progress

- Module playback engine implemented (ModPlayer with AudioTrack streaming)
- JNI bridge and native C wrapper for libopenmpt complete
- Library scanning with format filtering and validation
- Track selection with repeat avoidance
- ViewModel integration for lifecycle, mute state, and music-enabled state
- TrackInfoOverlay UI component
- Main-tune preference persisted through settings and preferred on game start
- Music playback correctly blocked when music is disabled, independent from global mute
- Unit tests for ModLibrary (10), ModTrackInfo (5), track selection (6)
- NDK build configuration (CMakeLists.txt, abiFilters)
- Runtime permission request for `READ_EXTERNAL_STORAGE` on API 23-32
- Scoped-storage library selection flow for modern Android via music-folder picker

## Exit criteria

Module files in Downloads/Mods play as background music during gameplay. Playback pauses/resumes with game state and respects mute. Track info overlay appears on track changes. No native crashes or leaked AudioTrack instances. Graceful fallback when no tracks are present or libopenmpt source is not available.
