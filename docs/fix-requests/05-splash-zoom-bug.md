## Issue: Splash screen image zoomed in — bitmap drawn at density-scaled intrinsic size instead of fitting the screen

### Summary

The splash screen artwork (`splashscreen.png`) appears heavily zoomed in: only the centre of the image is visible and the edges are clipped. The cause is that the image is stored in `drawable/` without a density qualifier (treated as mdpi / 1×), so Android scales it up by the device's pixel ratio before drawing. With `android:gravity="center"` the oversized image is centred in the window and only its middle portion shows. Additionally, the SplashScreen API is not wired up correctly — the activity theme does not extend `Theme.SplashScreen` and `postSplashScreenTheme` is not set — meaning the compat library does not manage the splash at all on Android 11 and below, and the raw `windowBackground` bitmap is what the user sees.

---

### Affected files

| File | Issue |
|---|---|
| `app/src/main/res/drawable/splash_background.xml` | `<bitmap android:gravity="center">` draws image at intrinsic density-scaled size |
| `app/src/main/res/drawable/splashscreen.png` | Stored in `drawable/` (mdpi baseline) — no density-bucketed variants |
| `app/src/main/res/values/themes.xml` | Activity theme extends `Theme.Material3.Dark.NoActionBar`, not `Theme.SplashScreen` |
| `app/src/main/AndroidManifest.xml` | Activity uses the wrong starting theme — `postSplashScreenTheme` not configured |

---

### Root cause 1 — bitmap density scaling

`splashscreen.png` is in `res/drawable/`, which Android treats as the mdpi (1×, 160 dpi) bucket. At runtime, the drawable system multiplies the image's pixel dimensions by the device's screen density before placing it on canvas. On a common xxhdpi phone (3× density), a 1080 × 1440 px source image is treated as if it were 360 × 480 dp, drawn as 1080 × 1440 px logical pixels — which at 3× density is 3240 × 4320 physical pixels. The image vastly exceeds the screen.

`android:gravity="center"` centres this already-upscaled image in the window bounds without clipping it to fit. The result is that only the centre portion is visible — a zoomed-in crop.

```xml
<!-- splash_background.xml — current -->
<item>
    <bitmap
        android:src="@drawable/splashscreen"
        android:gravity="center" />   <!-- draws at intrinsic size, centred — wrong -->
</item>
```

**Fix A — correct the gravity to fill the window:**

Change `android:gravity` to `fill` so the bitmap is stretched to fill the window bounds:

```xml
<item>
    <bitmap
        android:src="@drawable/splashscreen"
        android:gravity="fill" />
</item>
```

`fill` scales the image to exactly match the window width and height. Slight aspect-ratio distortion is possible on unusually wide or narrow screens, but for a portrait-locked game with artwork that bleeds to the edges this is acceptable and the distortion is imperceptible.

**Fix B — alternative: supply density-bucketed images:**

Place correctly sized variants of the artwork in the appropriate density folders:

| Folder | Scale | Typical resolution |
|---|---|---|
| `drawable-mdpi/` | 1× | 360 × 800 px |
| `drawable-hdpi/` | 1.5× | 540 × 1200 px |
| `drawable-xhdpi/` | 2× | 720 × 1600 px |
| `drawable-xxhdpi/` | 3× | 1080 × 2400 px |
| `drawable-xxxhdpi/` | 4× | 1440 × 3200 px |

Android will then pick the right image at each density, and `gravity="center"` will place the correctly-sized image without overflow. This is the highest-quality option but requires supplying multiple image exports.

Fix A is minimal and sufficient. Fix B is optional polish.

---

### Root cause 2 — SplashScreen API not wired

`MainActivity.onCreate()` calls `installSplashScreen()` from `androidx.core:core-splashscreen`. For the compat library to work correctly — particularly on Android 11 and below — the activity's **starting theme** must:

1. Extend `Theme.SplashScreen` (or `Theme.SplashScreen.IconBackground`)
2. Declare `postSplashScreenTheme` pointing to the real app theme

Current `themes.xml`:
```xml
<style name="Theme.BlockDrop" parent="Theme.Material3.Dark.NoActionBar">
    <item name="android:windowBackground">@drawable/splash_background</item>
    ...
</style>
```

The activity's starting theme and its main theme are the same. There is no `postSplashScreenTheme`. As a result:

- **Android 12+:** The system draws its own splash (the app icon centred on the background colour). The `windowBackground` drawable appears as a brief flash before the system splash paints, then again when `installSplashScreen` dismisses and the Compose content isn't drawn yet. On API 31+ the `windowBackground` is largely ignored by the SplashScreen API.
- **Android 11 and below:** The compat library cannot function without `Theme.SplashScreen` as the starting theme. It falls back to showing the raw `windowBackground`, which is the zoomed bitmap.

**Fix — add a dedicated splash theme and wire it correctly:**

`app/src/main/res/values/themes.xml`:
```xml
<!-- New: starting theme used only during launch -->
<style name="Theme.BlockDrop.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/splash_background</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
    <item name="postSplashScreenTheme">@style/Theme.BlockDrop</item>
</style>

<!-- Existing: remove windowBackground from here once the splash theme handles it -->
<style name="Theme.BlockDrop" parent="Theme.Material3.Dark.NoActionBar">
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
    <item name="android:windowLightStatusBar">false</item>
    <item name="android:windowLightNavigationBar">false</item>
    <!-- windowBackground removed: handled by Theme.BlockDrop.Splash -->
</style>
```

`app/src/main/AndroidManifest.xml`:
```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:screenOrientation="portrait"
    android:theme="@style/Theme.BlockDrop.Splash">   <!-- changed from Theme.BlockDrop -->
```

With this setup:
- On Android 12+: the system splash uses `windowSplashScreenBackground` + `windowSplashScreenAnimatedIcon` (the app icon). `postSplashScreenTheme` transitions to `Theme.BlockDrop` after the splash.
- On Android 11 and below: the compat library simulates the same icon-centred splash using the theme attributes, then transitions to `Theme.BlockDrop`.
- `installSplashScreen().setKeepOnScreenCondition { … < 2_000L }` continues to hold the splash on screen for the 2-second delay as intended.

The full-screen `splashscreen.png` artwork is no longer the window background and will not be shown during the system/compat splash. If the intent is to show the artwork for 2 seconds, the recommended approach is to render it as a Compose screen (e.g. an intro `AnimatedContent` in `BlockDropApp`) gated on a ViewModel flag, which the `setKeepOnScreenCondition` already creates a natural hook for. This is out of scope for this fix but is the correct long-term path.

---

### Acceptance criteria

1. The splash screen image is not zoomed in — the full artwork or the icon is visible and correctly scaled on devices from 1× to 4× density.
2. No brief flash of the wrong background during app launch.
3. The 2-second splash hold continues to work (`setKeepOnScreenCondition`).
4. The main app theme (`Theme.BlockDrop`) applies correctly after splash dismissal — status bar, nav bar, and Material3 styling are unaffected.

---

### Out of scope

- Displaying the `splashscreen.png` artwork as the full-screen background within the 2-second delay. The SplashScreen API is designed for icon-based splashes, not full-screen artwork. Full-screen artwork should be handled as a Compose intro screen in the app content, not as a window background.
- Replacing the placeholder `ic_launcher_foreground.xml` with the final icon asset — that is a separate task (covered in M5 launch prep).
