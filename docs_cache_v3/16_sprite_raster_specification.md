# 16. Sprite Specification: Palette & Raster

## Overview
Sprites are 2D images used for the UI and icons. They are stored in groups within JAG archives. To maintain a small memory footprint, RuneScape uses an **Indexed Color Palette** system rather than 32-bit ARGB.

---

## 1. File Structure
A sprite group consists of an `index.dat` (shared across the group) and a `data.dat` (raw pixel indices).

### A. The Group Palette (`index.dat`)
| Offset | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| `0` | `UInt16` | `max_width` | Maximum canvas width for all sprites in group. |
| `2` | `UInt16` | `max_height`| Maximum canvas height. |
| `4` | `UInt8` | `palette_size`| Number of colors in the palette. |
| `5` | `UInt24[N]`| `colors` | RGB palette. 0 is reserved for transparency. |

### B. The Individual Sprite Metadata
Following the palette in `index.dat` is a sequence of headers for each sprite in the group.
| Offset | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| `0` | `UInt8` | `offset_x` | X-offset on the canvas. |
| `1` | `UInt8` | `offset_y` | Y-offset on the canvas. |
| `2` | `UInt16` | `width` | Image width. |
| `4` | `UInt16` | `height` | Image height. |
| `6` | `UInt8` | `format` | 0=Row Major, 1=Column Major. |

---

## 2. Pixel Decompression (Verified)
The pixel indices are stored in the secondary `.dat` file.

```python
# Format 0: Row Major
for i in range(width * height):
    palette_index = data_stream.read_byte()
    if palette_index != 0:
        pixels[i] = palette[palette_index]
    else:
        pixels[i] = TRANSPARENT # 0x000000 in RS
```

---

## 3. The "Magic Pink" Hack (Verified)
The Mystic Client source code (`Sprite.java`) includes a hardcoded transparency fix:
```java
public void setTransparency(int r, int g, int b) {
    for (int i = 0; i < myPixels.length; i++) {
        if (myPixels[i] == (r << 16) + (g << 8) + b) {
            myPixels[i] = 0;
        }
    }
}
```
**Tool Builder Requirement**: When converting a `.png` to a sprite, you must map the alpha channel to RGB `(255, 0, 255)`. The client will then convert this specific color to `0` (transparent) at runtime.

---

## 4. Packing Logic
To pack a new Sprite (e.g., a custom Item Icon):
1.  **Quantize**: Reduce the color count of your PNG to 255 unique colors.
2.  **Generate Palette**: Write the 24-bit RGB values to `index.dat`.
3.  **Rasterize**: Write the width/height metadata and the pixel indices to the data buffer.
4.  **Row vs Column**: Row-Major (0) is more common and easier to implement.
