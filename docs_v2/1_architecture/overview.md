# System Overview: Ub3r Architecture

## Philosophy
The Ub3r server is built as a hybrid architecture: it maintains the legacy 317 protocol to interface with the Mystic Updated Client, but replaces the traditional "monolithic `Client.java`" approach with a modern, modular Kotlin-based engine. 

The architecture enforces strict separation between:
1.  **I/O (Networking/Database)**: Fully asynchronous, running on Netty worker threads and HikariCP database pools.
2.  **Game Logic**: Strictly single-threaded, deterministic, and isolated within the 600ms game loop.

## The Core Layers

### 1. Network Layer (Netty 4.x)
Handles all incoming and outgoing byte streams. 
- **Inbound**: Decrypts the ISAAC stream, reads opcodes/sizes, and places `GamePacket` objects into the `Client`'s thread-safe `InboundPacketMailbox`. It *does not* execute game logic.
- **Outbound**: The game thread pushes `OutgoingPacket` implementations to the Netty channel, which flushes them asynchronously.

### 2. The Game Engine (Tick Scheduler)
A `ScheduledExecutorService` pulses exactly once every 600ms, driving `GameLoopService`.
- **Determinism**: By ensuring all state mutations (moving, dropping items, taking damage) happen on this single thread, the server avoids complex lock management and race conditions.

### 3. Content Registration (KSP)
Rather than instantiating classes manually or using reflection at runtime, Ub3r uses **Kotlin Symbol Processing (KSP)**. The `ksp-processor` module scans the source code during compilation, finds all objects implementing specific interfaces (like `SkillPlugin`), and writes a static `GeneratedPluginModuleIndex` class.

### 4. The Dispatcher Pattern
When a network packet requests an action (e.g., "Click Object 132"), the server uses the `GeneratedPluginModuleIndex` to find the relevant content module.
- `InteractionProcessor`: Checks distance, line-of-sight, and routes the action.
- `ContentRegistry`: Matches the object/NPC ID to the injected KSP module and executes its `click()` logic.

### 5. Asynchronous Persistence
To prevent database latency from lagging the 600ms game tick, saving and loading are decoupled:
- **Snapshotting**: The game thread creates an immutable `PlayerSaveSnapshot` of the player's current state.
- **Queueing**: The snapshot is handed to `DbAsyncRepository`.
- **Execution**: A dedicated database thread pool executes the `UPDATE` SQL statements without blocking the game world.