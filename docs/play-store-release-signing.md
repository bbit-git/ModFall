# Play Store Release Signing

Google Play should receive release builds as Android App Bundles (`.aab`), not unsigned APKs.

## Play App Signing

Use Google Play App Signing for production releases.

The normal release flow is:

1. Build `bundleRelease`.
2. Sign the `.aab` with the app upload key.
3. Upload the signed `.aab` to Play Console.
4. Google Play verifies the upload key.
5. Google Play generates device APKs and signs them with the app signing key managed by Google.

This means CI does not need the final app signing key when Play App Signing is enabled. CI only needs the upload key.

## Local Bundle Build

Build the release bundle with:

```bash
./gradlew bundleRelease
```

Expected output:

```text
app/build/outputs/bundle/release/app-release.aab
```

## CI Secrets

Store the upload keystore in GitHub Actions secrets, usually base64 encoded.

Suggested secrets:

```text
ANDROID_UPLOAD_KEYSTORE_BASE64
ANDROID_UPLOAD_KEYSTORE_PASSWORD
ANDROID_UPLOAD_KEY_ALIAS
ANDROID_UPLOAD_KEY_PASSWORD
```

The workflow should decode the keystore into a temporary file during the build and pass the path/passwords to Gradle through environment variables.

## Gradle Signing Shape

The Android release signing config should read from environment variables, for example:

```kotlin
signingConfigs {
    create("release") {
        val keystorePath = System.getenv("ANDROID_UPLOAD_KEYSTORE_PATH")
        if (!keystorePath.isNullOrBlank()) {
            storeFile = file(keystorePath)
            storePassword = System.getenv("ANDROID_UPLOAD_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_UPLOAD_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_UPLOAD_KEY_PASSWORD")
        }
    }
}
```

Then attach it to the release build type only when the keystore env var is present.

## Tag Release

This project already derives `versionName` from an exact Git tag.

Recommended tag flow:

```bash
git tag v1.0.0
git push origin v1.0.0
```

On tag builds, CI should:

1. Run tests.
2. Build `assembleRelease` for an APK artifact if useful.
3. Build `bundleRelease` for Play Store upload.
4. Attach the `.aab` to the GitHub Release.

## Notes

- Unsigned APKs are acceptable as CI artifacts for inspection or sideload testing only after debug signing or manual signing.
- Play Console production upload should use a signed `.aab`.
- Keep the upload key recoverable. If it is lost, Play Console can reset the upload key, but not every distribution channel can.
