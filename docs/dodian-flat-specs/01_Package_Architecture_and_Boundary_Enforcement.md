# Spec 01: Package Architecture and Boundary Enforcement
### Dodian-Flat Final Draft — Based on `game-server old` Actual Codebase Audit

---

> ⛔ **READ [00_MASTER_RULES_READ_FIRST.md](./00_MASTER_RULES_READ_FIRST.md) BEFORE THIS SPEC.**
> All code examples in this document illustrate how **existing** code should be restructured.
> No new NPC scripts, dialogues, interfaces, items, or gameplay features may be added during this migration.
> Moving a file is the only permitted action. Behavior must be identical before and after.

---

## 1. Executive Summary

This document is the ground-truth blueprint for the package structure in `game-server old`. Unlike the reference spec, this version is written against the actual directory tree and source files that exist today — not against a hypothetical future state.

The codebase is already in a **partial migration state**. As of this writing the following top-level namespaces exist under `net.dodian.uber.game`:

| Package | Status |
|:---|:---|
| `net.dodian.uber.game.engine` | Partially established — `loop`, `sync`, `tasking`, `net`, `phases`, `scheduler`, `processing`, `metrics`, `lifecycle` all present |
| `net.dodian.uber.game.systems` | Partially established — `interaction`, `ui`, `action`, `combat`, `animation`, `api`, `world`, `zone` present |
| `net.dodian.uber.game.content` | Legacy structure — nested 3+ levels deep, still in heavy use |
| `net.dodian.uber.game.model` | Mixed — partly engine concern, partly player concern |
| `net.dodian.uber.game.persistence` | Kotlin — already substantially modernized |
| `net.dodian.uber.game.event` | Flat event bus machinery + event data classes colocated here |
| `net.dodian.uber.game.config` | Small — `DotEnv`, runtime config keys |
| `net.dodian.uber.game.webapi` | Separate — Spark Java REST endpoints |
| `net.dodian.uber.game.netty` | Java source — Netty bootstrapping, codecs, packet classes in `.java` |

The goal of this spec is to clarify **exactly which moves are safe**, which boundaries already exist, and what must still be done. No gameplay logic will change. No MySQL schema changes are permitted. All moves are purely structural.

---

## 2. The Three-Tier Boundary Model (Reality Check)

### 2.1 What Already Exists

The engine/systems/content split has been started but not completed. Key engine packages are already correctly placed:

- `net.dodian.uber.game.engine.loop` → `GameLoopService`, `GameCycleClock` (present)
- `net.dodian.uber.game.engine.sync` → Full player/NPC sync pipeline (present, complex, do not disturb)
- `net.dodian.uber.game.engine.tasking` → `GameTask`, `GameTaskRuntime`, `TaskCoroutineFacade` (present)
- `net.dodian.uber.game.engine.net` → Kotlin-side net bridge (partially present)

The following packages are **NOT yet correctly placed** and require migration work:

- `net.dodian.uber.game.event` lives at the root. The bus machinery (`GameEventBus`, `EventListener`, `EventFilter`, etc.) should be in `engine.event`. The data-only event classes (`NpcClickEvent`, `ObjectClickEvent`, etc.) should be flat in `net.dodian.uber.game.events`.
- `net.dodian.uber.game.model` is a grab-bag. Entity models (`entity/npc`, `entity/player`) live in Java under `net/dodian/uber/game/model/entity`. Kotlin wrappers (`Npc`, `Position`, `UpdateFlags`, `GameItem`, `ItemContainer`, `RS2Object`, `Chunk`, etc.) live under Kotlin's `net.dodian.uber.game.model`. These will stay largely in place — this is not the migration's highest priority.
- `net.dodian.uber.game.content.interfaces` is 17 sub-packages deep. This must be flattened.
- `net.dodian.uber.game.content.skills` is 3 levels deep per skill. This must be consolidated.
- `net.dodian.uber.game.content.npcs.spawns` contains all NPC scripts. This must be flattened to `content.npcs`.

### 2.2 Definitive Three-Tier Boundaries

```
net.dodian.uber.game.engine     ← Infrastructure (sealed)
net.dodian.uber.game.systems    ← Translation / Bridge / DSL APIs
net.dodian.uber.game.content    ← Game domain (content dev territory)
net.dodian.uber.game.events     ← Flat signal data classes only (no logic)
net.dodian.uber.game.model      ← Shared domain models (entity, item, world)
net.dodian.uber.game.persistence← Database I/O (sealed from content)
net.dodian.uber.game.event      ← Transitional: bus machinery (target: engine.event)
```

