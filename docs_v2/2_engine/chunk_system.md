# Spatial Partitioning: The Chunk System

## Overview
To maintain high performance with hundreds of players and thousands of NPCs, the server cannot perform "global" searches every time it needs to find nearby entities. Instead, it partitions the 32,000 x 32,000 world map into **8x8 tile "Chunks"**.

## 1. The Chunk Manager (`ChunkManager.kt`)
The `ChunkManager` is the orchestrator for all spatial lookups. It stores a map of all "Active" chunks (chunks that contain at least one player or NPC).

- **Data Structure**: `ConcurrentHashMap<Long, ChunkRepository>`
- **Key**: A bit-packed `Long` containing the Chunk X and Chunk Y coordinates.

## 2. The Chunk Repository (`ChunkRepository.kt`)
Every 8x8 chunk has its own repository that tracks the entities currently standing inside its boundaries.
- It maintains separate sets for `Players`, `NPCs`, and `GroundItems`.
- When an entity moves, the `MovementFinalizePhase` checks if they have crossed a chunk boundary. If so, they are removed from the old `ChunkRepository` and added to the new one.

## 3. Use Cases

### Viewport Synchronization (Entity Updating)
During the `OutboundPacketProcessor` phase, the server needs to know "Which NPCs should Player A see?".
1.  The server identifies Player A's current chunk.
2.  It looks at a 3x3 or 5x5 grid of surrounding chunks (the "Viewport").
3.  It only iterates through the NPCs stored in those specific `ChunkRepositories`.
This reduces the complexity from `O(Total NPCs)` to `O(Local NPCs)`, which is a massive performance win.

### Regional Events
Systems like "Global Yell" (within a region) or "Multi-Combat Zones" use the chunk system to quickly identify all players within a certain radius.

### Ground Items
When an item is dropped, it is registered to a chunk. When a player walks into that chunk, the server sends the "Create Ground Item" packet. When the item expires, it is only removed for players who are currently observing that specific chunk.

## 4. Performance Notes
- **Lazy Loading**: Chunks are only created when an entity enters them and are cleared/removed when they become empty to save memory.
- **Thread Safety**: The `ChunkManager` uses concurrent collections because Netty threads might be querying positions while the Game Thread is updating them. However, most mutations are restricted to the `MovementFinalizePhase`.
