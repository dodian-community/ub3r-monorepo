# Phase 1: Package Restructure

## Goal
Reorganize the Kotlin source tree from a technical-layered structure to a **domain-driven, content-first** structure. After this phase, a developer looking for "anything related to Woodcutting" finds it in one place.

## Prerequisites
- Phase 0 audit complete and reviewed
- All architecture tests passing on current `main`
- Full project backup / clean git state

---

## 1.1 Design Principles for New Package Structure

### Rule 1: Domain First, Technical Second
```
✅ net.dodian.uber.game.skill.woodcutting
❌ net.dodian.uber.game.content.skills.woodcutting
```

### Rule 2: Group by What, Not How
```
✅ net.dodian.uber.game.npc.banker          (all banker-related code)
❌ net.dodian.uber.game.content.npcs.Banker  (just one file among 200)
```

### Rule 3: Max 3 Levels of Nesting Below `game`
```
✅ game.skill.woodcutting.WoodcuttingPlugin
✅ game.npc.shopkeeper.Gerrant
❌ game.content.skills.woodcutting.plugin.WoodcuttingSkillPlugin
```

### Rule 4: Runtime vs Content Split
```
game/
├── [domain packages]    ← Content developers work here
├── model/               ← Pure domain objects (no deps)
├── runtime/             ← Engine internals (loop, sync, net)
├── persistence/         ← DB adapters
├── event/               ← Event bus + contracts
└── api/                 ← Stable content-facing surface
```

---

## 1.2 Target Package Structure

