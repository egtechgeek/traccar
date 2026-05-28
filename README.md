# [Traccar](https://www.traccar.org)

## GL200 / Queclink GV350M improvements

This section documents local changes to Traccar’s **gl200** protocol for newer Queclink devices (especially **GV350M** and related firmware such as `F10413`), including GTFRI report parsing and outbound commands. **Add future GL200/GV350M changelog notes here** (this README section), not in a separate file.

### Adding a device in Traccar

1. **Protocol:** `gl200` (device usually auto-detects).
2. **Identifier:** device **IMEI** (15 digits), e.g. `862599050523883`.
3. **Extra tab → Model:** set to **`GV350M`** to enable:
   - Enhanced **GTFRI** parsing (packed IO status, battery, odometer, etc.)
   - GV350M-style **`AT+GTOUT`** command formatting for engine stop/resume
   - Default command password **`gv350m`** when no override is configured

   If Model is left empty, Traccar can still infer GV350M from the protocol version in reports (e.g. `F10413` → prefix `F1`), but setting **Model** explicitly is recommended for commands.

4. **Device attributes → Add:**
   - **Type:** String  
   - **Name:** `devicePassword`  
   - **Value:** the password configured on the tracker (case-sensitive), e.g. `gv350m` or `gl200`

   Only required when the device password is **not** the default Traccar assumes (see [Command password](#command-password-devicepassword) below). If the default matches your device, you can omit this attribute.

5. **Optional I/O configuration** (device attributes):

   | Name | Type | Purpose |
   |------|------|---------|
   | `immobilizerOutput` | Number | Which digital output (1, 2, or 3) is used for **Engine stop** / **Engine resume**. Default: `1`. |
   | `in1Name`, `in2Name` | String | Friendly label for digital inputs (e.g. `Door`, `Panic`). |
   | `out1Name`, `out2Name`, `out3Name` | String | Friendly label for outputs (e.g. `Immobilizer`, `Buzzer`). |
   | `out2CycleDurationMs` | Number | OUT2 cycle duration in milliseconds (`Duration` field in `AT+GTOUT`, unit = 100 ms). Example: `20000` → `200`. |
   | `out2CycleDurationSec` | Number | OUT2 cycle duration in seconds (fallback). Example: `20` → `200`. |
   | `out2CycleCount` | Number | OUT2 number of cycles (`0` = steady behavior). |
   | `out3CycleDurationMs` | Number | OUT3 cycle duration in milliseconds (preferred). Example: `20000` → `200`. |
   | `out3CycleDurationSec` | Number | OUT3 cycle duration in seconds (fallback). Example: `20` → `200`. |
   | `out3CycleCount` | Number | OUT3 number of cycles (`0` = steady behavior). |
   | `out3PulseDurationMs` | Number | Legacy alias for `out3CycleDurationMs` (still supported). |
   | `out3PulseDurationSec` | Number | Legacy alias for `out3CycleDurationSec` (still supported). |

   On each position, Traccar still sets standard keys `in1`, `in2`, `out1`, `out2`, `out3` (boolean). If a name is configured, the position also includes `in1Name` / `out1Name` (etc.) and an attribute keyed by the friendly name with the same boolean value (e.g. `Immobilizer` = `true`), so you can use it in the UI or computed attributes.

### GTFRI report decoding

#### Problem

Newer GTFRI packets (empty VIN columns, 8-digit cell IDs, tail fields like `,,0.0,,,100,110007`) often failed the legacy regex parser. Traccar then fell back to minimal parsing and exposed only basic GPS fields (hdop, distance, motion, etc.), not packed IO status.

Example tail: `…,04181C01,,0.0,,,100,110007,,,20260527152135,781D$`

- `100` → battery level  
- `110007` → packed IO status (fed into existing `decodeStatus()` → ignition, `in1`/`in2`, `out1`/`out2`, input/output bitmasks)

#### Solution

- **Newer devices** (GV350M family and matching protocol prefixes): comma-split parser (`decodeFriSplit`) aligned with `decodeEri` tail layout.
- **Older devices:** original regex parser (`decodeFriPattern`) unchanged to reduce regression risk.

Routing uses device **Model** and/or protocol version prefix (`F1`, `802004`, `802005`, `80201E`, `5E`, etc.).

#### Model field (reports)

| Source | Behavior |
|--------|----------|
| Traccar device **Model** (UI) | Used first if set |
| Protocol version in message (e.g. `F10413`) | Mapped via `PROTOCOL_MODELS` (e.g. `F1` → `GV350M`) |

The Queclink **device name** field in messages (sometimes `GV350M`) is not copied into Traccar’s Model automatically.

### Commands

#### Built-in commands (UI)

| Command | AT action (simplified) |
|---------|-------------------------|
| Get location | `AT+GTRTO=…,1,…` |
| Reboot | `AT+GTRTO=…,3,…` |
| Identification / version | `AT+GTRTO=…,8,…` |
| Engine stop | `AT+GTOUT=…` – activates the output selected by `immobilizerOutput` (default OUT1) |
| Engine resume | `AT+GTOUT=…` – turns all configured outputs off |
| Get device status | `AT+GTRTO=…,A,…` – **Query IO** → `+RESP:GTIOS` (live digital output bitmask) |
| **Custom** | Sends your full AT string (see below) |

Commands are sent as **plain text** on the open TCP/UDP session. The device must be **online** in Traccar. If it is offline, configure via **SMS** on the device instead.

#### Custom commands

- Type: **Custom**
- Enter the full AT command, including password, e.g.  
  `AT+GTRTO=gv350m,1,,,,,,FFFF$`
- Traccar appends `$` only if you omit the trailing `$`
- Traccar does **not** inject `devicePassword` into custom strings—you must include the password in the command text

Any valid Queclink `AT+…$` string works unchanged (e.g. `AT+GTOUT=gv350m,1,,,0,0,0,0,0,0,,,,,,,0,0,0,0,,,0,FFFF$`). Built-in engine stop/resume templates do not apply to **Custom**.

#### GTOUT format by model

| Model family | GTOUT template |
|--------------|----------------|
| GV350M, GV500MAP, GV58LAU, GV355CEU, GV30CEU, GV600M, GV600MG, GV800W, GV600W | Extended layout: `AT+GTOUT=%s,{out1},,,{out2},0,0,{out3},0,0,,,,,,,0,0,0,0,,,0,FFFF$` |
| Other GL200 devices | Legacy Traccar template |

Set **Model** to `GV350M` (or the appropriate type) so the correct template is used.

On **GV350MG (F104)** the tail must be `0,0,0,0,,,0` before `FFFF$`. The older support-doc tail `7,0,0,0,,,FFFF` is accepted on some builds but **did not drive OUT1** on this unit (no `+ACK:GTOUT` / no relay). Bench-confirmed working strings are in the table below.

### Digital output control (GV350M) — Queclink official commands

Commands below are from **Queclink Technical Support**. Traccar sends them as **Custom** commands (device must be **online**). The encoder uses the same strings for **Engine stop** / **Engine resume** when Model = `GV350M`.

#### One-time: configure Output 3 (pin 7)

```text
AT+GTCFG=gv350m,,GV350M,0,0,,,003F,2,,14E3,1,1,0,300,20,,0,0,001F,0,,,,,,24,10,5,,0,0001,FFFF$
```

#### Output ON/OFF (`AT+GTOUT`)

| Action | Command |
|--------|---------|
| OUT1 ON | `AT+GTOUT=gv350m,1,,,0,0,0,0,0,0,,,,,,,0,0,0,0,,,0,FFFF$` |
| OUT1 OFF | `AT+GTOUT=gv350m,0,,,0,0,0,0,0,0,,,,,,,0,0,0,0,,,0,FFFF$` |
| OUT2 ON | `AT+GTOUT=gv350m,0,,,1,0,0,0,0,0,,,,,,,0,0,0,0,,,0,FFFF$` |
| OUT2 OFF | `AT+GTOUT=gv350m,0,,,0,0,0,0,0,0,,,,,,,0,0,0,0,,,0,FFFF$` |
| OUT3 ON | `AT+GTOUT=gv350m,0,,,0,0,0,1,0,0,,,,,,,0,0,0,0,,,0,FFFF$` |
| OUT3 OFF | `AT+GTOUT=gv350m,0,,,0,0,0,0,0,0,,,,,,,0,0,0,0,,,0,FFFF$` |
| OUT2 and OUT3 cycled | `AT+GTOUT=gv350m,0,,,1,200,5,0,200,5,,,,,,,0,0,0,0,,,0,FFFF$` |
| All 3 ON | `AT+GTOUT=gv350m,1,,,1,0,0,1,0,0,,,,,,,0,0,0,0,,,0,FFFF$` |
| All 3 OFF | `AT+GTOUT=gv350m,0,,,0,0,0,0,0,0,,,,,,,0,0,0,0,,,0,FFFF$` |

Note: every **OFF** row uses the same string (all three slots = 0). That is normal Queclink format, not a different command type.

#### OUT3: steady ON vs pulsed (critical)

OUT2/OUT3 slot layout in `AT+GTOUT` (after password): `out1,,,out2,duration2,cycles2,out3,duration3,cycles3`.

Where:
- `duration2` = OUT2 cycle duration (x100 ms)
- `cycles2` = OUT2 cycle count
- `duration3` = OUT3 cycle duration (x100 ms)
- `cycles3` = OUT3 cycle count

| Mode | OUT3 fields | Behavior (GV350MG F104, bench) |
|------|-------------|--------------------------------|
| **Steady ON** | `...,0,0,0,1,0,0,...` | Relay stays ON until an explicit all-off command |
| **Steady OFF** | all outputs `0` (same as OUT3 OFF row above) | Relay OFF |
| **Pulsed** | `...,0,0,0,1,{N},0,...` | Relay **cycles**: ON for ~**N/10 seconds** (e.g. `200` ≈ **20 s ON**), then OFF, then ON again, repeating until superseded by another `AT+GTOUT` |

Example pulsed OUT3 (≈20 s ON per cycle):

```text
AT+GTOUT=gv350m,0,,,0,0,0,1,200,0,,,,,,,0,0,0,0,,,0,FFFF$
```

`Duration` fields are in 100 ms units. Traccar now supports device attributes:
- `out2CycleDurationMs` / `out3CycleDurationMs` (preferred): `ticks = ms / 100`
- `out2CycleDurationSec` / `out3CycleDurationSec` (fallback): `ticks = sec * 10`
- `out2CycleCount` / `out3CycleCount`: number of cycles
- Legacy aliases `out3PulseDurationMs` / `out3PulseDurationSec` are still accepted
- Default when unset: duration/count `0` (steady ON/OFF behavior)

**Do not** use a non-zero duration on OUT3 for immobilizer or “hold relay closed” — use **OUT1** or **OUT2** with `0` duration for steady output. To stop pulsing, send the all-off command (or steady OUT3 ON/OFF as needed).

GTFRI may show `110004` / `110000` on alternating minutes while pulsing; use **Query IO** or listen for relay clicks to verify real pin state.

#### Serial programmer vs Traccar `AT+GTOUT`

| Path | What it does | Relay |
|------|----------------|-------|
| **USB serial tool** (Enable/Disable on OUT1) | Writes configuration / drives the pin on the bench link | Clicks immediately — proves coil wiring and OUT1 hardware |
| **Traccar `AT+GTOUT` over cellular** | One ASCII command on the open `gl200` TCP session | Clicks only if the device returns **`+ACK:GTOUT`** and (ideally) **`GTDOS ,,1,1`** or **GTIOS `01`** |

Traccar **does not parse** commands on the way out; the log line `gl200 > … AT+GTOUT=…` is the exact bytes sent. **Parsing only affects** how `out1`/`out2`/`out3` appear in the UI after **GTIOS/GTDOS/GTFRI** — it cannot block the relay.

If serial works but OTA does not, check **GTOUT tail** (`0,0,0,0,,,0` vs `7,0,0,0`), firmware output enabled, `GTCFG` for OUT3, serial cable unplugged, and that **GTFRI** reporting is running (not heartbeat/GTINF only).

**Traccar command delivery (this build):**
- When the device is offline, commands are queued.
- After each inbound report, this build sends **at most one** queued command per report so Queclink is not flooded.
- After each non-`getDeviceStatus` command is sent, Traccar now automatically sends `getDeviceStatus` (`AT+GTRTO=...,A`) so live I/O state refreshes without a second manual click.

#### Relay wiring (GV350M / GV350MG)

Queclink digital outputs are **open-drain** (low-side switch), **not** a +12 V current source into the coil.

| Wrong assumption | Actual behavior |
|------------------|-----------------|
| Output pin supplies **+12 V / “75 mA positive”** to the relay coil | Output pin **sinks to ground** when ON (ENABLE state) |
| Coil (+) to OUTx, coil (−) to vehicle ground | Coil **(+)** to fused **+12 V**, coil **(−)** to **OUTx** (or module IN that goes low) |

When the output is ON, the tracker connects the pin toward **GND** so current flows **+12 V → coil → output pin → device ground**. If polarity is reversed, the relay will not pull in even when Traccar shows OUT1/OUT2/OUT3 **Yes** and the log has `+ACK:GTOUT`.

Typical hookup for a bare relay coil:

```text
  +12 V (fused, ignition-switched if immobilizer) ----[ coil + ]----[ coil - ]---- OUT1 (or OUT2/OUT3)
                                                                              |
                                                                         GV350M GND
```

Use a relay or relay module rated for **open-drain / low-side** drive. Queclink specifies on the order of **150 mA max** per output (check the GV350M/GV350MG hardware manual for your revision). Flyback protection (diode or module with built-in protection) is recommended.

In the serial programmer: set each used output to **digital output**, **waveform 1** (steady ON/OFF), and the desired **ENABLE/DISABLE** resting state, then save configuration to the device.

#### Saved commands in Traccar (database)

Pre-defined rows live in `tc_commands` (Settings → Saved commands, or linked per device). Install on a new server:

```bash
mysql traccar < setup/gv350m-saved-commands.sql
# then link command ids to your device in tc_device_command
```

On this server, device `862599050523883` already has commands **Output 1 ON/OFF** through **Query IO status** linked in the UI.

#### Firmware programmer (ENABLE / DISABLE STATE)

The tool only offers **DISABLE STATE** or **ENABLE STATE** per output — that is the resting logical level Queclink reports in GTFRI (`110000` = all disable, `110001` = OUT1 enable, etc.). There is no separate “default” menu.

Use **DISABLE STATE** for outputs that should be off at rest; use **`AT+GTOUT`** from Traccar to turn outputs on temporarily.

#### Traccar device settings

| Field | Value |
|-------|--------|
| Protocol | `gl200` |
| Model | `GV350M` |
| `immobilizerOutput` | `1`, `2`, or `3` for Engine stop/resume |
| `out1Name`, `out2Name`, `out3Name` | Optional UI labels |

#### Output status in Traccar (when it updates)

| Source | Speed | Attributes |
|--------|-------|------------|
| **`+RESP:GTFRI`** | ~every 60s | `out1`/`out2`/`out3` from Device Status hex (`110000`–`110007`) |
| **`+RESP:GTDOS`** | After output changes (when the device sends them) | Updates `out1`/`out2`/`out3` on a new position |
| **`+RESP:GTIOS`** | After **Query IO status** command | Full output bitmask from device |

**Query IO status** (built-in command, Model = `GV350M`): sends `AT+GTRTO=gv350m,A,,,,,,FFFF$` → `+RESP:GTIOS` with actual digital output bitmask (`00`–`07`). Use this whenever the map shows wrong output state.

Periodic **GTFRI** `110007` reflects firmware ENABLE/DISABLE configuration, not pin state. On GV350M, Traccar **does not** set outputs from GTFRI; it only carries forward outputs from the last **GTIOS** or **GTDOS**. Until you run **Get device status** once, the map may show stale output fields from older data—run Query IO to refresh.

Success checks: `+ACK:GTOUT` in the log; GTFRI hex `...,100,11000X,...`; or a **GTDOS** position right after toggling an output.

#### Troubleshooting GTOUT (no `+ACK`, outputs unchanged)

| Symptom | Likely cause | Fix |
|---------|----------------|-----|
| Command sent, no `+ACK:GTOUT` | OUT3 not configured on pin 7 | Run **Configure Output 3** once; wait for `+ACK:GTCFG` |
| ACK OK, GTFRI still `110007` | Programmer **ENABLE STATE** on all outputs | Set resting state to **DISABLE**; GTFRI reflects firmware default, not momentary GTOUT |
| ACK OK, UI slow to update | Only periodic GTFRI | Run **Query IO status** or wait for **GTDOS**; redeploy jar with `decodeDos` / `decodeIos` |
| ACK OK, relay does not click | Coil wired for **+V from output** (reverse polarity) | See **Relay wiring** — output **sinks GND**, not sources +12 V |
| OFF commands look identical | Normal | All OFF use `0,,,0,0,0,0,0,0` |

After **Configure Output 3**, **All outputs ON** should log `+ACK:GTOUT` and three `+RESP:GTDOS` lines (outputs 1–3). Traccar then updates `out1`/`out2`/`out3` on those positions without waiting for the next minute report.

#### Deploy

Deploy **`tracker-server.jar` and all `lib/*.jar` together** from this tree (GTFRI parser, GTOUT encoder, **GTDOS/GTIOS** decoders).

### Command password (`devicePassword`)

Queclink AT commands require the tracker password as the first parameter:

`AT+GTRTO=**password**,1,,,,,,FFFF$`

#### Resolution order (highest wins)

1. Password already on the command object (rare for UI commands)
2. Device attribute **`devicePassword`** (String) — per device in UI
3. Server config **`gl200.devicePassword`** in `traccar.xml`
4. **Model-based default** in the encoder (when 2 and 3 are unset)

#### Model-based defaults

| Device model (Traccar) | Default password |
|------------------------|------------------|
| GV350M, GV355CEU, GV30CEU, GV58LAU, GV600M, GV600MG, GV800W, GV600W | `gv350m` |
| GV500MAP | `gv500map` |
| Other / model empty | `gl200` |

#### Change vs stock Traccar

Previously, missing `devicePassword` produced an **empty** password (`AT+GTOUT=,1,…`), which most devices reject. This build uses sensible defaults (`gl200` / `gv350m` / `gv500map`) based on model.

#### Optional server-wide override

In `traccar.xml`:

```xml
<entry key='gl200.devicePassword'>yourPassword</entry>
```

Applies to all gl200 devices without a per-device `devicePassword` attribute.

### Files changed

| File | Changes |
|------|---------|
| `Gl200TextProtocolDecoder.java` | Dual GTFRI parsers; `decodeFriTail`; `decodeDos` / `decodeIos` for live output status |
| `Gl200ProtocolEncoder.java` | Model-aware GTOUT; password defaults; `immobilizerOutput`; OUT2/OUT3 duration and cycle count attributes |
| `Gl200Io.java` | I/O keys, immobilizer output, OUT2/OUT3 cycle helpers, friendly names, output bitmask helpers |
| `setup/gv350m-saved-commands.sql` | Queclink official Custom commands for `tc_commands` |
| `Gl200Protocol.java` | Registers supported commands and auto-sends Query IO (`getDeviceStatus`) after each non-status command |
| `Gl200TextProtocolDecoderTest.java` | GV350M GTFRI sample test |
| `Gl200ProtocolEncoderTest.java` | Encoder tests (including OUT2/OUT3 duration + cycle count) |

### Build and test

Requires **JDK 17+**.

```powershell
cd <traccar-root>
.\gradlew test --tests "org.traccar.protocol.Gl200TextProtocolDecoderTest"
.\gradlew test --tests "org.traccar.protocol.Gl200ProtocolEncoderTest"
.\gradlew build
```

### Verification checklist

- [ ] Device online; send **Get location** or **Reboot**; confirm `+ACK:GTRTO` (or command result) in server logs
- [ ] GTFRI reports show `batteryLevel`, `ignition`, `in1`/`out1`, etc. when IO field is present
- [ ] Firmware defaults: idle GTFRI shows **`110000`**, not `110007`, when outputs should be off
- [ ] Set **`immobilizerOutput`** (Number `1`–`3`) to match the wired output; set **`out1Name`** / **`out2Name`** / **`out3Name`** for labels
- [ ] **Engine stop/resume** on immobilizer OUT; **Custom** `AT+GTOUT` for other outputs
- [ ] After OUT1 ON, next GTFRI has **`110001`** and UI `out1` = Yes only
- [ ] If commands fail, set **`devicePassword`** (String) to match the unit’s configured password

### References

- Queclink @Track Air Interface Protocol (GL200 / GV350M PDFs on [traccar.org/protocol](https://www.traccar.org/protocol/))
- Traccar forum: GL200 commands are sent as plain AT text while the device is connected

## Overview

Traccar is an open source GPS tracking system. This repository contains Java-based back-end service. It supports more than 200 GPS protocols and more than 2000 models of GPS tracking devices. Traccar can be used with any major SQL database system. It also provides easy to use [REST API](https://www.traccar.org/traccar-api/).

Other parts of Traccar solution include:

- [Traccar web app](https://github.com/traccar/traccar-web)
- [Traccar Manager app](https://github.com/traccar/traccar-manager)

There is also a set of mobile apps that you can use for tracking mobile devices:

- [Traccar Client app](https://github.com/traccar/traccar-client)

## Features

Some of the available features include:

- Real-time GPS tracking
- Driver behaviour monitoring
- Detailed and summary reports
- Geofencing functionality
- Alarms and notifications
- Account and device management
- Email and SMS support

## Build

Please read [build from source documentation](https://www.traccar.org/build/) on the official website.

## Team

- Anton Tananaev ([anton@traccar.org](mailto:anton@traccar.org))
- Andrey Kunitsyn ([andrey@traccar.org](mailto:andrey@traccar.org))

## License

    Apache License, Version 2.0

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
