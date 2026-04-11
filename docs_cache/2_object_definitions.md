# Mystic Cache: Object Definitions (`loc.dat` / `loc.idx`)

## Overview
Object definitions dictate the size, shape, interactions, and collision properties of all static and dynamic world objects (trees, walls, bank booths, doors). They are packed into two files:
1.  **`loc.idx`**: The index file. It contains the byte offsets for where each object's data begins in the `.dat` file.
2.  **`loc.dat`**: The data file. It contains the actual properties for every object, encoded sequentially using opcodes.

## Initialization & Structure
The client loads these files on startup in `ObjectDefinition.init()`:
-   Reads `loc.idx` to calculate `totalObjects`.
-   Populates `streamIndices` array with offsets.
-   When an object is requested (`ObjectDefinition.lookup(id)`), the stream seeks to `streamIndices[id]` and begins reading opcodes.
-   **Note**: The client caches up to 20 recent object definitions in memory to save CPU.

## Opcode Map
The client reads an unsigned byte (the opcode). If it's `0`, it marks the end of the object's definition.

| Opcode | Property Name | Data Type Read | Description |
| :--- | :--- | :--- | :--- |
| **0** | End of Definition | (None) | Tells the parser to stop reading. |
| **1** | `modelIds` & `modelTypes`| `UnsignedByte` (Length `L`), then `L` loops of: `UShort` (Model ID), `UnsignedByte` (Model Type) | Defines the 3D models and their specific "types" (e.g., wall, floor decoration, standard object). |
| **2** | `name` | `String` (N bytes) | The name of the object. |
| **3** | `description` | `String` (N bytes) | The examine text of the object. |
| **5** | `modelIds` (No Types) | `UnsignedByte` (Length `L`), then `L` loops of: `UShort` (Model ID) | Defines 3D models without specific types. |
| **14** | `objectSizeX` | `UnsignedByte` (1 byte) | The width of the object in tiles (default 1). |
| **15** | `objectSizeY` | `UnsignedByte` (1 byte) | The length/depth of the object in tiles (default 1). |
| **17** | `solid` | (None) | If present, sets `solid = false`. (Default is true). Means players can walk through it. |
| **18** | `impenetrable` | (None) | If present, sets `impenetrable = false`. (Default is true). Relates to projectiles passing through. |
| **19** | `isInteractive` | `UnsignedByte` (1 byte) | If `1`, the object has options you can click. |
| **21** | `contouredGround` | (None) | If present, the 3D model bends to match the terrain height. |
| **22** | `delayShading` | (None) | If present, sets `delayShading = false`. |
| **23** | `occludes` | (None) | If present, the object blocks the camera's view of things behind it. |
| **24** | `animation` | `UShort` (2 bytes) | The ID of the animation the object constantly plays (e.g., a burning fire). `65535` is treated as `-1`. |
| **28** | `decorDisplacement` | `UnsignedByte` (1 byte) | Used for wall decorations (like torches) to offset them from the wall. |
| **29** | `ambientLighting` | `SignedByte` (1 byte) | Ambient light value. |
| **39** | `lightDiffusion` | `SignedByte` (1 byte) | Light diffusion/contrast value. |
| **30-34** | `interactions[0-4]` | `String` (N bytes) | The right-click options for the object (e.g., "Chop down", "Bank"). "hidden" is parsed as `null`. |
| **40** | `modifiedModelColors` | Array of `UShort` tuples | Reads 1 byte for length `L`. Then reads `L` pairs of `UShort`s (original color, replacement color). |
| **41** | `modifiedTexture` | Array of `UShort` tuples | Reads 1 byte for length `L`. Then reads `L` pairs of `UShort`s (original texture, replacement texture). |
| **62** | `inverted` | (None) | If present, the 3D model is rendered inside-out or mirrored. |
| **64** | `castsShadow` | (None) | If present, sets `castsShadow = false`. (Default is true). |
| **65** | `scaleX` | `UShort` (2 bytes) | Model width scale (default 128). |
| **66** | `scaleY` | `UShort` (2 bytes) | Model height scale (default 128). |
| **67** | `scaleZ` | `UShort` (2 bytes) | Model depth scale (default 128). |
| **68** | `mapscene` | `UShort` (2 bytes) | ID used to draw a specific sprite on the minimap (e.g., a tree icon). |
| **69** | `surroundings` | `UnsignedByte` (1 byte) | Collision/surrounding flag. |
| **70** | `translateX` | `Short` (2 bytes) | X offset. |
| **71** | `translateY` | `Short` (2 bytes) | Y offset. |
| **72** | `translateZ` | `Short` (2 bytes) | Z offset. |
| **73** | `obstructsGround` | (None) | If present, sets `obstructsGround = true`. |
| **74** | `hollow` | (None) | If present, sets `hollow = true` (also forces `solid = false` and `impenetrable = false`). |
| **75** | `supportItems` | `UnsignedByte` (1 byte) | Determines if items dropped on this object appear on top of it (e.g., dropping items on a table). |
| **78** | (Ambient Sound) | `UShort`, `UnsignedByte` | Read but discarded in the client. |
| **79** | (Sound effects) | `UShort`, `UShort`, `UnsignedByte`, then `L` loops of `UShort` | Read but discarded in the client. |
| **81** | (Unknown) | `UnsignedByte` (1 byte) | Read but discarded. |
| **82** | `minimapFunction` | `UShort` (2 bytes) | Another minimap icon ID. (Has custom math to offset the ID). |
| **77 / 92**| Transform Object (Varp/Varbit) | `UShort` (varp), `UShort` (varbit), [if 92: `UShort` (value)], `UnsignedByte` (len), then `L` loops of `UShort` | Used for objects that change state based on player progress (e.g., farming patches). The client reads the varbit/varp and determines which actual Object ID to render from the `childrenIDs` array. |

## Hardcoded Overrides
Just like items, the `ObjectDefinition.java` file contains massive amounts of hardcoded logic applied *after* reading the cache:
-   **Obelisks** (IDs 14826-14831): Hardcoded to have the "Activate" option.
-   **Altars** (Lunar, Ancient, Magical): Hardcoded options to "Toggle-spells" or "Venerate".
-   **Agility Obstacles**: Hardcoded names and options for "Squeeze-Through" (Gap), "Fix" (Trawler Net), etc.
-   **Shading Fix**: `objectDef.delayShading = false;` is hardcoded globally as a "Cheap fix for: edgeville ditch, raids, wintertodt fire etc" to fix black squares on models.
