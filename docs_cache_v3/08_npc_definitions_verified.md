# 08. NPC Definitions: Opcode Specification

## Overview
NPC definitions manage the 3D models, combat levels, animations, and right-click options for all Non-Player Characters. They are stored in `npc.dat` and indexed by `npc.idx` (Relative Lengths).

---

## 1. The Parser Loop
The client reads an `UnsignedByte` opcode. If it is `0`, the definition is finished.

---

## 2. Verified Opcodes (Mystic Client)

| Opcode | Data Type | Field | Description |
| :--- | :--- | :--- | :--- |
| **1** | `UByte`, `UInt16[]`| `models` | `Len`, then `Len` 3D model IDs for the NPC body. |
| **2** | `String` | `name` | The display name. |
| **3** | `Byte[]`* | `description`| Examine text (read until byte `10`). |
| **12** | `Int8` | `size` | Tiles occupied (e.g., 1 for 1x1, 3 for 3x3). |
| **13** | `UInt16` | `stand_anim` | Idle animation ID. |
| **14** | `UInt16` | `walk_anim` | Moving animation ID. |
| **17** | `UInt16[4]` | `movement` | Walk, Turn180, Turn90CW, Turn90CCW animations. |
| **30-39**| `String` | `actions` | Right-click options (Index `op - 30`). |
| **40** | `UByte`, `UInt16[]`| `colors` | `Len`, then `Len` pairs of (Old Color, New Color). |
| **60** | `UByte`, `UInt16[]`| `add_models` | Additional models layered onto the body. |
| **90-92**| `UInt16` | (Dummy) | Read and discarded. |
| **93** | (None) | `minimap` | If present, `drawMinimapDot = false`. |
| **95** | `UInt16` | `combat` | Combat level displayed in menu. |
| **97** | `UInt16` | `scale_xz` | Width scale (Default 128). |
| **98** | `UInt16` | `scale_y` | Height scale (Default 128). |
| **99** | (None) | `priority` | If present, `priorityRender = true`. |
| **100** | `Int8` | `ambient` | Ambient light modifier. |
| **101** | `Int8` | `contrast` | Contrast/shadow (Value multiplied by 5). |
| **102** | `UInt16` | `headicon` | Sprite ID for overhead icons. |
| **103** | `UInt16` | `degrees` | Rotation degrees (Default 32). |
| **106 / 118**| `Mixed` | `morphed` | Varp, Varbit, and child IDs for transformations. |
| **109** | (None) | `clickable` | If present, `clickable = false`. |
| **107 / 111**| (None) | (Dummy) | Empty opcodes (Ignored). |

*\*Note: Opcode 3 reads description as raw bytes via `readBytes()` until terminator `10`, unlike Item/Object which use `readString()`.*

---

## 3. Transformation Logic (Opcode 106/118)
Similar to objects, NPCs can change appearance based on player state.
1. Read `varbit` (`UInt16`). If `0xFFFF`, set to -1.
2. Read `varp` (`UInt16`). If `0xFFFF`, set to -1.
3. If Opcode 118: Read `default_id` (`UInt16`).
4. Read `count` (`UByte`).
5. Read `count + 1` child IDs (`UInt16`).

---

## 4. Packing Constraints (Verified)
- **14-Bit ID Limit**: The Mystic Client's `NpcUpdating` protocol is hardcoded to 14 bits for the NPC ID. This restricts your cache to a maximum of **16,383** NPCs. Exceeding this will cause client-side overflows and crashes during rendering.
- **Model Compatibility**: NPCs often share models with players (e.g., weapons, capes). Ensure any added NPC models are correctly weighted for the standard 317 skeleton in `idx2`.
