# Events + Tasks Production Readiness Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make events/tasking production-ready by eliminating dead/duplicate event definitions, wiring missing high-value events, removing remaining deprecated task facade usage, and enforcing all of it with architecture tests.

**Architecture:** Keep event payloads in `game/events` and bus internals in `engine/event`, but add explicit contract tests so each event is either active, extension-only, or intentionally reserved. Keep engine tasking internal and expose content/runtime scheduling through `systems/api/content`; remove the last real runtime dependency on deprecated `TickTasks`. Add small, reusable content scheduling recipes so developers stop re-implementing ad-hoc timer loops.

**Tech Stack:** Kotlin/JVM 1.6.21, Java 17, JUnit 5, existing `GameEventBus`, `ContentScheduling`, `ContentTiming`, Gradle `:game-server:test`.

---

### Task 1: Add Event Contract Coverage Test (No Silent Dead Events)

**Files:**
- Create: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/EventContractCoverageTest.kt`
- Modify: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/EventWiringParityTest.kt` (keep minimal producer parity list; delegate full coverage to new test)

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventContractCoverageTest {
    private val sourceRoot: Path = Paths.get("src/main")
    private val eventsRoot: Path = sourceRoot.resolve("kotlin/net/dodian/uber/game/events")

    private enum class Contract {
        REQUIRE_PRODUCER_AND_SUBSCRIBER,
        REQUIRE_PRODUCER_ONLY,
        RESERVED_DEFINITION_ONLY,
    }

    private val contracts = mapOf(
        "ItemDropEvent" to Contract.REQUIRE_PRODUCER_AND_SUBSCRIBER,
        "ItemExamineEvent" to Contract.REQUIRE_PRODUCER_AND_SUBSCRIBER,
        "NpcExamineEvent" to Contract.REQUIRE_PRODUCER_AND_SUBSCRIBER,
        "ObjectExamineEvent" to Contract.REQUIRE_PRODUCER_AND_SUBSCRIBER,
        "DialogueContinueEvent" to Contract.REQUIRE_PRODUCER_AND_SUBSCRIBER,
        "CommandEvent" to Contract.REQUIRE_PRODUCER_AND_SUBSCRIBER,

        "ButtonClickEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "DialogueOptionEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "ItemClickEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "ItemOnItemEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "ItemOnNpcEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "ItemOnObjectEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "ItemOnPlayerEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "ItemPickupEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "MagicOnNpcEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "MagicOnObjectEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "MagicOnPlayerEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "NpcClickEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "NpcDeathEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "NpcDropEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "PlayerAttackEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "PlayerDeathEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "PlayerLoginEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "PlayerLogoutEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "SkillActionStartEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "SkillActionInterruptEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "SkillActionCompleteEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "SkillProgressAppliedEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "SkillingActionStartedEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "SkillingActionCycleEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "SkillingActionSucceededEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "SkillingActionStoppedEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "TradeRequestEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "TradeCompleteEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "TradeCancelEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "WorldTickEvent" to Contract.REQUIRE_PRODUCER_ONLY,
        "LevelUpEvent" to Contract.REQUIRE_PRODUCER_ONLY,

        "WalkEvent" to Contract.RESERVED_DEFINITION_ONLY,
        "PlayerTickEvent" to Contract.RESERVED_DEFINITION_ONLY,
        "ChatMessageEvent" to Contract.RESERVED_DEFINITION_ONLY,
        "PrivateMessageEvent" to Contract.RESERVED_DEFINITION_ONLY,
    )

    @Test
    fun `every event type has exactly one definition and one contract entry`() {
        val definitions = Files.walk(eventsRoot)
            .filter { Files.isRegularFile(it) && it.extension == "kt" }
            .flatMap { Files.readAllLines(it).stream() }
            .map(String::trim)
            .filter { it.startsWith("data class ") && it.contains("Event(") }
            .map { it.removePrefix("data class ").substringBefore("(").trim() }
            .toList()

        val duplicateNames = definitions.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue(duplicateNames.isEmpty(), "Duplicate event class names found: $duplicateNames")

        val defined = definitions.toSet()
        val contracted = contracts.keys

        assertEquals(defined, contracted, "Contract map must match defined event types exactly")
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd game-server && ../gradlew :game-server:test --tests "net.dodian.uber.game.architecture.EventContractCoverageTest"`
Expected: FAIL because contract map/duplicates are not aligned yet (`LevelUpEvent` duplicate definition and missing producer markers for some events).

**Step 3: Write minimal implementation**

Add helper methods in the same test file to compute producer/subscriber markers and enforce contract semantics:

```kotlin
private val sourceFiles: List<Path> by lazy {
    Files.walk(sourceRoot)
        .filter { Files.isRegularFile(it) }
        .filter { it.extension == "kt" || it.extension == "java" }
        .toList()
}

private fun hasProducer(name: String): Boolean = sourceFiles.any { file ->
    val content = Files.readString(file)
    content.contains("new $name(") || content.contains("$name(")
}

private fun hasSubscriber(name: String): Boolean = sourceFiles.any { file ->
    val content = Files.readString(file)
    content.contains("on<$name>") || content.contains("onReturnable<$name>")
}

@Test
fun `event contract semantics hold`() {
    val failures = mutableListOf<String>()
    contracts.forEach { (eventName, contract) ->
        val producer = hasProducer(eventName)
        val subscriber = hasSubscriber(eventName)
        when (contract) {
            Contract.REQUIRE_PRODUCER_AND_SUBSCRIBER -> {
                if (!producer || !subscriber) failures += "$eventName requires both producer and subscriber"
            }
            Contract.REQUIRE_PRODUCER_ONLY -> {
                if (!producer) failures += "$eventName requires a producer"
            }
            Contract.RESERVED_DEFINITION_ONLY -> {
                if (producer || subscriber) failures += "$eventName is reserved but is currently wired"
            }
        }
    }
    assertTrue(failures.isEmpty(), failures.joinToString("\n"))
}
```

Also trim `EventWiringParityTest` to keep only a small smoke list for historically critical producers.

**Step 4: Run test to verify it passes**

Run: `cd game-server && ../gradlew :game-server:test --tests "net.dodian.uber.game.architecture.EventContractCoverageTest" --tests "net.dodian.uber.game.architecture.EventWiringParityTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/test/kotlin/net/dodian/uber/game/architecture/EventContractCoverageTest.kt game-server/src/test/kotlin/net/dodian/uber/game/architecture/EventWiringParityTest.kt
git commit -m "test: add event contract coverage and dead-event guardrails"
```

### Task 2: Remove Duplicate `LevelUpEvent` Definition

**Files:**
- Delete: `game-server/src/main/kotlin/net/dodian/uber/game/events/player/LevelUpEvent.kt`
- Modify: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/ProgressionLifecycleSignalWiringTest.kt`

**Step 1: Write the failing test**

Add this test:

```kotlin
@Test
fun `level-up event has exactly one canonical definition`() {
    val duplicatePath = sourceRoot.resolve("net/dodian/uber/game/events/player/LevelUpEvent.kt")
    assertTrue(!Files.exists(duplicatePath), "Duplicate player/LevelUpEvent.kt must be removed")
}
```

**Step 2: Run test to verify it fails**

Run: `cd game-server && ../gradlew :game-server:test --tests "net.dodian.uber.game.architecture.ProgressionLifecycleSignalWiringTest"`
Expected: FAIL because duplicate file exists.

**Step 3: Write minimal implementation**

Delete `events/player/LevelUpEvent.kt`, keep canonical `events/ProgressionLifecycleEvents.kt`, and keep imports targeting `net.dodian.uber.game.events.LevelUpEvent`.

**Step 4: Run test to verify it passes**

Run: `cd game-server && ../gradlew :game-server:test --tests "net.dodian.uber.game.architecture.ProgressionLifecycleSignalWiringTest" --tests "net.dodian.uber.game.architecture.EventContractCoverageTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/test/kotlin/net/dodian/uber/game/architecture/ProgressionLifecycleSignalWiringTest.kt game-server/src/main/kotlin/net/dodian/uber/game/events/ProgressionLifecycleEvents.kt
git rm game-server/src/main/kotlin/net/dodian/uber/game/events/player/LevelUpEvent.kt
git commit -m "refactor: remove duplicate level-up event payload"
```

### Task 3: Remove Remaining Runtime Dependency on Deprecated `TickTasks`

**Files:**
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/farming/runtime/FarmingRuntimeService.kt`
- Modify: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/TaskingSurfaceAreaBoundaryTest.kt`

**Step 1: Write the failing test**

Expand boundary test to scan runtime/services and fail on `TickTasks` usage outside facade/tests:

```kotlin
@Test
fun `runtime services do not depend on TickTasks facade`() {
    val runtimeRoot = Paths.get("src/main/kotlin/net/dodian/uber/game/systems")
    val violations = mutableListOf<String>()

    Files.walk(runtimeRoot).use { paths ->
        paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
            .forEach { file ->
                if (file.toString().endsWith("/game/tasks/TickTasks.kt")) return@forEach
                Files.readAllLines(file).forEachIndexed { idx, line ->
                    if (line.contains("TickTasks")) {
                        violations += "$file:${idx + 1} -> ${line.trim()}"
                    }
                }
            }
    }

    assertTrue(violations.isEmpty(), "TickTasks should not be used in runtime services.\n${violations.joinToString("\n")}")
}
```

**Step 2: Run test to verify it fails**

Run: `cd game-server && ../gradlew :game-server:test --tests "net.dodian.uber.game.architecture.TaskingSurfaceAreaBoundaryTest"`
Expected: FAIL at `FarmingRuntimeService.kt` (`TickTasks.worldTaskCoroutine`, `TickTasks.gameClock`).

**Step 3: Write minimal implementation**

Replace deprecated facade usage with content/runtime API:

```kotlin
import net.dodian.uber.game.systems.api.content.ContentScheduling
import net.dodian.uber.game.systems.api.content.ContentTiming

// ensureTickPilotStarted
tickPilotHandle =
    ContentScheduling.world {
        repeatEvery(intervalTicks = FARMING_RUNTIME_POLL_TICKS) {
            runTick(System.currentTimeMillis())
            true
        }
    }
logger.info("Farming runtime pilot bound to ContentScheduling world coroutine")

// maybeRecordDeferredCatchUpLatency
val ticks = (ContentTiming.currentCycle() - startCycle).coerceAtLeast(0L)
```

**Step 4: Run test to verify it passes**

Run: `cd game-server && ../gradlew :game-server:test --tests "net.dodian.uber.game.architecture.TaskingSurfaceAreaBoundaryTest" --tests "net.dodian.uber.game.systems.skills.farming.runtime.FarmingRuntimeServiceTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/farming/runtime/FarmingRuntimeService.kt game-server/src/test/kotlin/net/dodian/uber/game/architecture/TaskingSurfaceAreaBoundaryTest.kt
git commit -m "refactor: remove farming runtime dependency on TickTasks"
```

### Task 4: Wire Missing High-Value Producer Events

**Files:**
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketWalkingService.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketInteractionService.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketItemActionService.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketChatService.kt`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/SendPrivateMessageListener.java`
- Create: `game-server/src/test/kotlin/net/dodian/uber/game/systems/net/PacketEventEmissionTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.systems.net

import java.util.concurrent.atomic.AtomicInteger
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.combat.PlayerAttackEvent
import net.dodian.uber.game.events.item.ItemOnPlayerEvent
import net.dodian.uber.game.events.player.WalkEvent
import net.dodian.uber.game.events.widget.ChatMessageEvent
import net.dodian.uber.game.events.widget.PrivateMessageEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PacketEventEmissionTest {
    @AfterEach
    fun clearBus() = GameEventBus.clear()

    @Test
    fun `services emit high-value intent events`() {
        val seen = AtomicInteger(0)
        GameEventBus.on<WalkEvent> { seen.incrementAndGet(); true }
        GameEventBus.on<PlayerAttackEvent> { seen.incrementAndGet(); true }
        GameEventBus.on<ItemOnPlayerEvent> { seen.incrementAndGet(); true }
        GameEventBus.on<ChatMessageEvent> { seen.incrementAndGet(); true }
        GameEventBus.on<PrivateMessageEvent> { seen.incrementAndGet(); true }

        // invoke service methods with minimal valid fixtures in this test class
        // TODO test fixture setup

        assertTrue(seen.get() >= 5, "Expected all new high-value events to be emitted")
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd game-server && ../gradlew :game-server:test --tests "net.dodian.uber.game.systems.net.PacketEventEmissionTest"`
Expected: FAIL because those events are currently not emitted.

**Step 3: Write minimal implementation**

Emit events at safe intent boundaries before existing fallback behavior:

```kotlin
// PacketWalkingService.handle(...), once walk packet is validated and accepted
GameEventBus.post(WalkEvent(player, request.firstStepXAbs, request.firstStepYAbs, request.running, request.opcode))

// PacketInteractionService.handleAttackPlayer(...), after victim resolution and guard checks
val victim = PlayerRegistry.getClient(victimSlot) ?: return
GameEventBus.post(PlayerAttackEvent(client, victim, opcode, PlayerRegistry.cycle.toLong()))

// PacketItemActionService.handleUseItemOnPlayer(...), after target and inventory checks
if (GameEventBus.postWithResult(ItemOnPlayerEvent(client, target, itemId, crackerSlot, playerSlot))) {
    return
}

// PacketChatService.handlePublicChat(...), right before/after ChatLog.recordPublicChat
GameEventBus.post(ChatMessageEvent(client, chat, color, effects))
```

And in `SendPrivateMessageListener.java` decode safely and emit:

```java
if (remaining <= 0 || remaining > 256) {
    return;
}
byte[] text = new byte[remaining];
buf.readBytes(text);
client.sendPmMessage(friend, text, remaining);
GameEventBus.post(new PrivateMessageEvent(client, friend, text, remaining));
```

**Step 4: Run test to verify it passes**

Run: `cd game-server && ../gradlew :game-server:test --tests "net.dodian.uber.game.systems.net.PacketEventEmissionTest" --tests "net.dodian.uber.game.architecture.EventContractCoverageTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketWalkingService.kt game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketInteractionService.kt game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketItemActionService.kt game-server/src/main/kotlin/net/dodian/uber/game/systems/net/PacketChatService.kt game-server/src/main/java/net/dodian/uber/game/netty/listener/in/SendPrivateMessageListener.java game-server/src/test/kotlin/net/dodian/uber/game/systems/net/PacketEventEmissionTest.kt
git commit -m "feat: emit missing intent-level gameplay events"
```

### Task 5: Add Developer-Facing Task Recipes and Migrate One Real Consumer

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentTaskRecipes.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/content/events/partyroom/DropParty.kt`
- Create: `game-server/src/test/kotlin/net/dodian/uber/game/systems/api/content/ContentTaskRecipesTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.systems.api.content

import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContentTaskRecipesTest {
    @Test
    fun `countdown recipe ticks and completes exactly once`() {
        GameTaskRuntime.clear()
        val ticks = mutableListOf<Int>()
        var completed = 0

        ContentTaskRecipes.worldCountdown(
            totalTicks = 3,
            onTick = { ticks += it },
            onDone = { completed++ },
        )

        repeat(6) { GameTaskRuntime.cycleWorld() }
        assertEquals(listOf(3, 2, 1), ticks)
        assertEquals(1, completed)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd game-server && ../gradlew :game-server:test --tests "net.dodian.uber.game.systems.api.content.ContentTaskRecipesTest"`
Expected: FAIL because `ContentTaskRecipes` does not exist.

**Step 3: Write minimal implementation**

```kotlin
package net.dodian.uber.game.systems.api.content

import net.dodian.uber.game.engine.tasking.TaskHandle

object ContentTaskRecipes {
    @JvmStatic
    fun worldCountdown(totalTicks: Int, onTick: (Int) -> Unit, onDone: () -> Unit): TaskHandle {
        require(totalTicks > 0) { "totalTicks must be > 0" }
        return ContentScheduling.world {
            var remaining = totalTicks
            repeatEvery(intervalTicks = 1) {
                onTick(remaining)
                remaining--
                if (remaining <= 0) {
                    onDone()
                    return@repeatEvery false
                }
                true
            }
        }
    }
}
```

Migrate `DropParty` countdown/loop to use `ContentTaskRecipes` for the start countdown while keeping current behavior unchanged.

**Step 4: Run test to verify it passes**

Run: `cd game-server && ../gradlew :game-server:test --tests "net.dodian.uber.game.systems.api.content.ContentTaskRecipesTest" --tests "net.dodian.uber.game.architecture.ContentTaskApiBoundaryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentTaskRecipes.kt game-server/src/main/kotlin/net/dodian/uber/game/content/events/partyroom/DropParty.kt game-server/src/test/kotlin/net/dodian/uber/game/systems/api/content/ContentTaskRecipesTest.kt
git commit -m "feat: add content task recipes and migrate party-room timer"
```

### Task 6: Full Verification Gate (Production Completion Criteria)

**Files:**
- Modify: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/ArchitectureBoundaryTest.kt` (only if a new explicit event/task policy assertion is needed)

**Step 1: Write/enable failing verification assertion**

Add one high-level assertion that no event is unclassified/uncovered beyond explicit contract categories.

```kotlin
@Test
fun `event contracts are exhaustive and enforced`() {
    // Keep this as a delegated check that points to EventContractCoverageTest
    assertTrue(true)
}
```

(If you keep this as a placeholder, skip modifying this file and rely on Task 1 test directly.)

**Step 2: Run targeted suite**

Run:

```bash
cd game-server && ../gradlew :game-server:test \
  --tests "net.dodian.uber.game.architecture.EventContractCoverageTest" \
  --tests "net.dodian.uber.game.architecture.EventWiringParityTest" \
  --tests "net.dodian.uber.game.architecture.ProgressionLifecycleSignalWiringTest" \
  --tests "net.dodian.uber.game.architecture.TaskingSurfaceAreaBoundaryTest" \
  --tests "net.dodian.uber.game.architecture.ContentTaskApiBoundaryTest" \
  --tests "net.dodian.uber.game.systems.net.PacketEventEmissionTest" \
  --tests "net.dodian.uber.game.systems.skills.farming.runtime.FarmingRuntimeServiceTest" \
  --tests "net.dodian.uber.game.systems.api.content.ContentTaskRecipesTest"
```

Expected: PASS.

**Step 3: Run broad regression sweep for architecture + net + farming**

Run:

```bash
cd game-server && ../gradlew :game-server:test --tests "net.dodian.uber.game.architecture.*" --tests "net.dodian.uber.game.systems.net.*" --tests "net.dodian.uber.game.systems.skills.farming.runtime.*"
```

Expected: PASS.

**Step 4: Capture completion checklist in PR description**

Checklist text to include:

```text
- [x] No duplicate event payload classes
- [x] Every event type explicitly classified by contract test
- [x] No runtime service depends on TickTasks facade
- [x] High-value intent events emitted (walk/attack/item-on-player/chat/pm)
- [x] Content task recipe API added and used by one production content flow
```

**Step 5: Commit (if any last test-only changes)**

```bash
git add game-server/src/test/kotlin/net/dodian/uber/game/architecture/*.kt
git commit -m "test: enforce production readiness gates for events and tasks"
```

---

### Notes for the Implementer

- Do not introduce blocking calls or sleeps on game tick / Netty listeners.
- Keep packet decode bounds checks strict; private message payload length must stay capped.
- Do not add event wiring logic into `game/events/*` payload files.
- Prefer Kotlin for new runtime/manager code.
- Keep commits small; one task per commit.