```
net.dodian.uber.game/
│
├── skill/                              # All skill content
│   ├── woodcutting/                    # Woodcutting plugin + data + state
│   │   ├── WoodcuttingPlugin.kt        # The KSP-discovered plugin object
│   │   ├── WoodcuttingData.kt          # Tree defs, axe defs
│   │   └── WoodcuttingState.kt         # Player state for active woodcutting
│   ├── mining/
│   │   ├── MiningPlugin.kt
│   │   ├── MiningData.kt
│   │   └── MiningState.kt
│   ├── fishing/
│   ├── cooking/
│   ├── fletching/
│   ├── crafting/
│   ├── smithing/
│   ├── herblore/
│   ├── farming/                        # Farming keeps its runtime service here
│   │   ├── FarmingPlugin.kt
│   │   ├── FarmingRuntime.kt
│   │   └── FarmingData.kt
│   ├── firemaking/
│   ├── prayer/
│   ├── runecrafting/
│   ├── agility/
│   ├── thieving/
│   ├── slayer/
│   ├── Skillcape.kt
│   └── guide/                          # Skill guide UI
│       └── SkillGuidePlugin.kt
│
├── npc/                                # All NPC content
│   ├── shopkeeper/                     # Shopkeepers by location/type
│   │   ├── Gerrant.kt
│   │   ├── Horvik.kt
│   │   ├── Zaff.kt
│   │   └── ShopKeeperGeneric.kt
│   ├── banker/
│   │   ├── Banker.kt
│   │   └── BankerSpawnEntries.kt
│   ├── quest/                          # Quest-related NPCs
│   │   ├── DukeHoracio.kt
│   │   └── Koftik.kt
│   ├── slayermaster/                   # Slayer masters
│   │   ├── Turael.kt
│   │   ├── Mazchna.kt
│   │   ├── Vannaka.kt
│   │   └── Duradel.kt
│   ├── monster/                        # Combat NPCs (grouped by area/type)
│   │   ├── demon/
│   │   │   ├── LesserDemon.kt
│   │   │   ├── GreaterDemon.kt
│   │   │   └── BlackDemon.kt
│   │   ├── dragon/
│   │   │   ├── GreenDragon.kt
│   │   │   ├── BlueDragon.kt
│   │   │   └── KingBlackDragon.kt
│   │   ├── slayer/                     # Slayer-only monsters
│   │   │   ├── AbyssalDemon.kt
│   │   │   ├── Gargoyle.kt
│   │   │   └── CaveHorror.kt
│   │   └── boss/
│   │       ├── TztokJad.kt
│   │       ├── KalphiteQueen.kt
│   │       └── Venenati.kt
│   ├── ambient/                        # Non-combat NPCs
│   │   ├── Man.kt
│   │   ├── Guard.kt
│   │   └── Farmer.kt
│   ├── transport/                      # Transport NPCs
│   │   ├── CaptainTobias.kt
│   │   └── RugMerchant.kt
│   ├── utility/                        # Utility NPCs
│   │   ├── MakeoverMage.kt
│   │   ├── Tanner.kt
│   │   └── Zahur.kt
│   └── _shared/                        # Shared NPC DSL, data, spawn helpers
│       ├── NpcPluginDsl.kt
│       ├── NpcContent.kt
│       ├── NpcSpawnDef.kt
│       └── NpcDataPreset.kt
│
├── object/                             # World object content
│   ├── bank/                           # Bank booths, chests
│   │   └── BankBoothPlugin.kt
│   ├── door/                           # Door open/close
│   │   └── DoorPlugin.kt
│   ├── ladder/                         # Ladders, stairs
│   │   └── LadderPlugin.kt
│   ├── altar/                          # Prayer altars
│   │   └── AltarPlugin.kt
│   └── _shared/                        # Shared object DSL
│       ├── ObjectContent.kt
│       ├── ObjectBinding.kt
│       └── ObjectContentDsl.kt
│
├── item/                               # Item content
│   ├── food/                           # Eating food
│   │   └── FoodPlugin.kt
│   ├── potion/                         # Drinking potions
│   │   └── PotionPlugin.kt
│   ├── equipment/                      # Equipping items
│   │   └── EquipmentPlugin.kt
│   └── _shared/
│       └── ItemContent.kt
│
├── combat/                             # Combat system
│   ├── melee/
│   │   └── MeleeCombat.kt
│   ├── ranged/
│   │   └── RangedCombat.kt
│   ├── magic/
│   │   └── MagicCombat.kt
│   ├── special/                        # Special attacks
│   │   └── SpecialAttackRegistry.kt
│   ├── hit/                            # Hit queue, damage processing
│   │   └── CombatHitQueueService.kt
│   ├── death/                          # Death handling
│   │   └── PlayerDeathService.kt
│   └── _shared/                        # Combat interfaces, state
│       ├── CombatTargetState.kt
│       ├── CombatStartService.kt
│       └── CombatRuntimeService.kt
│
├── social/                             # Player-to-player
│   ├── trade/
│   ├── duel/
│   ├── chat/
│   └── friends/
│
├── activity/                           # Quests, minigames
│   ├── quest/
│   └── minigame/
│       └── casino/
│
├── command/                            # Chat commands
│   ├── admin/
│   ├── player/
│   └── dev/
│
├── ui/                                 # Interface/widget content
│   ├── button/
│   ├── dialogue/
│   └── skillguide/
│
├── model/                              # Pure domain objects (KEEP AS-IS mostly)
│   ├── entity/
│   │   ├── Entity.kt
│   │   ├── player/
│   │   └── npc/
│   ├── item/
│   ├── object/
│   ├── chunk/
│   ├── EntityType.kt
│   └── Position.kt
│
├── runtime/                            # Engine internals (renamed from engine/)
│   ├── loop/                           # GameLoopService, GameCycleClock
│   ├── task/                           # Coroutine facade (renamed from tasking/)
│   ├── sync/                           # Entity synchronization
│   ├── net/                            # Netty bootstrap, packet decode/encode
│   ├── phase/                          # Tick phase definitions (renamed from phases/)
│   ├── config/                         # DotEnv, server config
│   ├── lifecycle/                      # Startup/shutdown hooks
│   ├── metrics/                        # Tick budget tracking
│   ├── processing/                     # Packet processing pipeline
│   └── scheduler/                      # ScheduledExecutorService wrapper
│
├── persistence/                        # Database adapters (KEEP AS-IS mostly)
│   ├── account/
│   ├── player/
│   ├── world/
│   ├── audit/
│   ├── admin/
│   ├── db/
│   └── repository/
│
├── event/                              # Events unified (merge events/ + engine/event/)
│   ├── bus/                            # GameEventBus implementation
│   ├── combat/                         # Combat events
│   ├── item/                           # Item events
│   ├── npc/                            # NPC events
│   ├── player/                         # Player events (death, level, login)
│   ├── skill/                          # Skilling events (renamed from skilling/)
│   ├── trade/                          # Trade events
│   ├── magic/                          # Magic events
│   ├── widget/                         # Widget events
│   ├── GameEvent.kt
│   └── WorldTickEvent.kt
│
├── api/                                # Stable content-facing surface
│   ├── plugin/                         # Unified plugin interfaces
│   │   ├── ContentPlugin.kt           # Base marker interface
│   │   ├── SkillContentPlugin.kt      # Skill-specific plugin interface
│   │   ├── NpcContentPlugin.kt
│   │   ├── ObjectContentPlugin.kt
│   │   ├── ItemContentPlugin.kt
│   │   └── CommandContentPlugin.kt
│   ├── schedule/                       # ContentScheduling (renamed from api/content/)
│   │   ├── ContentScheduling.kt
│   │   └── ContentScheduleScope.kt
│   ├── action/                         # ContentActions, PolicyPreset
│   │   ├── ContentActions.kt
│   │   ├── PolicyPreset.kt
│   │   └── ActionBuilder.kt
│   ├── timing/                         # ContentTiming
│   │   └── ContentTiming.kt
│   ├── safety/                         # ContentSafety
│   │   └── ContentSafety.kt
│   ├── interaction/                    # Content interaction helpers
│   │   └── ContentInteraction.kt
│   └── inventory/                      # Inventory manipulation API (NEW)
│       └── ContentInventory.kt
│
└── _legacy/                            # Temporary home for not-yet-migrated code
    ├── interaction/                    # systems/interaction/ stuff not yet cleaned
    ├── pathing/                        # A*, collision (stays until runtime migration)
    ├── follow/                         # FollowService
    ├── cache/                          # CacheBootstrapService
    └── zone/                           # Zone services
```

