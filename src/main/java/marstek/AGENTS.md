# AGENTS.md — src/main/java/marstek/

Agent-oriented documentation for the `marstek/` package (Marstek B2500 battery
integration) in the HomeAuto codebase.

## Overview

This package implements the Marstek B2500 battery integration:

- `B2500.java` — battery gateway: MQTT client to the device and to the cloud
  (via AWS IoT). Publishes cloud device id and subscribes to the device topics.
- `HameApi.java` — cloud API client (login, device list, salt resolution).

## Conventions

- The B2500 device protocol is "hame_energy/{type}/..." topics.
- The cloud (hame-2025) uses "marstek_energy/..." topics.
- The cloud topic id is derived from the device type + MAC (AES encrypted for
  scheme 1 / salt-based for scheme 2).
- Polling: cd=01 requests are sent in a fixed interval (default 5 min). The
  device does not push data on its own, so this is what keeps the values
  fresh. When the web UI was used recently, the interval shortens to 30 s.
- UI_ACTIVE_WINDOW (2 min): UI use accelerates polling for 2 min.
- LOCAL_CLIENT_ID = "homeauto-b2500".
- TOPIC_ID_AES_KEY = "!@#$%^&*()_+{}[]".
- The device connects (via BLE) to the embedded broker.

## Key files

- `B2500.java` — battery gateway (MQTT client to the device + cloud via AWS
  IoT), `HameApi` (cloud API client).
- `HameApi.java` — cloud API client (login, device list, salt resolution).
