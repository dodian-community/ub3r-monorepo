# Phase 0: Audit & Inventory

## Goal
Establish a complete, verified map of the current Kotlin source tree so that all subsequent phases work from facts, not assumptions. This document IS the audit.

---

## 0.1 Current Package Tree (Kotlin)

Root: `game-server/src/main/kotlin/net/dodian/uber/game/`

```
game/
├── content/                          # Gameplay features (the "what")
│   ├── combat/                       # 18 files — attack styles, hit queue, target resolution
│   ├── commands/                     # 4 subdirs (admin/, beta/, dev/, player/)
│   ├── dialogue/                     # Dialogue DSL and display
│   ├── events/                       # Content-specific event wiring
│   ├── items/                        # ItemContent interface + item plugins
│   ├── minigames/                    # Casino, Plunder, etc.
│   ├── npcs/                         # 200+ NPC module files (FLAT — biggest pain point)
│   │   ├── store/                    # NPC data store
│   │   └── unknown/                  # Placeholder/unidentified NPCs
│   ├── objects/                      # ObjectContent + banking/, doors/, travel/
│   ├── shop/                         # ShopPlugin system
│   ├── skills/                       # Per-skill subdirs + runtime/
│   │   ├── agility/
│   │   ├── cooking/
│   │   ├── crafting/
│   │   ├── farming/
│   │   ├── firemaking/
│   │   ├── fishing/
│   │   ├── fletching/
│   │   ├── herblore/
│   │   ├── mining/
│   │   ├── prayer/
│   │   ├── runecrafting/
│   │   ├── runtime/                  # Shared skill runtime (action, requirements, parity)
│   │   ├── skillguide/
│   │   ├── slayer/
│   │   ├── smithing/
│   │   ├── thieving/
│   │   └── woodcutting/
│   ├── social/                       # Friends, ignore, PM
│   └── ui/                           # Interface-specific content
│
├── engine/                           # Runtime infrastructure (the "how")
│   ├── config/                       # DotEnv, server config
│   ├── event/                        # Core EventBus
│   ├── lifecycle/                    # Startup/shutdown hooks
│   ├── loop/                         # GameLoopService, GameCycleClock
│   ├── metrics/                      # Tick budget tracking
│   ├── net/                          # Netty bootstrap
│   ├── phases/                       # Tick phase definitions
│   ├── processing/                   # Packet processing pipeline
│   ├── scheduler/                    # ScheduledExecutorService wrapper
│   ├── sync/                         # Entity synchronization
│   └── tasking/                      # Coroutine facade, TaskHandle, priorities
│
├── events/                           # Event contracts (interfaces + data classes)
│   ├── combat/
│   ├── item/
│   ├── magic/
│   ├── npc/
│   ├── objects/
│   ├── player/
│   ├── skilling/
│   ├── trade/
│   ├── widget/
│   ├── GameEvent.kt
│   ├── PlayerDeathEvent.kt
│   ├── ProgressionLifecycleEvents.kt
│   └── WorldTickEvent.kt
│
├── model/                            # Pure domain objects (no DB/network deps)
│   ├── chunk/                        # ChunkManager, ChunkRepository
│   ├── entity/                       # Entity base, Player/NPC model
│   ├── item/                         # Equipment slots, item model
│   ├── object/                       # Game object model
│   ├── player/                       # Player-specific model (skills enum, etc.)
│   ├── EntityType.kt
│   └── Position.kt
│
├── persistence/                      # Database adapters
│   ├── account/                      # Account load/save
│   ├── admin/                        # Admin DB operations
│   ├── audit/                        # Trade/Chat/Item/Duel logging
│   ├── db/                           # HikariCP, MyBatis, table prefixing
│   ├── player/                       # Player save/load
│   ├── repository/                   # Generic DB repository
│   ├── world/                        # World state persistence
│   ├── DbDispatchers.kt
│   ├── WorldSavePublisher.kt
│   ├── WorldSaveResultStore.kt
│   └── WorldSaveSnapshot.kt
│
├── systems/                          # Orchestration & adapters
│   ├── action/                       # PolicyPreset, UnifiedPolicyDsl
│   ├── animation/                    # Animation service
│   ├── api/content/                  # THE stable content-facing surface
│   │   ├── ContentActions.kt
│   │   ├── ContentInteraction.kt
│   │   ├── ContentRuntimeApi.kt
│   │   ├── ContentSafety.kt
│   │   ├── ContentScheduling.kt
│   │   ├── ContentTaskRecipes.kt
│   │   └── ContentTiming.kt
│   ├── cache/                        # CacheBootstrapService
│   ├── dispatch/                     # (EMPTY — legacy, already cleaned)
│   ├── follow/                       # FollowService, A* path follow
│   ├── interaction/                  # 30+ files — intents, processors, clip, doors
│   ├── net/                          # PacketListenerManager, packet services
│   ├── pathing/                      # A*, CollisionManager, collision flags
│   ├── plugin/                       # ContentBootstrap, ContentModuleIndex, PluginRegistry
│   ├── skills/                       # ProgressionService, SkillDoctor, plugin DSL
│   │   ├── farming/                  # Farming runtime
│   │   └── plugin/                   # SkillPlugin, SkillPluginDsl, ResourceSkillContent
│   ├── ui/                           # Interface buttons, dialogue display
│   ├── world/                        # World object management
│   └── zone/                         # Zone/region services
│
└── tasks/                            # Legacy TickTasks surface (deprecated)
    └── TickTasks.kt
```

