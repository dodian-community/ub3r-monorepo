# Phase 7: Production Hardening

## Goal
Make the server **safe, stable, and performant** for live players. Address security vulnerabilities, performance bottlenecks, and operational concerns that are acceptable in development but critical in production.

## Prerequisites
- Phases 1–6 complete (or at least Phase 1 + 2)
- Access to a production-like environment for testing

---

## 7.1 Security Hardening

### 7.1.1 RSA Login Encryption

**Current state**: Pseudo-RSA (plaintext login credentials).
**Risk**: Credentials can be sniffed on the network.

**Implementation**:

1. Generate a 1024-bit RSA keypair:
```bash
openssl genrsa -out private.pem 1024
openssl rsa -in private.pem -pubout -out public.pem
# Extract modulus and exponent for the client
openssl rsa -in private.pem -text -noout
```

2. Update `LoginProcessorHandler.java`:
```java
// Replace plaintext read with RSA decryption
BigInteger modulus = new BigInteger("YOUR_MODULUS_HERE");
BigInteger privateExponent = new BigInteger("YOUR_PRIVATE_EXPONENT_HERE");

byte[] rsaBlock = new byte[rsaBlockSize];
buf.readBytes(rsaBlock);
BigInteger encrypted = new BigInteger(rsaBlock);
BigInteger decrypted = encrypted.modPow(privateExponent, modulus);
byte[] decryptedBytes = decrypted.toByteArray();
// Parse username/password from decryptedBytes
```

3. Update the Mystic client's `Configuration.java`:
```java
ENABLE_RSA = true;
RSA_MODULUS = "YOUR_MODULUS_HERE";
RSA_EXPONENT = "YOUR_PUBLIC_EXPONENT_HERE";
```

4. Store the private key as an environment variable, never commit to git:
```
RSA_PRIVATE_KEY_MODULUS=...
RSA_PRIVATE_KEY_EXPONENT=...
```

### 7.1.2 Netty Traffic Shaping

**Current state**: No rate limiting on the Netty pipeline.
**Risk**: Packet-flood DDoS can exhaust memory and CPU.

**Implementation**:

```java
// In GameChannelInitializer.java
pipeline.addLast("traffic-shaper",
    new ChannelTrafficShapingHandler(
        0,         // writeLimit (0 = unlimited)
        64 * 1024, // readLimit: 64KB/s per connection
        600        // checkInterval: 600ms (matches tick)
    ));
```

Additionally, add a connection rate limiter:
```kotlin
object ConnectionRateLimiter {
    private val connectionCount = ConcurrentHashMap<String, AtomicInteger>()
    private const val MAX_CONNECTIONS_PER_IP = 3
    private const val MAX_NEW_CONNECTIONS_PER_SECOND = 10

    fun allowConnection(ip: String): Boolean {
        val count = connectionCount.getOrPut(ip) { AtomicInteger(0) }
        return count.incrementAndGet() <= MAX_CONNECTIONS_PER_IP
    }

    fun onDisconnect(ip: String) {
        connectionCount[ip]?.decrementAndGet()
    }
}
```

### 7.1.3 Packet Decoder Safeguards

**Current state**: No max payload size enforcement.
**Risk**: Malformed packets can cause OOM.

```java
// In GamePacketDecoder.java
private static final int MAX_PACKET_SIZE = 5000; // bytes

if (packetSize > MAX_PACKET_SIZE) {
    logger.warn("Dropping oversized packet from {}: opcode={} size={}",
        client.getPlayerName(), opcode, packetSize);
    buf.skipBytes(Math.min(packetSize, buf.readableBytes()));
    return;
}
```

### 7.1.4 Boss Group Thread Count

```java
// In NettyGameServer.java — already identified in docs_v2
EventLoopGroup bossGroup = new NioEventLoopGroup(1); // Was: new NioEventLoopGroup()
```

### 7.1.5 IP-Based Connection Limit

Enforce in the Netty handler:
```java
// In GameChannelInitializer or a custom ChannelInboundHandler
@Override
public void channelActive(ChannelHandlerContext ctx) {
    String ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
    if (!ConnectionRateLimiter.allowConnection(ip)) {
        logger.warn("Rejected connection from {} (too many connections)", ip);
        ctx.close();
        return;
    }
    super.channelActive(ctx);
}
```

