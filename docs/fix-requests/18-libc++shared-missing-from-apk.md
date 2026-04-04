# 18 — libopenmpt_jni not loaded: libc++_shared.so missing from APK

## Reported symptom

Tapping a track in the music library shows the error state. Logcat:

```
E ModPlayer: libopenmpt_jni not available
E ModPlayer: java.lang.UnsatisfiedLinkError: No implementation found for long
    com.bigbangit.blockdrop.music.OpenMptJni.nativeOpen(byte[], int)
    (tried Java_com_bigbangit_blockdrop_music_OpenMptJni_nativeOpen and
     Java_com_bigbangit_blockdrop_music_OpenMptJni_nativeOpen___3BI)
```

No audio plays on any track regardless of file, device, or ABI.

## Root cause

`libopenmpt.so` was built with `APP_STL := c++_shared` (see `scripts/build-libopenmpt.sh:82`). This means the `.so` has a runtime dependency on `libc++_shared.so`:

```
$ readelf -d libopenmpt.so | grep NEEDED
  NEEDED  libz.so
  NEEDED  libc.so
  NEEDED  libm.so
  NEEDED  libc++_shared.so    ← required at runtime
  NEEDED  libdl.so
```

`libc++_shared.so` is present in the prebuilt directory:

```
app/src/main/cpp/libopenmpt/prebuilt/arm64-v8a/arm64-v8a/libc++_shared.so
app/src/main/cpp/libopenmpt/prebuilt/armeabi-v7a/armeabi-v7a/libc++_shared.so
app/src/main/cpp/libopenmpt/prebuilt/x86_64/x86_64/libc++_shared.so
```

But it is **not packaged into the APK**. The APK contains:

```
lib/arm64-v8a/libopenmpt.so      ✓
lib/arm64-v8a/libopenmpt_jni.so  ✓
lib/arm64-v8a/libc++_shared.so   ✗ MISSING
```

AGP does not automatically include `libc++_shared.so` from the prebuilt path because:
- `CMakeLists.txt` imports `libopenmpt` as `IMPORTED SHARED` — AGP packages this `.so`, but not its transitive dependencies.
- `libc++_shared.so` is never mentioned in `CMakeLists.txt`, so AGP never adds it to the APK.
- `libc++_shared.so` is **not** a public Android platform library — it must be bundled by the app.

The load chain at runtime:

```
System.loadLibrary("openmpt_jni")
  → dlopen(libopenmpt_jni.so)        ← succeeds (file is in APK)
    → dlopen(libopenmpt.so)           ← succeeds (file is in APK)
      → dlopen(libc++_shared.so)      ← FAILS — not in APK, not on device
  → dlopen returns error, LoadLibrary throws UnsatisfiedLinkError
```

`runCatching` in `OpenMptJni.init` silently swallows the `UnsatisfiedLinkError` from `loadLibrary`. Later, when `ModPlayer.load()` calls `nativeOpen`, a second `UnsatisfiedLinkError` is thrown ("No implementation found") because the library was never registered. `ModPlayer` catches this and logs "libopenmpt_jni not available".

## Fix

Package `libc++_shared.so` for each ABI into the APK.

**Option A — jniLibs source directory (recommended):**

Create symlinks or copy `libc++_shared.so` into `app/src/main/jniLibs/`:

```
app/src/main/jniLibs/arm64-v8a/libc++_shared.so
app/src/main/jniLibs/armeabi-v7a/libc++_shared.so
app/src/main/jniLibs/x86_64/libc++_shared.so
```

AGP automatically packages everything in `jniLibs/` into the APK under `lib/<abi>/`.

**Option B — extend CMakeLists.txt to include libc++_shared:**

Add `libc++_shared.so` as a second imported library in `CMakeLists.txt`:

```cmake
add_library(c++_shared SHARED IMPORTED)
set_target_properties(c++_shared PROPERTIES
    IMPORTED_LOCATION "${LIBOPENMPT_PREBUILT_ROOT}/${LIBOPENMPT_ABI}/${LIBOPENMPT_ABI}/libc++_shared.so"
)
target_link_libraries(openmpt_jni openmpt c++_shared ${log-lib} ${z-lib})
```

AGP packages imported shared libraries that appear in target link dependencies.

**Option C — rebuild libopenmpt with static STL:**

Change `APP_STL := c++_shared` to `APP_STL := c++_static` in the build script. Static STL is linked into `libopenmpt.so` itself — no separate `.so` required. This increases `libopenmpt.so` size by ~300–400 KB but eliminates the packaging dependency. Requires rebuilding the prebuilt libraries.

Option A is the smallest change and does not require a rebuild. Option C is the most self-contained but requires a full rebuild.

## Impact

Blocking — no track plays on any device. `libopenmpt_jni` is never loaded, so all track selections silently fail after showing the error state.
