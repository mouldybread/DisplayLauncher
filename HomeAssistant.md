# Home Assistant Integration Guide

Complete guide for integrating Display Launcher with Home Assistant for automated digital signage control, including support for intent extras.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Basic Setup](#basic-setup)
- [REST Commands](#rest-commands)
- [Input Helpers](#input-helpers)
- [Scripts](#scripts)
- [Automation Examples](#automation-examples)
- [Advanced Examples](#advanced-examples)
- [Intent Extras Integration](#intent-extras-integration)
- [Troubleshooting](#troubleshooting)

---

## Overview

Display Launcher's REST API makes it perfect for Home Assistant automation. Control your Android displays based on:

- **Time of day** - Switch content on a schedule
- **Presence** - Show specific content when home/away
- **Motion sensors** - Wake displays when movement detected
- **Media playback** - Auto-launch apps when playing media
- **Manual control** - Dashboards and scripts for one-touch control
- **App-specific parameters** - Launch apps with custom configurations using intent extras

---

## Prerequisites

1. Display Launcher installed on Android device(s)
2. Device set as default launcher
3. Device IP address(es) (set static IPs recommended)
4. Home Assistant instance on same network
5. Complete API reference: [API.md](./API.md)

---

## Basic Setup

### Step 1: Add REST Commands

Add these to your `configuration.yaml`:

```yaml
rest_command:
  # Launch app on specific device
  launch_app_device:
    url: "http://{{ device_ip }}:9091/api/launch"
    method: POST
    content_type: "application/json"
    payload: '{"packageName":"{{ package_name }}"}'
  # Launch app with intent (YouTube, URLs, deep links)
  launch_app_with_intent:
    url: "http://{{ device_ip }}:9091/api/launch-intent"
    method: POST
    content_type: "application/json"
    payload: '{"packageName":"{{ package_name }}","action":"{{ action }}","data":"{{ data }}","extra_string":"{{ extra_string | default("") }}"}'
    timeout: 10
  # Uninstall app
  uninstall_app:
    url: "http://{{ device_ip }}:9091/api/uninstall"
    method: POST
    content_type: "application/json"
    payload: '{"packageName":"{{ package_name }}"}'
```

### Step 2: Restart Home Assistant

```
Developer Tools → YAML → Restart
```

### Step 3: Test

```yaml
service: rest_command.launch_app_device
data:
  device_ip: "192.168.1.100"
  package_name: "com.android.chrome"
```

---

## REST Commands

Complete REST command configuration with intent extras support:

```yaml
rest_command:
  # Basic app launch
  launch_app_device:
    url: "http://{{ device_ip }}:9091/api/launch"
    method: POST
    content_type: "application/json"
    payload: '{"packageName":"{{ package_name }}"}'
  # Launch with intent and extras
  launch_app_with_intent:
    url: "http://{{ device_ip }}:9091/api/launch-intent"
    method: POST
    content_type: "application/json"
    payload: '{"packageName":"{{ package_name }}","action":"{{ action }}","data":"{{ data }}","extra_string":"{{ extra_string | default("") }}"}'
    timeout: 10
  # Uninstall app
  uninstall_app:
    url: "http://{{ device_ip }}:9091/api/uninstall"
    method: POST
    content_type: "application/json"
    payload: '{"packageName":"{{ package_name }}"}'
```

---

## Input Helpers

Create these helpers for easier automation.

### Input Select - Devices

```yaml
input_select:
  display_launcher_device:
    name: Display Device
    options:
      - All Devices
      - Living Room (192.168.1.100)
      - Kitchen (192.168.1.101)
      - Bedroom (192.168.1.102)
    initial: All Devices
    icon: mdi:television
```

### Input Select - Apps

```yaml
input_select:
  display_launcher_app:
    name: Launch App
    options:
      - Chrome (com.android.chrome)
      - YouTube (com.google.android.youtube)
      - Netflix (com.netflix.mediaclient)
      - Stream Viewer (com.tpn.streamviewer)
    icon: mdi:application
```

### Input Text - YouTube Video ID

```yaml
input_text:
  youtube_video_id:
    name: YouTube Video ID
    initial: ""
    icon: mdi:youtube
```

### Input Select - Camera Names (for Stream Viewer)

```yaml
input_select:
  camera_name:
    name: Camera to Display
    options:
      - FRONTDOOR
      - DRIVEWAY
      - BACKYARD
      - GARAGE
    initial: FRONTDOOR
    icon: mdi:cctv
```

---

## Scripts

### Script: Launch App

```yaml
script:
  launch_display_app:
    alias: Launch Display App
    fields:
      device:
        description: Device name or IP
        example: "Living Room (192.168.1.100)"
      app:
        description: App with package name
        example: "Chrome (com.android.chrome)"
    sequence:
      - variables:
          # Extract IP from device string
          device_ip: >
            {% if "All Devices" in device %}
              ""
            {% else %}
              {{ device.split("(")[1].split(")")[0] }}
            {% endif %}
          # Extract package name from app string
          package_name: >
            {{ app.split("(")[1].split(")")[0] }}
      - choose:
          # Launch on all devices
          - conditions:
              - condition: template
                value_template: "{{ device_ip == '' }}"
            sequence:
              - service: rest_command.launch_app_device
                data:
                  device_ip: "192.168.1.100"
                  package_name: "{{ package_name }}"
              - service: rest_command.launch_app_device
                data:
                  device_ip: "192.168.1.101"
                  package_name: "{{ package_name }}"
              - service: rest_command.launch_app_device
                data:
                  device_ip: "192.168.1.102"
                  package_name: "{{ package_name }}"
        # Launch on specific device
        default:
          - service: rest_command.launch_app_device
            data:
              device_ip: "{{ device_ip }}"
              package_name: "{{ package_name }}"
```

### Script: Launch YouTube Video

```yaml
script:
  launch_youtube_video:
    alias: Launch YouTube Video
    fields:
      device_ip:
        description: Device IP address
        example: "192.168.1.100"
      video_id:
        description: YouTube video ID
        example: "dQw4w9WgXcQ"
    sequence:
      - service: rest_command.launch_app_with_intent
        data:
          device_ip: "{{ device_ip }}"
          package_name: "com.google.android.youtube"
          action: "android.intent.action.VIEW"
          data: "vnd.youtube://{{ video_id }}"
          extra_string: ""
```

### Script: Launch Camera App with Specific Camera (NEW)

```yaml
script:
  launch_camera_view:
    alias: Launch Camera View
    fields:
      device_ip:
        description: Device IP address
        example: "192.168.1.100"
      camera_name:
        description: Camera name to display
        example: "FRONTDOOR"
    sequence:
      - service: rest_command.launch_app_with_intent
        data:
          device_ip: "{{ device_ip }}"
          package_name: "com.tpn.streamviewer"
          action: "android.intent.action.MAIN"
          data: ""
          extra_string: "camera_name:{{ camera_name }}"
```

### Script: Open URL

```yaml
script:
  display_open_url:
    alias: Display Open URL
    fields:
      device_ip:
        description: Device IP address
        example: "192.168.1.100"
      url:
        description: URL to open
        example: "https://example.com"
    sequence:
      - service: rest_command.launch_app_with_intent
        data:
          device_ip: "{{ device_ip }}"
          package_name: "com.android.chrome"
          action: "android.intent.action.VIEW"
          data: "{{ url }}"
          extra_string: ""
```

---

## Automation Examples

### Example 1: Schedule-Based Content Rotation

```yaml
automation:
  - alias: Display Schedule - Morning News
    trigger:
      platform: time
      at: "07:00:00"
    action:
      service: script.launch_display_app
      data:
        device: "Living Room (192.168.1.100)"
        app: "YouTube (com.google.android.youtube)"
  - alias: Display Schedule - Evening Netflix
    trigger:
      platform: time
      at: "19:00:00"
    action:
      service: script.launch_display_app
      data:
        device: "Living Room (192.168.1.100)"
        app: "Netflix (com.netflix.mediaclient)"
```

### Example 2: Motion-Activated Display

```yaml
automation:
  - alias: Display Wake on Motion
    trigger:
      platform: state
      entity_id: binary_sensor.living_room_motion
      to: "on"
    condition:
      condition: time
      after: "06:00:00"
      before: "23:00:00"
    action:
      service: script.launch_display_app
      data:
        device: "Living Room (192.168.1.100)"
        app: "Home Assistant (io.homeassistant.companion.android)"
```

### Example 3: Presence-Based Automation

```yaml
automation:
  - alias: Display Welcome Home
    trigger:
      platform: state
      entity_id: person.john
      to: "home"
    action:
      service: script.launch_youtube_video
      data:
        device_ip: "192.168.1.100"
        video_id: "{{ states('sensor.favorite_video_id') }}"
```

### Example 4: Doorbell Camera Display (NEW)

```yaml
automation:
  - alias: Show Front Door Camera on Doorbell
    trigger:
      platform: state
      entity_id: binary_sensor.doorbell
      to: "on"
    action:
      service: script.launch_camera_view
      data:
        device_ip: "192.168.1.100"
        camera_name: "FRONTDOOR"
```

### Example 5: Manual Control Dashboard

```yaml
# In your Lovelace dashboard
type: vertical-stack
cards:
  - type: entities
    title: Display Launcher Control
    entities:
      - entity: input_select.display_launcher_device
      - entity: input_select.display_launcher_app
      - entity: input_select.camera_name
  - type: horizontal-stack
    cards:
      - type: button
        name: Launch App
        tap_action:
          action: call-service
          service: script.launch_display_app
          service_data:
            device: "{{ states('input_select.display_launcher_device') }}"
            app: "{{ states('input_select.display_launcher_app') }}"
      - type: button
        name: Launch Camera
        tap_action:
          action: call-service
          service: script.launch_camera_view
          service_data:
            device_ip: "192.168.1.100"
            camera_name: "{{ states('input_select.camera_name') }}"
```

### Example 6: Rotate YouTube Live Streams

```yaml
automation:
  - alias: Rotate Live Streams
    trigger:
      platform: time_pattern
      minutes: "/15" # Every 15 minutes
    action:
      service: script.launch_youtube_video
      data:
        device_ip: "192.168.1.100"
        video_id: "{{ states('sensor.current_live_stream') }}"
```

---

## Advanced Examples

### Multi-Device Synchronized Launch

```yaml
script:
  launch_all_displays_sync:
    alias: Launch All Displays (Synchronized)
    fields:
      app:
        description: App package name
        example: "com.android.chrome"
    sequence:
      - parallel:
          - service: rest_command.launch_app_device
            data:
              device_ip: "192.168.1.100"
              package_name: "{{ app }}"
          - service: rest_command.launch_app_device
            data:
              device_ip: "192.168.1.101"
              package_name: "{{ app }}"
          - service: rest_command.launch_app_device
            data:
              device_ip: "192.168.1.102"
              package_name: "{{ app }}"
```

### Dynamic YouTube Playlist Rotation

```yaml
automation:
  - alias: YouTube Playlist Rotation
    trigger:
      platform: time_pattern
      minutes: "/10"
    action:
      service: script.launch_youtube_video
      data:
        device_ip: "192.168.1.100"
        video_id: >
          {% set videos = [
            'dQw4w9WgXcQ',
            'oHg5SJYRHA0',
            'L_jWHffIx5E'
          ] %}
          {{ videos | random }}
```

### Conditional Content Based on Time

```yaml
automation:
  - alias: Smart Display Content
    trigger:
      platform: time_pattern
      hours: "*"
    action:
      choose:
        # Morning: News
        - conditions:
            - condition: time
              after: "06:00:00"
              before: "12:00:00"
          sequence:
            - service: script.launch_display_app
              data:
                device: "Living Room (192.168.1.100)"
                app: "YouTube (com.google.android.youtube)"
        # Afternoon: Home Dashboard
        - conditions:
            - condition: time
              after: "12:00:00"
              before: "18:00:00"
          sequence:
            - service: script.launch_display_app
              data:
                device: "Living Room (192.168.1.100)"
                app: "Home Assistant (io.homeassistant.companion.android)"
        # Evening: Entertainment
        - conditions:
            - condition: time
              after: "18:00:00"
              before: "23:00:00"
          sequence:
            - service: script.launch_display_app
              data:
                device: "Living Room (192.168.1.100)"
                app: "Netflix (com.netflix.mediaclient)"
```

---

## Intent Extras Integration

### Camera Rotation Across Multiple Displays

Rotate different cameras on multiple displays using intent extras:

```yaml
automation:
  - alias: Camera Rotation - 3 Displays
    trigger:
      platform: time_pattern
      minutes: "/5" # Every 5 minutes
    action:
      - parallel:
          # Device 1: FRONTDOOR
          - service: rest_command.launch_app_with_intent
            data:
              device_ip: "192.168.1.100"
              package_name: "com.tpn.streamviewer"
              action: "android.intent.action.MAIN"
              data: ""
              extra_string: "camera_name:FRONTDOOR"
          # Device 2: DRIVEWAY
          - service: rest_command.launch_app_with_intent
            data:
              device_ip: "192.168.1.101"
              package_name: "com.tpn.streamviewer"
              action: "android.intent.action.MAIN"
              data: ""
              extra_string: "camera_name:DRIVEWAY"
          # Device 3: BACKYARD
          - service: rest_command.launch_app_with_intent
            data:
              device_ip: "192.168.1.102"
              package_name: "com.tpn.streamviewer"
              action: "android.intent.action.MAIN"
              data: ""
              extra_string: "camera_name:BACKYARD"
      # Wait 5 minutes, then rotate
      - delay:
          minutes: 5
      - parallel:
          # Rotate cameras
          - service: rest_command.launch_app_with_intent
            data:
              device_ip: "192.168.1.100"
              package_name: "com.tpn.streamviewer"
              action: "android.intent.action.MAIN"
              data: ""
              extra_string: "camera_name:DRIVEWAY"
          - service: rest_command.launch_app_with_intent
            data:
              device_ip: "192.168.1.101"
              package_name: "com.tpn.streamviewer"
              action: "android.intent.action.MAIN"
              data: ""
              extra_string: "camera_name:BACKYARD"
          - service: rest_command.launch_app_with_intent
            data:
              device_ip: "192.168.1.102"
              package_name: "com.tpn.streamviewer"
              action: "android.intent.action.MAIN"
              data: ""
              extra_string: "camera_name:FRONTDOOR"
```

### App with Multiple Parameters

Launch app with multiple custom parameters:

```yaml
script:
  launch_app_with_params:
    alias: Launch App with Parameters
    fields:
      device_ip:
        description: Device IP
      package_name:
        description: App package name
      param1:
        description: First parameter
      param2:
        description: Second parameter
    sequence:
      - service: rest_command.launch_app_with_intent
        data:
          device_ip: "{{ device_ip }}"
          package_name: "{{ package_name }}"
          action: "android.intent.action.MAIN"
          data: ""
          extra_string: "param1:{{ param1 }},param2:{{ param2 }}"
```

### Conditional Camera Display

Display different cameras based on conditions:

```yaml
automation:
  - alias: Smart Camera Display
    trigger:
      platform: state
      entity_id: binary_sensor.motion_sensor
      to: "on"
    action:
      service: rest_command.launch_app_with_intent
      data:
        device_ip: "192.168.1.100"
        package_name: "com.tpn.streamviewer"
        action: "android.intent.action.MAIN"
        data: ""
        extra_string: >
          {% if is_state('sun.sun', 'below_horizon') %}
            camera_name:NIGHTVISION
          {% else %}
            camera_name:FRONTDOOR
          {% endif %}
```

---

## Troubleshooting

### REST Command Fails

**Check connection:**

```bash
curl http://192.168.1.100:9091/api/apps
```

**Enable logging:**

```yaml
logger:
  default: info
  logs:
    homeassistant.components.rest_command: debug
```

### App Doesn't Launch

1. Verify package name is correct
2. Check Display Launcher is set as default launcher
3. Test manually via web interface first
4. Check Home Assistant logs

### Intent Extras Not Working

1. Verify `extra_string` format: `key:value,key2:value2`
2. Check that target app supports the extras
3. Test with ADB: `adb shell am start -n package/.Activity --es key value`
4. Enable logging in target app to verify receipt
5. Ensure no special characters in values (use only alphanumeric and underscores)

### Camera Not Loading

1. Verify camera name matches exactly (case-sensitive)
2. Check that camera exists in Stream Viewer configuration
3. Ensure go2rtc server URL is configured in Stream Viewer
4. Test camera manually in Stream Viewer web UI first

### Template Errors

Test templates in Developer Tools → Template:

```yaml
{% set device = "Living Room (192.168.1.100)" %}
{{ device.split("(")[1].split(")")[0] }}
```

### Service Not Found

Restart Home Assistant after adding REST commands:

```
Developer Tools → YAML → Restart
```

---

## Complete Example Configuration

Save as `packages/display_launcher.yaml`:

```yaml
# Display Launcher Integration Package
input_select:
  display_device:
    name: Display Device
    options:
      - All Devices
      - Living Room (192.168.1.100)
      - Kitchen (192.168.1.101)
    initial: All Devices
    icon: mdi:television
  display_app:
    name: Display App
    options:
      - Chrome (com.android.chrome)
      - YouTube (com.google.android.youtube)
      - Stream Viewer (com.tpn.streamviewer)
    icon: mdi:application
  camera_name:
    name: Camera Name
    options:
      - FRONTDOOR
      - DRIVEWAY
      - BACKYARD
    initial: FRONTDOOR
    icon: mdi:cctv

rest_command:
  launch_app_device:
    url: "http://{{ device_ip }}:9091/api/launch"
    method: POST
    content_type: "application/json"
    payload: '{"packageName":"{{ package_name }}"}'
  launch_app_with_intent:
    url: "http://{{ device_ip }}:9091/api/launch-intent"
    method: POST
    content_type: "application/json"
    payload: '{"packageName":"{{ package_name }}","action":"{{ action }}","data":"{{ data }}","extra_string":"{{ extra_string | default("") }}"}'
    timeout: 10

script:
  launch_display:
    alias: Launch Display App
    fields:
      device:
        description: Device
      app:
        description: App
    sequence:
      - variables:
          device_ip: >
            {% if "All" not in device %}
              {{ device.split("(")[1].split(")")[0] }}
            {% else %}
              ""
            {% endif %}
          package_name: >
            {{ app.split("(")[1].split(")")[0] }}
      - service: rest_command.launch_app_device
        data:
          device_ip: "{{ device_ip if device_ip else '192.168.1.100' }}"
          package_name: "{{ package_name }}"
  launch_camera_view:
    alias: Launch Camera View
    fields:
      device:
        description: Device
      camera:
        description: Camera name
    sequence:
      - variables:
          device_ip: >
            {{ device.split("(")[1].split(")")[0] }}
      - service: rest_command.launch_app_with_intent
        data:
          device_ip: "{{ device_ip }}"
          package_name: "com.tpn.streamviewer"
          action: "android.intent.action.MAIN"
          data: ""
          extra_string: "camera_name:{{ camera }}"

automation:
  - alias: Display Morning Routine
    trigger:
      platform: time
      at: "07:00:00"
    action:
      service: script.launch_display
      data:
        device: "Living Room (192.168.1.100)"
        app: "YouTube (com.google.android.youtube)"
  - alias: Show Camera on Motion
    trigger:
      platform: state
      entity_id: binary_sensor.front_door_motion
      to: "on"
    action:
      service: script.launch_camera_view
      data:
        device: "Living Room (192.168.1.100)"
        camera: "FRONTDOOR"
```

---

## Additional Resources

- [Display Launcher API Reference](./API.md)
- [Display Launcher README](./README.md)
- [Home Assistant REST Command Documentation](https://www.home-assistant.io/integrations/rest_command/)
- [Home Assistant Templating](https://www.home-assistant.io/docs/configuration/templating/)
- [Android Intent Documentation](https://developer.android.com/reference/android/content/Intent)
