# Game Loop & Scheduler

## Overview
The beating heart of the Ub3r server is `GameLoopService.kt`. It ensures that all game logic executes sequentially, precisely once every 600 milliseconds.

## The `ScheduledExecutorService`
The server relies on a single-threaded Java `ScheduledExecutorService` named `GameLoopService-Ticker`.
- **Interval**: 600ms.
- **Job**: Executes the `runScheduledTick()` function.
- **Fail-Safe**: The execution is wrapped in a `try-catch(Throwable)` block. If a massive error occurs during a tick (e.g., a NullPointerException in a combat formula), the error is logged via `logLoopFailure`, but the server *will not crash*. The next tick will continue 600ms later.

## Phase Breakdown
Every 600ms tick executes the following phases in a strict, unchangeable order:

1.  **Inbound Packet Phase**
    - Iterates over all connected clients.
    - Polls up to 200 packets from each client's `InboundPacketMailbox`.
    - Dispatches packets to their respective `PacketListener` (e.g., handling walking, clicking objects).
2.  **World Maintenance (Input)**
    - Processes asynchronous responses from the database.
    - Prepares global object updates (like spawning a tree back after it was chopped down).
3.  **NPC Main Phase**
    - Iterates over all active NPCs.
    - Processes `Npc.process()` which handles random walking, aggressive AI aggro-checks, and combat retaliation.
4.  **Player Main Phase**
    - Iterates over all online Players.
    - Processes combat (checking if it's time to swing a weapon).
    - Processes `InteractionQueueTask` (moving towards an object to click it).
    - Processes active Coroutine `GameTask`s (e.g., currently cutting a tree).
5.  **Hit Queue & Ground Items**
    - Processes delayed combat hits (e.g., an arrow landing after travel time).
    - Despawns old ground items and makes private items visible to all.
6.  **Movement Finalization Phase**
    - After all packets, AI, and combat logic have determined *where* entities want to go, this phase actually updates their X/Y coordinates.
    - It handles clipping checks and updates the `ChunkManager` so the server knows which region the entity is now in.
7.  **Outbound Packet Processor (Entity Updating)**
    - The most CPU-intensive phase.
    - Calculates the "Viewport" for every player (who they can see).
    - Constructs the massive bit-packed synchronization packets (`PlayerUpdating` and `NpcUpdating`).
    - Flushes these buffers to the Netty channel to be sent over the network.

## The `GameCycleClock`
The server tracks the total number of ticks that have occurred since startup in `currentCycle`. 
- Many systems (like combat delays or potion timers) do not use `System.currentTimeMillis()`. Instead, they use cycle counts. 
- Example: If a weapon speed is 4, the player's `nextAttackCycle` is set to `currentCycle + 4`.