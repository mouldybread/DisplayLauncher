# Display Launcher REST API Reference

Complete REST API documentation for Display Launcher.

## Base URL

```text
http://[device-ip]:9091
```

Default port: `9091`

---

## Endpoints

### GET /api/apps

Get a list of all installed launchable applications.

**Request:**

```http
GET /api/apps HTTP/1.1
Host: [device-ip]:9091
```

**Response:**

```json
[
  {
    "name": "Chrome",
    "packageName": "com.android.chrome"
  },
  {
    "name": "YouTube",
    "packageName": "com.google.android.youtube"
  }
]
```

**Status Codes:**
- `200 OK` - Success

---

### POST /api/launch

Launch an application by package name.

**Request:**

```http
POST /api/launch HTTP/1.1
Host: [device-ip]:9091
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

**Status Codes:**
- `200 OK` - Success or failure (check `success` field)

**Error Response:**

```json
{
  "success": false,
  "message": "Failed to launch app"
}
```

---

### POST /api/launch-intent

Launch an application with a custom Android intent (deep links, URLs, etc.) and optional intent extras.

**Request:**

```http
POST /api/launch-intent HTTP/1.1
Host: [device-ip]:9091
Content-Type: application/json

{
  "packageName": "com.google.android.youtube",
  "action": "android.intent.action.VIEW",
  "data": "vnd.youtube://dQw4w9WgXcQ"
}
```

**With Intent Extras (Method 1 - Comma-Separated String):**

```json
{
  "packageName": "com.tpn.streamviewer",
  "action": "android.intent.action.MAIN",
  "data": "",
  "extra_string": "camera_name:FRONTDOOR,protocol:mse"
}
```

**With Intent Extras (Method 2 - Individual Parameters):**

```json
{
  "packageName": "com.tpn.streamviewer",
  "action": "android.intent.action.MAIN",
  "data": "",
  "extra_camera_name": "FRONTDOOR",
  "extra_protocol": "mse"
}
```

**Parameters:**

| Field | Type | Required | Description |
|---|---|---|---|
| `packageName` | string | Yes | Target app package name |
| `action` | string | No | Android intent action (default: `android.intent.action.MAIN`) |
| `data` | string | No | Intent data URI (e.g., YouTube video ID, URL, deep link) |
| `extra_string` | string | No | Comma-separated key:value pairs for intent extras |
| `extra_*` | string | No | Individual intent extras (e.g., `extra_camera_name`) |

**Intent Extras Format:**

There are two ways to pass intent extras:

1. **Comma-Separated String** (recommended for multiple extras):
```json
"extra_string": "key1:value1,key2:value2,key3:value3"
```

2. **Individual Parameters** (recommended for single extras):
```json
"extra_key1": "value1",
"extra_key2": "value2"
```

**Response:**

```json
{
  "success": true,
  "message": "App launched successfully with intent"
}
```

**Common Intent Actions:**

| Action | Description |
|---|---|
| `android.intent.action.VIEW` | View content (URLs, videos, etc.) |
| `android.intent.action.MAIN` | Launch main activity |
| `android.intent.action.SEARCH` | Open search |

**Intent Data Examples:**

| Type | Example |
|---|---|
| YouTube video | `vnd.youtube://VIDEO_ID` |
| URL | `https://example.com` |
| Deep link | `app://path/to/content` |
| Camera stream | Empty string with extras: `"data": "", "extra_string": "camera_name:FRONT"` |

**Intent Extras Use Cases:**

| Use Case | Example |
|---|---|
| Launch camera app with specific camera | `{"packageName":"com.tpn.streamviewer","action":"android.intent.action.MAIN","extra_string":"camera_name:FRONTDOOR"}` |
| Launch app with multiple parameters | `{"packageName":"com.example.app","extra_string":"mode:fullscreen,theme:dark"}` |
| Launch app with single parameter | `{"packageName":"com.example.app","extra_mode":"fullscreen"}` |

