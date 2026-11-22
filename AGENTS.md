# AGENTS.md

Agent-oriented documentation for the HomeAuto codebase — a home automation
project that reads EBus data from a Hautec heat pump (TEM controller), SML data
from a smart meter, Shelly UDP meter data, and Marstek B2500 battery data via
an embedded MQTT gateway, and sets the speed of the ventilation system.

## Project overview

- **Runtime**: Java 11 (Maven, pi4j-parent 2.1.0), built as a self-contained
  fat jar with `maven-shade-plugin` (mainClass: `Main`).
- **Target hardware**: Raspberry Pi (pi4j-core 1.4, rxtx, jSerialComm).
- **Purpose**: Home automation — reads EBus data from a Hautec heat pump (TEM
  controller), SML data from a smart meter, Shelly UDP meter data, and Marstek
  B2500 battery data via an embedded MQTT gateway, and sets the speed of the
  ventilation system.

## Architecture

Entry point: `src/main/java/Main.java` → `init/HomeAutoState` wires up all
components (threads implementing `Closeable`) and registers them for shutdown.

Key packages:

- `data/` — `ValueRegistry` (TreeMap of `KnownValueEntry` + queue for `Sender`),
  `KnownValueEntry` (value + text + history of up to 500 entries),
  `DebugRegistry` (EBus raw frames), `Sender` (cloud push thread).
- `ebus/` — EBus serial readers (`HautecHeatingReader`,
  `WestaflexVentilationReader`), `EBusProcessorThread`, serial connections
  (`RPISerialConnection`, `RPIPipedSerialConnection`, `SerialConnection`).
- `sml/` — SML (smart meter) protocol parser and messages.
- `shelly/` — `ShellyUDP`: UDP listener that mimics a Shelly Pro 3EM meter; it
  also reacts to battery state changes (`ValueListener`) and republishes to
  the real Shelly cloud.
- `marstek/` — `B2500` battery gateway (MQTT client to the device + cloud via
  AWS IoT), `HameApi` (cloud API client).
- `mqtt/` — `MqttBroker`: embedded Moquette MQTT broker.
- `ventilation/` — `VentilationWriter`: GPIO control of the ventilation system.
- `ui/` — `HomeAutoWebHandler`: minimal HTTP UI (com.sun.net.httpserver).
- `conn/` — `ConnTest`, `PowerPoll` (background pollers).
- `init/` — `HomeAutoState` wires up all components and registers them for
  shutdown; `HomeUIAccess` exposes actions to the web UI.

`Main.java` creates `HomeAutoState`, starts an `HttpServer` with a
`ThreadPoolExecutor(1, 4, ...)` on port 80 (root) or 8000 (non-root).

## Conventions

- **Value keys**: dotted registry keys (TreeMap) via `ValueRegistry`. Values
  are `KnownValueEntry` (value + text + history of up to 500 entries).
  `setValue(key, value, text)` queues changed values for `Sender` (cloud push);
  `setValueDebug(key, value)` sets debug values only.
- **Value keys**: dotted registry keys (TreeMap) via `ValueRegistry`.
- **Registry keys also used by other components**: `Sender` (cloud push thread)
  reads the `ValueRegistry` queue.
- **Embedded broker**: `mqtt/MqttBroker.java` (Moquette) — no external broker.
- **Value flow**: components (`ValueListener`) → `ValueRegistry` → `Sender`
  (cloud push thread) → web UI.
- **Components**: `Thread` + `Closeable` pattern; registered in
  `HomeAutoState` and closed via shutdown hook.
- **Config**: `homeauto.properties` read from the working directory; defaults
  are auto-written back into the file on first run.
- **Value keys**: dotted registry keys (TreeMap) via `ValueRegistry`.
- **Registry keys also used by other components**: `Sender` (cloud push thread)
  reads the `ValueRegistry` queue.
- **Embedded broker**: `mqtt/MqttBroker.java` (Moquette) — no external broker.
- **Serial drivers**: `ebus/conn/impl/RPISerialConnection`,
  `RPIPipedSerialConnection` via `SerialConnectionFactory`.

## Key files

- `src/main/java/Main.java` — entry point; creates `HomeAutoState`, starts an
  `HttpServer` with a `ThreadPoolExecutor(1, 4, ...)` on port 80 (root) or 8000
  (non-root).
- `src/main/java/init/HomeAutoState.java` — wires up all components and
  registers them for shutdown.
- `src/main/java/marstek/B2500.java` — battery gateway (MQTT client to the
  device + cloud via AWS IoT).
- `src/main/java/marstek/HameApi.java` — cloud API client (login, device list,
  salt resolution).
- `src/main/java/mqtt/MqttBroker.java` — embedded Moquette MQTT broker.
- `src/main/java/ebus/` — EBus serial readers (`HautecHeatingReader`,
  `WestaflexVentilationReader`), `EBusProcessorThread`.
- `src/main/java/sml/` — SML (smart meter) protocol parser and messages.
- `src/main/java/shelly/ShellyUDP.java` — UDP listener that mimics a Shelly
  Pro 3EM meter; also reacts to battery state changes (`ValueListener`).
- `src/main/java/data/` — `ValueRegistry`, `KnownValueEntry`,
  `KnownValueQueueEntry`, `DebugRegistry`, `Sender`, `ValueListener`.
- `src/main/java/ventilation/VentilationWriter.java` — GPIO control of the
  ventilation system.
- `src/main/java/ui/HomeAutoWebHandler.java` — minimal HTTP UI
  (com.sun.net.httpserver).
- `src/main/java/conn/` — `ConnTest`, `PowerPoll` (background pollers).

## Code style

- Java 11 (source/target 11), Maven, pi4j-parent 2.1.0, built as a
  self-contained fat jar with `maven-shade-plugin`.
- Components follow the `Thread` + `Closeable` pattern; registered in
  `HomeAutoState` and closed via shutdown hook.
- Registry keys are dotted strings via `ValueRegistry`; values are
  `KnownValueEntry` (value + text + history of up to 500 entries).
- `setValue(key, value, text)` queues changed values for `Sender` (cloud push);
  `setValueDebug(key, value)` sets debug values only.
- Config: `homeauto.properties` read from the working directory; defaults are
  auto-written back into the file on first run.
