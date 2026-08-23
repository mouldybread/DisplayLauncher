# Home Assistant Integration Guide

Complete guide for integrating Display Launcher with Home Assistant for automated digital signage, kiosk management, and stream controller integration using REST commands and intent extras.

## Overview

Display Launcher's REST API allows Home Assistant to control target Android displays based on:

- **Time schedules** - Content rotation throughout the day.
- **Presence & Motion** - Waking displays or loading state-specific views when movement or user presence is detected.
- **Doorbell & Alarm Events** - Automating popups and deep links (e.g., camera feeds on doorbell ring).
- **Custom App Parameters** - Passing intent extras directly to target applications (such as camera names, media URLs, or display parameters).

---

## Prerequisites

1. Display Launcher installed and set as the default launcher on target Android devices[cite: 8].
2. Static IP addresses assigned to target Android devices.
3. Home Assistant instance on the same local network[cite: 8].
4. Reference documentation: [API Reference](./API.md), [README](./README.md)[cite: 8].

---

## REST Commands Setup

Add the following REST commands to your `configuration.yaml` (or within a package file). These commands provide endpoints for standard app launches, intent launches with extras, and remote package removal.

```yaml
rest_command:
  # Launch app on a specific device by package name
  display_launcher_launch_app:
    url: "http://{{ device_ip }}:9091/api/launch"
    method: POST
    content_type: "application/json"
    payload: '{"packageName":"{{ package_name }}"}'

  # Launch app with intent actions, data URIs, and extra string parameters
  display_launcher_launch_intent:
    url: "http://{{ device_ip }}:9091/api/launch-intent"
    method: POST
    content_type: "application/json"
    payload: >-
      {
        "packageName": "{{ package_name }}",
        "action": "{{ action | default('android.intent.action.MAIN') }}",
        "data": "{{ data | default('') }}",
        "extra_string": "{{ extra_string | default('') }}"
      }
    timeout: 10

  # Trigger remote uninstall prompt on target device
  display_launcher_uninstall_app:
    url: "http://{{ device_ip }}:9091/api/uninstall"
    method: POST
    content_type: "application/json"
    payload: '{"packageName":"{{ package_name }}"}'
```

After updating `configuration.yaml`, reload REST commands via **Developer Tools → YAML → REST Commands**.

---

## Input Helpers

Define input helpers in Home Assistant to facilitate dashboard controls and dynamic UI selection.

### Input Selects

```yaml
input_select:
  display_launcher_device:
    name: Target Display Device
    options:
      - All Devices
      - Living Room (192.168.1.100)
      - Kitchen (192.168.1.101)
      - Bedroom (192.168.1.102)
    initial: Living Room (192.168.1.100)
    icon: mdi:television

  display_launcher_app:
    name: Target Application
    options:
      - Chrome (com.android.chrome)
      - YouTube (com.google.android.youtube)
      - Netflix (com.netflix.mediaclient)
      - Stream Viewer (com.tpn.streamviewer)
    icon: mdi:application

  stream_viewer_camera:
    name: Stream Viewer Camera
    options:
      - FRONTDOOR
      - DRIVEWAY
      - BACKYARD
      - GARAGE
    initial: FRONTDOOR
    icon: mdi:cctv
```

### Input Text

```yaml
input_text:
  youtube_video_id:
    name: YouTube Video ID
    initial: ""
    icon: mdi:youtube
```

---

## Scripts

### Launch App Handler

Generic script parsing device IP and package name from input string helpers.

```yaml
script:
  launch_display_app:
    alias: Launch Display App
    fields:
      device:
        description: Device selection string containing IP address
        example: "Living Room (192.168.1.100)"
      app:
        description: Application selection string containing package name
        example: "Chrome (com.android.chrome)"
    sequence:
      - variables:
          device_ip: >
            {% if "(" in device and ")" in device %}
              {{ device.split("(")[1].split(")")[0] }}
            {% else %}
              {{ device }}
            {% endif %}
          package_name: >
            {% if "(" in app and ")" in app %}
              {{ app.split("(")[1].split(")")[0] }}
            {% else %}
              {{ app }}
            {% endif %}
      - choose:
          - conditions:
              - condition: template
                value_template: "{{ device == 'All Devices' }}"
            sequence:
              - action: rest_command.display_launcher_launch_app
                data:
                  device_ip: "192.168.1.100"
                  package_name: "{{ package_name }}"
              - action: rest_command.display_launcher_launch_app
                data:
                  device_ip: "192.168.1.101"
                  package_name: "{{ package_name }}"
              - action: rest_command.display_launcher_launch_app
                data:
                  device_ip: "192.168.1.102"
                  package_name: "{{ package_name }}"
        default:
          - action: rest_command.display_launcher_launch_app
            data:
              device_ip: "{{ device_ip }}"
              package_name: "{{ package_name }}"
```

