# Technical Debt & Core Modernization

This document tracks verified architectural debt and high-priority refactoring tasks required to bring the server up to modern engineering standards.

---

## 🏗️ 1. Network & Security (Netty)
Current Status: **Functional but Legacy (NIO)**

- **Transport Upgrade**: Switch from `NioEventLoopGroup` to `EpollEventLoopGroup` (Linux) or `KQueueEventLoopGroup` (macOS). This reduces syscall overhead and improves throughput.
- **DDoS Mitigation**: The Netty pipeline lacks traffic shaping. Need to implement `ChannelTrafficShapingHandler` to prevent packet-flood crashes.
- **RSA Implementation**: As noted in `NetworkConstants.java`, RSA key configuration is currently missing. The protocol uses "Pseudo-RSA" (plaintext) which is vulnerable to credential sniffing.
- **Decoder Safeguards**: `GamePacketDecoder` and `LoginPayloadDecoder` do not enforce max payload sizes, making the server vulnerable to memory exhaustion (OOM) attacks from malformed packets.

## ⚡ 2. Game Engine Performance
Current Status: **Partially Modernized**

- **Zero-Allocation Updating**: `Player.java` still uses `LinkedHashSet<Player> playersUpdating` and `LinkedHashSet<Npc> localNpcs`. These should be replaced with primitive bitsets or pre-allocated arrays to eliminate GC pressure during the 600ms tick.
- **Combat Engine Migration (Phase 7)**: Combat logic is still tightly coupled with the monolithic `Client.process()` loop. It should be fully extracted into the `GameTaskRuntime` system to allow for better testing and tick-stamped accuracy.
- **Garbage Collection (GC) Pressure**: Hot execution paths (Distance checks, entity discovery) frequently instantiate `new Position()` and `Iterator` objects. These should be refactored to use primitive-based comparisons or pooled objects.
- **UI Packet Caching**: Highly-invoked methods like `Client.updateBonus(int)` dispatch `SendString` packets every call. These should be cached so packets are only sent when the value actually changes.

## 💾 3. Database & State Management
Current Status: **Stable but Inefficient**

- **JDBC Batching**: High-volume logging (Trades, Duels, Chat) currently executes one SQL statement per event. This should be refactored to use `addBatch()` and `executeBatch()` to reduce network round-trips to the MySQL server.
- **Standardize SQL**: The codebase contains many MySQL-specific `INSERT ... SET` queries. These should be converted to standard ANSI SQL for better portability and maintainability.
- **World-ID Based Saving**: (TODO in `Client.java`) Implement configuration saving per world ID to support multi-world clusters.

## 🎮 4. Content & Mechanics TODOs
Verified bugs and missing logic from code comments:

- **PvP & Wilderness**: Fix wilderness boundary checks and PvP-specific flags (`Client.java:1047`).
- **Farming Bugs**: Fix loops in item-to-bin input and custom logic for cutting down farming trees (`FarmingService.kt`).
- **Agility Timing**: Adjust `runLater` delays in `Agility.java` to match actual animation durations to prevent visual jitter.