---

## 7.2 Performance Hardening

### 7.2.1 Netty Native Transport

Switch from NIO to native transport for the production OS:

```java
// In NettyGameServer.java
boolean useEpoll = Epoll.isAvailable();
boolean useKQueue = KQueue.isAvailable();

EventLoopGroup bossGroup;
EventLoopGroup workerGroup;
Class<? extends ServerChannel> channelClass;

if (useEpoll) {
    bossGroup = new EpollEventLoopGroup(1);
    workerGroup = new EpollEventLoopGroup();
    channelClass = EpollServerSocketChannel.class;
} else if (useKQueue) {
    bossGroup = new KQueueEventLoopGroup(1);
    workerGroup = new KQueueEventLoopGroup();
    channelClass = KQueueServerSocketChannel.class;
} else {
    bossGroup = new NioEventLoopGroup(1);
    workerGroup = new NioEventLoopGroup();
    channelClass = NioServerSocketChannel.class;
}
```

Add Netty native dependencies to `build.gradle.kts`:
```kotlin
dependencies {
    runtimeOnly("io.netty:netty-transport-native-epoll:4.1.x:linux-x86_64")
    runtimeOnly("io.netty:netty-transport-native-kqueue:4.1.x:osx-x86_64")
}
```

### 7.2.2 Pooled ByteBuf Allocator

```
# JVM arguments for production
-Dio.netty.allocator.type=pooled
-Dio.netty.leakDetection.level=disabled
```

### 7.2.3 Pre-Sized Collections

```kotlin
// In WorldSynchronizationService.kt
private val activePlayerBuffer = ArrayList<Client>(MAX_PLAYERS) // Was: ArrayList<Client>()
```

### 7.2.4 JDBC Batching for Audit Logs

```kotlin
// In TradeLog.kt, ChatLog.kt, etc.
object AuditBatchWriter {
    private val queue = ConcurrentLinkedQueue<AuditEntry>()
    private const val BATCH_SIZE = 50
    private const val FLUSH_INTERVAL_MS = 5_000L

    fun enqueue(entry: AuditEntry) {
        queue.add(entry)
        if (queue.size >= BATCH_SIZE) {
            flush()
        }
    }

    fun flush() {
        val batch = ArrayList<AuditEntry>(BATCH_SIZE)
        while (batch.size < BATCH_SIZE) {
            val entry = queue.poll() ?: break
            batch.add(entry)
        }
        if (batch.isEmpty()) return

        DbAsyncRepository.submit {
            connection.prepareStatement(INSERT_AUDIT_SQL).use { ps ->
                for (entry in batch) {
                    ps.setString(1, entry.type)
                    ps.setString(2, entry.player)
                    ps.setString(3, entry.detail)
                    ps.setTimestamp(4, Timestamp(entry.timestamp))
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }
}
```

### 7.2.5 UI Packet Caching

Track last-sent values and only send packets when changed:

```kotlin
// On Client
private val cachedBonusStrings = IntArray(12) { -1 } // Last sent values

fun updateBonusIfChanged(slot: Int, value: Int) {
    if (cachedBonusStrings[slot] != value) {
        cachedBonusStrings[slot] = value
        send(SendString("$value", bonusInterfaceId(slot)))
    }
}
```

---

## 7.3 Graceful Shutdown

### 7.3.1 Shutdown Sequence

```kotlin
object GracefulShutdown {
    private val logger = LoggerFactory.getLogger("uber.admin")

    fun initiate(reason: String, delaySeconds: Int = 30) {
        logger.info("Shutdown initiated: {} (delay={}s)", reason, delaySeconds)

        // 1. Announce to all players
        GameMessage.yell("Server shutting down in $delaySeconds seconds. Please find a safe place!")

        // 2. Block new logins
        LoginBlocker.block("Server shutting down")

        // 3. Schedule shutdown
        GameSchedule.world {
            delayTicks(GameTiming.ticksForMs(delaySeconds * 1000L))

            // 4. Save all players
            logger.info("Saving all {} players...", PlayerRegistry.onlineCount())
            for (player in PlayerRegistry.allOnline()) {
                AccountPersistenceService.saveAndWait(player)
            }
            logger.info("All players saved.")

            // 5. Disconnect all players
            for (player in PlayerRegistry.allOnline()) {
                player.disconnect("Server shutdown: $reason")
            }

            // 6. Flush audit logs
            AuditBatchWriter.flush()

            // 7. Close database pool
            Database.shutdown()

            // 8. Stop Netty
            NettyGameServer.stop()

            // 9. Exit
            logger.info("Shutdown complete. Goodbye!")
            System.exit(0)
        }
    }
}
```

