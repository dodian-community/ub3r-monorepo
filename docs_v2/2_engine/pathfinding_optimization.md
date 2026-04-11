# Pathfinding Performance Optimization

This document outlines high-ROI optimizations for the A* pathfinding system to eliminate cycle-time spikes without degrading path quality.

---

## 🚀 1. Node Pooling (Zero-Allocation Search)
**Problem**: The current algorithm creates a `new Node()` for every tile it inspects. Expanding 8,000 nodes across 100 NPCs results in 800,000 objects created per tick.
**Optimization**: Use a **Thread-Local Node Pool**.
- Pre-allocate a large array of `Node` objects on startup.
- Instead of `new Node()`, grab the next available node from the pool.
- Reset the "used" count at the start of every path search.
**Result**: Near-zero GC pressure from pathfinding.

## 🚀 2. Fast-Lookup "Closed Set" (BitSet or LongArray)
**Problem**: `HashSet<Long>` or `HashSet<Position>` is slow due to hashing and object overhead.
**Optimization**: Use a **Primitive BitSet** or a `LongArray` mapped to the local search area.
- Since we only search within a 24-tile `SEARCH_MARGIN`, we can use a fixed-size bitset (e.g., `BitSet(48 * 48)`) to track visited tiles.
- Checking `bitset.get(index)` is significantly faster than `hashSet.contains(position)`.

## 🚀 3. Heuristic: Octile Distance
**Problem**: Manhattan distance is simple but can lead to "zig-zag" paths that require more node expansions.
**Optimization**: If the server supports diagonal movement, switch to **Octile Distance**.
- It more accurately reflects the cost of diagonal vs. cardinal steps, helping A* find the "center" of the path faster and reducing the total number of nodes explored.

## 🚀 4. Early Exit: "Stuck" Detection
**Problem**: The most expensive paths are the ones that **fail**. If a target is inside a solid wall, A* will expand all 8,192 nodes before giving up.
**Optimization**: Implement a "Hierarchical" check or a "Flood Fill" cache.
- Before running a full A* search, check if the target's chunk is even reachable from the player's chunk using a low-resolution "connectivity map."
- If the target is unreachable, exit instantly without exploring a single node.

## 🚀 5. Path Caching (NPCs)
**Problem**: Multiple NPCs chasing the same player often calculate nearly identical paths in the same tick.
**Optimization**: Cache the result of a path for 2-3 ticks.
- If an NPC calculated a path to Player A on Tick 100, and they are still pathing to Player A on Tick 101, they can reuse the remaining steps of the old path instead of running a full A* again.

## 🚀 6. Breadth-First Search (BFS) for Simple Distances
**Problem**: A* is overkill for very short distances (e.g., 2-3 tiles).
**Optimization**: If the target is within 4 tiles and there are no complex obstacles, use a lightweight BFS or a simple "Direct Step" check with collision detection.

---

## Summary of Impact
| Optimization | Target | Difficulty | Stability |
| :--- | :--- | :--- | :--- |
| **Node Pooling** | Memory / GC | Medium | ⭐⭐⭐⭐⭐ |
| **BitSet ClosedSet**| CPU Time | Easy | ⭐⭐⭐⭐⭐ |
| **Path Caching** | CPU Time | Hard | ⭐⭐⭐⭐ |
| **Octile Distance** | Accuracy | Easy | ⭐⭐⭐⭐⭐ |