---

## 3. Definitive Package Migration Map

The following table specifies the **precise move** for every source folder that must change. Folders marked `STAY` require no package move — only internal refactoring noted in other specs.

### 3.1 Engine Moves (No Content Dev Needed)

| Current Path (Kotlin) | Target Path | Action | Notes |
|:---|:---|:---|:---|
| `uber/game/event/GameEventBus.kt` | `uber/game/engine/event/GameEventBus.kt` | Move | Keep `@JvmStatic` annotations |
| `uber/game/event/EventListener.kt` | `uber/game/engine/event/EventListener.kt` | Move | |
| `uber/game/event/EventFilter.kt` | `uber/game/engine/event/EventFilter.kt` | Move | |
| `uber/game/event/GameEvent.kt` | `uber/game/engine/event/GameEvent.kt` | Move | Interface only, no logic |
| `uber/game/event/GameEventScheduler.kt` | `uber/game/engine/event/GameEventScheduler.kt` | Move | |
| `uber/game/event/ReturnableEventListener.kt` | `uber/game/engine/event/ReturnableEventListener.kt` | Move | |
| `uber/game/event/bootstrap/` | `uber/game/engine/event/bootstrap/` | Move | |
| `uber/game/config/` | `uber/game/engine/config/` | Move | `DotEnvConfig`, runtime keys |

### 3.2 Flat Signal Moves

| Current Path (Kotlin) | Target Path | Action | Notes |
|:---|:---|:---|:---|
| `uber/game/event/events/NpcClickEvent.kt` | `uber/game/events/NpcClickEvent.kt` | Move | Data class only |
| `uber/game/event/events/ObjectClickEvent.kt` | `uber/game/events/ObjectClickEvent.kt` | Move | Data class only |
| `uber/game/event/events/ButtonClickEvent.kt` | `uber/game/events/ButtonClickEvent.kt` | Move | Data class only |
| `uber/game/event/events/CommandEvent.kt` | `uber/game/events/CommandEvent.kt` | Move | Data class only |
| `uber/game/event/events/DialogueContinueEvent.kt` | `uber/game/events/DialogueContinueEvent.kt` | Move | Data class only |
| `uber/game/event/events/DialogueOptionEvent.kt` | `uber/game/events/DialogueOptionEvent.kt` | Move | Data class only |
| `uber/game/event/events/PlayerTickEvent.kt` | `uber/game/events/PlayerTickEvent.kt` | Move | Data class only |
| `uber/game/event/events/WorldTickEvent.kt` | `uber/game/events/WorldTickEvent.kt` | Move | Data class only |
| `uber/game/event/events/skilling/SkillingEvents.kt` | `uber/game/events/skilling/SkillingEvents.kt` | Move | Data classes only |

### 3.3 Content Flattening (Highest Volume Work)

