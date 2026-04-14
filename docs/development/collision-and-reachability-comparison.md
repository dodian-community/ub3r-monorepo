# Cache Decoding & Pathfinding: System Comparison (Updated)

**Objective:** Compare the object decoding and collision systems of Luna, Mystic Client, and our Game Server to resolve clipping, over-blocking, and reachability issues.

---

## 1. System Architectural Overview

| Feature | Luna (Server) | Mystic (Client) | Our Game Server |
| :--- | :--- | :--- | :--- |
| **Cache Focus** | Vanilla #377 | Extended RS2 (Custom) | Extended RS2 (Mystic) |
| **Object Decoding** | Strict #377 opcodes | Comprehensive (Visuals + Meta) | Extended (Custom + Inference) |
| **Reachability** | Full Client-side Emulation | Client-native | Movement-only (Missing Reach) |
| **Water Handling** | Explicit (Overlay 6) | Visual + Collision | Attribute-only (Missing Overlay) |

---

## 2. Object Definition Decoding (`loc.dat`)

### 2.1 Opcode Comparison & Impact

| Opcode | Field | Luna | Mystic | Our Server | Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **69** | `direction` | `direction` (stored) | `surroundings` (stored) | **Skipped** | **High**: Used for restricting interaction faces (e.g., front of a bank booth). |
| **73** | `obstructive` | `obstructive = true` | `obstructsGround = true` | `obstructive = true` | **Med**: Used for floor-level collision rules. |
| **74** | `hollow` | N/A | `hollow = true` | `breakRouteFinding = true` | **Med**: Mystic uses this to disable collision entirely (`hollow`). |
| **77/92** | `morphs` | `varpDef` (stored) | `childrenIDs` (stored) | **Skipped** | **High**: Necessary for doors/gates that change state via Varbits. |
| **249** | `params` | N/A | N/A | Decoded (stored) | **Low**: Custom parameters for server-side scripts. |

### 2.2 Interactive Inference Logic
Both Mystic and our Server infer interactivity if an object has a name and models but no explicit "interaction" opcode. However, Mystic's inference is more permissive, which we should align with to ensure all usable objects (like decorative doors) remain clickable.

---

## 3. Collision & Pathfinding

### 3.1 Water Tile Blocking
**Luna (`MapTile.isBlocked`):**
```java
public boolean isBlocked() {
    return (attributes & BLOCKED) != 0 || overlay == 6; // Explicit water check
}
```
**Our Server (`DecodedMapTile.isBlocked`):**
```kotlin
fun isBlocked(): Boolean = (attributes and BLOCKED) == BLOCKED // Missing overlay check
```
**Result:** Any tile using the water overlay (ID 6) without the `BLOCKED` flag is currently walkable on our server.

### 3.2 Interaction Reachability (The Missing Link)
Luna provides a dedicated `reachedObject` system that we are currently missing. This logic handles three distinct cases:
1.  **Walls (Types 0-3, 9)**: Check if the player is adjacent to the specific blocked face.
2.  **Decorations (Types 6-8)**: Rotation-aware adjacency checks.
3.  **General Objects (Types 10-11, 22)**: Uses the `direction` (opcode 69) bitmask to determine which of the 4 faces (North, South, East, West) allow interaction.

**Without this logic, our server allows players to "reach" objects through walls as long as they are within distance 1.**

---

## 4. Accurate System Gaps

### Priority 1: Reachability Suite
We need to port `reachedObject`, `reachedWall`, and `reachedFacingEntity` from Luna. These methods must be adapted to use our `CollisionFlag` bitmask layout. 
- **Critical Requirement**: We must stop skipping Opcode 69 in `ObjectDefinitionDecoder.kt` to feed the `direction` bitmask into this logic.

### Priority 2: Terrain Blocking
Update `DecodedMapTile` to recognize `overlay == 6` as a blocked tile. This will immediately fix "Jesus walking" bugs in rivers and lakes.

### Priority 3: Morphing Objects
We are currently skipping Opcode 77 and 92. This means our server has no idea that "Door (Open)" and "Door (Closed)" are linked. We need to decode the `childrenIDs` so the pathfinder can correctly evaluate gated routes.

### Priority 4: Opcode 74 (`hollow`)
Mystic treats Opcode 74 as `hollow`, which explicitly sets `solid = false` and `impenetrable = false`. Our server treats it as `breakRouteFinding`. We should likely adopt the `hollow` behavior for better client-server collision alignment.