---

## 1.3 Step-by-Step Migration Order

The order matters. We move leaf packages first, then update references.

### Step 1: Create the new root directories
```bash
mkdir -p game-server/src/main/kotlin/net/dodian/uber/game/{skill,npc,object,item,combat,social,activity,command,ui}
mkdir -p game-server/src/main/kotlin/net/dodian/uber/game/{runtime,api,event,_legacy}
mkdir -p game-server/src/main/kotlin/net/dodian/uber/game/npc/{shopkeeper,banker,quest,slayermaster,monster,ambient,transport,utility,_shared}
mkdir -p game-server/src/main/kotlin/net/dodian/uber/game/npc/monster/{demon,dragon,slayer,boss}
mkdir -p game-server/src/main/kotlin/net/dodian/uber/game/object/{bank,door,ladder,altar,_shared}
mkdir -p game-server/src/main/kotlin/net/dodian/uber/game/item/{food,potion,equipment,_shared}
mkdir -p game-server/src/main/kotlin/net/dodian/uber/game/combat/{melee,ranged,magic,special,hit,death,_shared}
mkdir -p game-server/src/main/kotlin/net/dodian/uber/game/api/{plugin,schedule,action,timing,safety,interaction,inventory}
mkdir -p game-server/src/main/kotlin/net/dodian/uber/game/event/bus
mkdir -p game-server/src/main/kotlin/net/dodian/uber/game/runtime/{loop,task,sync,net,phase,config,lifecycle,metrics,processing,scheduler}
```

### Step 2: Move skill packages (lowest risk, self-contained)
For each skill:
1. Move `content/skills/<skill>/` → `skill/<skill>/`
2. Update `package` declarations
3. Update imports project-wide (IntelliJ "Move Package" handles this)

**Order**: woodcutting → mining → fishing → cooking → fletching → crafting → smithing → herblore → farming → firemaking → prayer → runecrafting → agility → thieving → slayer

Also move:
- `content/skills/runtime/` → `skill/_runtime/` (shared skill runtime)
- `content/skills/skillguide/` → `skill/guide/`
- `content/skills/Skillcape.kt` → `skill/Skillcape.kt`

### Step 3: Move engine → runtime
1. `engine/loop/` → `runtime/loop/`
2. `engine/tasking/` → `runtime/task/`
3. `engine/sync/` → `runtime/sync/`
4. `engine/net/` → `runtime/net/`
5. `engine/phases/` → `runtime/phase/`
6. `engine/config/` → `runtime/config/`
7. `engine/lifecycle/` → `runtime/lifecycle/`
8. `engine/metrics/` → `runtime/metrics/`
9. `engine/processing/` → `runtime/processing/`
10. `engine/scheduler/` → `runtime/scheduler/`

