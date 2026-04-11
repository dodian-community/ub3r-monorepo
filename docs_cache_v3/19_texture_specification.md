# 19. Texture Specification: 3D Mapping

## Overview
Textures in the RuneScape engine are applied to 3D model faces. They allow for complex surface details (like water ripples, stone walls, or wooden planks) without increasing the vertex count of the model.

In the Mystic Client, textures are managed by `Rasterizer3D.java` and are stored in the `textures.jag` archive.

---

## 1. Storage & Limits
- **Archive**: `textures.jag` (Store 0, File 6).
- **Capacity**: The engine is hardcoded to support exactly **60 textures** (ID 0 to 59).
- **File Names**: Files inside the archive are named numerically: `"0"`, `"1"`, `"2"`, ..., `"59"`.

---

## 2. Technical Format
Textures are stored as **IndexedImage** objects (identical to the Sprite format but without an `index.dat` file).

### Dimensions
- **Standard**: 128x128 pixels.
- **Low Memory**: The client downscales textures to 64x64 at runtime if memory is low.

### The Palette System
Like sprites, each texture has a 256-color palette.
- **Transparency**: Textures support transparency using the same "Magic Pink" or "Zero-index" logic as sprites. If a texture contains a transparent pixel, the 3D rasterizer will draw the model color or terrain beneath it.

---

## 3. The Rendering Pipeline (Verified)
1.  **Request**: When a model face with a texture ID is drawn, `getTexturePixels(id)` is called.
2.  **Caching**: The client maintains a `textureRequestPixelBuffer` (a cache of recently used texture pixel arrays).
3.  **Brightness Adjustment**: At runtime, the client applies the user's brightness setting to the entire texture palette to ensure it matches the 3D scene lighting.
4.  **Rasterization**: The pixels are mapped onto the 3D triangle using UV coordinates (calculated from the face vertices).

---

## 4. Building a Texture Packer
To add a custom texture (e.g., Animated Lava):
1.  **Format**: Convert your 128x128 image to an indexed byte array.
2.  **Naming**: Choose a free ID between 0-59. Hash the string ID (e.g., `"55"`) using the RS hashing algorithm.
3.  **Packing**: Append the raw pixel data and the 256-color palette to the `textures.jag` archive under that hash.
4.  **Implementation**: Map the texture to a model face using Opcode 1 in the Object definition or the model's texture block.
