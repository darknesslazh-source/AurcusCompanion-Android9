# Aurcus Companion — Android 9 target

Standalone Kotlin/Jetpack Compose companion starter. `minSdk = 28` means Android 9 or later.
`compileSdk = 35` is used only to compile the app; it does not require Android 15 to run.

## Included
- Build planner with generic demonstration indicators
- Searchable fictional sample items
- Manual quest/farming checklist saved locally in SharedPreferences JSON
- Basic equipment comparison
- Dark Material 3 UI
- Local performance snapshot for the companion process/device memory
- GitHub Actions workflow to build a debug APK

## Build locally
Requirements: Android Studio, JDK 17, Android SDK Platform 35.
Open the project folder in Android Studio, sync Gradle, then choose:
Build > Build Bundle(s) / APK(s) > Build APK(s).

Expected output:
`app/build/outputs/apk/debug/app-debug.apk`

## Build with GitHub Actions
1. Create a GitHub repository and upload the contents of this folder.
2. Open Actions and enable workflows if prompted.
3. Run "Build Android APK" (or push to main).
4. Download artifact `aurcus-companion-android9-debug-apk`.

## Compatibility and boundaries
- This is a starter template, not a verified integration with Aurcus Online.
- All item stats are fictional sample data, not official data.
- Build indicators are generic and not official game formulas.
- Performance snapshot does not inspect game process or game FPS.
- No game memory reading, packet manipulation, anti-cheat bypass, game automation, or server calls.
- Public data importer is not implemented yet. Only import public data from sources that permit reuse.
- Android 9 compatibility still needs runtime testing on an Android 9 emulator.
