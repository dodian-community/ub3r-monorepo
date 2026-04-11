# 09. Model Specification: Geometry & Textures

## Overview
RuneScape 3D models (Store 1, `idx1`) use a proprietary, highly compressed geometry format. Vertices and faces are stored in separate byte blocks to maximize "Smart" integer efficiency.

---

## 1. The Footer (Metadata)
Every model ends with an 18-byte footer. You must read this first to calculate block offsets.

| Offset (from EOF) | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| `-18` | `UInt16` | `num_vertices` | Total number of points in 3D space. |
| `-16` | `UInt16` | `num_triangles` | Total number of faces connecting vertices. |
| `-14` | `UInt8` | `num_textured` | Number of faces with texture mapping. |
| `-13` | `UInt8` | `type_opcode` | Has face types? (1 = Yes). |
| `-12` | `UInt8` | `prior_opcode` | Has per-face priorities? (255 = Yes). |
| `-11` | `UInt8` | `alpha_opcode` | Has per-face transparency? (1 = Yes). |
| `-10` | `UInt8` | `tskin_opcode` | Has triangle animation skins? (1 = Yes). |
| `-9` | `UInt8` | `vskin_opcode` | Has vertex animation skins? (1 = Yes). |
| `-8` | `UInt16` | `x_data_len` | Byte size of X-coordinate block. |
| `-6` | `UInt16` | `y_data_len` | Byte size of Y-coordinate block. |
| `-4` | `UInt16` | `z_data_len` | Byte size of Z-coordinate block. |
| `-2` | `UInt16` | `index_len` | Byte size of Triangle Index block. |

---

## 2. Block Sequence (Physical Order)
Blocks appear in the byte array in the following order. Use the footer data to calculate the starting index for each.

1.  **Vertex Flags**: `num_vertices` bytes.
2.  **Triangle Types**: `num_triangles` bytes (if `type_opcode == 1`).
3.  **Priorities**: `num_triangles` bytes (if `prior_opcode == 255`).
4.  **Triangle Skins**: `num_triangles` bytes (if `tskin_opcode == 1`).
5.  **Vertex Skins**: `num_vertices` bytes (if `vskin_opcode == 1`).
6.  **Alpha**: `num_triangles` bytes (if `alpha_opcode == 1`).
7.  **Triangle Indices**: `index_len` bytes.
8.  **Face Colors**: `num_triangles * 2` bytes.
9.  **Face Textures**: `num_textured * 6` bytes.
10. **X Coordinates**: `x_data_len` bytes.
11. **Y Coordinates**: `y_data_len` bytes.
12. **Z Coordinates**: `z_data_len` bytes.
13. **Footer**: 18 bytes.

---

## 3. Vertex Decompression Algorithm
Vertices are stored as relative offsets using `Smart` integers.

```python
x, y, z = 0, 0, 0
for i in range(num_vertices):
    flag = flags_block[i]
    if flag & 0x1: x += x_coords.read_smart()
    if flag & 0x2: y += y_coords.read_smart()
    if flag & 0x4: z += z_coords.read_smart()
    vertices[i] = (x, y, z)
```

---

## 4. Triangle Connection Algorithm (Opcodes)
Faces are connected using an opcode stream to save space by reusing previous vertex indices.

```python
a, b, c = 0, 0, 0
offset = 0
for i in range(num_triangles):
    op = triangle_indices.read_byte()
    if op == 1:
        a = triangle_indices.read_smart() + offset
        offset = a
        b = triangle_indices.read_smart() + offset
        offset = b
        c = triangle_indices.read_smart() + offset
        offset = c
    elif op == 2:
        b = c
        c = triangle_indices.read_smart() + offset
        offset = c
    elif op == 3:
        a = c
        c = triangle_indices.read_smart() + offset
        offset = c
    elif op == 4:
        temp = a
        a = b
        b = temp
        c = triangle_indices.read_smart() + offset
        offset = c
    faces[i] = (a, b, c)
```

---

## 5. Tool Builder Notes
- **Color Mapping**: Face colors are 16-bit HSL. 
- **Inversion**: Some models are mirrored using the `inverted` flag in Object/NPC definitions.
- **Scaling**: Standard unit scale is 128. OSRS-imported models often require `scale(132)` to align with 317 character models.
