# Display Launcher

A headless Android launcher designed for digital signage, kiosks, and remote displays[cite: 8]. Controls application execution via a local HTTP API or browser-based control panel[cite: 8].

> [!CAUTION]
> This application has **NO built-in authentication or encryption**. The web server runs on port 9091 with **unrestricted access** to network clients[cite: 8].
>
> ❌ **DO NOT** expose this app directly to the internet[cite: 8]  
> ❌ **DO NOT** port forward port 9091[cite: 8]  
> ❌ **DO NOT** deploy on untrusted networks[cite: 8]  
> ❌ **DO NOT** assume any built-in security controls exist[cite: 8]

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture & Components](#architecture--components)
- [Installation](#installation)
- [Usage](#usage)
  - [Web Interface](#web-interface)
  - [REST API](#rest-api)
- [UI Access](#ui-access)
- [Configuration](#configuration)
- [Limitations](#limitations)
- [Permissions](#permissions)
- [Troubleshooting](#troubleshooting)
- [Technical Specifications](#technical-specifications)

---

## Features

- REST API endpoints for programmatic application launching and intent execution[cite: 8].
- Embedded web interface for device management[cite: 8].
- Remote APK upload, installation, and package removal[cite: 8].
- Intent parameter passing (actions, data URIs, and extra key-value pairs)[cite: 8].
- Automatic service initialization on system boot via `BootReceiver`.
- Persistent foreground service daemon[cite: 8].
- D-pad and remote control focus handling for Android TV/set-top boxes.

---

## Architecture & Components

The application consists of the following internal components:

1. **`MainActivity`**: Root launcher activity hosting the configuration interface (accessible via triple-tap gesture)[cite: 8].
2. **`LauncherService`**: Foreground service maintaining the HTTP server lifecycle independently of UI state[cite: 8].
3. **`BootReceiver`**: Broadcast receiver that triggers `LauncherService` upon system startup (`ACTION_BOOT_COMPLETED`).
4. **`LauncherWebServer`**: Embedded HTTP server running on port 9091[cite: 8].
5. **`InstallActivity` / `UninstallActivity`**: Transparent activity wrappers required for package management intents[cite: 8].

---

## Installation

### Requirements

- Android 7.0 (API 24) or higher[cite: 8].
- Android 14+ recommended for full foreground service type compliance[cite: 8].

### Setup Procedure

1. Install the APK package on the target device[cite: 8].
2. Launch the application locally[cite: 8].
3. Tap the center of the screen three times rapidly to invoke the settings UI[cite: 8].
4. Select **"Set as Default Launcher"** to register the app as the system home handler[cite: 8].
5. Grant required permissions[cite: 8].

### ADB Setup Method

```bash
adb shell cmd package set-home-activity com.tpn.displaylauncher/.MainActivity
```

---

## Usage

### Web Interface

Access the control panel from any HTTP client on the local network[cite: 8]:

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
| `packageName` | String | Target package identifier (required)[cite: 8]. |
| `action` | String | Intent action string (e.g., `android.intent.action.VIEW`)[cite: 8]. |
| `data` | String | Intent data URI string[cite: 8]. |
| `extra_string` | String | Comma-separated key-value pairs for intent extras (`key:value`)[cite: 8]. |

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

1. Return to the home screen (renders a black background)[cite: 8].
2. Tap the center of the display three times within a 1-second window[cite: 8].
3. Select **"Hide UI"** to return to headless mode[cite: 8].

---

## Configuration

- **Port Modification**: Edit the port parameter in `LauncherService.kt` (`LauncherWebServer(9091, ...)`[cite: 8]).
- **App Query Scope**: `AppLauncher.kt` queries launchable applications via `PackageManager.queryIntentActivities()` using `CATEGORY_LAUNCHER`, including pre-installed system apps.

---

## Limitations

- **Default Home Requirement**: Must be registered as the default home launcher to ensure proper foreground task switching[cite: 8].
- **Local Network Scope**: Restricted to local network interfaces; lacks TLS/HTTPS support[cite: 8].
- **User Prompts**: Package installation and removal actions require manual confirmation via system dialogs on the physical display[cite: 8].

---

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | Binds local HTTP server[cite: 8]. |
| `FOREGROUND_SERVICE` / `SPECIAL_USE` | Maintains background daemon execution[cite: 8]. |
| `RECEIVE_BOOT_COMPLETED` | Triggers service initialization on system boot. |
| `POST_NOTIFICATIONS` | Displays required foreground service notification[cite: 8]. |
| `REQUEST_INSTALL_PACKAGES` / `DELETE_PACKAGES` | Handles application lifecycle intents[cite: 8]. |

---

## Troubleshooting

- **Service Persistence**: Ensure battery optimization is disabled for the application if the OS terminates the background service[cite: 8].
- **Network Validation**: Verify interface connectivity and local port availability (`9091`)[cite: 8].
- **Logcat Verification**: Inspect logs using `adb logcat | grep DisplayLauncher`[cite: 8].

---

## Technical Specifications

- **Language**: Kotlin[cite: 8]
- **UI Framework**: Jetpack Compose[cite: 8]
- **HTTP Server**: NanoHTTPD[cite: 8]
- **Serialization**: Gson[cite: 8]
- **Min SDK**: 24[cite: 8]
- **Target SDK**: 35[cite: 8]

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