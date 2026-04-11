# 14. Terrain Algorithm: Natural Height Generation

## Overview
RuneScape maps do not store every single height value for Plane 0. To save space, "Natural" terrain heights are generated at runtime using a seedable, layered noise function.

---

## 1. The Entry Point
A tool must use the exact constants and scaling used by the client to generate terrain that matches.

```python
# Constants found in MapRegion.java:408
SEED_1 = 45365
SEED_2 = 91923  # 0x16713
SEED_3 = 10294
SEED_4 = 37821

def calculate_vertex_height(x, y):
    h1 = (interpolated_noise(x + SEED_1, y + SEED_2, 4) - 128)
    h2 = (interpolated_noise(x + SEED_3, y + SEED_4, 2) - 128) >> 1
    h3 = (interpolated_noise(x, y, 1) - 128) >> 2
    
    height = h1 + h2 + h3
    height = int(height * 0.3) + 35
    
    # Clamping
    if height < 10: height = 10
    if height > 60: height = 60
    
    return height
```

---

## 2. The Noise Layers
The `interpolated_noise` function used above is a multi-scale sampler.
- **Scale 4**: Macro terrain (Large hills).
- **Scale 2**: Mid-range features.
- **Scale 1**: Micro variations (Roughness).

---

## 3. Floor Transitions (Verified)
The client calculates `tileHeights[plane][x][y]` using these rules:
- **Plane 0**:
    - If Opcode 0: Use `calculate_vertex_height(x, y) * 8`.
    - If Opcode 1: Read `UByte H`. Height is `H * 8`.
- **Planes 1-3**:
    - If Opcode 0: Height is `tileHeights[plane - 1][x][y] - 240`.
    - If Opcode 1: Read `UByte H`. Height is `tileHeights[plane - 1][x][y] - (H * 8)`.

---

## 4. Normalization for Map Editors
When a map editor "exports" to `.idx4`:
1.  **Flattening**: Any tile that is exactly `35` (the noise baseline) should be saved as Opcode 0.
2.  **Smoothing**: The client applies a 3x3 average filter to the heights after loading. Your tool must preview the map with this filter applied to match the in-game visuals.
