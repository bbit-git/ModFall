# Translation Audit & Plan

## Summary

`values/strings.xml` defines **125** string keys. All 20 localized `values-*/strings.xml`
files contain **108** keys — the **same 17 keys are missing in every locale**, all added
recently and never translated.

Additionally, `values/strings.xml` itself contains one entry whose default value is in
Czech rather than English.

## Affected locales (20)

ar, cs, de, es, fr, hi, in, it, ja, ko, pl, pt-rBR, ru, sk, th, tr, uk, vi, zh-rCN, zh-rTW

Each is missing the full set of 17 keys listed below.

## Missing keys

| Key | English source (`values/strings.xml`) | Notes |
|---|---|---|
| `enable_particles_setting` | `Particles` | Settings toggle label |
| `level_and_lines_stat_compact` | `Úroveň %1$d · Řádky %2$d` | **BUG: default value is Czech, not English** |
| `music_library_disabled_placeholder` | `Music playback is disabled` | |
| `music_volume_setting` | `Music level` | |
| `particle_quality_high` | `Higher` | |
| `particle_quality_low` | `Lower` | |
| `particle_quality_setting` | `Particle density` | |
| `sfx_volume_setting` | `SFX level` | |
| `sound_off_description` | `Sound off` | Content description |
| `sound_on_description` | `Sound on` | Content description |
| `tutorial_music_download_desc` | `Example sources: The Mod Archive (modarchive.org) and scene.org (files.scene.org). Download module files there, then place them in Download/Mods/ or your selected music folder.` | |
| `tutorial_music_download_label` | `Where to Get Mods` | |
| `tutorial_music_set_folder_desc` | `Open Settings, enter Music Library, tap Set music folder, then pick the folder that contains your .mod / .xm / .s3m / .it files.` | |
| `tutorial_music_set_folder_label` | `Set Music Folder` | |
| `tutorial_music_set_main_desc` | `After scanning, pick a track in Music Library and tap Set as main tune if you want it to start automatically with a new game.` | |
| `tutorial_music_set_main_label` | `Choose Main Tune` | |
| `tutorial_section_music` | `MUSIC` | Tutorial section heading |

## Plan

### Step 1 — Fix the English source

`app/src/main/res/values/strings.xml:15` — replace the Czech default for
`level_and_lines_stat_compact` with an English string, e.g.:

```xml
<string name="level_and_lines_stat_compact">Level %1$d · Lines %2$d</string>
```

This must happen first; downstream translations should be derived from a correct source.

### Step 2 — Add the 17 keys to each locale

For each of the 20 `values-*/strings.xml` files, append translations for the 17 keys
above. Translation guidelines:

- Preserve the printf format specifiers (`%1$d`, `%2$d`, `%1$s`) exactly.
- Match the casing convention already used by neighboring keys in each file
  (e.g., `tutorial_section_music` should be uppercase to match `tutorial_section_movement`,
  `tutorial_section_dropping`, etc.).
- Keep `particle_quality_high` / `particle_quality_low` short (single word where possible)
  — they appear as segmented-button labels.
- For the long tutorial descriptions (`tutorial_music_*_desc`), keep the meaning
  precise; "Download/Mods/" is a literal path and should not be localized.
- RTL locale (ar): no special escaping needed beyond standard XML; let the layout
  engine handle direction.

### Step 3 — Verify

After edits, re-run the audit to confirm parity:

```bash
grep -oP 'name="\K[^"]+' app/src/main/res/values/strings.xml | sort > /tmp/keys_default.txt
for f in app/src/main/res/values-*/strings.xml; do
  loc=$(basename $(dirname $f))
  grep -oP 'name="\K[^"]+' "$f" | sort > /tmp/keys_loc.txt
  missing=$(comm -23 /tmp/keys_default.txt /tmp/keys_loc.txt | wc -l)
  echo "$loc: $missing missing"
done
```

Expected output: `0 missing` for every locale.

Build the app (`./gradlew assembleDebug`) to confirm no resource errors and run the
existing instrumentation tests to catch any layout regressions caused by longer
translated strings (notably German and the tutorial descriptions).
