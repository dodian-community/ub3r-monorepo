# Confirmed Bugs & Known Issues

This document tracks fully confirmed, reproducible bugs within the Ub3r server codebase. 

## 1. Dueling Confirmation Soft-Lock
*   **Severity**: Medium
*   **Location**: `Client.java` (Duel Confirmation Logic)
*   **Description**: The dueling state machine requires both players to confirm on Screen 1 and Screen 2. If Player A accepts the duel, but Player B leaves the interface open without clicking accept or decline, Player A is "soft-locked". They cannot walk away, teleport, or engage in other activities because `inDuel` is true, and there is no timeout mechanism to automatically cancel the duel request after a period of inactivity.
*   **Proof**: Reviewing `Client.java` shows no scheduled task or tick-based timeout that resets `duelRequested` or `inDuel` if the other player goes idle on the interface.

## 2. Interaction Queue Staling
*   **Severity**: Low
*   **Location**: `InteractionQueueTask.kt`
*   **Description**: When a player clicks an object from far away, an `InteractionQueueTask` is created. If the pathfinding algorithm fails to find a path (e.g., the object is completely surrounded by walls), the task enters a "waiting" state. While the system does have a `staleTicks` counter, it can sometimes result in the player standing still without any feedback ("I can't reach that") until they click somewhere else to overwrite the `pendingInteraction`.
*   **Proof**: The `execute()` method in `InteractionQueueTask` increments `staleTicks` but relies on the player overwriting the intent to fully clear the "stuck" feeling in some edge cases.

## 3. Netty Boss Group Thread Count
*   **Severity**: Low (Performance Optimization)
*   **Location**: `NettyGameServer.java`
*   **Description**: The `NioEventLoopGroup` for the "bossGroup" (which only accepts incoming connections) is instantiated without specifying a thread count, meaning it defaults to `CPU Cores * 2`. For a single port (`43594`) accepting connections, a single thread is optimal. Spawning 16 or 32 threads just to listen on one port is a waste of resources.
*   **Proof**: `EventLoopGroup bossGroup = new NioEventLoopGroup();` should be `EventLoopGroup bossGroup = new NioEventLoopGroup(1);`.

*(Note: Add more confirmed bugs here as they are discovered and verified against the source code.)*