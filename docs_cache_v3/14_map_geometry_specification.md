# 14. Map Geometry: Terrain & Height Specification

## Overview
Map Geometry (Terrain) defines the physical shape of the world: tile heights, collision flags, and floor material placement. This data is stored in Store 4 (`idx4`) and is resolved via the `map_index` hash (see [05. Map Loading](05_map_region_hashing.md)).

---

## 1. The Tile Parsing Algorithm
Terrain data is parsed tile-by-tile for a 64x64 region, across 4 height planes.

### Parser Loop Structure
```python
for plane in range(4):
    for x in range(64):
        for y in range(64):
            # Parse tile at (plane, x, y)
```

### Tile Opcodes (Verified)
The parser reads unsigned bytes until a termination opcode (0 or 1) is reached.

| Opcode | Logic | Description |
| :--- | :--- | :--- |
| **0** | **Done** | Calculates height based on Perlin noise (Plane 0) or offsets from below. |
| **1** | **Done** | Reads `UInt8`. Sets height to `height * 8` (relative to plane below). |
| **2 - 49** | **Overlay** | Reads `Int8` `overlay_id`. Shape = `(op - 2) / 4`. Orientation = `(op - 2) & 3`. |
| **50 - 81** | **Flags** | Sets `tile_flags` to `op - 49`. |
| **82 - 255**| **Underlay**| Sets `underlay_id` to `op - 81`. |

---

## 2. Height Calculation (Opcode 0/1)
RuneScape uses a coordinate-based noise function to generate "natural" terrain height for tiles with Opcode 0.
- **Plane 0**: `height = -noise(x, y) * 8`.
- **Planes 1-3**: `height = plane_below_height - 240`. (This creates the standard gap between floors).

---

## 3. The Bridge Flag (Verified)
This is the most critical logic for preventing map glitches (like players walking "under" a bridge but being seen "on" it).

**Flag**: `tile_flags & 0x2` (from Opcodes 50-81).
If bit `0x2` is set on a tile at **Plane 1**:
1.  The client treats that tile as if it were on **Plane 0** for rendering and collision.
2.  The server's `MovementFinalizePhase` must also respect this, or players will be "invisible" to others on different planes.

---

## 4. Tile Shapes (Overlay Shapes)
The shape ID derived from opcodes 2-49 (`(op-2)/4`) determines how the overlay is drawn:
- `0`: Simple square.
- `1-3`: Various triangular corners.
- `4-11`: Edge pieces and complex corners.

---

## 5. Tool Builder Algorithm
To pack a new map region:
1.  Initialize a 4x64x64 grid of heights and materials.
2.  **Order**: Write data starting from Plane 0, iterating X then Y.
3.  **Optimization**: If a tile is perfectly flat and has no materials, write Opcode `0` to save space.
4.  **Bridges**: If building a second-story walkway that players can walk under, set bit `0x2` in the `tile_flags` for the walkway tiles on Plane 1.
