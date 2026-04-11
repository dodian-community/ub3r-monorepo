# Mystic Cache: Item Definitions (`obj.dat` / `obj.idx`)

## Overview
Item definitions dictate how items appear in the inventory, on the ground, and when equipped. They are packed into two files:
1.  **`obj.idx`**: The index file. It contains the byte offsets for where each item's data begins in the `.dat` file.
2.  **`obj.dat`**: The data file. It contains the actual properties for every item, encoded sequentially using opcodes.

## Initialization & Structure
The client loads these files on startup in `ItemDefinition.init()`:
-   Reads `obj.idx` to calculate `item_count` (total items + 21).
-   Populates `streamIndices` array with offsets.
-   When an item is requested (`ItemDefinition.lookup(id)`), the stream seeks to `streamIndices[id]` and begins reading opcodes.

## Opcode Map
The client reads an unsigned byte (the opcode). If it's `0`, it marks the end of the item's definition. Otherwise, it reads specific data types based on the opcode.

| Opcode | Property Name | Data Type Read | Description |
| :--- | :--- | :--- | :--- |
| **0** | End of Definition | (None) | Tells the parser to stop reading. |
| **1** | `inventory_model` | `UShort` (2 bytes) | The 3D model ID used when the item is in the inventory or on the ground. |
| **2** | `name` | `String` (N bytes) | The name of the item (terminated by byte `10`). |
| **3** | `description` | `String` (N bytes) | The examine text of the item. |
| **4** | `model_zoom` | `UShort` (2 bytes) | How far zoomed in the model is in the inventory interface. |
| **5** | `rotation_y` | `UShort` (2 bytes) | Pitch/rotation. |
| **6** | `rotation_x` | `UShort` (2 bytes) | Roll/rotation. |
| **7** | `translate_x` | `UShort` (2 bytes) | X offset in the inventory slot. (>32767 becomes negative) |
| **8** | `translate_yz` | `UShort` (2 bytes) | Y/Z offset in the inventory slot. (>32767 becomes negative) |
| **10** | (Dummy) | `UShort` (2 bytes) | Read but discarded. |
| **11** | `stackable` | (None) | If present, the item is stackable. |
| **12** | `value` | `Int` (4 bytes) | The base shop/alchemy value. |
| **16** | `is_members_only` | (None) | If present, marks the item as members only. |
| **23** | `equipped_model_male_1` | `UShort`, `SignedByte` | Primary equipped model for males and Y-translation. |
| **24** | `equipped_model_male_2` | `UShort` (2 bytes) | Secondary equipped model (arms/sleeves) for males. |
| **25** | `equipped_model_female_1` | `UShort`, `SignedByte` | Primary equipped model for females and Y-translation. |
| **26** | `equipped_model_female_2`| `UShort` (2 bytes) | Secondary equipped model for females. |
| **30-34** | `groundActions[0-4]` | `String` (N bytes) | The right-click options when the item is on the floor (e.g., "Take"). "hidden" is parsed as `null`. |
| **35-39** | `actions[0-4]` | `String` (N bytes) | The right-click options in the inventory (e.g., "Wield", "Eat"). |
| **40** | `modified_model_colors` | Array of `UShort` tuples | Reads 1 byte for length `L`. Then reads `L` pairs of `UShort`s (original color, replacement color). Used for recoloring models (e.g., God capes). |
| **78** | `equipped_model_male_3` | `UShort` (2 bytes) | Tertiary equipped model (emblems) for males. |
| **79** | `equipped_model_female_3`| `UShort` (2 bytes) | Tertiary equipped model for females. |
| **90** | `equipped_model_male_dialogue_1` | `UShort` (2 bytes) | Primary chathead model for males. |
| **91** | `equipped_model_female_dialogue_1`| `UShort` (2 bytes) | Primary chathead model for females. |
| **92** | `equipped_model_male_dialogue_2` | `UShort` (2 bytes) | Secondary chathead model (hat) for males. |
| **93** | `equipped_model_female_dialogue_2`| `UShort` (2 bytes) | Secondary chathead model (hat) for females. |
| **95** | `rotation_z` | `UShort` (2 bytes) | Yaw/rotation. |
| **97** | `unnoted_item_id` | `UShort` (2 bytes) | The ID of the item this note represents. |
| **98** | `noted_item_id` | `UShort` (2 bytes) | The ID of the noted version of this item. |
| **100-109**| `stack_variant` | Two `UShort`s | Maps a stack size to a different item ID (e.g., coins look different at 1, 2, 3, 4, and 10000). Reads `variant_id` and `variant_size`. |
| **110** | `model_scale_x` | `UShort` (2 bytes) | Model width scale. |
| **111** | `model_scale_y` | `UShort` (2 bytes) | Model height scale. |
| **112** | `model_scale_z` | `UShort` (2 bytes) | Model depth scale. |
| **113** | `light_intensity` | `SignedByte` (1 byte) | Shading/lighting intensity. |
| **114** | `light_mag` | `SignedByte` (1 byte) | Shading/lighting magnitude. |
| **115** | `team` | `UnsignedByte` (1 byte) | Team cape ID (used for minimap dots in wilderness). |
| **139** | `unnoted_item_id` | `UShort` (2 bytes) | Alternative/Custom opcode for un-noted id. |
| **140** | `noted_item_id` | `UShort` (2 bytes) | Alternative/Custom opcode for noted id. |
| **148** | (Placeholder ID) | `UShort` (2 bytes) | Read but discarded in `ItemDefinition.java`. |
| **149** | (Placeholder Temp) | `UShort` (2 bytes) | Read but discarded in `ItemDefinition.java`. |

## Note Wrapping Mechanics
When an item is requested, the client checks if `noted_item_id != -1`. If true, it calls `toNote()`.
The client actually copies the 3D model properties of the "Bank Note" item, but sets the `name`, `value`, and `is_members_only` flags from the *original* item. It also automatically sets the description to: `"Swap this note at any bank for a/an [Item Name]."`.

## Hardcoded Overrides
There are several hardcoded modifications done directly in `Client.java` / `ItemDefinition.java` after reading the cache:
-   **Barrows Armor**: Opcodes missing the "Set" option are hardcoded to add `actions[2] = "Set"`.
-   **Dueling Rings**: Item IDs 2552-2566 have their teleports hardcoded (`equipActions[1-5]`).
-   **Scrolls**: Rigour, Augury, Preserve scrolls are hardcoded to copy the model of ID 1505 (a generic scroll) and change their names and actions.
