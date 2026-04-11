# 13. Terrain & Floor Definitions: Opcode Specification

## Overview
Floor definitions dictate the visual appearance of the ground tiles. They are divided into **Underlays** (base layers) and **Overlays** (detailed layers like paths or water). All floor data is stored in the `flo.dat` file inside the `config.jag` archive.

---

## 1. Underlay Definitions
Underlays are simple, non-textured ground colors.

### The Parser Loop
1. Read `UInt16` (Total Underlays).
2. For each underlay, read opcodes until `0`.

| Opcode | Data Type | Field | Description |
| :--- | :--- | :--- | :--- |
| **1** | `TriByte` (24-bit) | `rgb` | The 24-bit RGB color of the floor. |
| **0** | (None) | End | Finishes the underlay definition. |

---

## 2. Overlay Definitions
Overlays support textures and specific shapes (shapes are defined in map geometry, but overlays provide the material).

### The Parser Loop
1. Read `UInt16` (Total Overlays).
2. For each overlay, read opcodes until `0`.

| Opcode | Data Type | Field | Description |
| :--- | :--- | :--- | :--- |
| **1** | `TriByte` (24-bit) | `rgb` | The 24-bit RGB color. |
| **2** | `UInt8` | `texture` | The ID of the texture from `textures.dat`. |
| **5** | (None) | `occlude` | If present, sets `occlude = false`. |
| **7** | `TriByte` (24-bit) | `other_rgb` | A secondary RGB color used for blending. |
| **0** | (None) | End | Finishes the overlay definition. |

---

## 3. Custom HSL Math (Verified)
The client converts raw RGB into a custom 16-bit HSL value for the GPU. Your tool must replicate this to generate accurate minimap colors.

```python
def rgb_to_hsl(rgb):
    r, g, b = (rgb >> 16 & 0xFF)/256.0, (rgb >> 8 & 0xFF)/256.0, (rgb & 0xFF)/256.0
    # ... standard HSL conversion ...
    # Hue, Saturation, Luminance scaled to 0-255
    return h, s, l

def hsl24_to_16(h, s, l):
    # Saturation halving thresholds based on luminance
    if l > 179: s //= 2
    if l > 192: s //= 2
    if l > 217: s //= 2
    if l > 243: s //= 2
    
    # 16-bit bit-packed result
    return (h // 4 << 10) + (s // 32 << 7) + (l // 2)
```

---

## 4. Packing Considerations
When adding a new floor type:
1.  **Append to `flo.dat`**: Add your new underlay or overlay to the end of the file.
2.  **Update Counts**: You MUST update the `UInt16` count at the start of the relevant section.
3.  **Textures**: If using Opcode 2, the `texture_id` must exist in `textures.dat`.