| Current Path (Kotlin) | Target Path | Action |
|:---|:---|:---|
| `content/npcs/spawns/*.kt` | `content/npcs/*.kt` | Flatten — remove `spawns/` subdirectory |
| `content/interfaces/bank/` | `content/ui/BankInterface.kt` | Consolidate into one file |
| `content/interfaces/trade/` | `content/ui/TradeInterface.kt` | Consolidate |
| `content/interfaces/duel/` | `content/ui/DuelInterface.kt` | Consolidate |
| `content/interfaces/crafting/` | `content/ui/CraftingInterface.kt` | Consolidate |
| `content/interfaces/smithing/` | `content/ui/SmithingInterface.kt` | Consolidate |
| `content/interfaces/prayer/` | `content/ui/PrayerInterface.kt` | Consolidate |
| `content/interfaces/magic/` | `content/ui/MagicInterface.kt` | Consolidate |
| `content/interfaces/fletching/` | `content/ui/FletchingInterface.kt` | Consolidate |
| `content/interfaces/skilling/` | `content/ui/SkillingInterface.kt` | Consolidate |
| `content/interfaces/combat/` | `content/ui/CombatInterface.kt` | Consolidate |
| `content/interfaces/appearance/` | `content/ui/AppearanceInterface.kt` | Consolidate |
| `content/interfaces/settings/` | `content/ui/SettingsInterface.kt` | Consolidate |
| `content/interfaces/dialogue/` | Handled by `systems.ui.dialogue` | Delete and defer |
| `content/interfaces/slots/` | `content/ui/EquipmentInterface.kt` | Consolidate |
| `content/interfaces/rewards/` | `content/ui/RewardsInterface.kt` | Consolidate |
| `content/interfaces/quests/` | `content/ui/QuestInterface.kt` | Consolidate |
| `content/interfaces/emotes/` | `content/ui/EmotesInterface.kt` | Consolidate |
| `content/interfaces/skillguide/` | `content/ui/SkillGuideInterface.kt` | Consolidate |
| `content/interfaces/travel/` | `content/ui/TravelInterface.kt` | Consolidate |
| `content/interfaces/partyroom/` | `content/ui/PartyRoomInterface.kt` | Consolidate |
| `content/interfaces/ui/` | `content/ui/` | Merge in-place |
| `content/skills/mining/` | `content/skills/Mining.kt` (Brain) + `content/skills/MiningData.kt` (Data) | Consolidate per Spec 07 |
| `content/skills/woodcutting/` | `content/skills/Woodcutting.kt` + `WoodcuttingData.kt` | Consolidate |
| `content/skills/fishing/` | `content/skills/Fishing.kt` + `FishingData.kt` | Consolidate |
| `content/skills/cooking/` | `content/skills/Cooking.kt` + `CookingData.kt` | Consolidate |
| `content/skills/crafting/` | `content/skills/Crafting.kt` + `CraftingData.kt` | Consolidate |
| `content/skills/fletching/` | `content/skills/Fletching.kt` + `FletchingData.kt` | Consolidate |
| `content/skills/smithing/` | `content/skills/Smithing.kt` + `SmithingData.kt` | Consolidate |
| `content/skills/herblore/` | `content/skills/Herblore.kt` + `HerbloreData.kt` | Consolidate |
| `content/skills/thieving/` | `content/skills/Thieving.kt` + `ThievingData.kt` | Consolidate |
| `content/skills/agility/` | `content/skills/Agility.kt` + agility course files remain flat | Consolidate |
| `content/skills/farming/` | `content/skills/Farming.kt` (Brain only, data in systems) | Consolidate |
| `content/skills/prayer/` | `content/skills/Prayer.kt` | Consolidate |
| `content/skills/runecrafting/` | `content/skills/Runecrafting.kt` | Consolidate |
| `content/skills/slayer/` | `content/skills/Slayer.kt` | Consolidate |
| `content/skills/core/` | `systems/skills/` — this is engine infrastructure, not content | Move to systems layer |
| `content/objects/banking/` | `content/objects/Banking.kt` | Consolidate |
| `content/objects/doors/` | Remains in `content/objects/` | STAY, already flat enough |
| `content/objects/dsl/` | Absorbed into `systems/interaction/` | Move |
| `content/objects/services/` | Absorbed into `systems/interaction/` | Move |
| `content/objects/events/` | `content/events/` | Move, kept flat |
| `content/objects/travel/` | `content/objects/Travel.kt` | Consolidate |
| `content/items/combination/` | `content/items/` | Flatten |
| `content/items/consumables/` | `content/items/` | Flatten |
| `content/items/cosmetics/` | `content/items/` | Flatten |
| `content/items/equipment/` | `content/items/` | Flatten |
| `content/items/events/` | `content/events/` | Flatten |
| `content/items/rewards/` | `content/items/` | Flatten |
| `content/items/spawn/` | `content/items/` | Flatten |
| `content/items/utility/` | `content/items/` | Flatten |
| `content/items/admin/` | `content/items/` | Flatten |
| `content/commands/boss/` | `content/commands/BossCommands.kt` | Consolidate (Permission Group) |
| `content/commands/dev/` | `content/commands/DevCommands.kt` | Consolidate (Permission Group) |
| `content/commands/player/` | `content/commands/PlayerCommands.kt` | Consolidate (Permission Group) |
| `content/commands/social/` | `content/commands/SocialCommands.kt` | Consolidate (Permission Group) |
| `content/commands/staff/` | `content/commands/StaffCommands.kt` | Consolidate (Permission Group) |
| `content/commands/travel/` | `content/commands/TravelCommands.kt` | Consolidate (Permission Group) |
| `content/dialogue/modules/` | `content/dialogue/` | Flatten one level |

