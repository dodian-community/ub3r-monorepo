# Entity Updating & Synchronization

## Overview
At the end of every 600ms game tick, the server must tell each client what has changed in the world around them. This process is called "Entity Updating". It is the most performance-critical part of the engine and the most strictly bound to the client's protocol expectations.

## The Two-Part Packet
Both `PlayerUpdating` (opcode 81) and `NpcUpdating` (opcode 65) follow a strict two-part structure:

### 1. Movement Encoding (Bit Access)
The server switches the byte stream into "Bit Access" mode. It writes tiny 1-bit or 2-bit flags to tell the client:
- Did this entity move?
- Did they take 1 step (walk) or 2 steps (run)?
- Did they teleport?
- Do they have an "Update Block" coming up?

### 2. Update Blocks (Byte Access)
If the movement encoding indicated that an update block was required, the server writes a specific sequence of bytes containing the new state. 

These blocks *must* be appended in the exact order the client expects them.

## Update Flags & Masks
The server uses `UpdateFlags` to track what state changed during the tick. Each flag corresponds to a specific bitmask.

### Player Masks (Order Matters!)
When writing the Update Block for a player, the server combines the flags into a single mask byte (or short, if it exceeds 255). It then writes the blocks in this exact order:

1.  **`FORCED_MOTION`** (`0x400`): Moving from one tile to another over a specific time (e.g., jumping an Agility obstacle).
2.  **`GRAPHICS`** (`0x100`): A GFX ID and height delay.
3.  **`ANIM`** (`0x8`): An Animation ID and delay.
4.  **`FORCED_CHAT`** (`0x4`): Overhead text string.
5.  **`CHAT`** (`0x80`): Normal chat message (color, effects, text).
6.  **`FACE_ENTITY`** (`0x1`): The index of the entity to turn and look at.
7.  **`APPEARANCE`** (`0x10`): A massive block containing gender, equipment, colors, and combat level. **Note**: This block is cached because it is expensive to build and rarely changes.
8.  **`FACE_COORDINATE`** (`0x2`): X/Y coordinates to turn and look at.
9.  **`HIT`** (`0x20`): Primary damage amount, hit type (miss, normal, poison), and current/max HP.
10. **`HIT2`** (`0x200`): Secondary damage amount (e.g., from a Dragon Dagger special attack).

### NPC Masks (Order Matters!)
1.  **`ANIM`** (`0x10`)
2.  **`HIT2`** (`0x8`)
3.  **`FACE_ENTITY`** (`0x20`)
4.  **`FORCED_CHAT`** (`0x1`)
5.  **`HIT`** (`0x40`)
6.  **`GRAPHICS`** (`0x80`)
7.  **`APPEARANCE`** (`0x4`) - Custom to Mystic Client. Used for transforming NPCs (e.g., Kalphite Queen phases) or changing head icons.
8.  **`FACE_COORDINATE`** (`0x2`)

## The Viewport (`ChunkManager`)
The server cannot send updates for all 2,000 online players to everyone. That would overwhelm the bandwidth. Instead, the `ChunkManager` keeps track of which entities are in which 8x8 tile "Chunks".

During the update phase, the server calculates the player's "Viewport" by gathering all entities in the chunks immediately surrounding the player (typically a radius of 16-18 tiles).

### Local vs. Global Lists
- **`localPlayers` / `localNpcs`**: The list of entities the client is currently aware of.
- If a new entity enters the viewport, they are added to the local list, and the server sends an "Add Entity" instruction.
- If an entity leaves the viewport, the server sends a "Remove Entity" instruction (writing a specific bit flag), and they are dropped from the local list.

## Important Protocol Notes
- **14-Bit NPCs**: As documented in the Client Protocol, Ub3r uses 14 bits to encode the NPC index and ID, unlike the standard 12/13 bits.
- **Player Limit**: A client can only track a maximum of 255 local players. The `PlayerUpdating` class has strict logic to "prune" the list if it exceeds this protocol cap, prioritizing players closer to the camera.