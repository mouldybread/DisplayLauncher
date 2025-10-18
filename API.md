# Display Launcher API Reference

Complete REST API documentation for Display Launcher.

## Base URL

```
http://[device-ip]:9091
```

Default port: `9091`

---

## Endpoints

### GET /api/apps

Get a list of all installed user applications.

**Request:**

```
GET /api/apps
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

```
POST /api/launch
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

Launch an application with a custom Android intent (deep links, URLs, etc.).

**Request:**

```
POST /api/launch-intent
Content-Type: application/json

{
  "packageName": "com.google.android.youtube",
  "action": "android.intent.action.VIEW",
  "data": "vnd.youtube://dQw4w9WgXcQ"
}
```

**Parameters:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `packageName` | string | Yes | Target app package name |
| `action` | string | No | Android intent action (e.g., `android.intent.action.VIEW`) |
| `data` | string | No | Intent data URI (e.g., YouTube video ID, URL, deep link) |

**Response:**

```json
{
  "success": true,
  "message": "App launched successfully with intent"
}
```

**Common Intent Actions:**

| Action | Description |
|--------|-------------|
| `android.intent.action.VIEW` | View content (URLs, videos, etc.) |
| `android.intent.action.MAIN` | Launch main activity |
| `android.intent.action.SEARCH` | Open search |

**Intent Data Examples:**

| Type | Example |
|------|---------|
| YouTube video | `vnd.youtube://VIDEO_ID` |
| URL | `https://example.com` |
| Deep link | `app://path/to/content` |

**Status Codes:**
- `200 OK` - Success or failure (check `success` field)

---

### POST /api/uninstall

Trigger the uninstall dialog for an application.

**Request:**

```
POST /api/uninstall
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

```
POST /api/upload-apk
Content-Type: multipart/form-data

file: [APK binary data]
```

**cURL Example:**

```bash
curl -X POST http://192.168.1.100:9091/api/upload-apk \\
  -F "file=@/path/to/app.apk"
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
- APK file is automatically deleted after 5 seconds
- Maximum upload size depends on device storage

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
curl http://192.168.1.100:9091/api/apps

# Launch Chrome
curl -X POST http://192.168.1.100:9091/api/launch \\
  -H "Content-Type: application/json" \\
  -d '{"packageName":"com.android.chrome"}'

# Open YouTube video
curl -X POST http://192.168.1.100:9091/api/launch-intent \\
  -H "Content-Type: application/json" \\
  -d '{"packageName":"com.google.android.youtube","action":"android.intent.action.VIEW","data":"vnd.youtube://dQw4w9WgXcQ"}'

# Uninstall app
curl -X POST http://192.168.1.100:9091/api/uninstall \\
  -H "Content-Type: application/json" \\
  -d '{"packageName":"com.example.app"}'

# Upload APK
curl -X POST http://192.168.1.100:9091/api/upload-apk \\
  -F "file=@app.apk"
```

### Python

```python
import requests

# Get all apps
response = requests.get('http://192.168.1.100:9091/api/apps')
apps = response.json()
print(apps)

# Launch app
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

# Uninstall app
response = requests.post(
    'http://192.168.1.100:9091/api/uninstall',
    json={'packageName': 'com.example.app'}
)
print(response.json())

# Upload APK
with open('app.apk', 'rb') as f:
    files = {'file': f}
    response = requests.post(
        'http://192.168.1.100:9091/api/upload-apk',
        files=files
    )
    print(response.json())
```

### JavaScript

```javascript
// Get all apps
fetch('http://192.168.1.100:9091/api/apps')
  .then(r => r.json())
  .then(apps => console.log(apps));

// Launch app
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

// Uninstall app
fetch('http://192.168.1.100:9091/api/uninstall', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ packageName: 'com.example.app' })
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

## Error Handling

All endpoints return JSON with a `success` boolean field. Always check this field before assuming success.

**Common Errors:**

| Error | Cause | Solution |
|-------|-------|----------|
| `Package name is required` | Missing `packageName` in request | Include `packageName` field |
| `Failed to launch app` | App doesn't exist or can't be launched | Verify package name is correct |
| `No file uploaded` | Missing file in upload | Include file in multipart form data |

---

## Finding Package Names

### Method 1: Web Interface

Visit `http://[device-ip]:9091` and browse the app list. Package names are shown below app names.

### Method 2: ADB

```bash
adb shell pm list packages
```

### Method 3: API

```bash
curl http://[device-ip]:9091/api/apps
```

---

## Security Considerations

⚠️ **No Authentication** - This API has no built-in authentication. Anyone on the network can control the device.

⚠️ **No HTTPS** - All traffic is unencrypted HTTP. Do not use on untrusted networks.

⚠️ **APK Installation** - Anyone can upload and install APKs. Use only on trusted networks.

**Recommendations:**
- Use on isolated/trusted networks only
- Consider using a firewall to restrict access
- Do not expose to the internet
- Future versions will include authentication options
