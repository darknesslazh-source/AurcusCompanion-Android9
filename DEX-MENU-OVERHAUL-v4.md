# Aurcus Companion — DEX Menu Overhaul v4

## Changes
- Floating navigation reordered around the DEX-backed category index, DEX string search, build planner, quest checklist, and companion tools.
- Added a searchable DEX keyword index inside the floating panel. Selecting a keyword opens its mapped category details.
- Feature details now distinguish literal strings found in the uploaded DEX from a callable game API.
- Added `app/src/main/assets/dex_verified_strings.txt`, a compact list of selected string literals found in the uploaded `classes.dex`.
- Existing branding assets and Android 9 minimum SDK are preserved.

## Integration boundary
The uploaded file is a DEX, not a documented SDK. Strings such as `CURRENCY_NORMAL`, `PVP_ROOM_TITLE`, `NORMAL_ATTACK_HIT_EFFECT`, and `SpeedHack` are evidence of string-pool entries only. This version does not invoke game-private methods, inject into the process, read or write game memory, automate gameplay, alter packets, or change server state.

To make a true runtime connection, a supported public API/SDK, documented intent/deep link, or other authorized external interface must be identified first. The current app can run its own planner, searchable index, notes, and checklists independently.

## Verification
- ZIP archive integrity checked after packaging.
- Kotlin changes are source-level; Gradle compilation and Android 9 runtime tests have not been run in this environment. Do not treat this ZIP as a verified APK.