### Step 4: Unify events
1. Move `engine/event/` → `event/bus/`
2. Move all `events/*` contracts into `event/` (preserving subdirs)
3. Move `content/events/` event wiring into respective domain packages
4. Delete empty `events/` and `engine/event/` directories

### Step 5: Reorganize NPC directory (the big one)
This is the most labor-intensive step. For each NPC file in `content/npcs/`:

1. **Categorize**: Is this NPC a shopkeeper, banker, quest NPC, slayer master, monster, ambient, transport, or utility?
2. **Move** to the appropriate subdirectory under `npc/`
3. **Update** package declaration

Use this categorization guide:

| Category | NPCs (examples) |
|----------|-----------------|
| `shopkeeper/` | Gerrant, Horvik, Aubury, ShopKeeper, ShopAssistant, BowArrowSalesman, ArmourSalesman, Wydin, Jatix, Peksa |
| `banker/` | Banker, BankerSpawnEntries |
| `quest/` | DukeHoracio, Koftik, Glough, Ungadulu, HeadMourner, PlagueWarning, WatchtowerWizard, Kolodion |
| `slayermaster/` | Turael, Mazchna, Vannaka, Duradel |
| `monster/demon/` | LesserDemon, GreaterDemon, BlackDemon |
| `monster/dragon/` | GreenDragon, BlueDragon, RedDragon, BlackDragon, MithrilDragon, KingBlackDragon |
| `monster/slayer/` | AbyssalDemon, Gargoyle, CaveHorror, Nechryael, Banshee, CrawlingHand, Pyrefiend, InfernalMage, Jelly, Bloodveld |
| `monster/boss/` | TztokJad, JalTokJad, KalphiteQueen, Venenati, SlashBash |
| `ambient/` | Man, Guard, Farmer, Monk, Chicken, Cow, Sheep, Dwarf, Knight, Hero, Skeleton, Zombie, Ghost |
| `transport/` | CaptainTobias, RugMerchant, CustomsOfficer, Shantay |
| `utility/` | MakeoverMage, Tanner, Zahur, Diango, MakeoverMage, Sedridor, WizardCromperty, Mac |

Move shared NPC DSL infrastructure:
- `NpcPluginDsl.kt` → `npc/_shared/`
- `NpcContent.kt` → `npc/_shared/`
- `NpcSpawnDef.kt` → `npc/_shared/`
- `NpcDataPreset.kt` → `npc/_shared/`
- `NpcDialogueDsl.kt` → `npc/_shared/`
- `NpcModuleDsl.kt` → `npc/_shared/`
- `NpcPluginModels.kt` → `npc/_shared/`
- `NpcSpawnGroups.kt` → `npc/_shared/`

### Step 6: Move object content
1. `content/objects/banking/` → `object/bank/`
2. `content/objects/doors/` → `object/door/`
3. `content/objects/travel/` → `object/ladder/` (or `travel/`)
4. `content/objects/ObjectContent.kt` → `object/_shared/ObjectContent.kt`
5. `content/objects/ObjectBinding.kt` → `object/_shared/ObjectBinding.kt`
6. `content/objects/ObjectContentDsl.kt` → `object/_shared/ObjectContentDsl.kt`

### Step 7: Move item content
1. `content/items/` → `item/`
2. Group by category as items get added

### Step 8: Move combat
1. `content/combat/MeleeCombat.kt` → `combat/melee/MeleeCombat.kt`
2. `content/combat/RangedCombat.kt` → `combat/ranged/RangedCombat.kt`
3. `content/combat/MagicCombat.kt` → `combat/magic/MagicCombat.kt`
4. Move shared combat files → `combat/_shared/`
5. `content/combat/style/` → `combat/style/`

### Step 9: Move social
1. `content/social/` → `social/`
2. Separate into `social/trade/`, `social/chat/`, `social/friends/`

### Step 10: Move commands
1. `content/commands/admin/` → `command/admin/`
2. `content/commands/player/` → `command/player/`
3. `content/commands/dev/` → `command/dev/`
4. `content/commands/beta/` → `command/beta/`

