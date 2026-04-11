# Mystic Cache: NPC Definitions (`npc.dat` / `npc.idx`)

## Overview
NPC definitions control the appearance, size, combat level, animations, and interaction options of all Non-Player Characters. They are packed into two files:
1.  **`npc.idx`**: The index file. Contains byte offsets for where each NPC's data starts in the `.dat` file.
2.  **`npc.dat`**: The data file. Contains the actual properties for every NPC, encoded sequentially using opcodes.

## Initialization & Structure
The client loads these files on startup in `NpcDefinition.init()`:
-   Reads `npc.idx` to calculate the total number of NPCs (`size`).
-   Populates `offsets` array.
-   When an NPC is requested (`NpcDefinition.lookup(id)`), the stream seeks to `offsets[id]` and begins reading opcodes.
-   **Note**: The client caches up to 20 recent NPC definitions in memory to save CPU.

## Opcode Map
The client reads an unsigned byte (the opcode). If it's `0`, it marks the end of the NPC's definition.

| Opcode | Property Name | Data Type Read | Description |
| :--- | :--- | :--- | :--- |
| **0** | End of Definition | (None) | Tells the parser to stop reading. |
| **1** | `modelId` | `UnsignedByte` (Length `L`), then `L` loops of `UShort` | The 3D model IDs that make up the NPC's body. |
| **2** | `name` | `String` (N bytes) | The name of the NPC. |
| **3** | `description` | `String` (N bytes) / `readBytes` | The examine text of the NPC. |
| **12** | `size` | `SignedByte` (1 byte) | The size of the NPC in tiles (e.g., 1x1, 2x2). |
| **13** | `standAnim` | `UShort` (2 bytes) | The default standing/idle animation ID. |
| **14** | `walkAnim` | `UShort` (2 bytes) | The default walking animation ID. |
| **17** | `walkAnim` & Turns | 4x `UShort` (8 bytes) | Defines specific animations for walking, turning 180, turning 90 CW, and turning 90 CCW. |
| **30-39** | `actions[0-9]` | `String` (N bytes) | The right-click options for the NPC (e.g., "Talk-to", "Attack", "Trade"). "hidden" is parsed as `null`. Note: Opcode is `- 30`. |
| **40** | `recolourTarget` | Array of `UShort` tuples | Reads 1 byte for length `L`. Then reads `L` pairs of `UShort`s (original color, replacement color). |
| **60** | `aditionalModels` | `UnsignedByte` (Length `L`), then `L` loops of `UShort` | Extra model IDs layered onto the NPC. |
| **90-92** | (Unknown) | `UShort` (2 bytes) | Read but discarded. |
| **93** | `drawMinimapDot` | (None) | If present, sets `drawMinimapDot = false` (hides the yellow dot on the minimap). |
| **95** | `combatLevel` | `UShort` (2 bytes) | The NPC's combat level displayed in the right-click menu. |
| **97** | `scaleXZ` | `UShort` (2 bytes) | Model width scale (default 128). |
| **98** | `scaleY` | `UShort` (2 bytes) | Model height scale (default 128). |
| **99** | `priorityRender` | (None) | If present, sets `priorityRender = true`. |
| **100** | `lightModifier` | `SignedByte` (1 byte) | Ambient light value. |
| **101** | `shadowModifier` | `SignedByte` (1 byte) * 5 | Shadow/diffusion value multiplied by 5. |
| **102** | `headIcon` | `UShort` (2 bytes) | Sprite ID for a head icon (e.g., overhead prayers or specific quest markers). |
| **103** | `degreesToTurn` | `UShort` (2 bytes) | How far the NPC turns (default 32). |
| **106** | Transform NPC (Varp/Varbit) | `UShort` (varbit), `UShort` (settingId), [if opcode 118: `UShort` (value)], `UnsignedByte` (len), then loops of `UShort` | Used for NPCs that change appearance based on player state (e.g., diseased vs. healthy crops/NPCs). Looks up `childrenIDs`. `65535` is treated as `-1`. |
| **109** | `clickable` | (None) | If present, sets `clickable = false`. The NPC exists visually but cannot be interacted with. |
| **107, 111** | (Unknown) | (None) | Read but no action taken. |

## Hardcoded Overrides
The `NpcDefinition.java` contains a massive `switch(id)` block inside `lookup()` that manually overrides cache data. This is typically done to fix broken cache entries or add custom server-specific NPCs without having to repack the entire cache.
Examples of hardcoded overrides in Mystic Client:
-   **Pets**: (Venenatis, Chaos Ele, Vet'ion, Scorpia, etc.) Hardcoded to have the "Pick-up" action, fixed "slide" animations (`fixSlide()`), and adjusted `size` and `scaleXZ` for certain bosses like Callisto.
-   **Coins (ID 995)**: Recolored.
-   **Bob (ID 7456)**: Added "Repairs" option.
-   **Bots (Maxed Bot 1158, Archer Bot 4096)**: Completely hardcoded custom NPCs with specific model IDs (`modelId[]` arrays) to assemble platebodies, weapons, and capes dynamically.
-   **Make-over Mage (1306)**: Added "Make-over" option.
