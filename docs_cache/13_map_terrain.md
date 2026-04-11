# Mystic Cache: Terrain & Floors (`flo.dat`)

## Overview
While the `MapRegion` classes dictate *where* the terrain goes (height, clipping, and which floor ID is on which tile), the `flo.dat` file dictates *what* that floor looks like (color, texture, shading).

This file is loaded from the `config.jag` archive during `FloorDefinition.init()`.

## Structure
The `flo.dat` file is split into two distinct sections: **Underlays** and **Overlays**.
-   **Underlays**: The base color of the ground (e.g., grass, dirt). It does not support textures and blends smoothly with adjacent underlays.
-   **Overlays**: A layer drawn *on top* of the underlay. This is used for paths, roads, water, or specific textured tiles (like the wooden floor of a house). Overlays have hard edges and can be textured.

### 1. Underlays
The file begins by reading the number of underlays:
```java
int underlayAmount = buffer.getShort();
```
It then loops `underlayAmount` times, reading opcodes.

#### Underlay Opcode Map
| Opcode | Property | Description |
| :--- | :--- | :--- |
| **0** | End of Definition | Stop reading this underlay. |
| **1** | `rgb` | Reads 3 bytes (`buffer.get() & 0xff` shifted 16, 8, 0). The 24-bit RGB color. |

After reading the RGB, the client calculates the HSL (Hue, Saturation, Luminance) values via `generateHsl()`. This is critical because the client blends underlays together based on Hue, not raw RGB.

### 2. Overlays
After parsing all underlays, it reads the number of overlays:
```java
int overlayAmount = buffer.getShort();
```
It then loops `overlayAmount` times.

#### Overlay Opcode Map
| Opcode | Property | Description |
| :--- | :--- | :--- |
| **0** | End of Definition | Stop reading this overlay. |
| **1** | `rgb` | Reads 3 bytes. The 24-bit RGB color. |
| **2** | `texture` | Reads 1 byte (`buffer.get() & 0xff`). The ID of the texture from `textures.dat` to draw on this tile. |
| **5** | `occlude` | If present, sets `occlude = false`. (Default is true). Used for transparent/translucent overlays. |
| **7** | `anotherRgb` | Reads 3 bytes. A secondary/fallback RGB color. |

## HSL Conversion
The `rgbToHsl` mathematical conversion in `FloorDefinition.java` is completely custom to the RuneScape engine. If you are building a map editor, you **must** copy this exact Java math to calculate `blendHue`, `saturation`, and `luminance` correctly, otherwise your terrain blending on the minimap and in the 3D scene will look completely wrong (colors will bleed improperly).