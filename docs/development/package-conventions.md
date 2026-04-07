# Package Conventions

This document records the architectural decisions that govern where code lives in the `game-server` Kotlin source tree. Read this before adding or moving a file.

---

## The Golden Rule

> **`model` = data / state. `systems` = behaviour / lifecycle. `content` = player-facing game features.**

If a class _only_ holds fields and simple accessors → it belongs in `model`.  
If a class _does something_ on a tick, reacts to events, or drives a loop → it belongs in `systems`.  
If a class implements a game feature a player interacts with (skill action, shop, minigame) → it belongs in `content`.

---

## Recurring Split Decisions

### NPC — `model/entity/npc/` vs `systems/world/npc/`

| Package | What lives here |
|---|---|
| `model/entity/npc/` | NPC definition interop, drop math, update-mask building |
| `systems/world/npc/` | NPC lifecycle (spawn, despawn, aggro timer, respawn) and the `NpcManager` registry |

**Rule of thumb:** If it answers "what is this NPC?" → `model`. If it answers "what should this NPC do next?" → `systems/world/npc`.

### Farming — `content/skills/farming/` vs `systems/world/farming/`

| Package | What lives here |
|---|---|
| `content/skills/farming/` | Player-facing farming actions (`Farming.kt`), data tables (`FarmingData.kt`) |
| `systems/world/farming/` | Runtime patch state (`FarmingRuntimeModels.kt`), the grow-tick service (`FarmingRuntimeService.kt`), persistence codec |

**Rule of thumb:** Code triggered by a player click → `content/skills/farming`. Code driven by the world tick regardless of player presence → `systems/world/farming`.

### Chunk vs Zone — `model/chunk/` vs `systems/zone/`

| Package | What lives here |
|---|---|
| `model/chunk/` | `Chunk` (the 8×8 tile grouping used for player update visibility), `ChunkManager`, `ChunkRepository`, `ChunkPlayerComparator` |
| `systems/zone/` | Protocol-level zone delta flushing: `ZoneDelta`, `ZoneFlushService`, `ZoneUpdateBus`, `ChunkDeltaQueue`. Also `RegionSong` (maps zones to music IDs on region transition). |

**Rule of thumb:** `chunk` = physical 8×8 tile boundary. `zone` = event/protocol layer that reacts to players entering/leaving those boundaries.

---

## Content vs Model — Where Do Features Live?

Content features that _appear inside model packages_ are a bug, not a design. Specifically:

- **Mini-games** → `content/minigames/<name>/` (e.g. `content/minigames/casino/`)
- **Skill capes** → `content/skills/Skillcape.kt` (item data tied to skills, not player state)
- **Shops** → `content/shop/ShopManager.kt` — future `Shop.kt` / `ShopRestockService.kt` go alongside it
- **Quest state** (`QuestSend`) → `model/player/quests/` is acceptable for UI/send helpers that are pure data; quest _logic_ goes in `content/`
- **Skills as state** (`model/player/skills/`) → skill _levels_ and _experience_ live here; skill _actions_ live in `content/skills/<skill>/`

---

## Persistence Naming

The persistence layer uses domain language close to what RSPS developers expect:

| Class | What it does |
|---|---|
| `WorldSavePublisher` | Publishes a world snapshot to the DB save pipeline each maintenance cycle |
| `WorldSaveSnapshot` | Immutable snapshot of world state (player count, online IDs) sent to the DB |
| `WorldSaveResultStore` | Holds the latest result returned from the DB after a save cycle |
| `persistence/admin/CommandDbService` | Offline player operations used by admin/dev commands (view bank, remove items, modify XP). Not command _dispatch_ — that lives in `content/commands/`. |

---

## Event System — Where Do Events Live?

All game events are `data class : GameEvent` and live in `game/events/`. Grouped by concern:

| File | Events |
|---|---|
| `ProgressionLifecycleEvents.kt` | `LevelUpEvent`, `DeathEvent` |
| `PlayerLoginEvent.kt` / `PlayerLogoutEvent.kt` | Login/logout lifecycle |
| `GroundItemEvents.kt` | `ItemPickupEvent`, `ItemDropEvent` |
| `NpcCombatEvents.kt` | `NpcDeathEvent`, `NpcDropEvent` |
| `TradeEvents.kt` | `TradeRequestEvent`, `TradeCompleteEvent`, `TradeCancelEvent` |
| `skilling/` | Skilling start/stop/resource events |

> If you are writing content that needs to react to a lifecycle moment (death, login, drop), subscribe to the event here rather than embedding the logic in the legacy Java tick methods.

---

## Combat Package Scope

`systems/combat/` owns: attack resolution, hit queue, intent/state, combat start/stop policies, and fight-style math.

It does **not** own:
- Animation resolution → `systems/animation/` (`PlayerBlockAnimationService`, `UnarmedAttackAnimationService`, `PlayerAnimationService`)
- Prayer data → `model/player/skills/prayer/`

`CombatPlayerExtensions.kt` contains `Client` extension functions for combat math (max hits, prayer bonus multipliers, slayer task checks). Despite being extension functions on `Client`, the logic is entirely combat-domain — it lives in `systems/combat/`.