### Launch YouTube Video

```yaml
script:
  launch_youtube_video:
    alias: Launch YouTube Video
    fields:
      device_ip:
        description: Target device IP address
        example: "192.168.1.100"
      video_id:
        description: YouTube video identifier
        example: "dQw4w9WgXcQ"
    sequence:
      - action: rest_command.display_launcher_launch_intent
        data:
          device_ip: "{{ device_ip }}"
          package_name: "com.google.android.youtube"
          action: "android.intent.action.VIEW"
          data: "vnd.youtube://{{ video_id }}"
```

### Launch Stream Viewer Camera

```yaml
script:
  launch_camera_view:
    alias: Launch Camera View
    fields:
      device_ip:
        description: Target device IP address
        example: "192.168.1.100"
      camera_name:
        description: Camera name identifier passed via intent extra
        example: "FRONTDOOR"
    sequence:
      - action: rest_command.display_launcher_launch_intent
        data:
          device_ip: "{{ device_ip }}"
          package_name: "com.tpn.streamviewer"
          action: "android.intent.action.MAIN"
          data: ""
          extra_string: "camera_name:{{ camera_name }}"
```

### Open Web URL in Browser

```yaml
script:
  display_open_url:
    alias: Display Open URL
    fields:
      device_ip:
        description: Target device IP address
        example: "192.168.1.100"
      url:
        description: Destination HTTP/HTTPS URL
        example: "[https://example.com](https://example.com)"
    sequence:
      - action: rest_command.display_launcher_launch_intent
        data:
          device_ip: "{{ device_ip }}"
          package_name: "com.android.chrome"
          action: "android.intent.action.VIEW"
          data: "{{ url }}"
```

---

## Automation Examples

### Schedule-Based Application Launching

```yaml
automation:
  - alias: "Display Schedule: Morning Dashboard"
    trigger:
      - trigger: time
        at: "07:00:00"
    action:
      - action: script.launch_display_app
        data:
          device: "192.168.1.100"
          app: "io.homeassistant.companion.android"

  - alias: "Display Schedule: Evening Entertainment"
    trigger:
      - trigger: time
        at: "19:00:00"
    action:
      - action: script.launch_display_app
        data:
          device: "192.168.1.100"
          app: "com.netflix.mediaclient"
```

### Motion Sensor Activation

```yaml
automation:
  - alias: "Display: Switch to Home Assistant on Motion"
    trigger:
      - trigger: state
        entity_id: binary_sensor.living_room_motion
        to: "on"
    condition:
      - condition: time
        after: "06:00:00"
        before: "23:00:00"
    action:
      - action: script.launch_display_app
        data:
          device: "192.168.1.100"
          app: "io.homeassistant.companion.android"
```

### Doorbell Event Integration

```yaml
automation:
  - alias: "Display: Show Front Door Camera on Doorbell Press"
    trigger:
      - trigger: state
        entity_id: binary_sensor.doorbell_ring
        to: "on"
    action:
      - action: script.launch_camera_view
        data:
          device_ip: "192.168.1.100"
          camera_name: "FRONTDOOR"
```

---

## Advanced Examples

### Synchronized Multi-Display Launch

```yaml
script:
  launch_all_displays_sync:
    alias: Synchronized Launch across All Displays
    fields:
      package_name:
        description: Target package identifier
        example: "com.android.chrome"
    sequence:
      - parallel:
          - action: rest_command.display_launcher_launch_app
            data:
              device_ip: "192.168.1.100"
              package_name: "{{ package_name }}"
          - action: rest_command.display_launcher_launch_app
            data:
              device_ip: "192.168.1.101"
              package_name: "{{ package_name }}"
          - action: rest_command.display_launcher_launch_app
            data:
              device_ip: "192.168.1.102"
              package_name: "{{ package_name }}"
```

### Dynamic Camera Rotation Sequence

```yaml
automation:
  - alias: "Display: Rotate Security Cameras"
    trigger:
      - trigger: time_pattern
        minutes: "/5"
    action:
      - action: rest_command.display_launcher_launch_intent
        data:
          device_ip: "192.168.1.100"
          package_name: "com.tpn.streamviewer"
          action: "android.intent.action.MAIN"
          extra_string: >
            {% set cameras = ['FRONTDOOR', 'DRIVEWAY', 'BACKYARD', 'GARAGE'] %}
            {% set index = (now().minute // 5) % 4 %}
            camera_name:{{ cameras[index] }}
```

