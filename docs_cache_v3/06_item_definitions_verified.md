# 06. Item Definitions: Opcode Specification

## Overview
Item definitions are stored in `obj.dat` and indexed by `obj.idx` (Relative Lengths). This document provides the byte-perfect opcode mapping verified against the Mystic Client source code.

---

## 1. The Parser Loop
The client reads an `UnsignedByte` opcode. If it is `0`, it finishes the item. Otherwise, it reads the data associated with that opcode.

---

## 2. Verified Opcodes (Mystic Client)

| Opcode | Data Type | Field | Description |
| :--- | :--- | :--- | :--- |
| **1** | `UInt16` | `inventory_model` | The 3D model ID from `idx1` used for inventory/ground. |
| **2** | `String` | `name` | The display name (terminated by `\n`). |
| **3** | `String` | `description` | The examine text (terminated by `\n`). |
| **4** | `UInt16` | `model_zoom` | Inventory render zoom level. |
| **5** | `UInt16` | `rotation_y` | Render pitch. |
| **6** | `UInt16` | `rotation_x` | Render roll. |
| **7** | `UInt16`* | `translate_x` | X-offset in inventory slot (Signed). |
| **8** | `UInt16`* | `translate_yz` | Y/Z-offset in inventory slot (Signed). |
| **10** | `UInt16` | (Dummy) | Read and discarded. |
| **11** | (None) | `stackable` | If present, item is stackable. |
| **12** | `Int32` | `value` | Shop/Alchemy price. |
| **16** | (None) | `is_members` | If present, members only. |
| **23** | `UInt16`, `Int8`| `male_equip_1` | Male primary model and Y-translation. |
| **24** | `UInt16` | `male_equip_2` | Male secondary model (arms). |
| **25** | `UInt16`, `Int8`| `female_equip_1`| Female primary model and Y-translation. |
| **26** | `UInt16` | `female_equip_2`| Female secondary model (arms). |
| **30-34**| `String` | `ground_actions`| Right-click floor options (Index `op - 30`). |
| **35-39**| `String` | `actions` | Right-click inventory options (Index `op - 35`). |
| **40** | `UByte`, `UInt16[]`| `colors` | `Len`, then `Len` pairs of (Old Color, New Color). |
| **78** | `UInt16` | `male_equip_3` | Male tertiary model (emblem). |
| **79** | `UInt16` | `female_equip_3`| Female tertiary model (emblem). |
| **90** | `UInt16` | `male_dialogue_1`| Male primary chathead model. |
| **91** | `UInt16` | `female_dialogue_1`| Female primary chathead model. |
| **92** | `UInt16` | `male_dialogue_2`| Male secondary chathead model. |
| **93** | `UInt16` | `female_dialogue_2`| Female secondary chathead model. |
| **95** | `UInt16` | `rotation_z` | Render yaw. |
| **97** | `UInt16` | `cert_template` | ID of the item this note represents. |
| **98** | `UInt16` | `cert_id` | ID of the noted version of this item. |
| **100-109**| `UInt16`, `UInt16`| `stack_variant` | `VariantID`, `VariantSize`. |
| **110** | `UInt16` | `scale_x` | Model width scale. |
| **111** | `UInt16` | `scale_y` | Model height scale. |
| **112** | `UInt16` | `scale_z` | Model depth scale. |
| **113** | `Int8` | `lighting` | Ambient light modifier. |
| **114** | `Int8` | `shadowing` | Contrast/shadow modifier. |
| **115** | `UInt8` | `team` | Team-cape ID. |
| **139** | `UInt16` | `cert_template_alt`| Custom alternate cert template. |
| **140** | `UInt16` | `cert_id_alt` | Custom alternate cert ID. |

*\*Note: For opcodes 7 and 8, the value is read as an Unsigned Short. If the value is > 32767, you must subtract 65536 to convert it to a negative signed short.*

---

## 3. Note Wrapping Protocol (Implicit Logic)
The client does not store unique definitions for every "Noted" item. Instead, it generates them dynamically.
1.  If `cert_id != -1` AND `cert_template != -1`:
    - Lookup the `cert_template` item (the generic "Bank Note" item).
    - Copy its models and inventory render settings (zoom, rotations).
    - Lookup the `cert_id` item (the original item).
    - Copy its `name`, `value`, and `is_members` flag.
    - Set the description to: `"Swap this note at any bank for a/an [Item Name]."`.

---

## 4. Packing Constraints
- **Model IDs**: Must refer to valid entries in Store 1 (`idx1`).
- **Color Length**: Opcode 40 reads 1 byte for length. You cannot have more than 255 color replacements per item.
- **ID Limit**: While the engine supports 65,535 items theoretically, the Mystic Client's hardcoded +21 offset and UI buffers typically cap the stable ID range around **32,000**.
