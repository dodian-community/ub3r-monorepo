# Phase 6: Debugging & Observability

## Goal
Make the server **debuggable in production** with structured logging, tick profiling, content tracing, and admin tooling. When something goes wrong with live players, you should be able to identify the exact content plugin, tick, and player involved within 30 seconds.

## Prerequisites
- Phase 2 (unified plugin system) — `PluginRegistry` available
- Phase 5 (content API) — standardized entry points to instrument

---

## 6.1 Structured Logging

### Replace ad-hoc logging with structured, categorized log output

#### 6.1.1 Log Categories

| Category Logger Name | Purpose | Default Level |
|---------------------|---------|---------------|
| `uber.runtime.loop` | Game loop tick timing, budget warnings | WARN |
| `uber.runtime.task` | Coroutine lifecycle (start, suspend, resume, cancel) | INFO |
| `uber.runtime.net` | Packet decode/encode, connection lifecycle | INFO |
| `uber.runtime.sync` | Entity updating metrics | WARN |
| `uber.content.skill` | Skill action start/stop/reward | DEBUG |
| `uber.content.npc` | NPC interaction dispatch | DEBUG |
| `uber.content.object` | Object interaction dispatch | DEBUG |
| `uber.content.item` | Item interaction dispatch | DEBUG |
| `uber.content.combat` | Combat hit/miss/death | INFO |
| `uber.content.command` | Command execution | INFO |
| `uber.persistence.save` | Player save/load events | INFO |
| `uber.persistence.audit` | Economic audit trail (trade, duel, drop) | INFO |
| `uber.plugin.registry` | Plugin discovery and registration | INFO |
| `uber.plugin.dispatch` | Plugin dispatch (which plugin handled what) | DEBUG |
| `uber.admin` | Admin command execution | INFO |

#### 6.1.2 Structured Log Format

Use MDC (Mapped Diagnostic Context) for structured fields:

```kotlin
object ContentLog {
    private val logger = LoggerFactory.getLogger("uber.content.skill")

    fun skillActionStart(player: Client, skill: Skill, pluginName: String) {
        MDC.put("player", player.playerName)
        MDC.put("playerId", player.odpiId.toString())
        MDC.put("skill", skill.name)
        MDC.put("plugin", pluginName)
        MDC.put("tick", GameTiming.currentCycle().toString())
        logger.info("Skill action started: {} on {}", pluginName, skill.name)
        MDC.clear()
    }

    fun skillActionReward(player: Client, skill: Skill, xp: Double, itemId: Int) {
        MDC.put("player", player.playerName)
        MDC.put("skill", skill.name)
        MDC.put("xp", xp.toString())
        MDC.put("itemId", itemId.toString())
        MDC.put("tick", GameTiming.currentCycle().toString())
        logger.debug("Skill reward: +{}xp, item={}", xp, itemId)
        MDC.clear()
    }
}
```

#### 6.1.3 Log4j2 Configuration Updates

```xml
<!-- log4j2.xml additions -->

<!-- Content category appender — separate file for content debugging -->
<RollingFile name="ContentLog"
    fileName="logs/content.log"
    filePattern="logs/content-%d{yyyy-MM-dd}-%i.log.gz">
    <PatternLayout pattern="%d{HH:mm:ss.SSS} [%level] [%X{player}] [%X{plugin}] %msg%n"/>
    <Policies>
        <SizeBasedTriggeringPolicy size="50MB"/>
        <TimeBasedTriggeringPolicy interval="1"/>
    </Policies>
</RollingFile>

<!-- Tick profiling appender -->
<RollingFile name="TickProfileLog"
    fileName="logs/tick-profile.log"
    filePattern="logs/tick-profile-%d{yyyy-MM-dd}-%i.log.gz">
    <PatternLayout pattern="%d{HH:mm:ss.SSS} %msg%n"/>
    <Policies>
        <SizeBasedTriggeringPolicy size="20MB"/>
    </Policies>
</RollingFile>

<Loggers>
    <Logger name="uber.content" level="DEBUG" additivity="false">
        <AppenderRef ref="ContentLog"/>
    </Logger>
    <Logger name="uber.runtime.loop" level="WARN" additivity="false">
        <AppenderRef ref="TickProfileLog"/>
    </Logger>
</Loggers>
```

---

## 6.2 Tick Profiler

### 6.2.1 Per-Phase Timing

Instrument `GameLoopService.runTick()` to measure each phase:

```kotlin
object TickProfiler {
    data class TickProfile(
        val cycle: Long,
        val totalMs: Double,
        val inboundMs: Double,
        val worldMaintenanceMs: Double,
        val npcPhaseMs: Double,
        val playerPhaseMs: Double,
        val movementMs: Double,
        val syncMs: Double,
        val housekeepingMs: Double,
        val playerCount: Int,
        val npcCount: Int,
    )

    private val recentTicks = ArrayDeque<TickProfile>(600) // Last 6 minutes

    fun record(profile: TickProfile) {
        if (recentTicks.size >= 600) recentTicks.removeFirst()
        recentTicks.addLast(profile)

        if (profile.totalMs > 400) {
            logger.warn("Tick {} took {:.1f}ms (budget=600ms) | sync={:.1f}ms players={} npcs={}",
                profile.cycle, profile.totalMs, profile.syncMs, profile.playerCount, profile.npcCount)
        }
    }

    fun averageMs(lastN: Int = 100): Double {
        return recentTicks.takeLast(lastN).map { it.totalMs }.average()
    }

    fun worstMs(lastN: Int = 100): Double {
        return recentTicks.takeLast(lastN).maxOfOrNull { it.totalMs } ?: 0.0
    }

    fun phaseBreakdown(lastN: Int = 100): Map<String, Double> {
        val ticks = recentTicks.takeLast(lastN)
        return mapOf(
            "inbound" to ticks.map { it.inboundMs }.average(),
            "worldMaint" to ticks.map { it.worldMaintenanceMs }.average(),
            "npcPhase" to ticks.map { it.npcPhaseMs }.average(),
            "playerPhase" to ticks.map { it.playerPhaseMs }.average(),
            "movement" to ticks.map { it.movementMs }.average(),
            "sync" to ticks.map { it.syncMs }.average(),
            "housekeeping" to ticks.map { it.housekeepingMs }.average(),
        )
    }
}
```

### 6.2.2 Admin Commands for Tick Profiling

```
::tickprofile          → Show average/worst tick time for last 100 ticks
::tickprofile 500      → Show for last 500 ticks
::tickbreakdown        → Show per-phase breakdown
::tickspike            → Show ticks that exceeded 400ms
```

---

## 6.3 Content Tracing

### 6.3.1 Per-Plugin Execution Metrics

Track how many times each plugin is invoked and how long it takes:

```kotlin
object PluginMetrics {
    data class PluginStats(
        val pluginName: String,
        var invocations: Long = 0,
        var totalTimeNs: Long = 0,
        var errors: Long = 0,
        var lastInvokedCycle: Long = 0,
    ) {
        val avgTimeMs: Double get() = if (invocations == 0L) 0.0 else (totalTimeNs / invocations) / 1_000_000.0
    }

    private val stats = ConcurrentHashMap<String, PluginStats>()

    fun record(pluginName: String, durationNs: Long, error: Boolean = false) {
        val s = stats.getOrPut(pluginName) { PluginStats(pluginName) }
        s.invocations++
        s.totalTimeNs += durationNs
        if (error) s.errors++
        s.lastInvokedCycle = GameTiming.currentCycle()
    }

    fun topByInvocations(n: Int = 10): List<PluginStats> =
        stats.values.sortedByDescending { it.invocations }.take(n)

    fun topByTime(n: Int = 10): List<PluginStats> =
        stats.values.sortedByDescending { it.avgTimeMs }.take(n)

    fun withErrors(): List<PluginStats> =
        stats.values.filter { it.errors > 0 }.sortedByDescending { it.errors }
}
```

### 6.3.2 Admin Commands for Content Metrics

```
::plugins              → List all registered plugins by type and count
::pluginstats          → Top 10 plugins by invocation count
::pluginslow           → Top 10 plugins by average execution time
::pluginerrors         → Plugins with errors
::plugintrace <name>   → Detailed trace for a specific plugin
```

---

## 6.4 Player Activity Tracing

### 6.4.1 Per-Player Action Log

For debugging individual player issues:

```kotlin
object PlayerTrace {
    // Ring buffer per player — last 50 actions
    private val traces = ConcurrentHashMap<Int, ArrayDeque<TraceEntry>>()

    data class TraceEntry(
        val cycle: Long,
        val timestamp: Long,
        val action: String,
        val detail: String,
    )

    fun log(player: Client, action: String, detail: String = "") {
        val queue = traces.getOrPut(player.odpiId) { ArrayDeque(50) }
        if (queue.size >= 50) queue.removeFirst()
        queue.addLast(TraceEntry(
            cycle = GameTiming.currentCycle(),
            timestamp = System.currentTimeMillis(),
            action = action,
            detail = detail,
        ))
    }

    fun recent(playerId: Int): List<TraceEntry> =
        traces[playerId]?.toList() ?: emptyList()
}
```

### 6.4.2 Admin Commands

```
::trace <username>     → Show last 50 actions for a player
::tracehere            → Show all player activity within 10 tiles
```

---

## 6.5 Error Handling & Reporting

### 6.5.1 Content Plugin Error Isolation

When a content plugin throws an exception, it should NOT crash the tick. Instead:

```kotlin
// In PluginDispatcher
fun dispatchObjectClick(player: Client, option: Int, objectId: Int, ...): Boolean {
    val plugin = PluginRegistry.objectPlugin(objectId) ?: return false
    return try {
        val start = System.nanoTime()
        val result = when (option) {
            1 -> plugin.onFirstClick(player, objectId, position, data)
            // ...
            else -> false
        }
        PluginMetrics.record(plugin.pluginName, System.nanoTime() - start)
        result
    } catch (e: Exception) {
        logger.error("Plugin '${plugin.pluginName}' threw exception handling object click " +
            "(objectId=$objectId, option=$option, player=${player.playerName})", e)
        PluginMetrics.record(plugin.pluginName, 0, error = true)
        PlayerTrace.log(player, "ERROR", "Plugin ${plugin.pluginName}: ${e.message}")
        player.sendMessage("Something went wrong. Please try again.")
        false
    }
}
```

### 6.5.2 Architecture Test: No Broad Catch in Content

The existing `CoreRuntimeExceptionCatchGuardTest` should be extended:

```kotlin
@Test
fun `content plugins must not silently catch Exception`() {
    // Scan content packages for catch(Exception) or catch(Throwable) without re-throw
    // Only PluginDispatcher is allowed to catch broadly (for isolation)
}
```

---

## 6.6 Web API Observability Endpoints

### Add new endpoints to the Spark Web API:

```
GET /api/server-status         → Existing (player list, uptime)
GET /api/tick-profile          → Tick profiler data (avg, worst, breakdown)
GET /api/plugins               → Plugin registry summary
GET /api/plugin-stats          → Top plugins by invocation/time
GET /api/player-trace/:name    → Player action trace (admin auth required)
GET /api/health                → Simple health check (200 OK)
```

---

## 6.7 Implementation Steps

### Step 1: Create logging infrastructure
- `api/debug/ContentLog.kt`
- `api/debug/TickProfiler.kt`
- `api/debug/PluginMetrics.kt`
- `api/debug/PlayerTrace.kt`

### Step 2: Instrument GameLoopService
Add timing around each phase in `runTick()`.

### Step 3: Instrument PluginDispatcher
Wrap all dispatch calls with timing and error isolation.

### Step 4: Add admin commands
Create `CommandPlugin` objects for `::tickprofile`, `::plugins`, `::pluginstats`, `::trace`.

### Step 5: Update Log4j2 configuration
Add content and tick-profile appenders.

### Step 6: Add Web API endpoints
Create new Spark routes for observability data.

### Step 7: Update architecture tests
Extend exception catch guard to content packages.

---

## 6.8 Verification Checklist

- [ ] Structured logging with MDC for player/plugin/tick
- [ ] Tick profiler recording and warning on >400ms ticks
- [ ] Plugin metrics tracking invocations and timing
- [ ] Player trace recording last 50 actions
- [ ] Error isolation in PluginDispatcher (no tick crashes from content bugs)
- [ ] Admin commands working: `::tickprofile`, `::plugins`, `::pluginstats`, `::trace`
- [ ] Log4j2 configured with separate content and tick-profile files
- [ ] Web API health and profiling endpoints functional
- [ ] Architecture test: no broad catch in content packages

---

## 6.9 Estimated Effort

| Step | Effort |
|------|--------|
| Logging infrastructure | 2–3 hours |
| Tick profiler | 2 hours |
| Plugin metrics | 2 hours |
| Player trace | 1 hour |
| Admin commands | 2 hours |
| Log4j2 config | 1 hour |
| Web API endpoints | 2 hours |
| Architecture tests | 1 hour |
| **Total** | **~14 hours** |