---

## 4. The 2-Level Nesting Rule (Enforced)

Every file in `net.dodian.uber.game.content` must be no more than 2 folders deep from the `content` root.

### 4.1 Allowed Patterns
```
content/npcs/Hans.kt              ✔ (depth 1)
content/skills/Mining.kt          ✔ (depth 1)
content/skills/MiningData.kt      ✔ (depth 1)
content/ui/BankInterface.kt       ✔ (depth 1)
content/commands/AdminCommands.kt ✔ (depth 1)
content/objects/AltarOfZamorak.kt ✔ (depth 1)
content/minigames/Barrows.kt      ✔ (depth 1)
```

### 4.2 Forbidden Patterns (Must Be Fixed)
```
content/skills/mining/objects/MiningRockObjects.kt   ✘ (depth 3)
content/interfaces/bank/BankComponents.kt            ✘ (depth 2 then deprecated structure)
content/objects/services/ObjectInteractionContext.kt ✘ (belongs in systems layer)
content/skills/core/runtime/GatheringTask.kt         ✘ (belongs in systems layer)
```

---

## 5. Kotlin Visibility Enforcement

### 5.1 The `internal` Keyword Usage

All classes in the `engine` and `systems` packages that are not intended to be used directly by content developers must be marked `internal`. This prevents content developers from accidentally referencing Netty, HikariCP, or database implementation details.

Examples of what must be `internal`:
- `GameEventBus` internals such as `processListeners`, `passesFilters` — already private inside the object, which is correct
- `PlayerSaveSqlRepository` — content cannot hold a reference to this; only `persistence` layer code calls it
- `InteractionProcessor` — content cannot call `process(player)` directly; it must use the event bus
- `GameTaskRuntime.queuePlayer()` — acceptable from `systems.skills`, but content should not call it directly; use the `task { }` DSL from `systems.api`

### 5.2 Prohibited Imports in Content Layer

Content files under `net.dodian.uber.game.content` must not import:
- `io.netty.*` — zero Netty in content. All output must go through the `Client` send helpers or a Kotlin-side wrapper.
- `java.sql.*` — zero raw SQL. All player data is accessed via the `AttributeMap` façade (Spec 03).
- `net.dodian.uber.game.engine.sync.*` — sync pipeline is engine-internal.
- `net.dodian.uber.game.persistence.db.*` — HikariCP and database config are engine-sealed.
- `net.dodian.uber.game.persistence.player.PlayerSaveSqlRepository` — content never serializes players.

---

## 6. Build Configuration (JDK 17 — Actual Target)

> ⚠️ **Critical Note:** The reference specs mention JDK 11. This is **incorrect** for `game-server old`. The actual `build.gradle.kts` already targets **JDK 17** with `jvmTarget = "17"` and `sourceCompatibility = JavaVersion.VERSION_17`. The Kotlin version is **1.6.21** and KSP version is **1.6.21-1.0.6**. Do not downgrade. Any spec referencing JDK 11 is outdated.

### 6.1 Current Build Configuration (Do Not Modify Unless Specified)
```kotlin
// Actual build.gradle.kts — game-server old
plugins {
    kotlin("jvm") version "1.6.21"
    id("com.google.devtools.ksp") version "1.6.21-1.0.6"
    id("application")
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}
```

### 6.2 KSP Processor
The project currently uses `ksp(project(":ksp-processor"))`. Any annotation-based auto-registration (e.g., `@NpcScript`, `@ObjectScript`) must integrate with this existing KSP subproject, not introduce a new annotation processor.

---

## 7. The Java / Kotlin Boundary

The codebase has a live mixed Java+Kotlin source tree. The Java code lives under `src/main/java/net/dodian/uber/game/...` and includes:

- `model/entity/player/Client.java` and its parent chain — the primary "Player" God class
- `model/entity/npc/Npc.java` — the NPC entity
- `netty/` — Netty bootstrap, codecs, `UpstreamHandler`, `LoginProcessorHandler`, all packet handler classes
- `net/dodian/utilities/Misc.java` — static utility class still heavily referenced

