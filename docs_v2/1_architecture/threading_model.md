# Threading Model & Tick Execution

## The Single-Threaded Philosophy
The Ub3r server employs a strictly single-threaded game engine. While I/O operations (Networking and Database) are handled asynchronously by worker pools, **all gameplay state mutations must occur on the Game Thread**.

This model provides massive benefits:
1.  **No Deadlocks**: Entities interacting with each other (e.g., Trading, Combat) do not require complex locking mechanisms.
2.  **Determinism**: Actions always execute in a predictable, reproducible sequence.
3.  **Tick-Based Accuracy**: Game mechanics that rely on 600ms cycles (like poison damage or pathfinding steps) are guaranteed to fire exactly when expected.

## Thread Boundaries & Ingress

Since Netty receives packets on its own worker threads, and HikariCP returns database queries on its own threads, the server needs a safe way to hand data to the Game Thread.

### 1. Packet Mailboxes
When Netty decodes an incoming packet, it *does not* execute it immediately. Instead, `GamePacketHandler.java` validates the rate limit and calls `client.queueInboundPacket(packet)`. This places the packet into a `ConcurrentLinkedQueue` (the `InboundPacketMailbox`). The Game Thread will process this mailbox later during the tick.

### 2. GameThreadIngress
For non-packet asynchronous tasks (like a database load finishing, or an admin command executed via the Web API), the server uses `net.dodian.uber.game.engine.loop.GameThreadIngress`.
```kotlin
GameThreadIngress.submitDeferred("Player-Login", Runnable {
    // This closure will be executed safely by the Game Thread
    PlayerInitializer().initializeCriticalLoginState(client)
})
```

## The 600ms Tick Lifecycle (`GameLoopService.kt`)

Every 600 milliseconds, the `ScheduledExecutorService` triggers `runTick()`. The tick executes the following phases in exact order:

1.  **`GameThreadTimers.drainDue()`**: Executes any scheduled micro-tasks that are due (e.g., `schedule("respawn", 5000) { ... }`).
2.  **`GameThreadIngress.drainTickIngress()`**: Executes tasks submitted from other threads (like login finalizations).
3.  **`inboundPhase.run()`**: Iterates all online players and polls their `InboundPacketMailbox`, dispatching packets to the appropriate `PacketListener`.
4.  **`worldMaintenancePhase` (Data/State Sync)**:
    *   `runWorldDbInputBuild()` & `runWorldDbResultRead()` & `runWorldDbApply()`: Synchronizes global object state with the database.
    *   `runPlunder()`: Triggers specific minigame logic.
5.  **`npcMainPhase.run()`**: Processes AI, movement, and combat for all active NPCs.
6.  **`playerMainPhase.run()`**: Processes combat, queued tasks, and skilling for all active Players.
7.  **`worldMaintenancePhase` (Environment Sync)**:
    *   `runWorldTasks()`
    *   `runGroundItems()`: Despawns old items, makes private drops public.
    *   `runShops()`: Restocks shop inventories.
8.  **`movementFinalizePhase.run()`**: Resolves movement collisions and calculates which entities are in which chunks for viewport updating.
9.  **`outboundPacketProcessor.run()`**: The critical "Entity Updating" phase. Constructs the movement and state masks for all players/NPCs and flushes the outgoing ByteBufs to Netty.
10. **`entityProcessor.runHousekeepingPhase()`**: Cleans up disconnected clients and resets update flags (e.g., `player.clearUpdateFlags()`).
11. **`GameThreadTimers.drainDue()`**: A final check for timers before the thread sleeps until the next 600ms boundary.

## Performance Constraints
The entire `runTick()` method must complete in under 600ms. If `elapsedMillis > 600`, the server logs a `"Game loop overran tick budget"` warning. Heavy tasks, such as full database saves, are pushed off the thread using `DbAsyncRepository` to prevent overruns.