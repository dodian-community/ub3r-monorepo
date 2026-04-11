# 15. Landscape Specification: World Object Spawns

## Overview
Landscape data (Store 4, `idx4`) defines where every static world object (trees, doors, statues) is placed within a 64x64 region. This data is heavily compressed using a recursive offset pattern.

---

## 1. The Parsing Algorithm (Verified)
The landscape file is read in two nested loops. The outer loop iterates through **Object IDs**, and the inner loop iterates through the **Locations** where that object appears.

### Outer Loop: Object ID Resolution
```python
object_id = -1
while True:
    id_offset = stream.read_usmart()
    if id_offset == 0: break # End of file
    object_id += id_offset
    # ... inner loop ...
```

### Inner Loop: Location & Config Resolution
```python
location_data = 0
while True:
    loc_offset = stream.read_usmart()
    if loc_offset == 0: break # End of locations for current Object ID
    location_data += loc_offset - 1
    
    # Unpack Location
    y = location_data & 0x3F          # Bits 0-5
    x = (location_data >> 6) & 0x3F   # Bits 6-11
    plane = location_data >> 12       # Bits 12+
    
    # Unpack Config
    config = stream.read_ubyte()
    type = config >> 2                # Top 6 bits (0-22)
    orientation = config & 3          # Bottom 2 bits (0-3)
```

---

## 2. Object Type Reference
The `type` field (0-22) determines how the model is rendered and rotated.

| Type | Name | Description |
| :--- | :--- | :--- |
| **0-3** | Walls | Straight walls, corners, and diagonal walls. |
| **4-8** | Wall Decor | Torches, paintings, or banners attached to walls. |
| **9** | Diagonal Walls| Single-tile diagonal wall segments. |
| **10** | Standard | Interactive world objects (Trees, Bank Booths, Portals). |
| **11** | Floor Decor | Ground-level objects (Small rocks, weeds). |
| **12-21**| Roofs | Roof geometry and slanted segments. |
| **22** | Ground | Simple floor textures or shadows. |

---

## 3. Tool Builder Strategy (Safe Packing)
To pack a new landscape without corrupting the file:
1.  **Parse to List**: Convert the existing binary file into an in-memory list of `Spawn` objects.
2.  **Add/Modify**: Add your new spawns to the list.
3.  **Sort (Critical)**: The algorithm **requires** that the data is written in ascending order of `ObjectID`. Within each ID, the locations MUST be written in ascending order of `packed_location_int`.
4.  **Difference Encoding**: Calculate the `id_offset` and `loc_offset` values relative to the previous entry in your sorted list.
5.  **Write**: Stream the resulting bytes using the `USmart` format.