### 7.1 Java Files That Must NOT Be Deleted (Yet)
The following Java files are load-bearing and cannot be deleted as part of this restructuring phase:
- `Client.java` / player entity chain — too large to port atomically; refactor via Spec 02/03 bridges
- All 80+ packet listener `out/` classes (e.g., `SendMessage`, `ShowInterface`, `RefreshSkill`) — Kotlin wrapper extensions are preferred but the Java originals must remain until a Kotlin packet layer exists
- `Misc.java` — contains `getObject()`, clipping helpers, `distance()`, and other critical utility methods still called throughout the codebase

### 7.2 Java Utility Migration Path
`Misc.java` methods must be shadowed by Kotlin extension functions but the Java original must remain until all callers are migrated:
```kotlin
// Target: net.dodian.uber.game.util.Extensions.kt
fun Position.distanceTo(other: Position): Int =
    Misc.distance(this.x, this.y, other.x, other.y)
```

---

## 8. The `Server.kt` God Object

The main entry point is `net.dodian.uber.game.Server` (a Kotlin `object`). It currently holds:
- Static references to `npcManager`, `itemManager`, `chunkManager`, `shopManager`, `doorRegistry`, etc.
- The `main()` bootstrap function
- Login session handling glue

This class must not be touched during the content flattening phase. Infrastructure moves (Spec 02, Spec 08) will address it. Content developers must never import `Server` directly — they must use `systems.api` or `systems.world` facades.

---

## 9. Convention over Configuration — File Naming Rules

| Rule | Example (Good) | Example (Bad) |
|:---|:---|:---|
| File name must match primary class/object name | `Hans.kt` → `object Hans` | `npcs.kt` with multiple NPCs |
| Package names must be lowercase | `content.npcs` | `content.Npcs` |
| No underscores in package names | `content.skillguide` | `content.skill_guide` |
| Content directories may use plurals | `content.npcs`, `content.items` | `content.npc`, `content.item` |
| Systems and engine directories use singular | `engine.event`, `systems.skill` | `engine.events`, `systems.skills` |
| One top-level declaration per file (in content) | `AuburyNpc.kt` | `VariousNpcs.kt` |

---

## 10. Pathfinding System — Do Not Expand

The current pathfinding implementation is in a transitional state. The primary movement resolution code lives in `Client.java` via legacy walk-queue arrays (`walkingQueue`, `wQueueReadPtr`, `wQueueWritePtr`, `primaryDirection`, `secondaryDirection`). The Kotlin `InteractionProcessor` calls `player.goodDistanceEntity()` which internally uses this legacy system.

**This system will be fully replaced in a future spec that has not yet been written.** Until that spec is published and implemented:

- Do not add features to the existing pathfinding code
- Do not create new pathfinding abstractions layered on top of the legacy system
- Do not move or rename the legacy walk-queue code in `Client.java`
- The `InteractionProcessor` already stubs a `Router` pattern; leave it as is

The right approach now is to treat the current walk-queue as a black box called only from `InteractionProcessor` and the engine processing phases. Content developers never need to call `player.getNextWalkingDirection()` or similar methods directly.

---

## 11. Migration Execution Order

To keep the server functional at every step, migrations must follow this exact order:

### Phase 1: Event Bus Relocation (Safe, No Logic Change)
1. Create `engine/event/` directory
2. Move bus machinery classes — `GameEventBus`, `EventListener`, `EventFilter`, `ReturnableEventListener`, `GameEvent` interface, `GameEventScheduler`, `bootstrap/`
3. Update all import statements across the codebase (IDE rename/move)
4. Create `events/` directory at game root
5. Move data-only event classes out of `event/events/` into the flat `events/` package
6. Verify: server boots, player can log in, dialogue and skilling events fire

### Phase 2: Engine Config Relocation
1. Move `config/` content to `engine/config/`
2. Update imports in `Server.kt` and all callers

### Phase 3: Content Flattening — NPC Scripts
1. Move all `.kt` files from `content/npcs/spawns/` to `content/npcs/`
2. Update package declarations in each file
3. Update `NpcContentDispatcher` to still find all scripts (see Spec 02 for registration)
4. Verify: NPC click handling still works for a sampling of NPCs

### Phase 4: Content Flattening — Interface Files
1. Consolidate `content/interfaces/*/` into flat `content/ui/` files per the mapping table in Section 3.3
2. This is the highest-volume phase — each interface directory becomes a single `.kt` file

