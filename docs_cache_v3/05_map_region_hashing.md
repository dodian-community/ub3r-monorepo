# 05. Map Loading: Coordinate Hashing & Discovery

## Overview
The world map is divided into a grid of 64x64 tile squares called **Regions**. To render the world as a player moves, the client must resolve absolute coordinates (e.g., X=3222, Y=3222) into File IDs stored in Store 4 (`idx4`).

---

## 1. The Region Hash Algorithm (Verified)
The cache does not use names for regions. It uses a bit-packed 16-bit integer hash.

**Formula**:
```python
region_x = absolute_x // 64
region_y = absolute_y // 64
region_hash = (region_x << 8) + region_y
```

**Verification Examples**:
- **Lumbridge (3222, 3222)**:
    - X: `3222 // 64 = 50`
    - Y: `3222 // 64 = 50`
    - Hash: `(50 << 8) + 50 = 12850`
- **Edgeville (3087, 3491)**:
    - X: `3087 // 64 = 48`
    - Y: `3491 // 64 = 54`
    - Hash: `(48 << 8) + 54 = 12342`

---

## 2. The `map_index` Registry
The relationship between a `region_hash` and the actual File IDs in `idx4` is stored in a file named **`map_index`** inside the `title.jag` archive (Store 0, File 1).

### Registry Format
| Offset | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| `0` | `UInt16` | `total_regions` | Number of region records in the file. |

**Record Structure (6 Bytes per record)**:
| Offset | Type | Name | Description |
| :--- | :--- | :--- | :--- |
| `0` | `UInt16` | `region_hash`| The `(X << 8) + Y` hash. |
| `2` | `UInt16` | `terrain_id` | The ID of the terrain file in `idx4`. |
| `4` | `UInt16` | `landscape_id`| The ID of the landscape file in `idx4`. |

### Important Constraints
- **File ID Limit**: The Mystic client enforces a limit of **3535** for map file IDs (`ID > 3535 ? -1 : ID`).
- **File Not Found**: If a hash is not present in `map_index`, the client renders a black void for that region.

---

## 3. Resolving a Region (Tool Implementation)
To extract the map for a specific coordinate in your editor:
1.  Open `title.jag` and extract `map_index`.
2.  Calculate the `region_hash` for your coordinates.
3.  Scan the `map_index` records for a matching hash.
4.  Once found, extract `terrain_id` and `landscape_id` from the record.
5.  Fetch those two File IDs from Store 4 (`idx4`) in the main cache.

---

## 4. Packing New Maps
To add a custom region (e.g., a new island):
1.  Choose a unique `region_hash` (an area of the map that is currently empty).
2.  Compress your terrain and landscape data into two new files at the **end** of `idx4`.
3.  Update `map_index`:
    - Increment the `total_regions` header.
    - Append a new 6-byte record with your `region_hash` and the new File IDs.
4.  Repack `title.jag` and write back to the main cache.
