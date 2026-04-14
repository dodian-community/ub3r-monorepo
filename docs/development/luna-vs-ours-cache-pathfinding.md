# Luna vs Ours — Cache Decoding & Pathfinding System Comparison

**Context:** We are experiencing clipping/blocking issues with world objects where no single rule can be applied
without creating either mass noclip or mass blocking. This document compares Luna's cache decoding and
pathfinding systems against ours, focusing on what Luna does that we don't — especially around object metadata
that feeds collision and reach-checking.

**Important note on cache compatibility:** Our server loads the *Mystic updated client* cache (`data/cache/`),
which is a newer/extended RS2 format with 6 index files (`idx0`–`idx5`), plus extra files like `obj.dat`,
`obj.idx`, `sprites.dat`, `tradable.dat`. Luna targets the vanilla #377 cache. Any system we adopt from Luna
must be adapted to our cache's opcode set and file layout — it should be achievable since the core binary format
is the same, just with additional opcodes and files in ours.

---

## 1. Object Definition Decoding (`loc.dat` / `loc.idx`)

Both systems read `loc.dat` and `loc.idx` out of archive `0/2` in the cache store, using a delta-index scheme
to locate each definition by ID. The binary parse loop is the same opcode-driven format. The differences are
in **which fields are captured** and **what is done with them**.

### 1.1 Fields Luna decodes and stores that we skip or mishandle

| Opcode | Luna Field | Luna Usage | Our Handling |
|--------|-----------|------------|--------------|
| `69` | `direction` (`int`) | Packed bit-field of approachable faces; fed into `reachedObject()` / `reachedFacingEntity()` reach logic | **Skipped** (`data.skip(1)`) — `GameObjectData` has no `direction` field |
| `73` | `obstructive` (`boolean`) | Stored on `GameObjectDefinition` as a hint | Decoded to `unknownValue`, stored but not used for collision |
| `77` | `varpDef` (`VarpChildDefinition`) | Full varp/varbit transform chain for morphing objects (doors, levers, etc.) | Data bytes are skipped after reading child count; no storage |

**The most impactful missing field is `direction` (opcode 69).** Luna uses this in `reachedObject()` to
determine which sides of a multi-tile object are interactable. A player can only trigger the "reached" condition
from a face that is both collision-clear *and* listed in the object's direction bitmask. Without it, we either
allow approach from blocked faces or deny it from open faces.

### 1.2 Opcode 27 — Partial block

Luna's decoder **does not handle opcode 27** at all (it falls through without reading any data).  
Our decoder sets `blockWalk = 1` when opcode 27 is present.

- `blockWalk = 2` → blocks movement and range  
- `blockWalk = 1` → blocks walk route-finding only  
- `blockWalk = 0` → does not block

This means for any object that has opcode 27 in our cache, we assign a partial-block flag that Luna would
ignore. Whether that difference matters depends on how many objects carry opcode 27 in our cache versus Luna's
#377 cache. It could cause some objects to be over-blocking compared to what the client expects.

### 1.3 Additional opcodes only in our cache

Our decoder handles several opcodes that Luna's `ObjectDefinitionDecoder` doesn't have. These are from the
newer/extended RS2 revision our cache is based on:

| Opcode | Purpose |
|--------|---------|
| `41` | Additional colour replacement table (skip only) |
| `61` | Unknown short (skip) |
| `74` | `breakRouteFinding = true` (adds `ROUTE_BLOCKER` flag during collision build) |
| `78` | Model recolouring (2 bytes + 1 byte, skip) |
| `79` | Complex model-to-texture entry (skip) |
| `82` | Unknown short (skip) |
| `92` | Extended varp/varbit child table (skip) |
| `249` | Custom parameters table (skip) |

Luna doesn't encounter these because its #377 cache simply doesn't contain them. Our decoder handles them
correctly (either storing or skipping), so this is not a bug — just worth knowing when comparing.

### 1.4 `interactive` inference difference

Luna reads `interactive` directly from opcode 19 (`data.readBoolean()`).  
Our decoder reads opcode 19 the same way, **but also infers interactivity at opcode 0** (end-of-definition):

```kotlin
val inferredInteractive =
    name != "null" &&
    hasModelData &&
    (firstModelType == null || firstModelType == 10)
val hasActions = interactive || inferredInteractive || interactions.any { it != null }
```