---

## 0.2 Plugin Interface Inventory

| Interface | Package | KSP Discovered? | Count | Notes |
|-----------|---------|-----------------|-------|-------|
| `ObjectContent` | `content.objects` | ✅ Yes | ~30 | Per-object-ID click handlers |
| `NpcModule` | `content.npcs` | ✅ Yes (by package convention) | ~200 | One `object` per NPC type, FLAT directory |
| `ItemContent` | `content.items` | ✅ Yes | ~15 | Inventory item click handlers |
| `InterfaceButtonContent` | `systems.ui.buttons` | ✅ Yes (by package + interface) | ~10 | Button click handlers |
| `SkillPlugin` | `systems.skills.plugin` | ❌ Manual wiring still | ~15 | Skill DSL definitions |
| `ShopPlugin` | `content.shop.plugin` | ❌ Manual wiring still | ~5 | Shop definitions |
| `ContentBootstrap` | `systems.plugin` | ✅ Yes | ~5 | Startup initialization hooks |
| `CommandContent` | `content.commands` | ✅ Yes (mentioned in index) | ~20 | Chat commands |
| Event Bootstraps | `event.bootstrap` | ✅ Yes (by name convention) | ~10 | Event listener registration |

### Problems Identified

1. **No unified base interface**: `ObjectContent`, `NpcModule`, `ItemContent`, `SkillPlugin`, `ShopPlugin`, and `CommandContent` are completely separate hierarchies with no shared contract
2. **SkillPlugin and ShopPlugin are NOT in the KSP generated index** — they appear to use separate manual wiring through `ContentModuleIndex` wrapper
3. **NPC discovery relies on package path heuristic** (`content.npcs.spawns.*`) plus an explicit exclude list in the KSP processor — fragile
4. **InterfaceButtonContent discovery relies on package path** (`content.interfaces.*`) — also fragile
5. **Event bootstrap discovery relies on name suffix** (`*Bootstrap`) in `event.bootstrap.*` — yet another convention

---

## 0.3 Content API Surface Inventory

The `systems.api.content` package is the intended stable surface for content developers:

| File | Purpose | Status |
|------|---------|--------|
| `ContentActions.kt` | Cancel/reset player actions | ✅ Used widely |
| `ContentInteraction.kt` | Interaction policy helpers | ✅ Used |
| `ContentRuntimeApi.kt` | Farming-specific runtime wrappers | ⚠️ Farming-only, not generic |
| `ContentSafety.kt` | Safety checks | ✅ Used |
| `ContentScheduling.kt` | `world()`, `player()`, `npc()` task launchers | ✅ Core API |
| `ContentTaskRecipes.kt` | Common task patterns | ✅ Used |
| `ContentTiming.kt` | `currentCycle()`, `ticksForDurationMs()` | ✅ Core API |

### Problems Identified

1. `ContentRuntimeApi` is dominated by farming-specific methods — should be generic or farming should have its own surface
2. No content-facing API for: inventory manipulation, dialogue starting, combat initiation, equipment checks, NPC spawning
3. Content developers still reach into `systems.skills.ProgressionService` directly rather than through a content API

---

## 0.4 NPC Module Pain Points (Critical)

The `content/npcs/` directory contains **200+ files in a single flat directory**. This is the single biggest developer experience problem:

- Finding a specific NPC requires knowing its class name
- No grouping by role (shopkeeper, quest, monster, ambient)
- No grouping by location (Lumbridge, Varrock, Wilderness)
- Many files follow identical patterns (spawn def + option handlers) with no shared template
- NPC module discovery in KSP uses a **fragile exclude list** that must be manually maintained

---

## 0.5 Skill Implementation Patterns (Inconsistent)

Current skill implementations use a mix of patterns:

| Skill | Pattern | Notes |
|-------|---------|-------|
| Woodcutting | `SkillPlugin` + `gatheringAction()` | Modern pattern, closest to target |
| Mining | `SkillPlugin` + `gatheringAction()` | Similar to Woodcutting |
| Fishing | `SkillPlugin` + custom action | Partially modernized |
| Cooking | Mixed legacy + Kotlin | Some in Java still |
| Fletching | Kotlin state machine | Custom pattern, not DSL-based |
| Crafting | Kotlin with `CraftingMode` enum | Custom state machine |
| Farming | Heavy custom runtime (`FarmingRuntimeService`) | Unique architecture |
| Herblore | Kotlin | Custom |
| Prayer | Kotlin | Custom |
| Smithing | Mixed | Partially migrated |
| Agility | Java-heavy (`Agility.java`) | Minimal Kotlin |
| Thieving | Kotlin | Custom |
| Slayer | Kotlin | Task assignment system |
| Runecrafting | Kotlin | Custom |
| Firemaking | Kotlin | Custom |

