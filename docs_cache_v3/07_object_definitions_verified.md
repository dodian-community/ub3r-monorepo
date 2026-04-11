# 07. Object Definitions: Opcode Specification

## Overview
Object definitions dictate the visual model, size, and interaction properties of all static and dynamic world objects (trees, walls, bank booths). They are stored in `loc.dat` and indexed by `loc.idx` (Relative Lengths).

---

## 1. The Parser Loop
The client reads an `UnsignedByte` opcode. If it is `0`, the definition is finished.

---

## 2. Verified Opcodes (Mystic Client)

| Opcode | Data Type | Field | Description |
| :--- | :--- | :--- | :--- |
| **1** | `UByte`, `(UInt16, UByte)[]`| `models` | `Len`, then `Len` pairs of (Model ID, Model Type). |
| **2** | `String` | `name` | The display name. |
| **3** | `String` | `description`| The examine text. |
| **5** | `UByte`, `UInt16[]`| `model_ids` | `Len`, then `Len` Model IDs (No types). |
| **14** | `UInt8` | `size_x` | Width in tiles (Default 1). |
| **15** | `UInt8` | `size_y` | Length in tiles (Default 1). |
| **17** | (None) | `solid` | If present, sets `solid = false`. |
| **18** | (None) | `impenetrable`| If present, sets `impenetrable = false`. |
| **19** | `UInt8` | `interactive` | If `1`, object has left/right-click options. |
| **21** | (None) | `contoured` | If present, model matches terrain height. |
| **22** | (None) | `delay_shade` | If present, sets `delayShading = true`. |
| **23** | (None) | `occludes` | If present, object occludes camera. |
| **24** | `UInt16` | `animation` | Constant animation ID. `0xFFFF` = -1. |
| **28** | `UInt8` | `decor_disp` | Displacement for wall decorations (Default 16). |
| **29** | `Int8` | `ambient` | Ambient lighting value. |
| **30-34**| `String` | `actions` | Right-click options (Index `op - 30`). |
| **39** | `Int8` | `contrast` | Light diffusion/contrast value. |
| **40** | `UByte`, `UInt16[]`| `colors` | `Len`, then `Len` pairs of (Old Color, New Color). |
| **62** | (None) | `inverted` | If present, model is mirrored. |
| **64** | (None) | `shadow` | If present, sets `castsShadow = false`. |
| **65** | `UInt16` | `scale_x` | Width scale (Default 128). |
| **66** | `UInt16` | `scale_y` | Height scale (Default 128). |
| **67** | `UInt16` | `scale_z` | Depth scale (Default 128). |
| **68** | `UInt16` | `mapscene` | Sprite ID for the minimap icon. |
| **69** | `UInt8` | `surround` | Surrounding/Collision flag. |
| **70** | `Int16` | `trans_x` | X-offset. |
| **71** | `Int16` | `trans_y` | Y-offset. |
| **72** | `Int16` | `trans_z` | Z-offset. |
| **73** | (None) | `obstruct` | If present, sets `obstructsGround = true`. |
| **74** | (None) | `hollow` | If present, sets `hollow = true` (also non-solid). |
| **75** | `UInt8` | `support` | Supports items dropped on top. |
| **77 / 92**| `Mixed` | `morphed` | Varp, Varbit, and child IDs for transformations. |
| **78** | `UInt16`, `UByte` | (Dummy) | Ambient sound ID (Discarded). |
| **79** | `Mixed` | (Dummy) | Sound effect metadata (Discarded). |
| **81** | `UInt8` | (Dummy) | Read and discarded. |
| **82** | `UInt16` | `minimap` | Minimap function ID (with custom math offsets). |

---

## 3. Transformation Logic (Opcode 77/92)
Used for objects that change state (e.g., a lever moving or a gate opening).
1. Read `varp` (`UInt16`). If `0xFFFF`, set to -1.
2. Read `varbit` (`UInt16`). If `0xFFFF`, set to -1.
3. If Opcode 92: Read `default_id` (`UInt16`).
4. Read `count` (`UByte`).
5. Read `count + 1` child IDs (`UInt16`).

---

## 4. Packing Logic
When packing objects, you must ensure that if an object is non-solid (Opcode 17), the server's `CollisionManager` is also updated via `ObjectClipService`. If the cache and server disagree on solidity, players will walk through solid objects or get stuck on invisible ones.
