# Production Readiness Checklist

This document outlines the final requirements and best practices for launching the Ub3r server to a production environment.

---

## 🏗️ 1. Infrastructure & Security
- [ ] **Transport Layer**: Switch Netty from NIO to Native Transport (Epoll for Linux, KQueue for macOS) in `NettyGameServer.java`.
- [ ] **RSA Security**: Implement true RSA encryption for the login block. Generate a keypair and update `NetworkConstants.java` and `LoginProcessorHandler.java`.
- [ ] **Traffic Shaping**: Add `GlobalTrafficShapingHandler` to the Netty pipeline to mitigate packet-flood attacks.
- [ ] **Decoder Safeguards**: Implement max-length checks in `GamePacketDecoder` to prevent memory exhaustion from oversized payloads.
- [ ] **Firewall**: Ensure only port `43594` (Game) and `80/4443` (Web API) are exposed.
- [ ] **IP Rate Limiting**: Enforce `MAX_CONNECTIONS_FROM_IP` to prevent socket exhaustion.

## ⚡ 2. Core Performance
- [ ] **Memory Allocation**: Configure the JVM to use Netty's `PooledByteBufAllocator` (`-Dio.netty.allocator.type=pooled`).
- [ ] **Updating Refactor**: Replace `LinkedHashSet` in `Player.java` with primitive bitsets or pre-allocated arrays to eliminate GC pressure.
- [ ] **Combat Migration**: Complete the extraction of combat logic from `Client.process()` into the `GameTaskRuntime`.
- [ ] **Database Batching**: Refactor audit loggers (`TradeLog`, `ChatLog`) to use JDBC batching for high-volume writes.
- [ ] **Pre-size Collections**: Ensure `activePlayerBuffer` and similar lists are pre-sized to `MAX_PLAYERS`.

## 📈 3. Observability & Monitoring
- [ ] **Log Level Management**: Set `DODIAN_LOG_LEVEL=info` for production.
- [ ] **ConsoleAudit Tuning**: Enable `info` level for `trade` and `duel` categories to track economic flow.
- [ ] **Budget Monitoring**: Monitor logs for "Game loop overran tick budget". Repeated warnings indicate a need for hardware upgrades or optimization.
- [ ] **Health Checks**: Implement a monitoring tool to ping the `/api/server-status` endpoint of the Web API.

## 🛠️ 4. Operational Procedures
- [ ] **Hot-Fix Readiness**: Familiarize the team with `::reload` commands for non-restart logic fixes.
- [ ] **Graceful Shutdown**: Always use controlled shutdown to ensure `AccountPersistenceService` drains all pending saves.
- [ ] **Backup Strategy**: Automate 24-hour SQL backups of the `dodiannet` database.
- [ ] **Panic Protocol**: Maintain a "Kill-Switch" (e.g., firewall script) to block ingress instantly in case of a critical exploit.