### 7.3.2 JVM Shutdown Hook

```kotlin
// In Server.java startup
Runtime.getRuntime().addShutdownHook(Thread {
    if (GameLoopService.isRunning()) {
        GracefulShutdown.initiate("JVM shutdown hook", delaySeconds = 5)
        Thread.sleep(10_000) // Wait for shutdown to complete
    }
})
```

---

## 7.4 Save Integrity

### 7.4.1 Double-Save Protection

Prevent the save system from corrupting data if two saves overlap:

```kotlin
object SaveIntegrityGuard {
    private val savingPlayers = ConcurrentHashMap.newKeySet<Int>()

    fun beginSave(playerId: Int): Boolean {
        return savingPlayers.add(playerId) // Returns false if already saving
    }

    fun endSave(playerId: Int) {
        savingPlayers.remove(playerId)
    }
}
```

### 7.4.2 Save Verification

After saving, read back the saved data and compare checksums:

```kotlin
fun verifyPlayerSave(playerId: Int, expectedChecksum: Long): Boolean {
    val savedData = loadPlayerData(playerId)
    val actualChecksum = savedData.checksum()
    if (actualChecksum != expectedChecksum) {
        logger.error("Save verification FAILED for player {}: expected={} actual={}",
            playerId, expectedChecksum, actualChecksum)
        return false
    }
    return true
}
```

### 7.4.3 Periodic Auto-Save

```kotlin
// During server startup
GameSchedule.worldRepeating(intervalTicks = 1500) { // Every 15 minutes (1500 * 600ms)
    logger.info("Auto-save: saving {} players", PlayerRegistry.onlineCount())
    for (player in PlayerRegistry.allOnline()) {
        AccountPersistenceService.saveAsync(player)
    }
    true // Continue repeating
}
```

---

## 7.5 Duel/Trade Timeout Fix

Fix the confirmed soft-lock bug (from docs_v2 known issues):

```kotlin
// Add to trade/duel state machine
object TradeTimeoutService {
    private const val TIMEOUT_TICKS = 500 // 5 minutes

    fun checkTimeout(player: Client) {
        if (!player.inTrade && !player.inDuel) return
        val elapsed = GameTiming.currentCycle() - player.tradeStartCycle
        if (elapsed > TIMEOUT_TICKS) {
            logger.info("Trade/duel timeout for {}", player.playerName)
            player.cancelTrade("Trade timed out.")
            player.cancelDuel("Duel timed out.")
        }
    }
}
```

---

## 7.6 JVM Tuning Recommendations

### Production JVM Arguments

```bash
java \
    -server \
    -Xms512m -Xmx1g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=50 \
    -XX:G1HeapRegionSize=16m \
    -XX:+ParallelRefProcEnabled \
    -Dio.netty.allocator.type=pooled \
    -Dio.netty.leakDetection.level=disabled \
    -Djava.net.preferIPv4Stack=true \
    -DDODIAN_LOG_LEVEL=info \
    -jar game-server.jar
```

### Key Tuning Points

| Parameter | Value | Why |
|-----------|-------|-----|
| `-XX:+UseG1GC` | G1 collector | Best for low-pause applications with moderate heap |
| `-XX:MaxGCPauseMillis=50` | 50ms | Keep GC pauses well under the 600ms tick budget |
| `-Xms512m -Xmx1g` | 512MB–1GB | Sufficient for 200–500 players |
| `netty.allocator.type=pooled` | pooled | Reduce ByteBuf allocation pressure |

---

## 7.7 Load Testing

### 7.7.1 Using the stress-client Module

The repo already has a `stress-client` module. Configure it for:

