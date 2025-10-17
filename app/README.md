# Display Launcher

**An API-controlled Android launcher for non-interactive displays like kiosks and digital signage.**

## Introduction

Display Launcher is a specialized Android launcher application designed for devices that function as passive displays rather than interactive interfaces. Unlike traditional launchers that rely on user touch input, Display Launcher enables applications to be launched programmatically through HTTP API calls. This makes it ideal for digital signage, information displays, kiosks, dashboards, and any scenario where an Android device needs to automatically switch between applications based on external triggers, schedules, or automation systems.

The launcher includes an embedded web server that provides a modern browser-based control panel for remote management, allowing administrators to view installed applications and test launches from any device on the network.

---

## [!IMPORTANT] Security Considerations

### **NO BUILT-IN SECURITY OR AUTHENTICATION**

This is a **minimum viable product (MVP)** designed for proof-of-concept and internal network use. It includes **ZERO security features**:

- ❌ No authentication or authorization
- ❌ No encryption (plain HTTP, not HTTPS)
- ❌ No rate limiting or abuse protection
- ❌ No input validation beyond basic checks
- ❌ No access control or user management

### **Critical Security Implications:**

1. **Anyone on the network can control your device** - Any device that can reach the launcher's IP address and port can launch any application without credentials
2. **Potential for malicious use** - An attacker could repeatedly launch apps to drain battery, cause device instability, or access sensitive applications
3. **Network exposure** - If your network is not properly isolated, the launcher could be accessed from outside your organization
4. **No audit trail** - There is no logging of who launched which apps or when

### **Do NOT use this launcher in production environments without:**

- Implementing proper authentication (API keys, OAuth, etc.)
- Adding HTTPS/TLS encryption
- Running on an isolated network segment
- Implementing rate limiting and abuse prevention
- Adding comprehensive logging and monitoring
- Conducting a security audit

### **Recommended Use Cases (ONLY):**

- ✅ Internal testing and development
- ✅ Proof-of-concept demonstrations
- ✅ Isolated private networks with trusted users only
- ✅ Home automation projects on secure home networks
- ✅ Educational purposes and learning projects

**You assume all risks associated with deploying this launcher. The authors are not responsible for any security breaches, data loss, or damages resulting from its use.**

---

## Features

- **API-Controlled Launch** - Launch any installed application via HTTP POST requests
- **Web-Based GUI** - Modern, responsive control panel accessible from any browser
- **App Discovery** - Automatically lists all installed applications
- **Headless Operation** - Designed for displays that don't require touch interaction
- **Embedded Web Server** - No external server infrastructure required
- **HOME Launcher** - Properly configured as an Android home screen replacement

---

## Requirements

- Android 7.0 (API 24) or higher
- Android Studio Hedgehog or newer
- Kotlin 1.9+
- Network connectivity between control device and Android device

---

## Installation

### Option 1: Build from Source

1. Clone or download this repository
2. Open the project in Android Studio
3. Connect your Android device via USB or use an emulator
4. Click **Run** or use `./gradlew installDebug`

### Option 2: Install APK

1. Build the APK: `./gradlew assembleDebug`
2. Locate the APK in `app/build/outputs/apk/debug/`
3. Transfer to your Android device and install
4. Enable installation from unknown sources if prompted

---

## Setup

### 1. Set as Default Launcher

After installation:

1. Press the **Home** button on your Android device
2. You'll be prompted to choose a launcher
3. Select **Display Launcher** and tap **Always** (or **Always open with this app**)

To change later, go to: **Settings** → **Apps** → **Default Apps** → **Home App**

### 2. Grant Required Permissions

The app requires permission to query installed packages:

- On Android 11+, this is declared in the manifest with `QUERY_ALL_PACKAGES`
- If prompted, allow the permission when first launching

### 3. Find Your Device IP Address

To access the web interface, you need your device's local IP address:

1. Go to **Settings** → **Network & Internet** → **Wi-Fi**
2. Tap on your connected network
3. Note the IP address (e.g., `192.168.1.100`)

The web interface will be available at: `http://[device-ip]:9091`

---

## Usage

### Web Interface

1. Open a browser on any device on the same network
2. Navigate to `http://[device-ip]:9091`
3. You'll see a list of all installed applications
4. Use the search box to filter apps
5. Click **Launch** next to any app to start it

### API Endpoints

#### Get List of Installed Apps

```bash
GET http://[device-ip]:9091/api/apps
```

**Response:**
```json
[
  {
    "name": "Chrome",
    "packageName": "com.android.chrome",
    "isSystemApp": true
  },
  {
    "name": "My Custom App",
    "packageName": "com.example.myapp",
    "isSystemApp": false
  }
]
```

#### Launch an Application

```bash
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

**Error Response:**
```json
{
  "success": false,
  "message": "Failed to launch app"
}
```

### Example Usage with curl

```bash
# Get all apps
curl http://192.168.1.100:9091/api/apps

# Launch Chrome
curl -X POST http://192.168.1.100:9091/api/launch \\
  -H "Content-Type: application/json" \\
  -d '{"packageName": "com.android.chrome"}'