---

## Intent Extras Integration

### Multi-Extra Key-Value Formatting

The `extra_string` field accepts comma-separated key-value pairs formatted as `key1:value1,key2:value2`.

```yaml
script:
  launch_app_multi_extras:
    alias: Launch App with Multiple Extras
    fields:
      device_ip:
        description: Device IP
      package_name:
        description: Target Package Name
      extra_pairs:
        description: Formatted extra string (e.g., key1:val1,key2:val2)
    sequence:
      - action: rest_command.display_launcher_launch_intent
        data:
          device_ip: "{{ device_ip }}"
          package_name: "{{ package_name }}"
          action: "android.intent.action.MAIN"
          extra_string: "{{ extra_pairs }}"
```

---

## Troubleshooting

### Connection & Execution Errors

1. **REST Command Timeout / Failure**:
   Validate API reachability directly via terminal:
   ```bash
   curl -X GET [http://192.168.1.100:9091/api/apps](http://192.168.1.100:9091/api/apps)
   ```

2. **Enable Home Assistant Debug Logging**:
   Add to `configuration.yaml` to inspect outgoing HTTP request payloads:
   ```yaml
   logger:
     default: info
     logs:
       homeassistant.components.rest_command: debug
   ```

3. **App Does Not Reach Foreground**:
   Ensure Display Launcher is registered as the default Home activity on the target Android device[cite: 8].

4. **Intent Extras Unhandled**:
    - Verify parameter key case sensitivity.
    - Confirm target app explicitly checks intent extras on activity startup.
    - Verify via ADB:
      ```bash
      adb shell am start -n com.tpn.streamviewer/.MainActivity --es camera_name FRONTDOOR
      ```

---

## Complete Package Configuration

Save the unified configuration below to `packages/display_launcher.yaml` for modular integration:

```yaml
# Display Launcher Home Assistant Package
input_select:
  display_device:
    name: Target Display Device
    options:
      - Living Room (192.168.1.100)
      - Kitchen (192.168.1.101)
  display_app:
    name: Target Display App
    options:
      - Chrome (com.android.chrome)
      - YouTube (com.google.android.youtube)
      - Stream Viewer (com.tpn.streamviewer)
  camera_name:
    name: Camera Selection
    options:
      - FRONTDOOR
      - DRIVEWAY
      - BACKYARD

rest_command:
  display_launcher_launch_app:
    url: "http://{{ device_ip }}:9091/api/launch"
    method: POST
    content_type: "application/json"
    payload: '{"packageName":"{{ package_name }}"}'

  display_launcher_launch_intent:
    url: "http://{{ device_ip }}:9091/api/launch-intent"
    method: POST
    content_type: "application/json"
    payload: >-
      {
        "packageName": "{{ package_name }}",
        "action": "{{ action | default('android.intent.action.MAIN') }}",
        "data": "{{ data | default('') }}",
        "extra_string": "{{ extra_string | default('') }}"
      }

script:
  launch_display:
    alias: Launch Selected Display App
    sequence:
      - variables:
          device_raw: "{{ states('input_select.display_device') }}"
          app_raw: "{{ states('input_select.display_app') }}"
          device_ip: "{{ device_raw.split('(')[1].split(')')[0] }}"
          package_name: "{{ app_raw.split('(')[1].split(')')[0] }}"
      - action: rest_command.display_launcher_launch_app
        data:
          device_ip: "{{ device_ip }}"
          package_name: "{{ package_name }}"

  launch_camera_selected:
    alias: Launch Selected Camera View
    sequence:
      - variables:
          device_raw: "{{ states('input_select.display_device') }}"
          device_ip: "{{ device_raw.split('(')[1].split(')')[0] }}"
          camera: "{{ states('input_select.camera_name') }}"
      - action: rest_command.display_launcher_launch_intent
        data:
          device_ip: "{{ device_ip }}"
          package_name: "com.tpn.streamviewer"
          action: "android.intent.action.MAIN"
          extra_string: "camera_name:{{ camera }}"
```

---

## Additional Resources

- [Display Launcher REST API Reference](./API.md)
- [Home Assistant REST Command Integration](https://www.home-assistant.io/integrations/rest_command/)
- [Android Developer Intent Documentation](https://developer.android.com/reference/android/content/Intent)