| Test | Players | Duration | What to Watch |
|------|---------|----------|---------------|
| Idle load | 100 | 30 min | Tick budget, memory growth |
| Walking load | 100 | 15 min | Pathfinding CPU, sync packet size |
| Combat load | 50 pairs | 15 min | Hit queue, death processing |
| Login storm | 50 simultaneous | 5 min | Login handler, DB pool |
| Mixed | 200 total | 1 hour | Everything |

### 7.7.2 Performance Targets

| Metric | Target | Alarm |
|--------|--------|-------|
| Average tick time | < 100ms | > 300ms |
| Worst tick time (p99) | < 400ms | > 550ms |
| Memory usage | < 800MB | > 1.2GB |
| GC pauses | < 30ms avg | > 100ms |
| Login time | < 2 seconds | > 5 seconds |
| Save time | < 500ms | > 2 seconds |

---

## 7.8 Operational Runbook

### 7.8.1 Pre-Launch Checklist

- [ ] RSA encryption enabled and tested
- [ ] Traffic shaping configured
- [ ] Packet size limits enforced
- [ ] Connection rate limiting active
- [ ] Boss group thread count = 1
- [ ] Native transport enabled (Epoll/KQueue)
- [ ] Pooled ByteBuf allocator configured
- [ ] JVM tuning arguments set
- [ ] Log levels set to production defaults
- [ ] Auto-save interval configured
- [ ] Graceful shutdown wired to JVM shutdown hook
- [ ] Database backups automated (24-hour cycle)
- [ ] Monitoring: `/api/health` endpoint tested
- [ ] Monitoring: tick budget alerting configured
- [ ] Load test passed with target player count
- [ ] Firewall: only ports 43594 and 4443 exposed
- [ ] DDoS mitigation: external provider configured (if applicable)

### 7.8.2 Emergency Procedures

| Scenario | Action |
|----------|--------|
| Tick budget consistently exceeded | Reduce player cap, check `::tickbreakdown` |
| Memory leak suspected | Take heap dump, check `::pluginstats` for runaway tasks |
| Dupe exploit reported | Immediately `::kick` affected players, check audit logs |
| Database unreachable | Server continues running (saves queue), fix DB, saves auto-drain |
| DDoS attack | Activate firewall rules, block offending IPs |
| Critical bug in content plugin | `::disable <plugin>` to deactivate without restart |

---

## 7.9 Implementation Steps

### Step 1: RSA implementation
- Generate keypair
- Update `LoginProcessorHandler.java`
- Update client `Configuration.java`
- Test login flow

### Step 2: Netty hardening
- Traffic shaping handler
- Connection rate limiter
- Packet size limits
- Boss group thread count

### Step 3: Performance optimizations
- Native transport
- Pooled allocator
- Pre-sized collections
- JDBC batching
- UI packet caching

### Step 4: Graceful shutdown
- Shutdown sequence
- JVM shutdown hook
- Save drain wait

### Step 5: Save integrity
- Double-save guard
- Auto-save timer
- Save verification (optional)

### Step 6: Bug fixes
- Duel/trade timeout
- Interaction queue stale handling improvement

### Step 7: JVM tuning + load testing
- Configure JVM args
- Run stress-client tests
- Profile and fix any bottlenecks

---

## 7.10 Verification Checklist

- [ ] RSA login encryption working (test with updated client)
- [ ] Traffic shaping rejects flood attacks
- [ ] Oversized packets are dropped with warning
- [ ] Connection limit enforced per IP
- [ ] Native transport active on Linux/macOS
- [ ] Graceful shutdown saves all players before exit
- [ ] Auto-save running on configured interval
- [ ] Duel/trade timeout prevents soft-lock
- [ ] Load test passes with target performance metrics
- [ ] All operational runbook items checked

---

## 7.11 Estimated Effort

| Step | Effort |
|------|--------|
| RSA implementation | 4–6 hours |
| Netty hardening | 3–4 hours |
| Performance optimizations | 4–6 hours |
| Graceful shutdown | 2–3 hours |
| Save integrity | 2 hours |
| Bug fixes | 2–3 hours |
| Load testing + tuning | 4–8 hours |
| **Total** | **~25 hours** |

