# Ayn Thor Auto Dim

Android application to automatically dim (show a black screen) the secondary screen of the Ayn Odin 2 DS (or similar devices) after a period of inactivity.

## Features

- **Secondary Screen Detection**: Automatically targets the external/bottom screen.
- **Configurable Timer**: Set your preferred inactivity delay (default: 3 seconds).
- **True Black Mode**: Displays a 100% opaque black overlay to simulate sleep mode and save battery.
- **Smooth Animation**: 500ms fade-in animation for a pleasant experience.
- **Touch to Wake**: Tap the black screen once to wake it up instantly.
- **Live Updates**: Changes to settings are applied immediately without restarting the service.
- **Optimized UI**: Two-column layout designed for landscape orientation.

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

Once configured, the bottom screen will dim after the specified duration of inactivity (3 seconds by default). Tap the screen to wake it up.