```

### Example Usage with Python

```python
import requests

# Device IP and port
BASE_URL = "http://192.168.1.100:9091"

# Get all installed apps
response = requests.get(f"{BASE_URL}/api/apps")
apps = response.json()
print(f"Found {len(apps)} apps")

# Launch an app
launch_data = {"packageName": "com.android.chrome"}
response = requests.post(f"{BASE_URL}/api/launch", json=launch_data)
result = response.json()
print(f"Launch result: {result['message']}")
```

### Example Usage with Node.js

```javascript
const axios = require('axios');

const BASE_URL = 'http://192.168.1.100:9091';

// Get all apps
async function getApps() {
  const response = await axios.get(`${BASE_URL}/api/apps`);
  console.log(`Found ${response.data.length} apps`);
  return response.data;
}

// Launch an app
async function launchApp(packageName) {
  const response = await axios.post(`${BASE_URL}/api/launch`, {
    packageName: packageName
  });
  console.log(`Launch result: ${response.data.message}`);
  return response.data;
}

// Usage
getApps();
launchApp('com.android.chrome');
```

---

## Configuration

### Change Web Server Port

By default, the web server runs on port 9091. To change this:

1. Open `MainActivity.kt`
2. Locate the line: `webServer = LauncherWebServer(9091, appLauncher)`
3. Change `9091` to your desired port
4. Rebuild and reinstall the app

### Filter System Apps

To hide system apps from the web interface, modify `AppLauncher.kt`:

```kotlin
return packages
    .filter { it.packageName != context.packageName }
    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 } // Add this line
    .map { appInfo ->
        // ... rest of the code
    }
```

---

## Project Structure

```
app/src/main/java/com/yourcompany/headlesslauncher/
├── MainActivity.kt           # Main activity with Compose UI
├── AppLauncher.kt           # Handles app discovery and launching
├── LauncherWebServer.kt     # Embedded NanoHTTPD web server
└── AppInfo.kt              # Data class for app information (in AppLauncher.kt)

app/src/main/
└── AndroidManifest.xml      # Launcher configuration and permissions
```

---

## Troubleshooting

### Can't Access Web Interface

- Verify both devices are on the same network
- Check Android firewall settings (some ROMs have built-in firewalls)
- Try disabling VPN if active on either device
- Confirm the app is running (visible in Recent Apps)
- Verify the IP address hasn't changed (DHCP can reassign IPs)

### App Won't Launch as Default Launcher

- Go to **Settings** → **Apps** → **Default Apps** → **Home App**
- Select **Display Launcher** manually
- Some devices may require a reboot after installation

### "Failed to Launch App" Error

- The app may not have a launch intent (not all apps can be launched this way)
- The app may be disabled or restricted
- Check Android logs: `adb logcat | grep DisplayLauncher`

### Can't See All Installed Apps

On Android 11+, you need the `QUERY_ALL_PACKAGES` permission:
- This is already declared in the manifest
- Some devices may require manual permission granting in app settings
- Alternative: Add specific package queries to the manifest

---

## Building for Production

If you plan to deploy this in a production environment, you **MUST** implement security features:

### Essential Security Enhancements:

1. **Add Authentication:**
    - Implement API key authentication
    - Use JWT tokens for session management
    - Consider OAuth 2.0 for enterprise deployments

2. **Enable HTTPS:**
    - Use a self-signed certificate (for internal use)
    - Implement proper certificate management
    - Consider using a reverse proxy with proper TLS

3. **Input Validation:**
    - Validate all incoming requests
    - Sanitize package names and parameters
    - Implement request size limits

4. **Rate Limiting:**
    - Limit requests per IP address
    - Implement cooldown periods between launches
    - Add CAPTCHA or similar for web interface

5. **Logging & Monitoring:**
    - Log all API requests with timestamps
    - Track which apps are launched and by whom
    - Set up alerts for suspicious activity

6. **Network Security:**
    - Run on an isolated VLAN
    - Use firewall rules to restrict access
    - Implement IP whitelisting

---

## Known Limitations

- No authentication or security (see Security Considerations above)
- Single-device only (no multi-device management)
- No app launch scheduling or automation built-in
- Cannot launch apps that don't have a launch intent
- No persistent configuration storage
- Web server stops when app is killed by Android
- No support for launching with specific intents or extras

---

## Future Enhancements (Roadmap)

- 🔐 Authentication and authorization system
- 🔒 HTTPS/TLS support
- 📅 Scheduled app launching
- 🎯 Intent extras and action support
- 💾 Configuration persistence
- 📊 Usage analytics and logging
- 🔄 Auto-restart after device reboot
- 🌐 MQTT integration for IoT automation
- 📱 Multi-device management dashboard

---

## Disclaimer

This software is provided "as is" without warranty of any kind. The authors and contributors are not responsible for any damages, security breaches, data loss, or other issues that may arise from using this software. Use at your own risk, especially in networked environments.

---

## Support

For issues, questions, or feature requests:
- Open an issue on GitHub
- Check existing issues for solutions
- Review the Troubleshooting section above

**Remember: This is an MVP without security features. Do not use in production without implementing proper security measures.**
