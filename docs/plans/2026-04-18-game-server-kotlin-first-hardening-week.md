# Game Server Kotlin-First Hardening Week Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Deliver P0/P1 runtime hardening for packet safety, main-thread safety, skill-plugin ownership, and trade/duel state reliability without adding new gameplay regressions.

**Architecture:** Keep inbound decode/validation in Java listener edge files, move policy/state machines into Kotlin runtime services, and enforce boundaries with architecture tests that fail on legacy bypasses. All blocking persistence stays in async scopes; game tick and Netty paths only do bounded, non-blocking work. For content-facing development, expose stable helper APIs from Kotlin runtime modules and keep skill ownership in plugin definitions only.

**Tech Stack:** Kotlin/JVM, Java 17, Netty, JUnit 5, Gradle multi-module (`:game-server`, `:stress-client`), existing `SkillDoctor` parity tooling.

**Related skills to use while executing:** @test-driven-development @verification-before-completion @defense-in-depth @root-cause-tracing

---

### Task 1: Packet Reject Contract (Shared Kotlin safety contract)

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketRejectReason.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/engine/metrics/PacketRejectTelemetry.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/engine/systems/net/PacketRejectReasonContractTest.kt`

**Step 1: Write the failing test**

```kotlin
package net.dodian.uber.game.engine.systems.net

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PacketRejectReasonContractTest {
    @Test
    fun `reject reasons use stable lowercase snake case wire names`() {
        val invalid = PacketRejectReason.values().filterNot { it.wire.matches(Regex("[a-z0-9_]+")) }
        assertTrue(invalid.isEmpty(), "Invalid reject reason wire keys: $invalid")
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.engine.systems.net.PacketRejectReasonContractTest"`
Expected: FAIL with unresolved symbol `PacketRejectReason`.

**Step 3: Write minimal implementation**

```kotlin
package net.dodian.uber.game.engine.systems.net

enum class PacketRejectReason(val wire: String) {
    SHORT_PAYLOAD("short_payload"),
    MALFORMED_PAYLOAD("malformed_payload"),
    INVALID_COORDINATE("invalid_coordinate"),
    INVALID_SLOT("invalid_slot"),
    INVALID_ID("invalid_id"),
    UNKNOWN_NPC("unknown_npc"),
    OPCODE_DISABLED("opcode_disabled"),
    LISTENER_EXCEPTION("listener_exception"),
}
```

And overload telemetry:

```kotlin
@JvmStatic
fun record(opcode: Int, reason: PacketRejectReason) {
    record(opcode, reason.wire)
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.engine.systems.net.PacketRejectReasonContractTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketRejectReason.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/engine/metrics/PacketRejectTelemetry.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/engine/systems/net/PacketRejectReasonContractTest.kt
git commit -m "feat: add shared packet reject reason contract"
```

### Task 2: Harden inbound listeners with strict bounds and standard rejects

**Files:**
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ObjectInteractionListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/NpcInteractionListener.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ExamineListener.java`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/PacketValidationBoundaryTest.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/netty/listener/in/InteractionListenersBoundaryTest.kt`

**Step 1: Write the failing test**

Add assertions requiring each target listener to reference `PacketRejectReason` and enforce maximum readable bytes checks for variable payload parsing.

```kotlin
assertTrue(source.contains("PacketRejectReason.SHORT_PAYLOAD"))
assertTrue(source.contains("readableBytes() >"))
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.PacketValidationBoundaryTest" --tests "net.dodian.uber.game.netty.listener.in.InteractionListenersBoundaryTest"`
Expected: FAIL because listeners still use string literals and ExamineListener lacks strict max-length guard.

**Step 3: Write minimal implementation**

- Replace string reject literals with `PacketRejectReason` enum usage.
- In `ExamineListener`, enforce exact/upper bounds before decode and reject unknown slot values.
- Add per-field range checks (`slot`, `ID`, `posX`, `posY`) before dispatch.

```java
if (buf.readableBytes() < 10 || buf.readableBytes() > 10) {
    PacketRejectTelemetry.record(packet.opcode(), PacketRejectReason.SHORT_PAYLOAD);
    return;
}
if (slot < 0 || slot > 2) {
    PacketRejectTelemetry.record(packet.opcode(), PacketRejectReason.MALFORMED_PAYLOAD);
    return;
}
```

**Step 4: Run tests to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.PacketValidationBoundaryTest" --tests "net.dodian.uber.game.netty.listener.in.InteractionListenersBoundaryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ObjectInteractionListener.java \
  game-server/src/main/java/net/dodian/uber/game/netty/listener/in/NpcInteractionListener.java \
  game-server/src/main/java/net/dodian/uber/game/netty/listener/in/ExamineListener.java \
  game-server/src/test/kotlin/net/dodian/uber/game/architecture/PacketValidationBoundaryTest.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/netty/listener/in/InteractionListenersBoundaryTest.kt
git commit -m "hardening: enforce strict inbound listener payload bounds"
```

### Task 3: Listener startup validation report + fail-fast critical opcodes

**Files:**
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/PacketListenerManager.java`
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/PacketRepository.java`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketRegistrationReport.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/NettyListenerBoundaryTest.kt`

**Step 1: Write the failing test**

Add boundary test asserting startup validation exists and checks critical opcodes (`2, 17, 35, 70, 72, 132, 155, 192, 228, 230, 234, 252`).

```kotlin
assertTrue(source.contains("validateCriticalOpcodesOrThrow"))
assertTrue(source.contains("intArrayOf(2, 17, 35"))
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.NettyListenerBoundaryTest"`
Expected: FAIL, method does not exist.

**Step 3: Write minimal implementation**

- Add startup validation method in `PacketListenerManager`.
- Produce structured report (registered, missing, duplicate-overwrite count).
- Throw `IllegalStateException` for missing critical opcodes.

```java
private static void validateCriticalOpcodesOrThrow() {
    int[] critical = new int[] {2,17,35,70,72,132,155,192,228,230,234,252};
    List<Integer> missing = new ArrayList<>();
    for (int opcode : critical) {
        if (!repository.has(opcode)) missing.add(opcode);
    }
    if (!missing.isEmpty()) {
        throw new IllegalStateException("Missing critical packet listeners: " + missing);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.NettyListenerBoundaryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/java/net/dodian/uber/game/netty/listener/PacketListenerManager.java \
  game-server/src/main/java/net/dodian/uber/game/netty/listener/PacketRepository.java \
  game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketRegistrationReport.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/architecture/NettyListenerBoundaryTest.kt
git commit -m "hardening: fail fast on missing critical packet listeners"
```

### Task 4: Per-opcode listener exception telemetry with player context

**Files:**
- Modify: `game-server/src/main/java/net/dodian/uber/game/model/entity/player/Client.java`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/engine/metrics/PacketErrorTelemetry.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/CoreRuntimeExceptionCatchGuardTest.kt`

**Step 1: Write the failing test**

Add assertions that queued packet dispatch catch path records `packet.listener.exception` counters and logs player/opcode context.

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.CoreRuntimeExceptionCatchGuardTest"`
Expected: FAIL because catch block only prints debug text.

**Step 3: Write minimal implementation**

In `Client.processQueuedPackets` catch:

```java
PacketRejectTelemetry.record(packet.opcode(), PacketRejectReason.LISTENER_EXCEPTION);
PacketErrorTelemetry.recordListenerException(packet.opcode(), getPlayerName(), getSlot(), packet.size(), ex);
logger.error("Inbound listener exception player={} slot={} opcode={} size={} recent={}",
    getPlayerName(), getSlot(), packet.opcode(), packet.size(), describeRecentInboundPackets(), ex);
```

Telemetry helper:

```kotlin
object PacketErrorTelemetry {
    @JvmStatic
    fun recordListenerException(opcode: Int, playerName: String, slot: Int, size: Int, throwable: Throwable) {
        OperationalTelemetry.incrementCounter("packet.listener.exception.total")
        OperationalTelemetry.incrementCounter("packet.listener.exception.opcode.${opcode.coerceIn(0, 255)}")
        OperationalTelemetry.incrementCounter("packet.listener.exception.player.$playerName")
    }
}
```

**Step 4: Run test to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.CoreRuntimeExceptionCatchGuardTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/java/net/dodian/uber/game/model/entity/player/Client.java \
  game-server/src/main/kotlin/net/dodian/uber/game/engine/metrics/PacketErrorTelemetry.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/architecture/CoreRuntimeExceptionCatchGuardTest.kt
git commit -m "hardening: add packet listener exception telemetry with player context"
```

### Task 5: Tick-thread blocking guardrail (runtime + persistence entrypoints)

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/engine/loop/TickThreadBlockingGuard.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/engine/processing/EntityProcessor.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/persistence/player/PlayerSaveService.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/persistence/account/AccountPersistenceService.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/NonBlockingRuntimeBoundaryTest.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/engine/persistence/PersistenceShutdownSourceGuardTest.kt`

**Step 1: Write the failing test**

Add test asserting persistence synchronous methods call `TickThreadBlockingGuard.requireNotGameThread(...)`.

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.NonBlockingRuntimeBoundaryTest" --tests "net.dodian.uber.game.engine.persistence.PersistenceShutdownSourceGuardTest"`
Expected: FAIL on missing guard call.

**Step 3: Write minimal implementation**

```kotlin
object TickThreadBlockingGuard {
    @JvmStatic
    fun requireNotGameThread(context: String) {
        check(!GameThreadContext.isGameThread()) { "Blocking call on game thread: $context" }
    }
}
```

Call guard at entry of `saveSynchronously` paths and any remaining sync account/persistence wrappers.

**Step 4: Run tests to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.NonBlockingRuntimeBoundaryTest" --tests "net.dodian.uber.game.engine.persistence.PersistenceShutdownSourceGuardTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/engine/loop/TickThreadBlockingGuard.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/engine/processing/EntityProcessor.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/persistence/player/PlayerSaveService.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/persistence/account/AccountPersistenceService.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/architecture/NonBlockingRuntimeBoundaryTest.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/engine/persistence/PersistenceShutdownSourceGuardTest.kt
git commit -m "safety: add game-thread blocking guardrails for persistence"
```

### Task 6: Trade/Duel state machine extraction from Client.java (Kotlin service owns transitions)

**Files:**
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/ui/TradeDuelStateMachine.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/ui/TradeDuelSessionService.kt`
- Modify: `game-server/src/main/java/net/dodian/uber/game/model/entity/player/Client.java`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/TradeDuelTransitionBoundaryTest.kt`

**Step 1: Write the failing test**

Add assertions that session service delegates to state machine methods (`advanceTradeStageOne`, `advanceTradeStageTwo`, `advanceDuelStageOne`, `advanceDuelStageTwo`).

**Step 2: Run test to verify it fails**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.TradeDuelTransitionBoundaryTest"`
Expected: FAIL because state transitions are still direct field toggles.

**Step 3: Write minimal implementation**

```kotlin
object TradeDuelStateMachine {
    @JvmStatic
    fun advanceTradeStageOne(client: Client, other: Client): Boolean { /* move current checks */ return true }
    @JvmStatic
    fun advanceTradeStageTwo(client: Client, other: Client): Boolean { /* move giveItems gate */ return true }
    @JvmStatic
    fun advanceDuelStageOne(client: Client, other: Client): Boolean { /* move duel rule gates */ return true }
    @JvmStatic
    fun advanceDuelStageTwo(client: Client, other: Client): Boolean { /* move start duel gate */ return true }
}
```

Session service calls state machine methods; Client retains only UI/render primitives and inventory mutations.

**Step 4: Run tests to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.TradeDuelTransitionBoundaryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/ui/TradeDuelStateMachine.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/ui/TradeDuelSessionService.kt \
  game-server/src/main/java/net/dodian/uber/game/model/entity/player/Client.java \
  game-server/src/test/kotlin/net/dodian/uber/game/architecture/TradeDuelTransitionBoundaryTest.kt
git commit -m "refactor: move trade duel transitions into kotlin state machine"
```

### Task 7: Skill routing cutover for Prayer/Slayer/Thieving + SkillDoctor CI gate

**Files:**
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/skill/runtime/parity/SkillDoctor.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/InteractionProcessor.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/skill/prayer/Prayer.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/skill/slayer/Slayer.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/skill/thieving/Thieving.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/engine/systems/skills/SkillDoctorTest.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/engine/systems/skills/ContentParityDoctorTest.kt`

**Step 1: Write failing test**

Add SkillDoctor assertions for Prayer/Slayer/Thieving route keys (item click/object click/npc click set).

**Step 2: Run test to verify fail**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.engine.systems.skills.SkillDoctorTest" --tests "net.dodian.uber.game.engine.systems.skills.ContentParityDoctorTest"`
Expected: FAIL until coverage checks are expanded.

**Step 3: Write minimal implementation**

- Add explicit route ownership checks in `SkillDoctor.scanMappedRouteOwnership` for:
  - Prayer bone item click + altar item-on-object.
  - Slayer gem option 1/2/3 and mask option 3 (already partial).
  - Thieving object option 1/2 IDs from plugin component arrays.
- Remove any remaining `InteractionProcessor` legacy skill markers for these skills.

**Step 4: Run tests to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.engine.systems.skills.SkillDoctorTest" --tests "net.dodian.uber.game.engine.systems.skills.ContentParityDoctorTest"`
Expected: PASS with clean report.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/skill/runtime/parity/SkillDoctor.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/InteractionProcessor.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/skill/prayer/Prayer.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/skill/slayer/Slayer.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/skill/thieving/Thieving.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/engine/systems/skills/SkillDoctorTest.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/engine/systems/skills/ContentParityDoctorTest.kt
git commit -m "skills: enforce prayer slayer thieving plugin route ownership"
```

### Task 8: Uniform skill-plugin contract enforcement (Data/Actions/SkillPlugin)

**Files:**
- Modify: `docs/development/skill_plugin_template.md`
- Create: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/SkillPluginLayoutBoundaryTest.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/skill/prayer/PrayerData.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/skill/prayer/PrayerActions.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/skill/prayer/Prayer.kt`

**Step 1: Write failing test**

Create boundary test scanning skill directories to require `*Data.kt`, `*Actions.kt`, and `*SkillPlugin` entrypoint per plugin-owned skill.

**Step 2: Run test to verify fail**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.SkillPluginLayoutBoundaryTest"`
Expected: FAIL for non-conforming module naming.

**Step 3: Write minimal implementation**

- Update template with mandatory enum sets: route IDs, action IDs, policy presets.
- Refactor Prayer module naming to explicitly expose `PrayerSkillPlugin` ownership while keeping behavior unchanged.

**Step 4: Run test to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.SkillPluginLayoutBoundaryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add docs/development/skill_plugin_template.md \
  game-server/src/test/kotlin/net/dodian/uber/game/architecture/SkillPluginLayoutBoundaryTest.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/skill/prayer/PrayerData.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/skill/prayer/PrayerActions.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/skill/prayer/Prayer.kt
git commit -m "standards: enforce uniform skill plugin module contract"
```

### Task 9: Content-facing task runtime helpers and task metadata

**Files:**
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/engine/tasking/GameTaskRuntime.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/engine/tasking/GameTask.kt`
- Create: `game-server/src/test/kotlin/net/dodian/uber/game/engine/tasking/GameTaskRuntimeContentApiTest.kt`

**Step 1: Write failing test**

Add tests for helper APIs:
- `queueSkillAction(player, actionName, ...)`
- `queueNpcAction(npc, actionName, ...)`
- `queueDialogueStep(player, stepName, ...)`

**Step 2: Run test to verify fail**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.engine.tasking.GameTaskRuntimeContentApiTest"`
Expected: FAIL unresolved helper methods.

**Step 3: Write minimal implementation**

```kotlin
@JvmStatic
fun queueSkillAction(player: Client, actionName: String, block: suspend GameTask.() -> Unit): TaskHandle =
    queuePlayer(player, TaskPriority.STANDARD) { setMetadata("skillAction", actionName); block() }
```

Add metadata map in `GameTask`:

```kotlin
private val metadata = HashMap<String, String>()
fun setMetadata(key: String, value: String) { metadata[key] = value }
fun metadataSnapshot(): Map<String, String> = metadata.toMap()
```

**Step 4: Run test to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.engine.tasking.GameTaskRuntimeContentApiTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/engine/tasking/GameTaskRuntime.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/engine/tasking/GameTask.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/engine/tasking/GameTaskRuntimeContentApiTest.kt
git commit -m "feat: add content facing task runtime helper APIs"
```

### Task 10: Event bus naming/payload standardization + skill event catalog

**Files:**
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/engine/event/GameEventBus.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/events/skilling/SkillEvents.kt`
- Create: `game-server/src/main/kotlin/net/dodian/uber/game/events/skilling/SkillEventCatalog.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/EventContractCoverageTest.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/DeathEventNamingBoundaryTest.kt`

**Step 1: Write failing test**

Add assertions enforcing naming suffix `Event` and required payload fields (`client`, `actionName` or domain id).

**Step 2: Run test to verify fail**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.EventContractCoverageTest" --tests "net.dodian.uber.game.architecture.DeathEventNamingBoundaryTest"`
Expected: FAIL on missing catalog and naming constraints.

**Step 3: Write minimal implementation**

- Add `SkillEventCatalog` containing canonical event class list and ownership comments.
- Normalize event names in `SkillEvents.kt` where needed (no behavior change).
- Add optional event metadata tags in `GameEventBus` logging path.

**Step 4: Run tests to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.EventContractCoverageTest" --tests "net.dodian.uber.game.architecture.DeathEventNamingBoundaryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/kotlin/net/dodian/uber/game/engine/event/GameEventBus.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/events/skilling/SkillEvents.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/events/skilling/SkillEventCatalog.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/architecture/EventContractCoverageTest.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/architecture/DeathEventNamingBoundaryTest.kt
git commit -m "refactor: standardize event naming payloads and catalog"
```

### Task 11: Legacy shim removal pass (`compat230`, `legacyNpcDefinition`)

**Files:**
- Modify: `game-server/src/main/java/net/dodian/uber/game/netty/listener/in/NpcInteractionListener.java`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/npc/NpcModuleDsl.kt`
- Create: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/NpcLegacyImportBoundaryTest.kt`

**Step 1: Write failing test**

Assert default for `npc.click.compat230.enabled` is false and `legacyNpcDefinition` usage count is zero for new modules.

**Step 2: Run test to verify fail**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.NpcLegacyImportBoundaryTest"`
Expected: FAIL with current default true and broad legacy usage.

**Step 3: Write minimal implementation**

- Set compat230 default false.
- Keep explicit rollback via startup property override only.
- Mark `legacyNpcDefinition` deprecated with migration target `simpleNpcDefinition` / plugin-native `npcPlugin`.

```kotlin
@Deprecated("Use npcPlugin/simpleNpcDefinition and explicit option routes")
fun legacyNpcDefinition(...)
```

**Step 4: Run test to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.architecture.NpcLegacyImportBoundaryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/java/net/dodian/uber/game/netty/listener/in/NpcInteractionListener.java \
  game-server/src/main/kotlin/net/dodian/uber/game/npc/NpcModuleDsl.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/architecture/NpcLegacyImportBoundaryTest.kt
git commit -m "cleanup: default off npc compat opcode and deprecate legacy npc dsl"
```

### Task 12: UI/network spam cleanup + tick-path allocation cleanup

**Files:**
- Modify: `game-server/src/main/java/net/dodian/uber/game/model/entity/player/Client.java`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/skills/SkillProgressionService.kt`
- Modify: `game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/world/player/PlayerRegistry.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/ui/MagicInterfaceTest.kt`
- Test: `game-server/src/test/kotlin/net/dodian/uber/game/engine/systems/world/OnlinePlayerIndexTest.kt`

**Step 1: Write failing tests**

- Add assertion to prefer cached string updates over raw sendString in stable UI loops.
- Add allocation-sensitive test validating `getLocalPlayers` avoids full array scan path for active players.

**Step 2: Run tests to verify fail**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.ui.MagicInterfaceTest" --tests "net.dodian.uber.game.engine.systems.world.OnlinePlayerIndexTest"`
Expected: FAIL until callsites and local scan path updated.

**Step 3: Write minimal implementation**

- Replace repeated `sendString` loops with `sendCachedString` where stable text is unchanged.
- In `PlayerRegistry.getLocalPlayers`, iterate active snapshot (`playersOnline.values`) instead of `players` full array.
- In trade/duel container refresh paths in `Client.java`, reuse mutable buffers instead of per-call new list allocations.

**Step 4: Run tests to verify pass**

Run: `./gradlew :game-server:test --tests "net.dodian.uber.game.ui.MagicInterfaceTest" --tests "net.dodian.uber.game.engine.systems.world.OnlinePlayerIndexTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add game-server/src/main/java/net/dodian/uber/game/model/entity/player/Client.java \
  game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/skills/SkillProgressionService.kt \
  game-server/src/main/kotlin/net/dodian/uber/game/engine/systems/world/player/PlayerRegistry.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/ui/MagicInterfaceTest.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/engine/systems/world/OnlinePlayerIndexTest.kt
git commit -m "perf: reduce UI spam and hot path allocations"
```

### Task 13: Day-7 verification gate (P0/P1 release criteria)

**Files:**
- Modify: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/PacketValidationBoundaryTest.kt`
- Modify: `game-server/src/test/kotlin/net/dodian/uber/game/architecture/NonBlockingRuntimeBoundaryTest.kt`
- Modify: `game-server/src/test/kotlin/net/dodian/uber/game/engine/systems/skills/SkillDoctorTest.kt`
- Optional scripts: `stress-client` run config in local test harness (no source change required)

**Step 1: Write failing gate test additions**

- Require no silent listener registration failures.
- Require listener exception telemetry symbols exist.
- Require SkillDoctor report clean.

**Step 2: Run targeted suite and confirm failures first**

Run:

```bash
./gradlew :game-server:test \
  --tests "net.dodian.uber.game.architecture.PacketValidationBoundaryTest" \
  --tests "net.dodian.uber.game.architecture.NonBlockingRuntimeBoundaryTest" \
  --tests "net.dodian.uber.game.engine.systems.skills.SkillDoctorTest"
```

Expected: FAIL until all prior tasks are merged.

**Step 3: Run full P0/P1 verification**

Run:

```bash
./gradlew :game-server:test
./gradlew :game-server:runSyncBenchmark
./gradlew :stress-client:run
```

For stress client, set bot config to 300 mixed actions in the launcher UI and run for at least 20 minutes.

**Step 4: Record pass/fail against non-negotiable criteria**

Pass only if all are true:
- No blocking call violations on game tick/Netty paths.
- No skill-owned routes outside SkillPlugin.
- No silent listener registration failures.
- Every listener exception logs player/opcode/context and increments opcode counter.
- `npc.click.compat230.enabled` default-off and rollback documented via explicit property.

**Step 5: Commit verification updates**

```bash
git add game-server/src/test/kotlin/net/dodian/uber/game/architecture/PacketValidationBoundaryTest.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/architecture/NonBlockingRuntimeBoundaryTest.kt \
  game-server/src/test/kotlin/net/dodian/uber/game/engine/systems/skills/SkillDoctorTest.kt
git commit -m "test: enforce p0 p1 release gate criteria"
```

## Delivery Sequence (Day-by-day)

1. Day 1: Tasks 1-5.
2. Day 2: Task 6.
3. Day 2-3: Task 7.
4. Day 3: Task 8.
5. Day 4: Tasks 9-10.
6. Day 5: Task 11.
7. Day 5-6: Task 12.
8. Day 7: Task 13.

## Rollback Plan

1. Keep `npc.click.compat230.enabled=true` as emergency runtime toggle only (default remains false).
2. Keep old Client trade/duel methods callable behind state machine wrappers until one release cycle completes.
3. If packet hardening causes false rejects, rollback by opcode-specific guard branch, not global disable.