This is a stricter inference that can mark more objects as interactive than Luna would. Objects that are
interactive but aren't explicitly flagged in the cache (e.g., have a name and models but no opcode 19) will
be treated as interactive in our system, which affects the floor-decoration collision check.

---

## 2. Collision Building — Who Blocks What

Both systems use the same overall pipeline: decode tiles → flag blocked/bridge tiles → loop objects → apply
per-object collision flags. The differences are in the rules used to decide if an object should block at all,
and how its footprint is computed.

### 2.1 "Should this object block?" — `unwalkable()` vs `isTypeWalkBlocking()`

**Luna (`CollisionUpdate.Builder.unwalkable()`):**

```java
private static boolean unwalkable(GameObjectDefinition definition, int type) {
    boolean isSolidFloorDecoration = type == GROUND_DECORATION.getId() && definition.isInteractive();
    boolean isRoof = type > DIAGONAL_DEFAULT.getId() && type < GROUND_DECORATION.getId();
    boolean isWall = type >= STRAIGHT_WALL.getId() && type <= RECTANGLE_CORNER_WALL.getId()
                     || type == DIAGONAL_WALL.getId();
    boolean isSolidInteractable = (type == DIAGONAL_DEFAULT.getId() || type == DEFAULT.getId())
                                  && definition.isSolid();
    return isWall || isRoof || isSolidInteractable || isSolidFloorDecoration;
}
```

Luna rules by type group:
- **Types 0–3 (walls) + type 9 (diagonal wall):** Always block regardless of `solid`.  
- **Types 12–21 (roofs):** Always block regardless of `solid`.  
- **Types 10–11 (default / diagonal default):** Block only if `solid == true`.  
- **Type 22 (floor decoration):** Block only if `interactive == true`.  
- **Types 4–8 (wall decorations):** Never block.

**Ours (`CollisionBuildService.isTypeWalkBlocking()`):**

```kotlin
fun isTypeWalkBlocking(type: Int, blockWalk: Int, objectName: String? = null): Boolean {
    if (type in 10..11 && isUnnamedDefinitionName(objectName)) return false
    return when (type) {
        in 4..8 -> false
        22 -> blockWalk == 1
        else -> blockWalk != 0
    }
}
```

Our rules:
- **Types 4–8:** Never block.  
- **Type 22 (floor decoration):** Block only if `blockWalk == 1`.  
- **Types 10–11 with name "null":** Never block (extra guard against unnamed objects).  
- **Everything else:** Block if `blockWalk != 0`.

**Key differences:**

| Scenario | Luna | Ours |
|----------|------|------|
| Roof type (12–21), `solid=false` | ✅ Blocks (always) | ⚠️ Only if `blockWalk != 0` |
| Floor decoration, `solid=true`, not interactive | ❌ Does not block | ⚠️ Blocks if `blockWalk == 1` |
| Floor decoration, `solid=true`, interactive | ✅ Blocks | ⚠️ Only if `blockWalk == 1` |
| Default/diagonal default (10–11), `solid=false` | ❌ Does not block | ⚠️ Blocks if `blockWalk != 0` |
| Default/diagonal default, name "null", `solid=true` | ✅ Blocks | ❌ Does not block |

The **floor decoration (`blockWalk == 1` requirement)** and the **"null" name guard on types 10–11** are the
most likely contributors to our blocking/noclip imbalance. Luna's model is simpler: solid + correct type =
blocks. Our model threads through a `blockWalk` integer that can take three values and interacts with
name-based guards.

### 2.2 Object footprint (size × rotation)

**Luna** uses `definition.getSizeX()` and `definition.getSizeY()` directly for interactable objects (types
9–21). It **does not swap** width and height based on rotation for these types.

**Ours** has two modes, controlled by `CollisionBuildService.LIVE_FOOTPRINT_MODE`:
- `ROTATED`: swaps sizeX/sizeY for rotations 1 and 3, for all types.
- `LUNA_UNROTATED_INTERACTABLE`: swaps only for wall types (not for types 9–21), matching Luna.

At startup, the bootstrap service checks a known reference tile (`2727, 9773`) and auto-detects which mode is
correct for the loaded cache. If the cache causes a mismatch at that tile in `ROTATED` mode, it switches to
`LUNA_UNROTATED_INTERACTABLE` and rebuilds. This is a heuristic — it may not catch every object type that
needs a different treatment, and it only checks one tile.

