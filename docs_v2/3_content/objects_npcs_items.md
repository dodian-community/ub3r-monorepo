# Objects, NPCs, and Items

## Overview
In the Ub3r architecture, there is a strict separation between **Logic** (Kotlin Code) and **Data** (Definitions and Spawns). This document explains where the data comes from and how it is managed.

## 1. NPCs (Non-Player Characters)
*   **Definitions (`GAME_NPC_DEFINITIONS`)**: This SQL table defines what an NPC *is*. It includes its Name, Description, Combat Level, Max HP, and combat stats. The server loads this into memory on startup.
*   **Spawns (`GAME_NPC_SPAWNS` / `npc_Spawn.json`)**: This defines *where* an NPC is. It includes the NPC ID, X, Y, Height level, and a walk range (how far it can wander from its spawn point). The server supports loading spawns from both the SQL database and local JSON files.
*   **Drops (`GAME_NPC_DROPS`)**: This SQL table defines what an NPC drops when it dies, including the Item ID, minimum/maximum amount, and the percentage chance of the drop occurring.

## 2. Items
*   **Definitions (`GAME_ITEM_DEFINITIONS`)**: This SQL table is the absolute source of truth for items. It defines:
    *   **Name & Description**
    *   **Properties**: Is it stackable? Is it tradeable?
    *   **Economy**: High alchemy value, Shop value.
    *   **Equipment**: If it's wearable, which slot does it go in? What are the stat bonuses (Strength, Magic Accuracy, etc.)?
*   **Ground Spawns**: Some items are coded to spawn globally on the ground (e.g., a Bronze Dagger in Lumbridge). These are typically managed in `GlobalGroundItemSpawns.kt`.

## 3. World Objects
*   **Definitions (Client Cache)**: The definition of a world object (Is it solid? How big is it? What are its interaction options?) is **not** stored in SQL. It is loaded directly from the client's cache data files (`loc.dat`/`loc.idx`). The server parses these files on startup using `GameObjectData`.
*   **Map Spawns (Client Cache)**: The default placement of trees, walls, and buildings is loaded from the map files in the cache.
*   **Custom Spawns (`GAME_WORLD_OBJECTS`)**: If a developer wants to permanently add a bank booth to a location that doesn't have one in the cache, they add a row to this SQL table. The server will spawn it on top of the map.
*   **Overrides (`StaticObjectOverrides.kt`)**: If the cache says a tile is solid, but you want players to walk through it (or vice versa), you add an entry here to force the `CollisionManager` to ignore the cache.

## The Reload Commands
Because Data (unlike Logic) is stored in the database, it can be updated while the server is running.
If an admin updates the drop rate of an item in the database, they do not need to restart the server. They simply type `::reloadnpc` in-game. The server will query the database on a background thread and seamlessly update the in-memory HashMaps.