### Phase 5: Content Flattening — Skills
1. Move `content/skills/core/` to `systems/skills/` (this is engine infrastructure, not domain content)
2. Consolidate each skill subdirectory into a Brain + Data file pair in `content/skills/`

### Phase 6: Items, Objects, Commands
1. Flatten all nested `content/items/*/` into `content/items/`
2. Consolidate `content/commands/*/` by permission group into files like `PlayerCommands.kt`, `StaffCommands.kt`, etc.
3. Move `content/objects/services/` and `content/objects/dsl/` to `systems/interaction/`

---

## 12. Layer Isolation Rules (ArchUnit Candidates)

The following rules must eventually be enforced in CI. Until then, code reviews enforce them manually:

| Rule | Violation Example |
|:---|:---|
| `content` must not import `io.netty.*` | `content/ui/BankInterface.kt` importing `ByteBuf` |
| `content` must not import `java.sql.*` | `content/skills/Mining.kt` importing `ResultSet` |
| `content` must not import `engine.sync.*` | Content script importing `PlayerSyncTemplate` |
| `engine` must not import `content.*` | `GameLoopService` importing `Mining.kt` |
| `systems` must not import `content.*` | `InteractionProcessor` importing specific NPC scripts |
| `persistence` must not be called from `content` | Content importing `PlayerSaveSqlRepository` |

Note: `InteractionProcessor` currently imports `NpcContentDispatcher` and fishing-specific types. These are exceptions during the transition. They must be removed as each content area adopts the event bus dispatch model.

---

## 13. Rollback Plan

If any phase causes a smoke test failure that cannot be fixed within 2 hours:

1. `git revert HEAD` — undo the last commit
2. Run `./gradlew build` — confirm it passes
3. Run smoke test — confirm it passes
4. File a bug issue describing exactly what went wrong
5. Do not re-attempt the move until the root cause is understood

**No "we'll fix it later" merges.** Broken builds on `develop` block all other developers.

### Emergency Contact Protocol

If the server is down in production during an emergency migration:
1. Immediately roll back to the last known-good tagged release: `git checkout tags/last-stable`
2. Deploy that version
3. Do NOT attempt to forward-patch the broken migration in production

---

## 14. Contents Inventory Audit (Run Before Phase 6)

Before flattening `content/items/`, `content/objects/`, and `content/commands/`, run this inventory to know exactly what is there:

```bash
# Count files in each content sub-directory
find "src/main/kotlin/net/dodian/uber/game/content" -name "*.kt" \
    | sed 's|.*/content/||' \
    | cut -d'/' -f1 \
    | sort | uniq -c | sort -rn
```

Paste the output into the PR description so reviewers know the scope of what changed.

---

## 15. Definition of Done for Spec 01

- [ ] `net.dodian.uber.game.engine.event` package exists with all bus machinery moved
- [ ] `net.dodian.uber.game.events` package exists with flat, data-only event classes
- [ ] `net.dodian.uber.game.engine.config` exists with config moved
- [ ] `net.dodian.uber.game.content.npcs` is flat (no `spawns/` subdirectory)
- [ ] `net.dodian.uber.game.content.ui` is flat (all interface files consolidated)
- [ ] `net.dodian.uber.game.content.skills` is flat (Brain/Data pattern per Spec 07)
- [ ] `net.dodian.uber.game.content.skills.core` is deleted and its contents moved to `systems.skills`
- [ ] `net.dodian.uber.game.content.items` is flat (no per-category subdirectories)
- [ ] `net.dodian.uber.game.content.commands` is flat (no per-category subdirectories)
- [ ] `net.dodian.uber.game.content.objects` has no `services/` or `dsl/` subdirectories
- [ ] No content file imports `io.netty.*`, `java.sql.*`, or `engine.sync.*`
- [ ] Server boots cleanly on JDK 17 after all moves
- [ ] Player login, NPC dialogue, skilling, and banking still work
- [ ] The pathfinding system is untouched
- [ ] MySQL schema is unchanged (`SHOW CREATE TABLE characters` matches pre-migration baseline)
- [ ] Kotlin version remains 1.6.21 and KSP plugin remains 1.6.21-1.0.6
- [ ] `git log --oneline` contains no commit messages that mention new dialogue, new features, or new gameplay
- [ ] No NPC dialogue, item behavior, or interface behavior changed from a player's perspective
- [ ] Rollback plan documented in PR description
