# Kotlin DevX Tasking + Domain Cleanup Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make `game-server` Kotlin runtime/content architecture significantly easier to understand and extend by unifying scheduling APIs, fixing event naming drift, and extracting high-churn scheduling logic from `Client.java` into Kotlin services.

**Architecture:** Use one content-facing scheduling API (`systems/api/content`) backed by `engine/tasking`, keep backward-compatible shims during migration, and enforce boundaries with tests. Move farming runtime under skill-domain naming while preserving behavior. Extract deterministic state-machine logic from Java into Kotlin services so contributors can modify behavior without touching the `Client` god class.

**Tech Stack:** Kotlin/JVM 1.6.21, Java 17, JUnit 5, Gradle (`:game-server:test`), existing `GameTaskRuntime` coroutine task engine.

---

### Task 1: Add Canonical Content Scheduling API (Single Entry Point)

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentScheduling.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/systems/api/content/ContentSchedulingTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.systems.api.content

import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContentSchedulingTest {
    @Test
    fun `world delay executes on expected tick`() {
        GameTaskRuntime.clear()
        val events = mutableListOf<String>()

        ContentScheduling.world {
            events += "start"
            delayTicks(2)
            events += "after-2"
        }

        repeat(4) { GameTaskRuntime.cycleWorld() }
        assertEquals(listOf("start", "after-2"), events)
    }

    @Test
    fun `repeating world loop stops when block returns false`() {
        GameTaskRuntime.clear()
        var loops = 0

        ContentScheduling.worldRepeating(intervalTicks = 1) {
            loops++
            loops < 3
        }

        repeat(6) { GameTaskRuntime.cycleWorld() }
        assertEquals(3, loops)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.api.content.ContentSchedulingTest"`
Expected: FAIL with unresolved reference errors for `ContentScheduling`.

**Step 3: Write minimal implementation**

```kotlin
package net.dodian.uber.game.systems.api.content

import net.dodian.uber.game.engine.tasking.TaskHandle
import net.dodian.uber.game.engine.tasking.TaskPriority
import net.dodian.uber.game.engine.tasking.npcTaskCoroutine
import net.dodian.uber.game.engine.tasking.playerTaskCoroutine
import net.dodian.uber.game.engine.tasking.worldTaskCoroutine
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object ContentScheduling {
    @JvmStatic
    @JvmOverloads
    fun world(
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend ContentScheduleScope.() -> Unit,
    ): TaskHandle = worldTaskCoroutine(priority = priority) { ContentScheduleScope(this).block() }

    @JvmStatic
    @JvmOverloads
    fun player(
        player: Client,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend ContentScheduleScope.() -> Unit,
    ): TaskHandle = playerTaskCoroutine(player = player, priority = priority) { ContentScheduleScope(this).block() }

    @JvmStatic
    @JvmOverloads
    fun npc(
        npc: Npc,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend ContentScheduleScope.() -> Unit,
    ): TaskHandle = npcTaskCoroutine(npc = npc, priority = priority) { ContentScheduleScope(this).block() }

    @JvmStatic
    @JvmOverloads
    fun worldRepeating(
        intervalTicks: Int,
        initialDelayTicks: Int = 0,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend () -> Boolean,
    ): TaskHandle = world(priority) {
        repeatEvery(intervalTicks = intervalTicks, initialDelayTicks = initialDelayTicks) {
            block()
        }
    }
}

class ContentScheduleScope internal constructor(
    private val delegate: Any,
) {
    suspend fun delayTicks(ticks: Int) {
        when (delegate) {
            is net.dodian.uber.game.engine.tasking.WorldTaskContext -> delegate.delay(ticks)
            is net.dodian.uber.game.engine.tasking.PlayerTaskContext -> delegate.delay(ticks)
            is net.dodian.uber.game.engine.tasking.NpcTaskContext -> delegate.delay(ticks)
            else -> error("Unknown schedule context")
        }
    }

    suspend fun repeatEvery(
        intervalTicks: Int,
        initialDelayTicks: Int = 0,
        block: suspend () -> Boolean,
    ) {
        when (delegate) {
            is net.dodian.uber.game.engine.tasking.WorldTaskContext ->
                delegate.repeatEvery(intervalTicks, initialDelayTicks) { block() }
            is net.dodian.uber.game.engine.tasking.PlayerTaskContext ->
                delegate.repeatEvery(intervalTicks, initialDelayTicks) { block() }
            is net.dodian.uber.game.engine.tasking.NpcTaskContext ->
                delegate.repeatEvery(intervalTicks, initialDelayTicks) { block() }
            else -> error("Unknown schedule context")
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.api.content.ContentSchedulingTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentScheduling.kt game-server/src/test/kotlin/net/dodian/uber/game/systems/api/content/ContentSchedulingTest.kt
git commit -m "feat: add canonical content scheduling api"
```

### Task 2: Deprecate Duplicate Scheduling Facades

**Files:**
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentCoroutines.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/tasks/TickTasks.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/systems/api/content/ContentSchedulingDeprecationTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.systems.api.content

import kotlin.reflect.full.findAnnotation
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ContentSchedulingDeprecationTest {
    @Test
    fun `legacy content coroutine facade is deprecated`() {
        val annotation = ContentCoroutines::class.findAnnotation<Deprecated>()
        assertNotNull(annotation)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.api.content.ContentSchedulingDeprecationTest"`
Expected: FAIL because `ContentCoroutines` is not annotated `@Deprecated`.

**Step 3: Write minimal implementation**

```kotlin
@Deprecated(
    message = "Use ContentScheduling instead of ContentCoroutines",
    replaceWith = ReplaceWith("ContentScheduling"),
)
object ContentCoroutines {
    // keep existing methods for compatibility
}
```

Also add deprecation KDoc to `TickTasks` object so engine-level use remains explicit but content modules avoid it.

**Step 4: Run test to verify it passes**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.api.content.ContentSchedulingDeprecationTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentCoroutines.kt game-server/src/main/kotlin/net/dodian/uber/game/tasks/TickTasks.kt game-server/src/test/kotlin/net/dodian/uber/game/systems/api/content/ContentSchedulingDeprecationTest.kt
git commit -m "refactor: deprecate duplicate content task facades"
```

### Task 3: Migrate Content Call Sites Off `GameEventScheduler`

**Files:**
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/content/skills/agility/Agility.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/content/skills/agility/AgilityTravel.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/content/skills/agility/AgilityWerewolf.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/content/skills/thieving/Thieving.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/content/events/partyroom/Balloons.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/content/events/partyroom/DropParty.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/content/commands/dev/DevDebugCommands.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/ContentSchedulerImportBoundaryTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentSchedulerImportBoundaryTest {
    @Test
    fun `content package does not import GameEventScheduler directly`() {
        val root = Paths.get("src/main/kotlin/net/dodian/uber/game/content")
        val violations = Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "kt" }
                .flatMap { path ->
                    Files.readAllLines(path).stream()
                        .filter { it.trim().startsWith("import net.dodian.uber.game.engine.event.GameEventScheduler") }
                        .map { "${path}: $it" }
                }
                .toList()
        }

        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.ContentSchedulerImportBoundaryTest"`
Expected: FAIL listing current content files importing `GameEventScheduler`.

**Step 3: Write minimal implementation**

Replace imports/usages with `ContentTiming` and `ContentScheduling`:

```kotlin
import net.dodian.uber.game.systems.api.content.ContentTiming

ContentTiming.runLaterMs(600) { /* ... */ }
ContentTiming.runRepeatingMs(600) { true }
```

For `DevDebugCommands.kt`, replace `ContentCoroutines.*` and `TickTasks.*` usage with `ContentScheduling.*` and `ContentTiming.currentCycle()` (or `GameCycleClock.currentCycle()` only if truly engine-level).

**Step 4: Run test to verify it passes**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.ContentSchedulerImportBoundaryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/content/skills/agility/Agility.kt game-server/src/main/kotlin/net/dodian/uber/game/content/skills/agility/AgilityTravel.kt game-server/src/main/kotlin/net/dodian/uber/game/content/skills/agility/AgilityWerewolf.kt game-server/src/main/kotlin/net/dodian/uber/game/content/skills/thieving/Thieving.kt game-server/src/main/kotlin/net/dodian/uber/game/content/events/partyroom/Balloons.kt game-server/src/main/kotlin/net/dodian/uber/game/content/events/partyroom/DropParty.kt game-server/src/main/kotlin/net/dodian/uber/game/content/commands/dev/DevDebugCommands.kt game-server/src/test/kotlin/net/dodian/uber/game/architecture/ContentSchedulerImportBoundaryTest.kt
git commit -m "refactor: route content scheduling through content api"
```

### Task 4: Move Farming Runtime to Skill-Domain Package Naming

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/farming/runtime/FarmingRuntimeService.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/farming/runtime/FarmingRuntimeModels.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/farming/runtime/FarmingRunStats.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/farming/runtime/FarmingPersistenceCodec.kt`
- Modify (shim): `game-server/src/main/kotlin/net/dodian/uber/game/systems/world/farming/FarmingRuntimeService.kt`
- Modify (shim): `game-server/src/main/kotlin/net/dodian/uber/game/systems/world/farming/FarmingRuntimeModels.kt`
- Modify (shim): `game-server/src/main/kotlin/net/dodian/uber/game/systems/world/farming/FarmingRunStats.kt`
- Modify (shim): `game-server/src/main/kotlin/net/dodian/uber/game/systems/world/farming/FarmingPersistenceCodec.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentRuntimeApi.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/systems/world/farming/FarmingRuntimeService.kt` (delete old logic after move)
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/systems/skills/farming/runtime/FarmingRuntimeServiceTest.kt` (moved)

**Step 1: Write the failing test**

Create package-path test to enforce new ownership:

```kotlin
package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FarmingRuntimePackageBoundaryTest {
    @Test
    fun `farming runtime canonical package is systems skills farming runtime`() {
        val canonical = Paths.get("src/main/kotlin/net/dodian/uber/game/systems/skills/farming/runtime/FarmingRuntimeService.kt")
        assertTrue(Files.exists(canonical), "Expected canonical farming runtime service in skills domain package")
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.FarmingRuntimePackageBoundaryTest"`
Expected: FAIL because canonical file does not yet exist.

**Step 3: Write minimal implementation**

Move files to new package and keep compatibility shims in `systems/world/farming`:

```kotlin
package net.dodian.uber.game.systems.world.farming

@Deprecated("Use net.dodian.uber.game.systems.skills.farming.runtime.*")
typealias FarmingRuntimeService = net.dodian.uber.game.systems.skills.farming.runtime.FarmingRuntimeService
```

Update imports in `ContentRuntimeApi.kt` to new canonical package.

**Step 4: Run tests to verify behavior parity**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.world.farming.FarmingRuntimeServiceTest" --tests "net.dodian.uber.game.architecture.FarmingRuntimePackageBoundaryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/skills/farming/runtime game-server/src/main/kotlin/net/dodian/uber/game/systems/world/farming game-server/src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentRuntimeApi.kt game-server/src/test/kotlin/net/dodian/uber/game/architecture/FarmingRuntimePackageBoundaryTest.kt game-server/src/test/kotlin/net/dodian/uber/game/systems/world/farming/FarmingRuntimeServiceTest.kt
git commit -m "refactor: move farming runtime under skills domain"
```

### Task 5: Unify Player Death Event Naming

**Files:**
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/engine/lifecycle/PlayerDeathTickService.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/events/ProgressionLifecycleEvents.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/DeathEventNamingBoundaryTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeathEventNamingBoundaryTest {
    @Test
    fun `player death tick posts PlayerDeathEvent not generic DeathEvent`() {
        val source = Files.readString(Paths.get("src/main/kotlin/net/dodian/uber/game/engine/lifecycle/PlayerDeathTickService.kt"))
        assertTrue(source.contains("PlayerDeathEvent("))
        assertFalse(source.contains("DeathEvent("))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.DeathEventNamingBoundaryTest"`
Expected: FAIL because `PlayerDeathTickService` currently posts `DeathEvent`.

**Step 3: Write minimal implementation**

```kotlin
import net.dodian.uber.game.events.combat.PlayerDeathEvent

GameEventBus.post(PlayerDeathEvent(player = player, cycle = player.currentGameCycle))
```

Then remove `DeathEvent` from `ProgressionLifecycleEvents.kt` (keep only genuinely used lifecycle events).

**Step 4: Run test to verify it passes**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.DeathEventNamingBoundaryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/engine/lifecycle/PlayerDeathTickService.kt game-server/src/main/kotlin/net/dodian/uber/game/events/ProgressionLifecycleEvents.kt game-server/src/test/kotlin/net/dodian/uber/game/architecture/DeathEventNamingBoundaryTest.kt
git commit -m "refactor: standardize player death event naming"
```

### Task 6: Extract Duel Countdown State Machine from `Client.java`

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/action/DuelCountdownService.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/systems/action/DuelCountdownServiceTest.kt`
- Modify: `game-server/src/main/java/net/dodian/uber/game/model/entity/player/Client.java`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.systems.action

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DuelCountdownServiceTest {
    @Test
    fun `countdown emits 3 2 1 then fight`() {
        var state = DuelCountdownState.initial()
        val chats = mutableListOf<String>()
        var canAttack = false

        while (!state.done) {
            val step = DuelCountdownService.advance(state)
            step.forceChat?.let { chats += it }
            canAttack = canAttack || step.enableCombat
            state = step.nextState
        }

        assertEquals(listOf("3", "2", "1", "Fight!"), chats)
        assertEquals(true, canAttack)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.action.DuelCountdownServiceTest"`
Expected: FAIL due missing `DuelCountdownState` / `DuelCountdownService`.

**Step 3: Write minimal implementation**

```kotlin
package net.dodian.uber.game.systems.action

data class DuelCountdownState(
    val rawCounter: Int,
    val done: Boolean,
) {
    companion object {
        @JvmStatic
        fun initial(): DuelCountdownState = DuelCountdownState(rawCounter = 7, done = false)
    }
}

data class DuelCountdownStep(
    val nextState: DuelCountdownState,
    val forceChat: String?,
    val enableCombat: Boolean,
)

object DuelCountdownService {
    @JvmStatic
    fun advance(state: DuelCountdownState): DuelCountdownStep {
        val next = state.rawCounter - 1
        if (next < 1) {
            return DuelCountdownStep(DuelCountdownState(next, done = true), "Fight!", enableCombat = true)
        }
        val chat = if (next % 2 == 0) "${next / 2}" else null
        return DuelCountdownStep(DuelCountdownState(next, done = false), chat, enableCombat = false)
    }
}
```

Refactor `Client.startDuel()` to call this service, keeping packet/chat behavior identical.

**Step 4: Run test to verify it passes**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.action.DuelCountdownServiceTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/action/DuelCountdownService.kt game-server/src/test/kotlin/net/dodian/uber/game/systems/action/DuelCountdownServiceTest.kt game-server/src/main/java/net/dodian/uber/game/model/entity/player/Client.java
git commit -m "refactor: extract duel countdown logic from client"
```

### Task 7: Extract Travel Route Validation/Scheduling Decisions from `Client.java`

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/systems/action/TravelRouteService.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/systems/action/TravelRouteServiceTest.kt`
- Modify: `game-server/src/main/java/net/dodian/uber/game/model/entity/player/Client.java`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.systems.action

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TravelRouteServiceTest {
    @Test
    fun `home route refuses non catherby selection`() {
        val decision = TravelRouteService.resolve(home = true, checkPos = 0, buttonId = 3058) { true }
        assertEquals(TravelDecision.Rejected("Please select Catherby!"), decision)
    }

    @Test
    fun `locked non-home route requests unlock dialogue`() {
        val decision = TravelRouteService.resolve(home = false, checkPos = 0, buttonId = 3059) { false }
        assertEquals(TravelDecision.RequireUnlockDialogue(48054), decision)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.action.TravelRouteServiceTest"`
Expected: FAIL due missing `TravelRouteService`.

**Step 3: Write minimal implementation**

```kotlin
package net.dodian.uber.game.systems.action

import net.dodian.uber.game.model.Position

sealed interface TravelDecision {
    data class Approved(val varbitValue: Int, val destination: Position) : TravelDecision
    data class Rejected(val message: String) : TravelDecision
    data class RequireUnlockDialogue(val dialogueId: Int) : TravelDecision
}

object TravelRouteService {
    private val posTrigger = intArrayOf(1, 3, 4, 7, 10, 2, 5, 6, 11)
    private val travel = arrayOf(
        intArrayOf(3057, 2803, 3421, 0),
        intArrayOf(3058, -1, -1, 0),
        intArrayOf(3059, 3511, 3506, 0),
        intArrayOf(3060, 3274, 2798, 0),
        intArrayOf(3056, 2863, 2971, 0),
        intArrayOf(48054, 2772, 3234, 0),
    )

    @JvmStatic
    fun resolve(
        home: Boolean,
        checkPos: Int,
        buttonId: Int,
        unlocked: (Int) -> Boolean,
    ): TravelDecision {
        val idx = travel.indexOfFirst { it[0] == buttonId }
        if (idx == -1) return TravelDecision.Rejected("This will lead you to nothing!")
        if ((!home && idx == 0) || (home && idx != 0)) {
            return TravelDecision.Rejected(if (!home) "You are already here!" else "Please select Catherby!")
        }
        if (travel[idx][1] == -1) {
            return TravelDecision.Rejected("This will lead you to nothing!")
        }
        if (idx > 0 && !unlocked(idx - 1)) {
            return TravelDecision.RequireUnlockDialogue(48054)
        }
        val varbit = if (home) posTrigger[checkPos + 3] else posTrigger[idx - 1]
        return TravelDecision.Approved(varbit, Position(travel[idx][1], travel[idx][2], 0))
    }
}
```

Then refactor `Client.travelTrigger(int checkPos, int buttonId)` to delegate route decisions and keep side effects (varbit/send/dialogue/scheduler) in Java.

**Step 4: Run test to verify it passes**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.action.TravelRouteServiceTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/action/TravelRouteService.kt game-server/src/test/kotlin/net/dodian/uber/game/systems/action/TravelRouteServiceTest.kt game-server/src/main/java/net/dodian/uber/game/model/entity/player/Client.java
git commit -m "refactor: extract travel route logic from client"
```

### Task 8: Add Guard Test for Tasking API Drift (Developer-Facing Contract)

**Files:**
- Create: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/TaskingSurfaceAreaBoundaryTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class TaskingSurfaceAreaBoundaryTest {
    @Test
    fun `content runtime api does not depend on TickTasks directly`() {
        val contentRuntimeApi = Files.readString(
            Paths.get("src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentRuntimeApi.kt")
        )
        assertFalse(contentRuntimeApi.contains("TickTasks"))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.TaskingSurfaceAreaBoundaryTest"`
Expected: FAIL while `ContentRuntimeApi.kt` still references `TickTasks`.

**Step 3: Write minimal implementation**

Refactor `ContentRuntimeApi.kt` to call `ContentScheduling` directly:

```kotlin
ContentScheduling.world(priority = priority) {
    repeatEvery(intervalTicks = intervalTicks, initialDelayTicks = initialDelayTicks) {
        block()
    }
}
```

and use `ContentTiming.currentCycle()` instead of `TickTasks.gameClock()`.

**Step 4: Run test to verify it passes**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.TaskingSurfaceAreaBoundaryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentRuntimeApi.kt game-server/src/test/kotlin/net/dodian/uber/game/architecture/TaskingSurfaceAreaBoundaryTest.kt
git commit -m "refactor: remove ticktasks dependency from content runtime api"
```

### Task 9: Full Verification + Regression Sweep

**Files:**
- Modify: none (verification only)

**Step 1: Run focused suites first**

Run:
- `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.api.content.ContentSchedulingTest"`
- `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.ContentSchedulerImportBoundaryTest"`
- `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.action.DuelCountdownServiceTest"`
- `./gradlew :game-server:test --tests "net.dodian.uber.game.systems.action.TravelRouteServiceTest"`

Expected: PASS.

**Step 2: Run broad module tests**

Run: `./gradlew :game-server:test`
Expected: PASS.

**Step 3: Verify no forbidden direct scheduler imports in content**

Run: `rg -n "import net\.dodian\.uber\.game\.engine\.event\.GameEventScheduler" game-server/src/main/kotlin/net/dodian/uber/game/content -g'*.kt'`
Expected: no matches.

**Step 4: Commit verification checkpoint**

```bash
git add -A
git commit -m "test: verify tasking and domain cleanup regression suite"
```

### Task 10: Optional Follow-Up (Second PR) — Retire Compatibility Shims

**Files:**
- Modify/Delete: deprecated shim files in `systems/world/farming` and deprecated methods in `ContentCoroutines` after one release cycle.
- Test: update architecture tests to forbid these shims entirely.

**Step 1: Write failing boundary test forbidding deprecated shims**

```kotlin
assertFalse(Files.exists(Paths.get("src/main/kotlin/net/dodian/uber/game/systems/world/farming/FarmingRuntimeService.kt")))
```

**Step 2: Remove shims and update imports everywhere**

**Step 3: Run full tests**

Run: `./gradlew :game-server:test`
Expected: PASS.

**Step 4: Commit**

```bash
git add -A
git commit -m "refactor: remove temporary tasking and farming compatibility shims"
```

---

## Execution Notes

- Use `@test-driven-development` on every task.
- Use `@verification-before-completion` before each commit and before final PR.
- Keep Java edits minimal and delegating only; business logic should move into Kotlin services.
- Do not introduce any blocking calls on the 600ms game loop.
- Keep package changes Kotlin-first; only touch Java where integration points require it.
