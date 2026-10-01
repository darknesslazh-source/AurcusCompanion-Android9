# Aurcus Companion Android 9+

A local Android companion with a draggable floating overlay and manual tools for the public Aurcus Online game. This project does not modify the game APK and does not read game memory, intercept packets, inject code, automate game input, spoof movement/damage, spawn mobs, teleport, or bypass server/security controls.

## Included
- Draggable floating overlay (requires Android's “Display over other apps” permission).
- Local damage estimate calculator (user-entered values; not the official server formula).
- Manual map/route notes.
- Manual mob/spawn observation log (no server-side spawning).
- Farming session timer and checklist (no automated gameplay).
- Speed-stat comparison (does not change character movement or game state).
- Existing local build planner, item examples, comparison, and performance monitor source retained where applicable.

## Build APK
Use GitHub Actions workflow `.github/workflows/build-apk.yml`; it builds the Android project under `AurcusCompanion-Android9/` and uploads a debug APK artifact. The APK must be built by CI/Android SDK; this source archive itself is not an APK.

## Important
The uploaded game ZIP is a packaged Android application (DEX, native `.so` libraries, resources and assets), not the original Kotlin source project. This companion is kept separate from the game package; the included demo map/mob/item entries are placeholders and should be replaced only with manually verified, permitted reference data.
