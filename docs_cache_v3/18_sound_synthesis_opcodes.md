# 18. Sound Specification: Synthesizer Opcodes

## Overview
Sound effects are packed into `sounds.jag` as a stream of synthesis parameters. This document details the exact byte-level sequence required to pack a sound effect so the client can synthesize it into a 22050Hz Mono PCM wave.

---

## 1. The Master Loop (JAG Archive Payload)
The data file inside `sounds.jag` contains sequential records.
1. Read `UInt16` `effect_id`.
2. If `effect_id == 65535`, stop.
3. For each effect, there are exactly **10 Synthesizers** (oscillators).
4. For each Synthesizer (0 to 9):
    - Read `UInt8` `is_active`.
    - If `is_active == 1`: Parse Synthesizer Data (see Section 2).
5. Read `UInt16` `loop_start`.
6. Read `UInt16` `loop_end`.

---

## 2. Synthesizer Data Block
If an oscillator is active, read these fields in order:

### A. Core Envelopes
1.  **Pitch Envelope**: `decodeEnvelope(stream)`
2.  **Volume Envelope**: `decodeEnvelope(stream)`
3.  **Pitch Modifier**:
    - Read `UInt8` `exists`.
    - If `exists != 0`: Backtrack 1 byte, then `decodeEnvelope(stream)` (Pitch Mod), then `decodeEnvelope(stream)` (Pitch Mod Amplitude).
4.  **Volume Multiplier**:
    - Read `UInt8` `exists`.
    - If `exists != 0`: Backtrack 1 byte, then `decodeEnvelope(stream)` (Vol Multi), then `decodeEnvelope(stream)` (Vol Multi Amplitude).
5.  **Gating (Release/Attack)**:
    - Read `UInt8` `exists`.
    - If `exists != 0`: Backtrack 1 byte, then `decodeEnvelope(stream)` (Release), then `decodeEnvelope(stream)` (Attack).

### B. Oscillator Parameters
Loop 10 times (even though only 5 are currently used by the mixer):
1.  Read `USmart` `volume`.
2.  If `volume == 0`, break loop.
3.  Read `Smart` `pitch_offset`.
4.  Read `USmart` `delay`.

### C. Master Metadata
1.  Read `USmart` `delay_time`.
2.  Read `USmart` `delay_decay`.
3.  Read `UInt16` `duration` (in milliseconds).
4.  Read `UInt16` `offset` (start delay).
5.  **Filter**: `decodeFilter(stream)`

---

## 3. Envelope Specification (`Envelope.java`)
| Byte | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| `0` | `UInt8` | `form` | 0=Linear, 1=Sine, etc. |
| `1` | `Int32` | `start` | Start value. |
| `5` | `Int32` | `end` | End value. |
| `9` | `UInt8` | `segments`| Number of envelope points. |

For each segment:
- `UInt16` `duration`.
- `UInt16` `peak_value`.

---

## 4. Filter Specification (`Filter.java`)
1. Read `UInt8` `pairs`.
2. If `pairs > 0`: `decodeEnvelope(stream)`.
3. Loop 2 times (Forward/Backward):
    - Loop `pairs` times: `UInt16` `coefficient`.

---

## 5. Tool Builder Notes
- **Offset Math**: The `readUSmart` and `readSmart` methods are used heavily to save space in the oscillator loops. 
- **Backtracking**: The client often reads an "existence byte" and then backtracks to parse the envelope if it wasn't zero. Your packer should write `0` if an envelope isn't needed, or `1` followed by the full envelope data.
