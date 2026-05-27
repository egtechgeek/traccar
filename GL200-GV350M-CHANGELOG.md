# GL200 / Queclink GV350M improvements

This document describes local changes to Traccar’s **gl200** protocol for newer Queclink devices (especially **GV350M** and related firmware such as `F10413`), including GTFRI report parsing and outbound commands.

---

## Adding a device in Traccar

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

   On each position, Traccar still sets standard keys `in1`, `in2`, `out1`, `out2`, `out3` (boolean). If a name is configured, the position also includes `in1Name` / `out1Name` (etc.) and an attribute keyed by the friendly name with the same boolean value (e.g. `Immobilizer` = `true`), so you can use it in the UI or computed attributes.

---

## GTFRI report decoding

### Problem

Newer GTFRI packets (empty VIN columns, 8-digit cell IDs, tail fields like `,,0.0,,,100,110007`) often failed the legacy regex parser. Traccar then fell back to minimal parsing and exposed only basic GPS fields (hdop, distance, motion, etc.), not packed IO status.

Example tail: `…,04181C01,,0.0,,,100,110007,,,20260527152135,781D$`

- `100` → battery level  
- `110007` → packed IO status (fed into existing `decodeStatus()` → ignition, `in1`/`in2`, `out1`/`out2`, input/output bitmasks)

### Solution

- **Newer devices** (GV350M family and matching protocol prefixes): comma-split parser (`decodeFriSplit`) aligned with `decodeEri` tail layout.
- **Older devices:** original regex parser (`decodeFriPattern`) unchanged to reduce regression risk.

Routing uses device **Model** and/or protocol version prefix (`F1`, `802004`, `802005`, `80201E`, `5E`, etc.).

### Model field (reports)

| Source | Behavior |
|--------|----------|
| Traccar device **Model** (UI) | Used first if set |
| Protocol version in message (e.g. `F10413`) | Mapped via `PROTOCOL_MODELS` (e.g. `F1` → `GV350M`) |

The Queclink **device name** field in messages (sometimes `GV350M`) is not copied into Traccar’s Model automatically.

---

## Commands

### Built-in commands (UI)

| Command | AT action (simplified) |
|---------|-------------------------|
| Get location | `AT+GTRTO=…,1,…` |
| Reboot | `AT+GTRTO=…,3,…` |
| Identification / version | `AT+GTRTO=…,8,…` |
| Engine stop | `AT+GTOUT=…` – activates the output selected by `immobilizerOutput` (default OUT1) |
| Engine resume | `AT+GTOUT=…` – turns all configured outputs off |
| **Custom** | Sends your full AT string (see below) |

Commands are sent as **plain text** on the open TCP/UDP session. The device must be **online** in Traccar. If it is offline, configure via **SMS** on the device instead.

### Custom commands

- Type: **Custom**
- Enter the full AT command, including password, e.g.  
  `AT+GTRTO=gv350m,1,,,,,,FFFF$`
- Traccar appends `$` only if you omit the trailing `$`
- Traccar does **not** inject `devicePassword` into custom strings—you must include the password in the command text

### GTOUT format by model

| Model family | GTOUT template |
|--------------|----------------|
| GV350M, GV500MAP, GV58LAU, GV355CEU, GV30CEU, GV600M, GV600MG, GV800W, GV600W | Extended (GV350M manual layout, trailing `0,2,0,0,,,` before serial) |
| Other GL200 devices | Legacy Traccar template |

Set **Model** to `GV350M` (or the appropriate type) so the correct template is used.

---

## Command password (`devicePassword`)

Queclink AT commands require the tracker password as the first parameter:

`AT+GTRTO=**password**,1,,,,,,FFFF$`

### Resolution order (highest wins)

1. Password already on the command object (rare for UI commands)
2. Device attribute **`devicePassword`** (String) — per device in UI
3. Server config **`gl200.devicePassword`** in `traccar.xml`
4. **Model-based default** in the encoder (when 2 and 3 are unset)

### Model-based defaults

| Device model (Traccar) | Default password |
|------------------------|------------------|
| GV350M, GV355CEU, GV30CEU, GV58LAU, GV600M, GV600MG, GV800W, GV600W | `gv350m` |
| GV500MAP | `gv500map` |
| Other / model empty | `gl200` |

### Change vs stock Traccar

Previously, missing `devicePassword` produced an **empty** password (`AT+GTOUT=,1,…`), which most devices reject. This build uses sensible defaults (`gl200` / `gv350m` / `gv500map`) based on model.

### Optional server-wide override

In `traccar.xml`:

```xml
<entry key='gl200.devicePassword'>yourPassword</entry>
```

Applies to all gl200 devices without a per-device `devicePassword` attribute.

---

## Files changed

| File | Changes |
|------|---------|
| `Gl200TextProtocolDecoder.java` | Dual GTFRI parsers; `decodeFriTail`; `useFriSplitDecoder()` |
| `Gl200ProtocolEncoder.java` | Model-aware GTOUT; password defaults; custom commands; `immobilizerOutput` |
| `Gl200Io.java` | Shared I/O attribute keys, immobilizer output, friendly names on positions |
| `Gl200Protocol.java` | Registers `custom` command type |
| `Gl200TextProtocolDecoderTest.java` | GV350M GTFRI sample test |
| `Gl200ProtocolEncoderTest.java` | Encoder tests |

`Gl200ProtocolDecoder.java` is unchanged (text vs binary routing only).

---

## Build and test

Requires **JDK 17+**.

```powershell
cd <traccar-root>
.\gradlew test --tests "org.traccar.protocol.Gl200TextProtocolDecoderTest"
.\gradlew test --tests "org.traccar.protocol.Gl200ProtocolEncoderTest"
.\gradlew build
```

---

## Verification checklist

- [ ] Device online; send **Get location** or **Reboot**; confirm `+ACK:GTRTO` (or command result) in server logs
- [ ] GTFRI reports show `batteryLevel`, `ignition`, `in1`/`out1`, etc. when IO field is present
- [ ] Set **`immobilizerOutput`** (Number `1`–`3`) to match the wired output; set **`out2Name`** (etc.) for labels
- [ ] **Engine stop/resume** with immobilizer on the configured output, or use custom `AT+GTOUT=…` if needed
- [ ] If commands fail, set **`devicePassword`** (String) to match the unit’s configured password

---

## References

- Queclink @Track Air Interface Protocol (GL200 / GV350M PDFs on [traccar.org/protocol](https://www.traccar.org/protocol/))
- Traccar forum: GL200 commands are sent as plain AT text while the device is connected
