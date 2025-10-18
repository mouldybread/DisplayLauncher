# Display Launcher - Home Assistant Integration Guide

Complete setup guide for controlling your Android TV displays remotely using Display Launcher's REST API from Home Assistant. Launch apps, open deep links, and automate your digital signage displays.

## Prerequisites

- Display Launcher installed on your Android TV devices
- Home Assistant instance on the same network
- Device IP addresses (static recommended)

---

## Configuration

### 1. REST Commands

Create or edit `rest_command.yaml`:

```yaml
# Basic app launching
launch_app_device:
  url: "http://{{ device_ip }}:9091/api/launch"
  method: POST
  content_type: "application/json"
  payload: '{"packageName": "{{ package_name }}"}'
  timeout: 10

# Launch with intent (for deep links, URLs, etc.)
launch_app_with_intent:
  url: "http://{{ device_ip }}:9091/api/launch-intent"
  method: POST
  content_type: "application/json"
  payload: '{"packageName": "{{ package_name }}", "action": "{{ action }}", "data": "{{ data }}"}'
  timeout: 10
```

### 2. Input Selects

Create or edit `input_select.yaml`:

```yaml
# Device selector
display_device:
  name: Display Device
  options:
    - Living Room (192.168.1.101)
    - Kitchen (192.168.1.102)
    - Bedroom (192.168.1.103)
    - All Devices
  icon: mdi:television

# App selector
display_app:
  name: App to Launch
  options:
    - YouTube (com.google.android.youtube.tv)
    - Netflix (com.netflix.ninja)
    - Plex (com.plexapp.android)
    - Spotify (com.spotify.tv.android)
    - SmartTube (com.liskovsoft.smarttubetv.beta)
    - Home (com.tpn.displaylauncher)
  icon: mdi:application
```

### 3. Scripts

Create or edit `scripts.yaml`:

```yaml
# Universal app launcher
launch_display_app:
  alias: Launch App on Display
  sequence:
    - variables:
        device_map:
          "Living Room (192.168.1.101)": "192.168.1.101"
          "Kitchen (192.168.1.102)": "192.168.1.102"
          "Bedroom (192.168.1.103)": "192.168.1.103"
        app_map:
          "YouTube (com.google.android.youtube.tv)": "com.google.android.youtube.tv"
          "Netflix (com.netflix.ninja)": "com.netflix.ninja"
          "Plex (com.plexapp.android)": "com.plexapp.android"
          "Spotify (com.spotify.tv.android)": "com.spotify.tv.android"
          "SmartTube (com.liskovsoft.smarttubetv.beta)": "com.liskovsoft.smarttubetv.beta"
          "Home (com.tpn.displaylauncher)": "com.tpn.displaylauncher"
    - choose:
        - conditions:
            - condition: state
              entity_id: input_select.display_device
              state: "All Devices"
          sequence:
            - service: rest_command.launch_app_device
              data:
                device_ip: "192.168.1.101"
                package_name: "{{ app_map[states('input_select.display_app')] }}"
            - service: rest_command.launch_app_device
              data:
                device_ip: "192.168.1.102"
                package_name: "{{ app_map[states('input_select.display_app')] }}"
            - service: rest_command.launch_app_device
              data:
                device_ip: "192.168.1.103"
                package_name: "{{ app_map[states('input_select.display_app')] }}"
      default:
        - service: rest_command.launch_app_device
          data:
            device_ip: "{{ device_map[states('input_select.display_device')] }}"
            package_name: "{{ app_map[states('input_select.display_app')] }}"
```

### 4. Configuration File

Ensure your `configuration.yaml` includes:

```yaml
rest_command: !include rest_command.yaml
input_select: !include input_select.yaml
script: !include scripts.yaml
```

---

## Lovelace Dashboard Card

Add this card to your dashboard for manual control:

```yaml
type: vertical-stack
cards:
  - type: entities
    title: 🚀 Display Launcher Control
    entities:
      - entity: input_select.display_device
      - entity: input_select.display_app
  - type: button
    name: Launch App
    icon: mdi:play-circle
    tap_action:
      action: call-service
      service: script.launch_display_app
```

---

## Automation Examples

### Example 1: Launch App on Schedule

Launch YouTube on all displays at 6 PM every day:

```yaml
automation:
  - alias: "Evening YouTube on All Displays"
    trigger:
      - platform: time
        at: "18:00:00"
    action:
      - service: rest_command.launch_app_device
        data:
          device_ip: "192.168.1.101"
          package_name: "com.google.android.youtube.tv"
      - service: rest_command.launch_app_device
        data:
          device_ip: "192.168.1.102"
          package_name: "com.google.android.youtube.tv"
      - service: rest_command.launch_app_device
        data:
          device_ip: "192.168.1.103"
          package_name: "com.google.android.youtube.tv"
```

### Example 2: Movie Mode

Launch Netflix on living room TV when movie mode is activated:

```yaml
automation:
  - alias: "Launch Netflix on Movie Mode"
    trigger:
      - platform: state
        entity_id: input_boolean.movie_mode
        to: "on"
    action:
      - service: rest_command.launch_app_device
        data:
          device_ip: "192.168.1.101"
          package_name: "com.netflix.ninja"
```

### Example 3: Motion-Triggered Display

Launch Plex when motion is detected:

```yaml
automation:
  - alias: "Launch Plex on Motion"
    trigger:
      - platform: state
        entity_id: binary_sensor.kitchen_motion
        to: "on"
    action:
      - service: rest_command.launch_app_device
        data:
          device_ip: "192.168.1.102"
          package_name: "com.plexapp.android"
```

### Example 4: Launch YouTube Video with Deep Link

Open a specific YouTube video using intent:

```yaml
automation:
  - alias: "Play YouTube Live Stream"
    trigger:
      - platform: state
        entity_id: sensor.live_stream_active
        to: "on"
    action:
      - service: rest_command.launch_app_with_intent
        data:
          device_ip: "192.168.1.101"
          package_name: "com.google.android.youtube"
          action: "android.intent.action.VIEW"
          data: "vnd.youtube://{{ states('sensor.youtube_video_id') }}"
```

### Example 5: Return to Launcher

Return all displays to the launcher (home screen):

```yaml
automation:
  - alias: "Return All Displays to Home"
    trigger:
      - platform: time
        at: "23:00:00"
    action:
      - service: rest_command.launch_app_device
        data:
          device_ip: "192.168.1.101"
          package_name: "com.tpn.displaylauncher"
      - service: rest_command.launch_app_device
        data:
          device_ip: "192.168.1.102"
          package_name: "com.tpn.displaylauncher"
      - service: rest_command.launch_app_device
        data:
          device_ip: "192.168.1.103"
          package_name: "com.tpn.displaylauncher"
```

### Example 6: Weather-Based Content

Launch weather app when it's raining:

```yaml
automation:
  - alias: "Show Weather on Rainy Days"
    trigger:
      - platform: state
        entity_id: weather.home
        attribute: condition
        to: "rainy"
    action:
      - service: rest_command.launch_app_device
        data:
          device_ip: "192.168.1.103"
          package_name: "com.google.android.googlequicksearchbox"
```

---

## Advanced Usage

### Using Intent Actions

The `/api/launch-intent` endpoint supports Android intent actions for advanced app launching.

**Common Intent Actions:**

- `android.intent.action.VIEW` - View content (URLs, videos, etc.)
- `android.intent.action.MAIN` - Launch main activity
- `android.intent.action.SEARCH` - Open search

#### Open URL in Browser

```yaml
service: rest_command.launch_app_with_intent
data:
  device_ip: "192.168.1.101"
  package_name: "com.android.chrome"
  action: "android.intent.action.VIEW"
  data: "https://www.example.com"
```

#### Open YouTube Channel

```yaml
service: rest_command.launch_app_with_intent
data:
  device_ip: "192.168.1.101"
  package_name: "com.google.android.youtube.tv"
  action: "android.intent.action.VIEW"
  data: "vnd.youtube://user/USERNAME"
```

---

## Testing

### Test REST Commands in Developer Tools

1. Go to **Developer Tools** → **Services**
2. Select `rest_command.launch_app_device`
3. Enter service data:

```yaml
device_ip: "192.168.1.101"
package_name: "com.google.android.youtube.tv"
```

4. Click **Call Service**

### Test with cURL

```bash
# Basic launch
curl -X POST http://192.168.1.101:9091/api/launch \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.google.android.youtube.tv"}'

# Launch with intent
curl -X POST http://192.168.1.101:9091/api/launch-intent \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.google.android.youtube","action":"android.intent.action.VIEW","data":"vnd.youtube://dQw4w9WgXcQ"}'
```

---

## Troubleshooting

### 404 Errors

- Verify Display Launcher is running on the device
- Check the endpoint: use `/api/launch` not `/launch`
- Ensure port 9091 is accessible

### 400 Errors

- Check JSON payload format
- Verify package name is correct
- Test with cURL first

### App Doesn't Launch

- Verify package name: `adb shell pm list packages | grep appname`
- Check if app is installed on the device
- Try launching manually from device first

### Connection Timeouts

- Verify device IP address is correct
- Check network connectivity
- Ensure firewall allows port 9091

---

## Finding Package Names

To find package names for apps on your device:

```bash
# List all installed packages
adb shell pm list packages

# Search for specific app
adb shell pm list packages | grep youtube

# Get currently running app
adb shell dumpsys window windows | grep -E 'mCurrentFocus'
```

---

## API Reference

### Endpoints

#### `GET /`
Web UI interface for manual control

#### `GET /api/apps`
List all installed apps

**Response:**
```json
[
  {
    "name": "YouTube",
    "packageName": "com.google.android.youtube.tv"
  }
]
```

#### `POST /api/launch`
Launch an app

**Request:**
```json
{
  "packageName": "com.google.android.youtube.tv"
}
```

#### `POST /api/launch-intent`
Launch with intent

**Request:**
```json
{
  "packageName": "com.google.android.youtube",
  "action": "android.intent.action.VIEW",
  "data": "vnd.youtube://dQw4w9WgXcQ"
}
```

---

## Security Considerations

- Display Launcher API has no authentication
- Use only on trusted local networks
- Consider network isolation for displays
- Do not expose port 9091 to the internet
- Use static IPs or DHCP reservations

---

## Tips & Best Practices

- **Use Static IPs:** Set static IPs or DHCP reservations for displays
- **Test First:** Always test commands in Developer Tools before creating automations
- **Error Handling:** Add conditions to check if commands succeed
- **Logging:** Enable Home Assistant logging to debug issues
- **Backup:** Keep backups of your Home Assistant configuration
- **Groups:** Create device groups for bulk operations
- **Templates:** Use templates for dynamic package names or IPs