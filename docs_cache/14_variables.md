# Mystic Cache: Variable Bits (`varbit.dat`)

## Overview
Variables (`Varp`s) and Variable Bits (`Varbit`s) are the client's way of tracking state that affects visual rendering. 
-   **Varp**: A 32-bit integer array (`client.settings[]`) synchronized from the server.
-   **Varbit**: A pointer to a specific bit-range *inside* a Varp.

These are extremely common for:
-   Farming patches (A single Varp tracks the growth stage of 4 different patches using 8 bits each).
-   Quests (Changing the dialogue or appearance of an NPC).
-   UI Elements (Toggling checkboxes or highlighting tabs).

## Structure
`varbit.dat` is loaded from `config.jag` during `VariableBits.init()`.

The file begins with a `UShort` indicating the total number of varbits (`size`).
The client loops sequentially, reading opcodes.

### Opcode Map
| Opcode | Property | Description |
| :--- | :--- | :--- |
| **0** | End of Definition | Stop reading this varbit. |
| **1** | `setting`, `low`, `high` | Reads `UShort`, `UnsignedByte`, `UnsignedByte`. |

### How it Works
When a `Varbit` is defined with:
-   `setting` = 100
-   `low` = 0
-   `high` = 4

It means this Varbit is looking at `client.settings[100]`. It creates a bitmask `(1 << (high - low)) - 1` and extracts the value from bits 0 through 4.

## `Varbit` in Object & NPC Definitions
If you review `2_object_definitions.md`, you'll notice Opcode 77 and 92 (`Transform Object`).
When an object is requested, the client checks if `varbit != -1`.
If so, it fetches the value of the varbit, and uses that value as the index into the `childrenIDs` array to determine which actual `ObjectDefinition` to render.

**Example**: A Farming Patch (Object ID 8000).
-   `varbit` = 500.
-   `childrenIDs` = [8001 (Weeds), 8002 (Seed), 8003 (Sapling), 8004 (Tree)].
If the server sends a packet setting Varbit 500 to `2`, the client will render Object 8003 instead of 8000.

## Cache Editing Implications
If you are adding new dynamic content (like a new farming patch or a customizable POH room), you must:
1.  Add a new entry to `varbit.dat`.
2.  Define the `low` and `high` bits carefully to ensure they do not overlap with another varbit using the same `setting` (Varp) ID.
3.  Add the transformation opcodes (`77` or `106`) to your custom Object or NPC in `loc.dat` / `npc.dat`.