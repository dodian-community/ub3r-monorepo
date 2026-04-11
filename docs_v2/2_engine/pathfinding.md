# Pathfinding & Collision

## Overview
The navigation system ensures that players and NPCs can move intelligently through the world, respecting walls, water, and solid objects. It is divided into three core components: the Collision Manager, the Object Clip Service, and the A* Algorithm.

## 1. The Collision Manager
The `CollisionManager` is a massive 3D grid storing the traversability of every tile in the game. 
- It uses bitmasks to store flags for North, East, South, West, and Center collisions.
- **Initialization**: On startup, it loads the static `.map` files from the cache, populating the base terrain (water, un-walkable black areas) and static objects (walls, scenery).

## 2. ObjectClipService
While the cache provides the *static* map, the game world is dynamic. Doors open, players drop cannons, and trees are cut down. `ObjectClipService` manages this dynamic clipping.

### How it Works
When a global object is spawned (e.g., via a quest script or a firemaking action):
1.  The system looks up the `GameObjectData` (the cache definition) for that Object ID.
2.  It checks `isSolid()` and `isWalkable()`.
3.  `ObjectClipService.applyDecodedObject()` calculates the size (e.g., 2x2 tiles) and rotation of the object.
4.  It applies the corresponding collision flags to the `CollisionManager`.

When a door is opened, the service *removes* the wall collision flag for that specific tile and direction, allowing players to walk through.

### Static Overrides
Sometimes the cache data is wrong, or custom content requires a tile to be blocked. `StaticObjectOverrides.kt` allows developers to manually enforce collision on specific tiles during server startup.

## 3. The A* Algorithm (`AStarPathfindingAlgorithm.kt`)
When an entity wants to move from point A to point B, the server uses the A* (A-Star) algorithm to find the optimal path.

### Characteristics
- **Heuristic**: Uses Manhattan Distance to estimate the cost to the destination.
- **Margin**: It restricts the search space to 24 tiles (`SEARCH_MARGIN`) around the start and end points. If a path requires walking 50 tiles out of the way to get around a long wall, A* will give up to save CPU cycles.
- **Max Expansions**: Capped at 8,192 nodes. If the target is unreachable (e.g., inside a locked cage), A* won't infinite-loop. It will exit and return an empty path.

## 4. FollowRouting
Following a moving target (like chasing a player in PVP) or walking to an object to click it uses `FollowRouting.kt`. 

This is a specialized wrapper around A* because you rarely want to walk *onto* the exact same tile as your target.
- **Interaction Distance**: It calculates the nearest valid tile that is adjacent to the target (or within X tiles for Magic/Ranged combat).
- **Corner Peeking**: It includes checks to prevent players from attacking diagonally through the solid corner of a building.