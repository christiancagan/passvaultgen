# PassVaultGen — Download

This folder is served by **GitHub Pages** as the app's public download page.

- Open the site at: `https://<your-username>.github.io/<your-repo>/`
- The download button serves `passvaultgen-release.apk` (v1.5.3, 2.9 MB, signed).

## Files
- `index.html` — the landing page with the download button/icon
- `passvaultgen-release.apk` — the Android app (signed release build)
- `logo.png` — the app icon for the landing page
- `.nojekyll` — disables Jekyll so the `.apk` is served as-is

## To update the APK
Replace `passvaultgen-release.apk` with a new build, update `index.html`
(version, size, SHA-256), commit, and push. GitHub Pages re-deploys automatically.

## Building a Signed Release APK
1. Ensure `keystore.properties` exists in the project root with your upload key.
2. Build: `.\gradlew.bat :app:assembleRelease --offline --console=plain --no-daemon`
3. The signed APK is at `app\build\outputs\apk\release\passvaultgen-release.apk`
4. Copy it to this folder as `passvaultgen-release.apk` and commit.

## Release Process
1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
2. Build and run tests: `.\gradlew.bat testDebugUnitTest assembleDebug`
3. Copy the release APK over `docs/passvaultgen-release.apk`
4. Update `docs/index.html` with the new version, SHA-256, and size
5. Commit and push to `origin main` — GitHub Pages re-deploys automatically