### 2.3 Water tiles — missing from `DecodedMapTile.isBlocked()`

Luna's `MapTile.isBlocked()`:

```java
public boolean isBlocked() {
    return (attributes & BLOCKED) != 0 || isWater();
}
public boolean isWater() {
    return overlay == 6; // overlay ID 6 = water texture
}
```

Our `DecodedMapTile.isBlocked()`:

```kotlin
fun isBlocked(): Boolean = (attributes and BLOCKED) == BLOCKED
```

**We do not check for water overlays.** Any tile with overlay ID 6 (water) that doesn't have the BLOCKED
attribute flag set will not be flagged as blocked. If our cache uses the same overlay ID 6 for water, water
tiles could be walkable, letting players walk through rivers/lakes or making the pathfinder route through them.

To fix this for our cache, we'd need to confirm what overlay ID our cache uses for water. Overlay ID 6 is the
standard #377 value, and our cache likely uses the same one since the terrain format is the same.

### 2.4 `ROUTE_BLOCKER` flag (opcode 74)

We have a `ROUTE_BLOCKER` flag (`1 shl 16`) that Luna does not have. It is set when an object has
`breakRouteFinding = true` (opcode 74) and is of type 9–21. Luna's cache simply doesn't encode opcode 74, so
this is a feature unique to our newer cache format. It should work correctly as-is, but if an object has
opcode 74 in our cache and should NOT be a route blocker, the flag might cause unwanted blocking.

---

## 3. Pathfinding System Comparison

Both systems use A* with the same CHEBYSHEV-equivalent movement cost (10 for cardinal, 14 for diagonal) and
the same diagonal decomposition (check both orthogonal components before allowing a diagonal step).

### 3.1 Traversability check

**Luna** (via `CollisionManager.traversable()`):
1. Load `ChunkRepository` for the destination tile.
2. Ask `CollisionMatrix.untraversable(x, y, entity, direction)` — checks the appropriate directional flag on
   the destination tile.
3. For diagonals: also check both orthogonal components.

**Ours** (via `CollisionManager.canStep()` / `isApproachBlocked()`):
1. Check `isAnyFullBlocked()` on each tile of the entity's footprint at the destination.
2. Check `isApproachBlocked()` using `CollisionFlag.approachMask(dx, dy)` — checks the approach-direction
   flags on the leading edge tiles.
3. For diagonals: decompose into X and Y steps first.

The logic is functionally equivalent. Both read directional flags from the same collision matrix and arrive at
the same conclusion for a given flag state. The pathfinding algorithm itself is not the root of the clipping
problem.

### 3.2 Reach / interaction reachability — the big gap

Luna has a full set of reach-check methods in `CollisionMatrix`:

| Method | Purpose |
|--------|---------|
| `reachedObject(start, object)` | Dispatches to wall, decoration, or facing-entity check based on type |
| `reachedWall(start, wallObject)` | Checks all valid approach positions for all wall types and directions |
| `reachedDecoration(start, decorationObject)` | Checks type-6/7/8 decorations with rotation-aware positions |
| `reachedFacingEntity(start, target, sizeX, sizeY, packedDirections)` | Generic reach check for any rectangular entity, respecting `direction` bitmask |

These methods use hardcoded collision flag bitmasks (e.g., `0x8` = `MOB_WEST`, `0x80` = `MOB_EAST`, `0x2` =
`MOB_SOUTH`, `0x20` = `MOB_NORTH`) to check whether a given side is open. Critically, `reachedFacingEntity`
also accepts the `packedDirections` value from the object definition (the `direction` field from opcode 69) to
restrict which sides can be approached even when the collision path is clear.

**We do not appear to have an equivalent of these methods in our server.** If our interaction/click system is
using a simple radius or tile-adjacency check instead of the full reach logic, players will:
- Be able to "interact" with an object through a wall (noclip-style interaction)
- Or fail to interact with an object they are standing right next to (over-blocking)

This is especially problematic for:
- **Doors** (straight walls with specific approach sides)
- **Counters / shop desks** (large multi-tile objects with restricted access faces)
- **Objects with `direction` > 0** (the cache explicitly says certain faces are not interactable)

### 3.3 The `direction` field pipeline (end-to-end)

Luna's full pipeline for object reachability:

```
Cache (loc.dat, opcode 69)
    → GameObjectDefinition.direction (int, packed bit-field)
        → reachedObject() is called with this definition
            → reachedFacingEntity(..., OptionalInt.of(packedDirections))
                → checks (packedDirections & 8) == 0 (west face accessible?)
                → checks (packedDirections & 2) == 0 (south face accessible?)
                → etc.
```

Our pipeline:
```
Cache (loc.dat, opcode 69)
    → data.skip(1)   ← field is discarded
```

To implement this for our cache, we would need to:
1. Add a `direction: Int` field to `GameObjectData`.
2. Decode it at opcode 69 (`direction = data.readUnsignedByte()`).
3. Implement `reachedObject()` / `reachedFacingEntity()` in our collision/interaction system.
4. The logic itself is cache-format-agnostic once the field is decoded — the same bitmask checks apply.

---

## 4. Our Cache Layout vs Luna's Cache

| Feature | Luna (#377 cache) | Ours (Mystic client cache) |
|---------|------------------|---------------------------|
| Data file | `main_file_cache.dat` | `main_file_cache.dat` |
| Index files | `idx0`–`idx4` (5 files) | `idx0`–`idx5` (6 files) |
| Object defs | `loc.dat` + `loc.idx` inside archive `0/2` | Same location, extended opcode set |
| Map tiles | Store `4`, gzip-compressed | Same |
| Map objects | Store `4`, gzip-compressed, same delta-smart format | Same |
| Map index | `map_index` inside archive `0/5` | Same |
| Extra files | None | `obj.dat`, `obj.idx`, `sprites.dat`, `sprites.idx`, `tradable.dat`, `packed_sprites/` |

The core map and object definition binary formats are the same — the only differences are extra opcodes in
`loc.dat` and the additional `idx5` / standalone files. Our `CacheStore` already handles the 6 index files
dynamically. The `obj.dat` / `obj.idx` files outside the archive may be a flat (non-archive) object definition
table that the client reads separately — worth investigating whether they contain more complete or overriding
definitions for some objects.

---

## 5. Prioritized Fix List

Based on the analysis, these are the most likely fixes ranked by impact on the clipping/blocking issue:

### Priority 1 — Water tile blocking (quick win)

Add a water overlay check to `DecodedMapTile.isBlocked()`:

```kotlin
// In DecodedMapTile:
fun isBlocked(): Boolean = (attributes and BLOCKED) == BLOCKED || isWater()
fun isWater(): Boolean = overlay == 6  // Confirm overlay ID for our cache
```

This is a one-line change. If water tiles are currently walkable, this causes large areas of the world to
be incorrectly open.

### Priority 2 — Decode and store `direction` (opcode 69)

Add `direction: Int = 0` to `GameObjectData` and decode it in `ObjectDefinitionDecoder`:

```kotlin
// In decodeEntry(), opcode 69:
//69 -> direction = data.readUnsignedByte();  // instead of data.skip(1)
```

This unblocks the ability to implement `reachedFacingEntity()` with proper face restriction. Without this, the
reach logic cannot work correctly even if implemented.  
**Note:** Our cache is a newer format, but opcode 69 still encodes a 1-byte orientation/facing value. The
semantics should be the same as in the #377 format.

### Priority 3 — Implement `reachedObject()` / `reachedFacingEntity()`

Port Luna's `CollisionMatrix.reachedObject()`, `reachedWall()`, `reachedDecoration()`, and
`reachedFacingEntity()` logic into our server's interaction/reach system. These methods:
- Use the already-populated collision flags (so no cache changes needed beyond #2)
- Use the `direction` field from #2
- Are the exact same RS2 #377 client logic — our cache uses the same object type IDs and rotation scheme

The hardcoded bitmask values (`0x8`, `0x80`, `0x2`, `0x20`) map to `MOB_WEST`, `MOB_EAST`, `MOB_SOUTH`,
`MOB_NORTH` which match our `CollisionFlag` values:

```kotlin
// Our CollisionFlag values — confirm these match the bitmasks used in Luna's reach checks
MOB_SOUTH  = 1 shl 6  = 0x40   // Note: Luna uses 0x2 for south-approach check
MOB_NORTH  = 1 shl 1  = 0x02   // Luna uses 0x20 for north-approach check
MOB_WEST   = 1 shl 3  = 0x08   // Luna uses 0x08 for west-approach check  
MOB_EAST   = 1 shl 4  = 0x10   // Luna uses 0x80 for east-approach check
```

⚠️ **Flag bit layout mismatch**: Luna's `CollisionFlag` uses a 1-indexed bit scheme (`1 << bit` where bit
goes 1–16), while ours uses 0-indexed (`1 shl 0` through `1 shl 15`). The raw integer values differ by one
position. The reach check bitmasks in Luna (`0x2`, `0x8`, `0x20`, `0x80`) need to be translated to our bit
layout before porting the `reachedFacingEntity` logic:

| Luna bitmask | Luna flag name | Our equivalent |
|-------------|---------------|---------------|
| `0x2` (`1 << 1`) | `MOB_NORTH` (bit 1) | `MOB_NORTH = 1 shl 1` = `0x2` ✅ same |
| `0x8` (`1 << 3`) | `MOB_WEST` (bit 3) | `MOB_WEST = 1 shl 3` = `0x8` ✅ same |
| `0x20` (`1 << 5`) | `MOB_SOUTH_WEST` (bit 5?) | Our bit 5 is `MOB_SOUTH_WEST` but Luna bit 5 would be... |

Wait — Luna's `CollisionFlag` is 1-indexed (bit 1 = `MOB_NORTH`, bit 2 = `MOB_NORTH`... let me re-read the
reach check carefully. Actually in Luna's `CollisionMatrix.reachedFacingEntity()`, the raw `get(start)` values
used are:
- `& 8` = check if `MOB_WEST` (bit 3, value 8) is set on start tile → west side of object is blocked
- `& 0x80` = check if `MOB_EAST` (bit 7 in the 16-bit value, value 128) is set
- `& 2` = check if `MOB_SOUTH` is set (bit 1 in the short)
- `& 0x20` = check if `MOB_NORTH` is set (bit 5 in the short)

These come from Luna's `CollisionFlag` where bit indices are 1-based and stored as `1 << bit`. So:
- `MOB_SOUTH (bit 6) = 1<<6 = 64 = 0x40` but `& 2` is checking bit 1 = `MOB_NORTH (value 2)`...

This needs careful verification during porting. The bit layout in the raw matrix values must be mapped
correctly to our `CollisionFlag` constants to avoid inverting north/south checks.

### Priority 4 — Audit opcode 27 handling

Check whether opcode 27 appears in our `loc.dat` and for which object IDs. If Luna's #377 `loc.dat` doesn't
have opcode 27 at all, the behavior difference (our `blockWalk = 1` vs Luna ignoring it) only matters if our
cache uses it. Log the set of objects that have opcode 27 and see if they correlate with problem areas.

### Priority 5 — Review floor decoration blocking rule

Luna: floor decoration (type 22) blocks if `interactive == true`.  
Ours: floor decoration blocks if `blockWalk == 1`.

These are not equivalent. An interactive floor decoration with `blockWalk == 2` (the default solid value) would
block in Luna but not in ours (since we require `blockWalk == 1` specifically). Consider aligning to Luna's
rule: type 22 blocks if `hasActionsFlag == true`.

---

## 6. Summary Table

| System Area | Luna | Ours | Gap |
|-------------|------|------|-----|
| `direction` field (opcode 69) | ✅ Decoded and stored | ❌ Skipped | Reach checks can't work |
| Water tile blocking | ✅ Overlay == 6 check | ❌ Attribute flag only | Water may be walkable |
| Opcode 27 (`blockWalk=1`) | ❌ Not handled | ✅ Sets partial block | Extra blocking vs Luna |
| `reachedObject()` logic | ✅ Full port of #377 client | ❌ Not found | Interaction reach is wrong |
| Floor decoration blocking | `interactive == true` | `blockWalk == 1` | Different objects block |
| Types 10–11, name "null" | ✅ Blocks (solid check only) | ❌ Does not block | Under-blocking unnamed objects |
| Roof types (12–21), solid=false | ✅ Always blocks | ⚠️ Only if `blockWalk != 0` | May miss non-solid roofs |
| `varpDef` (opcode 77) | ✅ Stored for transforms | ❌ Skipped | Morphing objects (doors) broken |
| Footprint rotation (types 9–21) | Unrotated | Auto-detect dual mode | Mostly fixed but heuristic |
| Pathfinding algorithm | A* + Chebyshev | A* + Manhattan+Chebyshev | Minor, not a root cause |
| Cache format (extra idx5, obj.dat) | Not applicable | Supported | No gap — already handled |

