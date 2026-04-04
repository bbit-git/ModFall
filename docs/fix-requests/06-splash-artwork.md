## Issue: Splash shows placeholder blue square — artwork must be shown via Compose, not the SplashScreen API icon

### What the user sees now

After fix 05, the launch screen shows a plain cyan (`#29BFFF`) square for 2 seconds. This is the placeholder `drawable/ic_launcher_foreground.xml`. The prepared splash artwork (`drawable/splashscreen.png`) is never shown.

The intended behaviour is: the `splashscreen.png` artwork fills the screen for 2 seconds, then the game starts. No icon should be shown during launch — only the artwork.

---

### Why the SplashScreen API cannot show the artwork

`windowSplashScreenAnimatedIcon` is constrained to 160 dp centred on screen. `windowSplashScreenBackground` accepts only a colour. There is no SplashScreen API attribute that supports a full-screen image. The artwork must be rendered inside Compose.

---

### Required changes

#### 1. Remove `windowSplashScreenAnimatedIcon` from the splash theme — `themes.xml`

The attribute is not needed. Removing it means the system splash shows only the background colour (`#12122A`) for the one or two frames before the first Compose frame paints — effectively invisible to the user. The app icon is not shown during launch.

```xml
<style name="Theme.BlockDrop.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/splash_background</item>
    <item name="postSplashScreenTheme">@style/Theme.BlockDrop</item>
</style>
```

#### 2. Remove `setKeepOnScreenCondition` — `MainActivity.kt`

The 2-second hold was there to extend the system splash. With artwork moved to Compose, the system splash should dismiss on the first rendered frame:

```kotlin
installSplashScreen()    // no setKeepOnScreenCondition
```

#### 3. Add Compose intro overlay — `BlockDropApp.kt`

Show `splashscreen.png` full-screen for 2 seconds, then fade out to the game:

```kotlin
@Composable
fun BlockDropApp(viewModel: GameViewModel) {
    val uiModel by viewModel.uiModel.collectAsState()
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2_000L)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BlockDropScreen(
            uiModel = uiModel,
            // ... all existing callbacks unchanged ...
        )

        AnimatedVisibility(
            visible = showSplash,
            enter = EnterTransition.None,
            exit = fadeOut(animationSpec = tween(durationMillis = 300)),
        ) {
            Image(
                painter = painterResource(R.drawable.splashscreen),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
```

`ContentScale.Crop` fills the screen while maintaining the artwork's aspect ratio. The game screen renders underneath during the 2 seconds so the ViewModel is fully initialised when the overlay fades.

Required new imports for `BlockDropApp.kt`:
```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import com.bigbangit.blockdrop.R
```

---

### What the launch sequence looks like after the fix

| Phase | Duration | What shows |
|---|---|---|
| System splash | ~1–2 frames | Dark background only (`#12122A`) — no icon |
| Compose intro | 2 000 ms | `splashscreen.png` artwork full-screen |
| Fade-out | 300 ms | Artwork fades to game screen |
| Game | — | Normal game UI |

---

### Cleanup

- `res/drawable/ic_launcher_foreground.xml` (placeholder vector, no longer referenced) — delete.
- `res/drawable/splash_background.xml` (orphaned since fix 05) — delete.

---

### What stays from fix 05 — unchanged

- `Theme.BlockDrop.Splash` extending `Theme.SplashScreen` ✓
- `postSplashScreenTheme` → `@style/Theme.BlockDrop` ✓
- `android:theme="@style/Theme.BlockDrop.Splash"` in the manifest activity ✓
- `android:windowBackground` removed from `Theme.BlockDrop` ✓

Only `windowSplashScreenAnimatedIcon` is removed from `themes.xml`; everything else is unchanged.

---

### Acceptance criteria

1. No icon, no blue square, no placeholder visible at any point during launch.
2. The `splashscreen.png` artwork fills the screen for approximately 2 seconds from first render.
3. A 300 ms fade-out transitions from artwork to the game screen.
4. The game UI is fully initialised and responsive immediately after the fade.
