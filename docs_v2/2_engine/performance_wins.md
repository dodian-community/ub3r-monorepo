# Performance "Easy Wins" (Safe Optimizations)

This document outlines confirmed, low-risk optimizations that reduce CPU and memory waste without affecting gameplay logic or visual fidelity.

---

## 🟢 1. Netty Boss Group Threading
**Location**: `NettyGameServer.java`
**Current Code**:
```java
EventLoopGroup bossGroup = new NioEventLoopGroup();
```
**Problem**: Without a specified thread count, Netty defaults to `CPU Cores * 2`. For a server listening on a single port (`43594`), this spawns 16-32 threads that do nothing but wait for a connection.
**Optimization**: Change to `new NioEventLoopGroup(1)`.
**Safe because**: One thread is more than sufficient to handle the "Accept" phase of TCP connections for thousands of players.

## 🟢 2. Eliminate redundant `Set` allocations in `ChunkManager`
**Location**: `ChunkManager.kt`
**Problem**: The `find()` method currently creates a new `HashSet` or `LinkedHashSet` for every call. During the entity updating phase, this happens hundreds of times per tick.
```kotlin
fun <E : Entity> find(center: Position, type: EntityType, distance: Int): MutableSet<E> {
    return find(center, type, distance, Supplier { HashSet<E>() }, Predicate { true })
}
```
**Optimization**: Use the already-existing `forEach` methods in `ChunkManager` instead of `find()` in the synchronization service. 
**Safe because**: `forEach` performs the same logic but uses a callback (Consumer) instead of collecting into a temporary set, eliminating thousands of short-lived allocations per second.

## 🟢 3. Log Guarding in Hot Paths
**Location**: `NpcUpdating.java`, `PlayerUpdating.java`
**Problem**: Debug logs like `logger.debug("npcMovementWrites viewer={} count={}", ...)` perform string parameter substitution and object array creation even when the log level is set to `INFO`.
**Optimization**: Wrap high-frequency debug logs in an `if (logger.isDebugEnabled())` block.
**Safe because**: It only affects performance when debug logging is *disabled* (standard production mode).

## 🟢 4. Atomic Counter Waste
**Location**: `NpcUpdating.java`
**Problem**: The server uses `AtomicInteger DEBUG_MOVEMENT_WRITE_COUNTER` to track debug metrics.
**Optimization**: Metrics tracking should be moved to the `SynchronizationCycle` metrics system which already uses primitive `Int` fields, avoiding the overhead of `Atomic` operations on every NPC movement write.
**Safe because**: These counters are only used for debug logging and do not affect game state.

## 🟢 5. Pre-sized Collections for Active Players
**Location**: `WorldSynchronizationService.kt`
**Problem**: The `activePlayerBuffer` might resize multiple times if the player count grows.
**Optimization**: Pre-size the initial buffer to the server's `MAX_PLAYERS` capacity (e.g., 2000).
**Safe because**: Memory is allocated once on startup rather than during the critical 600ms tick.
