# Ayn Thor Auto Dim

Android application to dim (show a black screen) the secondary screen after 10 seconds of inactivity.

## Features
- Secondary screen detection (external/bottom screen).
- 10-second timer without touch interaction.
- Black overlay display (touch-transparent) to simulate sleep mode.
- Smooth 300ms fade-in animation.
- Automatic wake-up on touch (touches are passed through to the underlying application).

## Build

The project uses Gradle. Ensure you have the Android SDK installed.

### Via Terminal
Use the included Gradle wrapper to ensure version compatibility:

```bash
chmod +x gradlew
./gradlew assembleDebug
```

The APK will be generated in `app/build/outputs/apk/debug/app-debug.apk`.

### Via Android Studio
1. Open the project folder in Android Studio.
2. Let the project sync.
3. Run `Run` or `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

## Installation and Setup
1. Install the APK on the device.
2. Launch the "Ayn Thor Auto Dim" application.
3. **Required Permissions**:
   - **Overlay**: Click the button to grant permission to draw over other apps.
   - **Accessibility Service**: Click "Enable Service" to activate the "Auto Dim Service" accessibility service. This service is required to detect touch activity and manage the overlay on the external screen.

Once configured, the bottom screen should dim after 10 seconds of inactivity on it.