### Problems Identified

1. No two skills use exactly the same pattern
2. The `gatheringAction()` DSL is good but only used by Woodcutting and Mining
3. No equivalent DSL for production skills (Cooking, Fletching, Smithing, Herblore)
4. No equivalent DSL for action skills (Agility, Thieving)
5. Farming has its own entire runtime service — necessary complexity but poorly integrated
6. Skill state is scattered across `Client` properties (`woodcuttingState`, `fletchingState`, `craftingState`, `isFiremaking`)

---

## 0.6 Event System Inventory

The event system is split across two locations:

1. **`events/`** (root-level): Event contracts (interfaces and data classes)
   - `GameEvent.kt`, `PlayerDeathEvent.kt`, `ProgressionLifecycleEvents.kt`, `WorldTickEvent.kt`
   - Subdirectories by domain: `combat/`, `item/`, `magic/`, `npc/`, `objects/`, `player/`, `skilling/`, `trade/`, `widget/`

2. **`engine/event/`**: The `GameEventBus` implementation itself

3. **`content/events/`**: Content-specific event wiring

### Problems Identified

1. Three separate locations for event-related code
2. Event contracts live at `game/events/` but event listeners are in `content/events/` — confusing
3. No clear naming convention for events (some are `XxxEvent`, some are `XxxLifecycleEvents`)

---

## 0.7 Architecture Test Coverage

Current architecture tests in `game-server/src/test/kotlin/.../architecture/`:

| Test | What It Guards |
|------|---------------|
| `ArchitectureBoundaryTest` | Legacy namespace removal, KSP-driven index |
| `ContentSchedulerImportBoundaryTest` | Content uses ContentScheduling, not raw tasking |
| `ContentTaskApiBoundaryTest` | Task API boundaries |
| `CoreRuntimeExceptionCatchGuardTest` | No broad `catch(Exception)` in core |
| `DeathEventNamingBoundaryTest` | Death event naming |
| `EngineTaskDomainBoundaryTest` | Engine/domain separation |
| `EventContractCoverageTest` | Event contract completeness |
| `EventWiringParityTest` | Event wiring consistency |
| `FarmingRuntimePackageBoundaryTest` | Farming isolation |
| `GeneratedContentIndexTest` | Generated index validity |
| `LegacyCachePackageBoundaryTest` | Cache package isolation |
| `LegacyCacheTickSafetyBoundaryTest` | Cache tick safety |
| `NettyListenerBoundaryTest` | Netty listener isolation |
| `NoRangableArchitectureTest` | No rangable pattern |
| `ProgressionLifecycleSignalWiringTest` | Progression lifecycle |
| `ServerLegacyCacheBootstrapBoundaryTest` | Server cache bootstrap |
| `SkillRuntimeOwnershipBoundaryTest` | Skill runtime ownership |
| `TaskingSurfaceAreaBoundaryTest` | Tasking surface area |

These tests are excellent and will need to be updated in Phase 1 (package restructure) and extended in Phases 2–4.

---

## 0.8 Dead Code & Candidates for Removal

| Location | Issue |
|----------|-------|
| `systems/dispatch/` | **Empty directory** — already cleaned out |
| `tasks/TickTasks.kt` | Legacy tick task surface — deprecated, should be removed |
| `content/ContentModuleIndex.kt` | Duplicate of `systems/plugin/ContentModuleIndex.kt` — one should go |
| Various `@JvmStatic` on Kotlin objects | Unnecessary in pure-Kotlin callers, only needed for Java interop |

---

## 0.9 Dependency Flow Violations

Expected: `content → api/systems → engine → model` (inward only)

Current violations:
1. `ContentRuntimeApi` directly references `FarmingRuntimeService.INSTANCE` — content API coupled to specific content implementation
2. Some skill implementations reach into `systems.interaction.*` directly instead of through content API
3. `Client` class (Java) is the god object — model, state, network I/O, and behavior all in one

---

## 0.10 Summary: Top 10 Problems to Solve

| # | Problem | Impact | Phase |
|---|---------|--------|-------|
| 1 | 200+ NPC files in flat directory | Dev experience: impossible to navigate | 1 |
| 2 | No unified plugin interface | 6+ different content registration patterns | 2 |
| 3 | Inconsistent skill implementations | Every skill is a snowflake | 3 |
| 4 | KSP processor uses fragile heuristics | Breaks when packages move | 4 |
| 5 | Content API surface is incomplete | Devs reach into engine internals | 5 |
| 6 | No structured logging or content tracing | Debugging is println-based | 6 |
| 7 | No traffic shaping or RSA | Vulnerable to attacks | 7 |
| 8 | No package-info.java anywhere | No discoverability for new devs | 8 |
| 9 | Skill state scattered across Client | Tight coupling, hard to test | 3 |
| 10 | Event system split across 3 locations | Confusing navigation | 1 |