**Status Codes:**
- `200 OK` - Success or failure (check `success` field)

---

### POST /api/uninstall

Trigger the uninstall dialog for an application.

**Request:**

```http
POST /api/uninstall HTTP/1.1
Host: [device-ip]:9091
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

**Important Notes:**
- The uninstall dialog appears **on the device screen**
- User must manually confirm uninstallation
- This is an Android security requirement

**Status Codes:**
- `200 OK` - Success or failure (check `success` field)

**Error Response:**

```json
{
  "success": false,
  "message": "Package name is required"
}
```

---

### POST /api/upload-apk

Upload and install an APK file.

**Request:**

```http
POST /api/upload-apk HTTP/1.1
Host: [device-ip]:9091
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="app.apk"
Content-Type: application/vnd.android.package-archive

[BINARY DATA]
------WebKitFormBoundary--
```

**cURL Example:**

```bash
curl -X POST [http://192.168.1.100:9091/api/upload-apk](http://192.168.1.100:9091/api/upload-apk) -F "file=@/path/to/app.apk"
```

**Response:**

```json
{
  "success": true,
  "message": "Install dialog opened for uploaded APK"
}
```

**Important Notes:**
- The install dialog appears **on the device screen**
- User must manually confirm installation
- APK file is automatically cleaned up after installation or startup
- Maximum upload size depends on available device storage

**Status Codes:**
- `200 OK` - Success or failure (check `success` field)

**Error Response:**

```json
{
  "success": false,
  "message": "No file uploaded"
}
```

---

## Examples

### cURL

```bash
# Get all apps
curl [http://192.168.1.100:9091/api/apps](http://192.168.1.100:9091/api/apps)

# Launch Chrome
curl -X POST [http://192.168.1.100:9091/api/launch](http://192.168.1.100:9091/api/launch) \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.android.chrome"}'

# Open YouTube video
curl -X POST [http://192.168.1.100:9091/api/launch-intent](http://192.168.1.100:9091/api/launch-intent) \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.google.android.youtube","action":"android.intent.action.VIEW","data":"vnd.youtube://dQw4w9WgXcQ"}'

# Launch camera app with specific camera (comma-separated)
curl -X POST [http://192.168.1.100:9091/api/launch-intent](http://192.168.1.100:9091/api/launch-intent) \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.tpn.streamviewer","action":"android.intent.action.MAIN","data":"","extra_string":"camera_name:FRONTDOOR"}'

# Launch camera app with specific camera (individual parameters)
curl -X POST [http://192.168.1.100:9091/api/launch-intent](http://192.168.1.100:9091/api/launch-intent) \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.tpn.streamviewer","action":"android.intent.action.MAIN","data":"","extra_camera_name":"FRONTDOOR"}'

# Launch app with multiple extras
curl -X POST [http://192.168.1.100:9091/api/launch-intent](http://192.168.1.100:9091/api/launch-intent) \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.tpn.streamviewer","action":"android.intent.action.MAIN","extra_string":"camera_name:FRONT,protocol:mse,timeout:30"}'

# Uninstall app
curl -X POST [http://192.168.1.100:9091/api/uninstall](http://192.168.1.100:9091/api/uninstall) \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.example.app"}'

# Upload APK
curl -X POST [http://192.168.1.100:9091/api/upload-apk](http://192.168.1.100:9091/api/upload-apk) \
  -F "file=@app.apk"
```

### Python

```python
import requests

BASE_URL = "[http://192.168.1.100:9091](http://192.168.1.100:9091)"

# Get all apps
response = requests.get(f"{BASE_URL}/api/apps")
apps = response.json()
print(apps)

# Launch app
response = requests.post(
    f"{BASE_URL}/api/launch", json={"packageName": "com.android.chrome"}
)
print(response.json())

# Launch with intent
response = requests.post(
    f"{BASE_URL}/api/launch-intent",
    json={
        "packageName": "com.google.android.youtube",
        "action": "android.intent.action.VIEW",
        "data": "vnd.youtube://dQw4w9WgXcQ",
    },
)
print(response.json())

# Launch camera app with extras (comma-separated)
response = requests.post(
    f"{BASE_URL}/api/launch-intent",
    json={
        "packageName": "com.tpn.streamviewer",
        "action": "android.intent.action.MAIN",
        "data": "",
        "extra_string": "camera_name:FRONTDOOR",
    },
)
print(response.json())

# Launch camera app with extras (individual)
response = requests.post(
    f"{BASE_URL}/api/launch-intent",
    json={
        "packageName": "com.tpn.streamviewer",
        "action": "android.intent.action.MAIN",
        "data": "",
        "extra_camera_name": "FRONTDOOR",
        "extra_protocol": "mse",
    },
)
print(response.json())

# Uninstall app
response = requests.post(
    f"{BASE_URL}/api/uninstall", json={"packageName": "com.example.app"}
)
print(response.json())

# Upload APK
with open("app.apk", "rb") as f:
    files = {"file": f}
    response = requests.post(f"{BASE_URL}/api/upload-apk", files=files)
    print(response.json())
```

### JavaScript

```javascript
const BASE_URL = '[http://192.168.1.100:9091](http://192.168.1.100:9091)';

// Get all apps
fetch(`${BASE_URL}/api/apps`)
  .then(r => r.json())
  .then(apps => console.log(apps));

// Launch app
fetch(`${BASE_URL}/api/launch`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ packageName: 'com.android.chrome' })
})
.then(r => r.json())
.then(data => console.log(data));

// Launch with intent
fetch(`${BASE_URL}/api/launch-intent`, {
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

// Launch camera app with extras (comma-separated)
fetch(`${BASE_URL}/api/launch-intent`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    packageName: 'com.tpn.streamviewer',
    action: 'android.intent.action.MAIN',
    data: '',
    extra_string: 'camera_name:FRONTDOOR'
  })
})
.then(r => r.json())
.then(data => console.log(data));

// Launch camera app with extras (individual)
fetch(`${BASE_URL}/api/launch-intent`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    packageName: 'com.tpn.streamviewer',
    action: 'android.intent.action.MAIN',
    data: '',
    extra_camera_name: 'FRONTDOOR',
    extra_protocol: 'mse'
  })
})
.then(r => r.json())
.then(data => console.log(data));

// Uninstall app
fetch(`${BASE_URL}/api/uninstall`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ packageName: 'com.example.app' })
})
.then(r => r.json())
.then(data => console.log(data));

// Upload APK
const formData = new FormData();
formData.append('file', fileInput.files[0]);
fetch(`${BASE_URL}/api/upload-apk`, {
  method: 'POST',
  body: formData
})
.then(r => r.json())
.then(data => console.log(data));
```

### Home Assistant

```yaml
# Launch camera app with specific camera
action: rest_command.launch_app_with_intent
data:
  device_ip: "192.168.1.100"
  package_name: "com.tpn.streamviewer"
  action: "android.intent.action.MAIN"
  data: ""
  extra_string: "camera_name:FRONTDOOR"
```

---

## Error Handling

All endpoints return JSON with a `success` boolean field. Always check this field before assuming success.

**Common Errors:**

| Error | Cause | Solution |
|---|---|---|
| `Package name is required` | Missing `packageName` in request | Include `packageName` field |
| `Failed to launch app` | App doesn't exist or can't be launched | Verify package name is correct |
| `No file uploaded` | Missing file in upload | Include file in multipart form data |
| Intent extras not working | Malformed `extra_string` or unsupported by target app | Verify format: `key:value,key2:value2` |

---

## Intent Extras Reference

### Format Specification

**Comma-Separated String:**

```text
key1:value1,key2:value2,key3:value3