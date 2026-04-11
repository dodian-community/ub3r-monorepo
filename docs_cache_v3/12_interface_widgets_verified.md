# 12. Interface & Widget Specification

## Overview
User Interfaces (UIs) in RuneScape are stored in the `interfaces.jag` archive. The primary logic is found in the `data` file, which is a serialized sequence of **Widgets** (formerly `RSInterface`).

---

## 1. The Global Loader Logic
The `data` file is read sequentially.

### The Parent Switch
If the first `UInt16` read is `65535` (`0xFFFF`):
1. Read the next `UInt16`: This becomes the `default_parent_id`.
2. Read the next `UInt16`: This is the `widget_id` of the first child.
Otherwise, the first `UInt16` is simply the `widget_id`.

---

## 2. Core Widget Metadata (Common to all Types)
| Offset | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| `0` | `UInt16` | `id` | The unique ID of this component. |
| `2` | `UInt8` | `type` | 0=Container, 2=Inventory, 3=Rect, 4=Text, 5=Sprite, 6=Model. |
| `3` | `UInt8` | `action_type` | 1=OK, 2=Usable, 3=Close, 6=Continue. |
| `4` | `UInt16` | `content_type`| Internal game behavior link. |
| `6` | `UInt16` | `width` | Width in pixels. |
| `8` | `UInt16` | `height` | Height in pixels. |
| `10` | `UInt8` | `opacity` | 0 (Opaque) to 255 (Transparent). |
| `11` | `UInt8` | `hover_id_1` | If != 0, reads next byte to form a 16-bit hover link. |

---

## 3. Client Scripts (CS1)
After metadata, the client reads logic operators used for dynamic state (e.g., changing text color if a player is poisoned).

1.  **Operators**: Read `UInt8` `count`. For `count` times:
    - Read `UInt8` `condition_type`
    - Read `UInt16` `required_value`
2.  **Scripts**: Read `UInt8` `count`. For `count` times:
    - Read `UInt16` `instruction_count`.
    - For `instruction_count` times: Read `UInt16` `instruction_id`.

---

## 4. Type-Specific Specifications

### Type 0: Container (Parent)
- `scroll_max`: `UInt16`
- `invisible`: `UInt8` (1=True)
- `child_count`: `UInt16`
- **Children**: Loop `child_count` times:
    - `child_id`: `UInt16`
    - `rel_x`: `Int16` (Signed)
    - `rel_y`: `Int16` (Signed)

### Type 2: Inventory (Item Grid)
- `swappable`: `UInt8`
- `has_actions`: `UInt8`
- `is_usable`: `UInt8`
- `replace_items`: `UInt8`
- `padding_x/y`: `UInt8`, `UInt8`
- **Sprites**: Loop 20 times:
    - `has_sprite`: `UInt8`
    - `off_x/y`: `Int16`, `Int16`
    - `name`: `String` (e.g., `media,10` refers to Sprite 10 in `media.jag`).
- **Actions**: Loop 5 times: `String`.

### Type 4: Text
- `centered`: `UInt8`
- `font_id`: `UInt8`
- `shadowed`: `UInt8`
- `text`: `String`
- `active_text`: `String`
- `colors`: 4x `Int32` (Normal, Active, NormalHover, ActiveHover).

### Type 5: Sprite
- `sprite_1_name`: `String` (e.g., `media,5`)
- `sprite_2_name`: `String`

### Type 6: 3D Model
- `model_1`: `(UByte << 8) + UByte` (Custom bit-shifted read).
- `model_2`: `(UByte << 8) + UByte`
- `anim_1`: `(UByte << 8) + UByte`
- `anim_2`: `(UByte << 8) + UByte`
- `zoom`: `UInt16`
- `rotation_1/2`: `UInt16`, `UInt16`

---

## 5. Tool Builder Warning: Hardcoded Overrides
The Mystic client (like most 317 clients) manually constructs several interfaces in Java **after** the cache is loaded. 
- **Check `Widget.load()`**: Look for calls like `bankInterface()`.
- **Impact**: If you edit the `data` file, you will not see these hardcoded components. You must either edit the Java source or rewrite these interfaces into your packer and remove the Java calls.
