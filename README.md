# Display Launcher

A headless Android launcher designed for digital signage, kiosks, and remote-controlled displays. Control which apps run on your Android device via a simple web API or browser interface.

> [!CAUTION]
> This application has **NO built-in authentication or encryption**. The web server runs on port 9091 with **unrestricted access** to anyone who can reach the device on your network.
> 
> ❌ **DO NOT** expose this app directly to the internet  
> ❌ **DO NOT** port forward 9091 to the internet  
> ❌ **DO NOT** use on untrusted networks (public WiFi, etc.)  
> ❌ **DO NOT** assume any built-in security exists  
> ⚠️ **NEW:** This app can install/uninstall APKs remotely - use only on trusted networks!

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [How It Works](#how-it-works)
- [Installation](#installation)
- [Usage](#usage)
  - [Web Interface](#web-interface)
  - [REST API](#rest-api)
  - [Examples](#examples)
- [On-Screen UI Access](#on-screen-ui-access)
- [Configuration](#configuration)
- [Caveats & Limitations](#caveats--limitations)
- [Permissions](#permissions)
- [Troubleshooting](#troubleshooting)
- [Technical Details](#technical-details)
- [Documentation](#documentation)
- [CI/CD](#cicd)

---

## Overview

Display Launcher runs as a minimal, invisible home screen that allows you to remotely switch between applications without user interaction. Perfect for:

- **Digital signage displays** - Switch content remotely
- **Kiosk systems** - Control which app is displayed
- **Smart home displays** - Change dashboards on demand
- **Presentation systems** - Switch between apps during demos
- **Projector control** - Manage content from any device on your network

---

## Features

- ✅ Web-based API for programmatic app launching
- ✅ Browser interface for manual control
- ✅ **NEW:** Upload and install APK files via web interface
- ✅ **NEW:** Uninstall apps remotely from web interface
- ✅ Intent-based app launching (deep links, YouTube videos, URLs)
- ✅ Headless operation - Shows black screen when not needed
- ✅ Persistent background service - Works even when other apps are running
- ✅ Triple-tap gesture to access settings when needed
- ✅ No accessibility services required

---

## How It Works

Display Launcher consists of five main components:

1. **MainActivity** - Minimal UI shown only when needed (triple-tap center screen)
2. **LauncherService** - Foreground service that keeps the web server running
3. **LauncherWebServer** - HTTP server on port 9091 for remote control
4. **InstallActivity** - Transparent activity for APK installation
5. **UninstallActivity** - Transparent activity for app uninstallation

When apps are launched via the API, they come to the foreground automatically. The launcher itself remains invisible in the background.

---

## Installation

### Requirements

- Android 7.0 (API 24) or higher
- Android 14+ recommended for full foreground service support

### Setup

1. Install the APK on your Android device
2. Open the Display Launcher app
3. Triple-tap the center of the screen to show settings
4. Tap **"Set Default"** and select Display Launcher as your home app
5. Grant any requested permissions
6. The web server starts automatically on port 9091

### Setting as Default Launcher (ADB Method)

```bash
adb shell cmd package set-home-activity com.tpn.displaylauncher/.MainActivity
```

---

## Usage

### Web Interface

Access the web interface from any device on the same network:

```
http://[device-ip-address]:9091
```

The web interface provides:

- List of all installed user apps
- One-click launch buttons
- **NEW:** One-click uninstall buttons
- **NEW:** APK file upload and installation
- Search functionality
- Real-time status messages

### REST API

For complete API documentation, see the [API Reference](./API.md).

#### Get list of installed apps

```
GET http://[device-ip]:9091/api/apps
```

**Response:**

```json
[
  {
    "name": "Chrome",
    "packageName": "com.android.chrome"
  }
]
```

#### Launch an app

```
POST http://[device-ip]:9091/api/launch
Content-Type: application/json

{
  "packageName": "com.android.chrome"
}
```

**Response:**

```json
{
  "success": true,
  "message": "App launched successfully"
}
```

#### Launch an app with intent (NEW)

```
POST http://[device-ip]:9091/api/launch-intent
Content-Type: application/json

{
  "packageName": "com.google.android.youtube",
  "action": "android.intent.action.VIEW",
  "data": "vnd.youtube://dQw4w9WgXcQ"
}
```

**Response:**

```json
{
  "success": true,
  "message": "App launched successfully with intent"
}
```

**Intent Examples:**

- YouTube video: `"data": "vnd.youtube://VIDEO_ID"`
- URL: `"data": "https://example.com"`
- Deep link: `"data": "app://path/to/content"`

#### Uninstall an app (NEW)

```
POST http://[device-ip]:9091/api/uninstall
Content-Type: application/json

{
  "packageName": "com.example.app"
}
```

**Response:**

```json
{
  "success": true,
  "message": "Uninstall dialog opened"
}
```

**Note:** The uninstall confirmation dialog appears on the device screen for security.

#### Upload and install APK (NEW)

```
POST http://[device-ip]:9091/api/upload-apk
Content-Type: multipart/form-data

(Form data with 'file' field containing APK)
```

**Response:**

```json
{
  "success": true,
  "message": "Install dialog opened for uploaded APK"
}
```

**Note:** The installation dialog appears on the device screen. Uploaded APK files are automatically cleaned up after 5 seconds.

### Examples

#### cURL

```bash
# Launch Chrome
curl -X POST http://192.168.1.100:9091/api/launch \\
  -H "Content-Type: application/json" \\
  -d '{"packageName":"com.android.chrome"}'

# Launch YouTube video
curl -X POST http://192.168.1.100:9091/api/launch-intent \\
  -H "Content-Type: application/json" \\
  -d '{"packageName":"com.google.android.youtube","action":"android.intent.action.VIEW","data":"vnd.youtube://dQw4w9WgXcQ"}'

# Uninstall an app
curl -X POST http://192.168.1.100:9091/api/uninstall \\
  -H "Content-Type: application/json" \\
  -d '{"packageName":"com.example.app"}'

# Upload APK
curl -X POST http://192.168.1.100:9091/api/upload-apk \\
  -F "file=@/path/to/app.apk"
```

#### Python

```python
import requests

# Launch an app
response = requests.post(
    'http://192.168.1.100:9091/api/launch',
    json={'packageName': 'com.android.chrome'}
)
print(response.json())

# Launch with intent
response = requests.post(
    'http://192.168.1.100:9091/api/launch-intent',
    json={
        'packageName': 'com.google.android.youtube',
        'action': 'android.intent.action.VIEW',
        'data': 'vnd.youtube://dQw4w9WgXcQ'
    }
)
print(response.json())

# Upload APK
with open('app.apk', 'rb') as f:
    files = {'file': f}
    response = requests.post('http://192.168.1.100:9091/api/upload-apk', files=files)
    print(response.json())
```

#### JavaScript

```javascript
// Launch an app
fetch('http://192.168.1.100:9091/api/launch', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ packageName: 'com.android.chrome' })
})
  .then(r => r.json())
  .then(data => console.log(data));

// Launch with intent
fetch('http://192.168.1.100:9091/api/launch-intent', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    packageName: 'com.google.android.youtube',
    action: 'android.intent.action.VIEW',
    data: 'vnd.youtube://dQw4w9WgXcQ'
  })
})
  .then(r => r.json())
  .then(data => console.log(data));

// Upload APK
const formData = new FormData();
formData.append('file', fileInput.files[0]);
fetch('http://192.168.1.100:9091/api/upload-apk', {
  method: 'POST',
  body: formData
})
  .then(r => r.json())
  .then(data => console.log(data));
```

---

## On-Screen UI Access

The launcher UI is hidden by default. To access it:

1. Press **HOME** button (shows black screen)
2. Tap center of screen **3 times quickly** (within 1 second)
3. Settings UI appears
4. Tap **"Hide UI"** to hide it again

> 💡 The subtle hint "Tap center 3x to show settings" appears on the black screen.

---

## Configuration

### Change Web Server Port

Edit `LauncherService.kt`:

```kotlin
webServer = LauncherWebServer(9091, appLauncher) // Change 9091 to desired port
```

### Show System Apps

Edit `AppLauncher.kt`, remove this line:

```kotlin
.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
```

### Adjust Triple-Tap Sensitivity

Edit `MainActivity.kt`:

```kotlin
if (currentTime - lastTapTime < 500) { // Increase/decrease time in ms
    tapCount++
    if (tapCount >= 3) { // Change number of required taps
        showUI = !showUI
    }
}
```

---

## Caveats & Limitations

### ⚠️ Must Be Set as Default Launcher

The app must be set as the default home launcher for app switching to work properly. Without this, launched apps will open in the background.

### 🌐 Requires Network Access

Both the device and control client must be on the same network. The web server only binds to the device's local network interface.

### 🔓 No HTTPS Support

The web server uses plain HTTP. Do not use over untrusted networks. Suitable for local network use only.

### 📱 User Apps Only (Default)

By default, only user-installed apps are shown. System apps (Settings, Calculator, etc.) are filtered out but can be included by modifying the code.

### 🔔 Foreground Service Notification

A persistent notification appears when the service is running. This is required by Android and cannot be hidden without root access.

### 🔋 Battery Optimization

On some devices, you may need to disable battery optimization for Display Launcher to ensure the service isn't killed:

```
Settings → Apps → Display Launcher → Battery → Unrestricted
```

### 🔐 No Authentication

The web API has no authentication. Anyone on the network can control the device. Suitable for trusted networks only.

### 📦 Install/Uninstall Requires User Confirmation (NEW)

Android security requires user confirmation for app installation and uninstallation. The dialogs appear on the device screen and must be approved manually.

---

## Permissions

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Required for web server |
| `FOREGROUND_SERVICE` | Keeps service running |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14+ requirement |
| `POST_NOTIFICATIONS` | Shows foreground service notification |
| `REQUEST_INSTALL_PACKAGES` | **NEW:** Allows APK installation |
| `REQUEST_DELETE_PACKAGES` | **NEW:** Allows app uninstallation |
| `HOME` category intent filter | Allows app to be a launcher |

---

## Troubleshooting

### Apps don't launch when clicked

- Verify Display Launcher is set as default home launcher
- Check logcat: `adb logcat | grep AppLauncher`
- Ensure device isn't in battery saver mode

### Can't access web interface

- Verify device is on the same network
- Check device IP address in settings
- Try accessing `http://[ip]:9091` directly
- Check firewall settings on controlling device

### Service stops randomly

- Disable battery optimization for Display Launcher
- Check for aggressive task killers
- Verify foreground service notification is visible

### UI doesn't appear with triple-tap

- Tap faster (within 500ms between taps)
- Ensure tapping center of screen
- Try tapping exact center multiple times

### Install/Uninstall doesn't work (NEW)

- Check that the install/uninstall dialog appears on the device screen
- Verify permissions are granted in Android Settings
- Check logcat for errors: `adb logcat | grep DisplayLauncher`
- Ensure APK file is valid (for uploads)

---

## Technical Details

### Architecture

- Kotlin with Jetpack Compose UI
- NanoHTTPD embedded web server
- Gson for JSON serialization
- Foreground Service for persistence
- FileProvider for secure APK file handling

### Build Configuration

- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 35 (Android 15)
- **Compile SDK:** 35
- **Kotlin:** 2.0.0
- **AGP:** 8.13.0

### Package Structure

```
com.tpn.displaylauncher/
├── MainActivity.kt              # Main launcher activity with UI
├── LauncherService.kt           # Foreground service
├── LauncherWebServer.kt         # HTTP server implementation
├── AppLauncher.kt               # App management logic
├── InstallActivity.kt           # APK installation handler (NEW)
└── UninstallActivity.kt         # App uninstallation handler (NEW)
```

---

## Documentation

- **[API Reference](./API.md)** - Complete REST API documentation with all endpoints, parameters, and examples
- **[Home Assistant Integration Guide](./HomeAssistant.md)** - Comprehensive guide for Home Assistant automation, scripts, and dashboard setup

---

## CI/CD

GitHub Actions workflow included for automated APK builds on releases. See `.github/workflows/build-release.yml`.

