@echo off
setlocal
cd /d "%~dp0"
echo Building Aurcus Companion debug APK...
where gradle >nul 2>nul
if errorlevel 1 (
  echo ERROR: Gradle was not found on PATH.
  echo Open this project in Android Studio, wait for Gradle Sync, then choose Build ^> Build APK(s).
  echo Or configure Gradle 8.9 and Android SDK platform 35, then run this script again.
  exit /b 1
)
gradle --no-daemon clean :app:assembleDebug
if errorlevel 1 exit /b %errorlevel%
echo.
echo APK: app\build\outputs\apk\debug\app-debug.apk
endlocal