### Step 11: Move dialogue + UI
1. `content/dialogue/` → `ui/dialogue/`
2. `content/ui/` → `ui/` (merge)
3. `systems/ui/buttons/` → `ui/button/`

### Step 12: Move content API surface
1. `systems/api/content/` → `api/`
2. Split into subdirectories per concern

### Step 13: Move systems to appropriate homes
1. `systems/skills/` → `api/skill/` (the skill API/dispatch surface)
2. `systems/plugin/` → `api/plugin/`
3. `systems/action/` → `api/action/`
4. `systems/interaction/` → `_legacy/interaction/` (until Phase 2 cleanup)
5. `systems/pathing/` → `_legacy/pathing/`
6. `systems/follow/` → `_legacy/follow/`
7. `systems/cache/` → `_legacy/cache/`
8. `systems/zone/` → `_legacy/zone/`
9. `systems/world/` → `_legacy/world/`
10. `systems/net/` → `runtime/net/` or `_legacy/net/`
11. `systems/animation/` → `runtime/animation/`

### Step 14: Delete empty directories
Remove all now-empty `content/`, `systems/`, `engine/`, and `events/` directories.

### Step 15: Delete TickTasks.kt
Remove `tasks/TickTasks.kt` and update any remaining references to use `ContentScheduling`.

---

## 1.4 Updating Architecture Tests

After the package move, every architecture boundary test will need updated package path strings. For each test:

1. Replace `net.dodian.uber.game.content.` → appropriate new package
2. Replace `net.dodian.uber.game.engine.` → `net.dodian.uber.game.runtime.`
3. Replace `net.dodian.uber.game.systems.` → appropriate new package
4. Replace `net.dodian.uber.game.events.` → `net.dodian.uber.game.event.`

Add a new test:
```kotlin
@Test
fun `no code remains in legacy content or systems packages`() {
    // Scan for any .kt files still in old package paths
}
```

---

## 1.5 Updating KSP Processor

The KSP processor in `ksp-processor/` uses hardcoded package path strings. Update:

| Current | New |
|---------|-----|
| `net.dodian.uber.game.content.interfaces` | `net.dodian.uber.game.ui.button` |
| `net.dodian.uber.game.content.objects.ObjectContent` | `net.dodian.uber.game.object._shared.ObjectContent` |
| `net.dodian.uber.game.content.items.ItemContent` | `net.dodian.uber.game.item._shared.ItemContent` |
| `net.dodian.uber.game.content.npcs.spawns` | `net.dodian.uber.game.npc` |
| `net.dodian.uber.game.event.bootstrap` | `net.dodian.uber.game.event.bootstrap` (keep) |

**Note**: Phase 4 will do a deeper KSP rewrite, but Phase 1 needs it to work with moved packages.

---

## 1.6 Verification Checklist

- [ ] `./gradlew clean build` passes
- [ ] `./gradlew :game-server:test` passes (all architecture tests)
- [ ] `./gradlew :game-server:run` starts successfully
- [ ] No Kotlin files remain under `content/`, `engine/`, `systems/`, or `events/`
- [ ] Every `.kt` file has a correct `package` declaration matching its directory
- [ ] No orphaned imports (IntelliJ "Optimize Imports" on project)
- [ ] `GeneratedPluginModuleIndex.kt` is generated with correct FQCNs

---

## 1.7 Rollback Strategy

Since this is entirely package renaming with no logic changes:
1. Every step should be a separate git commit
2. If a step breaks the build, `git revert` that commit
3. IntelliJ's "Refactor → Move" handles import updates automatically
4. The `_legacy/` directory provides a safe landing zone for code that isn't ready to move

---

## 1.8 Estimated Effort

| Step | Files Affected | Time Estimate |
|------|---------------|---------------|
| Steps 1–2 (skills) | ~60 files | 1–2 hours |
| Step 3 (engine→runtime) | ~40 files | 1 hour |
| Step 4 (events) | ~30 files | 30 min |
| Step 5 (NPCs) | ~200 files | 3–4 hours |
| Steps 6–11 (objects, items, combat, social, commands, UI) | ~80 files | 2 hours |
| Steps 12–15 (API, systems, cleanup) | ~50 files | 1–2 hours |
| Architecture test updates | ~18 tests | 1 hour |
| KSP processor update | ~3 files | 30 min |
| **Total** | **~480 files** | **~12 hours** |

