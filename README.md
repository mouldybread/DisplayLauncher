# Display Launcher

A headless Android launcher designed for digital signage, kiosks, and remote displays. Controls application execution via a local HTTP API or browser-based control panel.

> [!CAUTION]
> This application has **NO built-in authentication or encryption**. The web server runs on port 9091 with **unrestricted access** to network clients.
>
> ❌ **DO NOT** expose this app directly to the internet  
> ❌ **DO NOT** port forward port 9091  
> ❌ **DO NOT** deploy on untrusted networks  
> ❌ **DO NOT** assume any built-in security controls exist

---

## Installation

### Requirements

- Android 7.0 (API 24) or higher.
- Android 14+ recommended for full foreground service type compliance.

### Setup Procedure

1. Install the APK package on the target device.
2. Launch the application locally.
3. Press **D-Pad DOWN** 3 times rapidly (or tap the center of the screen 3 times within 1 second) to invoke the settings UI.
4. Select **"Set as Default Launcher"** to register the app as the system home handler.
5. Grant required permissions.

### ADB Setup Method

```bash
adb shell cmd package set-home-activity com.tpn.displaylauncher/.MainActivity
```

---

## Table of Contents

- [Installation](#installation)
- [Features](#features)
- [Architecture & Components](#architecture--components)
- [Usage](#usage)
  - [Web Interface](#web-interface)
  - [REST API Reference](#rest-api-reference)
- [UI Access](#ui-access)
- [Configuration](#configuration)
- [Limitations](#limitations)
- [Permissions](#permissions)
- [Troubleshooting](#troubleshooting)
- [Technical Specifications](#technical-specifications)
- [Examples](#examples)
- [Documentation](#documentation)

---

## Features

- REST API endpoints for programmatic application launching and intent execution.
- Embedded web interface for device management.
- Remote APK upload, installation, and package removal.
- Intent parameter passing (actions, data URIs, and extra key-value pairs).
- Self-healing automatic service initialization on system boot via `BootReceiver`.
- Persistent foreground service daemon with automated web server watchdog monitoring.
- D-pad and remote control focus handling for Android TV/set-top boxes.
- UI reveal via 3x D-Pad DOWN or 3x center-screen tap.

---

## Architecture & Components

The application consists of the following internal components:

1. **`MainActivity`**: Root launcher activity hosting the configuration interface (accessible via 3x D-Pad DOWN or 3x center tap).
2. **`LauncherService`**: Foreground service maintaining the HTTP server lifecycle independently of UI state. Performs self-healing checks for `BootReceiver` on startup.
3. **`BootReceiver`**: Broadcast receiver that triggers `LauncherService` upon system startup (`ACTION_BOOT_COMPLETED`).
4. **`LauncherWebServer`**: Embedded HTTP server running on port 9091.
5. **`InstallActivity` / `UninstallActivity`**: Transparent activity wrappers required for package management intents.

---

## Usage

### Web Interface

Access the control panel from any HTTP client on the local network:

```text
http://[device-ip-address]:9091
```

### REST API Reference

#### Get Installed Applications

```http
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

#### Launch Application

```http
POST http://[device-ip]:9091/api/launch
Content-Type: application/json

{
  "packageName": "com.android.chrome"
}
```

#### Launch Application with Intent and Extras

```http
POST http://[device-ip]:9091/api/launch-intent
Content-Type: application/json

{
  "packageName": "com.tpn.streamviewer",
  "action": "android.intent.action.MAIN",
  "extra_string": "camera_name:FRONTDOOR"
}
```

**Intent Payload Parameters:**

| Field | Type | Description |
|---|---|---|
| `packageName` | String | Target package identifier (required). |
| `action` | String | Intent action string (e.g., `android.intent.action.VIEW`). |
| `data` | String | Intent data URI string. |
| `extra_string` | String | Comma-separated key-value pairs for intent extras (`key:value`). |

#### Uninstall Application

```http
POST http://[device-ip]:9091/api/uninstall
Content-Type: application/json

{
  "packageName": "com.example.app"
}
```

#### Upload and Install APK

```http
POST http://[device-ip]:9091/api/upload-apk
Content-Type: multipart/form-data
```

---

## UI Access

To open the settings interface on a headless display:

1. Return to the home screen (renders a black background).
2. Press **D-Pad DOWN** 3 times quickly (or tap the center of the display 3 times within 1 second).
3. Select **"Hide UI"** to return to headless mode.

---

## Configuration

- **Port Modification**: Edit the port parameter in `LauncherService.kt` (`LauncherWebServer(9091, ...)`).
- **App Query Scope**: `AppLauncher.kt` queries launchable applications via `PackageManager.queryIntentActivities()` using `CATEGORY_LAUNCHER`, including pre-installed system apps.
- **Gesture / Key Modification**: Key event detection (`KEYCODE_DPAD_DOWN`) and tap thresholds can be adjusted in `MainActivity.kt`.

---

## Limitations

- **Default Home Requirement**: Must be registered as the default home launcher to ensure proper foreground task switching.
- **Local Network Scope**: Restricted to local network interfaces; lacks TLS/HTTPS support.
- **User Prompts**: Package installation and removal actions require manual confirmation via system dialogs on the physical display.

---

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | Binds local HTTP server. |
| `FOREGROUND_SERVICE` / `SPECIAL_USE` | Maintains background daemon execution. |
| `RECEIVE_BOOT_COMPLETED` | Triggers service initialization on system boot. |
| `POST_NOTIFICATIONS` | Displays required foreground service notification. |
| `REQUEST_INSTALL_PACKAGES` / `DELETE_PACKAGES` | Handles application lifecycle intents. |

---

## Troubleshooting

- **Service Persistence**: Ensure battery optimization is disabled for the application if the OS terminates the background service.
- **Network Validation**: Verify interface connectivity and local port availability (`9091`).
- **Logcat Verification**: Inspect logs using `adb logcat | grep DisplayLauncher`.

---

## Technical Specifications

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **HTTP Server**: NanoHTTPD
- **Serialization**: Gson
- **Min SDK**: 24
- **Target SDK**: 35

---

## Examples

### cURL

```bash
# Launch Chrome
curl -X POST [http://192.168.1.100:9091/api/launch](http://192.168.1.100:9091/api/launch) -H "Content-Type: application/json" -d '{"packageName":"com.android.chrome"}'

# Launch Intent with Extras
curl -X POST [http://192.168.1.100:9091/api/launch-intent](http://192.168.1.100:9091/api/launch-intent) -H "Content-Type: application/json" -d '{"packageName":"com.tpn.streamviewer","action":"android.intent.action.MAIN","extra_string":"camera_name:FRONTDOOR"}'
```

### Python

```python
import requests

requests.post('[http://192.168.1.100:9091/api/launch](http://192.168.1.100:9091/api/launch)', json={'packageName': 'com.android.chrome'})
```

---

## Documentation

- **[API Reference](./API.md)** - Detailed REST API endpoint specification.
- **[Home Assistant Integration Guide](./HomeAssistant.md)** - Integration examples for automation platforms.
