# Mystic Cache: Graphics / SpotAnims (`spotanim.dat`)

## Overview
Graphics (internally called `SpotAnim` or `Graphic` in refactored clients) refer to the 3D visual effects that play independently of an entity's main model. 
Common examples include:
- The ice block that encases a player during Ice Barrage.
- The fireworks that play when a player levels up a skill.
- The arrow or magic spell projectile flying through the air.

These are loaded from `spotanim.dat` inside the `config.jag` archive during `Graphic.init()`.

## Structure
The file begins with a `UShort` indicating the total number of graphics in the game (`length`).
The client allocates `cache = new Graphic[length + 1]` and loops sequentially, reading opcodes until the end of each graphic definition.

### Opcode Map
| Opcode | Property | Description |
| :--- | :--- | :--- |
| **0** | End of Definition | Tells the parser to stop reading this graphic. |
| **1** | `modelId` | `UShort` (2 bytes). The 3D model ID from `idx1` that represents the graphic. |
| **2** | `animationId` | `UShort` (2 bytes). The sequence ID from `seq.dat` that animates this model. |
| **4** | `resizeXY` | `UShort` (2 bytes). How much to scale the model's width and length. (Default 128 = 100%). |
| **5** | `resizeZ` | `UShort` (2 bytes). How much to scale the model's height. (Default 128 = 100%). |
| **6** | `rotation` | `UShort` (2 bytes). The starting rotation/orientation of the graphic. |
| **7** | `modelBrightness` | `UnsignedByte` (1 byte). Ambient lighting modifier. |
| **8** | `modelShadow` | `UnsignedByte` (1 byte). Contrast/shadow modifier. |
| **40** | `recolourTarget` | Reads an `UnsignedByte` `j` for length. Then reads `j` pairs of `UShort`s (`originalModelColours`, `modifiedModelColours`). Used to tint graphics (e.g., recoloring an arrow for different metal types). |
| **41** | `retexture` | Reads an `UnsignedByte` `len`. Then loops `len` times, reading two `UShort`s. *(Note: The client reads these but discards them; texture swapping is seemingly unsupported or broken in this specific client version).* |

## Usage in Game
When the server sends a "Create Graphic" packet (UpdateFlag.GRAPHICS), it sends the `Graphic ID` and a `Delay`. 
The client looks up `Graphic.cache[id]`, fetches the 3D model defined by `modelId`, colors/scales it according to the definition, and begins playing the animation defined by `animationId`.

## Building a GFX Packer
If you are building a tool to add new visual effects (e.g., custom boss mechanics):
1.  **Pack the Model**: Write the 3D geometry to `idx1`.
2.  **Pack the Animation**: Write the movement frames to `idx2` and the sequence metadata to `seq.dat`.
3.  **Pack the GFX**: Link them together by writing a new entry into `spotanim.dat` with Opcode 1 (`modelId`) and Opcode 2 (`animationId`).
4.  **Scale and Color**: Use Opcodes 4, 5, and 40 to fine-tune the size and color without needing to duplicate the actual 3D model in the cache.