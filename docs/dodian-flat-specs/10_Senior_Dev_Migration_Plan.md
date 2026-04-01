# Spec 10: Senior Developer Migration Plan — The Strangler Pattern
### Dodian-Flat Final Draft — Based on `game-server old` Actual Codebase Audit

---

> ⛔ **READ [00_MASTER_RULES_READ_FIRST.md](./00_MASTER_RULES_READ_FIRST.md) BEFORE THIS SPEC.**
> This migration plan is a restructuring plan only. No phase in this document authorizes adding new gameplay, new content, or new player-visible features.
> If you find yourself writing new dialogue, new item effects, new skill resources, or new command behavior as part of any phase, you have gone out of scope. Stop and refer back to [00_MASTER_RULES_READ_FIRST.md](./00_MASTER_RULES_READ_FIRST.md).

---

## 1. Executive Summary

This document is the master execution roadmap for transforming `game-server old` into the Dodian-Flat architecture defined in Specs 01–09. The driving principle is the **Strangler Fig Pattern**: the new architecture is incrementally grown around the legacy code, strangling legacy patterns one at a time while the server remains live and functional at every step.

**Non-negotiables at every milestone:**
- Server boots without errors
- At least one player can log in and log out cleanly
- NPC dialogue, banking, and skill XP gain work
- MySQL schema is unchanged
- No gameplay changes to player-facing mechanics

All work is organized into 7 sequential phases. Each phase has a clear entry condition, set of tasks, and exit verification checklist. Multiple tasks within a phase may be done in parallel by different developers, but phases themselves are sequential.

---

## 2. Developer Role Definitions

| Role | Responsibility | Who Can Approve PRs |
|:---|:---|:---|
| **Engine Dev** | Works on `engine.*`, `systems.*`, and Java file modifications. High risk — requires thorough testing. | Tech Lead only |
| **Content Dev** | Works only on `content.*`. Low risk — follows the DSL patterns defined in Specs 04–07. | Engine Dev or Tech Lead |
| **Migration Dev** | Executes package moves via IDE refactoring tools. Must run smoke tests after every batch. | Engine Dev or Tech Lead |

**Any developer outside their designated role must get explicit approval before touching code in that area.**

---

## 3. Phase 0: Pre-Migration Hardening (Must Complete Before Any Phase)

**Goal:** Ensure we have safety nets in place before any file moves.

### 3.1 Smoke Test Script

A minimal smoke test must exist that verifies core server functionality. This is not a comprehensive test — it is a "did the server blow up?" check.

**Smoke test criteria (all must pass before any commit):**
1. Server starts without exceptions in the log
2. A test player account can log in via local client connection
3. The player can click NPC ID 0 (Hans) and receive dialogue
4. The player can open the bank (click a bank booth object)
5. The player can gain woodcutting XP by clicking a tree
6. The player can log out cleanly
7. The player can log back in and their XP is saved correctly

> **The smoke test must pass as-is before you start any migration work.** If the smoke test already fails on `main`, that is a pre-existing bug, not a migration issue. Document it and do not proceed until it is fixed.

### 3.2 Establish Git Branch Strategy

```
main              ← Production-safe branch. Tagged releases only.
develop           ← Integration branch. All migration work lands here.
feature/phase-1-events ← Phase 1 work branch
feature/phase-2-config ← Phase 2 work branch
...               ← One branch per phase or major task
```

All feature branches must pass the smoke test before merging to `develop`. Never merge directly to `main` during migration.

### 3.3 IDE Setup — Required Configuration

All developers must use IntelliJ IDEA (2023.x or later) with the following configuration:

1. **Kotlin Plugin:** Must match Kotlin version 1.6.21. Check: `IntelliJ → Settings → Plugins → Kotlin`
2. **JDK:** Must target JDK 17. Check: `File → Project Structure → Project SDK`
3. **Code Style:** Import the project's `.editorconfig` or use default Kotlin code style
4. **Inspections:** Enable "Package naming convention" and "Unused imports" inspections

**Required IntelliJ settings for safe refactoring:**
- `Settings → Editor → Code Style → Kotlin → Imports` — set to optimize imports on reformat
- Always use `Refactor → Move` (not copy-paste) when moving files — this updates all usages automatically
- After any Move refactor, run `Code → Optimize Imports` on the modified files
- Before committing, run `Build → Rebuild Project` to catch any missed reference updates

**Verification step after any package move:**
1. Run `Build → Rebuild Project` in IntelliJ
2. Confirm zero errors in the `Build` output pane
3. Confirm zero red underlines in any of the moved files
4. Then run `./gradlew build` in terminal as final confirmation

**Never use find-and-replace to update package names.** IntelliJ's refactoring tools update all usages atomically. Manual find-replace misses Kotlin type aliases, KSP annotations, and Java interop references.

### 3.4 Confirm Build Configuration

Before starting Phase 1, verify:
- `./gradlew build` completes successfully
- `./gradlew test` completes with no failures
- `./gradlew syncTest` (if applicable) completes
- Kotlin version: `1.6.21` (confirmed in `build.gradle.kts`)
- JVM target: `17` (confirmed in `build.gradle.kts`)
- KSP: `1.6.21-1.0.6` (confirmed in `build.gradle.kts`)

---

## 4. Phase 1: Event Bus Relocation
**Duration estimate:** 2-3 days  
**Risk level:** Medium (many import changes, but no logic changes)  
**Who:** Engine Dev + Migration Dev

### 4.1 Tasks

| # | Task | Files Affected | Risk |
|:---|:---|:---|:---|
| 1.1 | Create `net.dodian.uber.game.engine.event` package | Directory only | None |
| 1.2 | Move `GameEvent.kt` (interface) to `engine.event` | All event class files import update | Low |
| 1.3 | Move `GameEventBus.kt` to `engine.event` | ~30 files update imports | Medium |
| 1.4 | Move `EventListener.kt` to `engine.event` | `GameEventBus` import update | Low |
| 1.5 | Move `EventFilter.kt` to `engine.event` | `GameEventBus` import update | Low |
| 1.6 | Move `ReturnableEventListener.kt` to `engine.event` | `GameEventBus` import update | Low |
| 1.7 | Move `GameEventScheduler.kt` to `engine.event` | 1-2 callers update | Low |
| 1.8 | Move `event/bootstrap/` to `engine.event.bootstrap` | `GameEventBus` import update | Low |
| 1.9 | Create `net.dodian.uber.game.events` flat package | Directory only | None |
| 1.10 | Move all `game.event.events.*` data classes to `game.events` | All listener registrations update | Medium |
| 1.11 | Move `SkillingEvents.kt` to `game.events.skilling` | Skill plugin imports update | Low |

### 4.2 Execution Steps for Each Move (Follow Exactly)

1. Open the file in IntelliJ
2. `Right-click on class name → Refactor → Move` to target package
3. Check "Update usages" is ticked
4. Click Refactor
5. Resolve any remaining red imports (IDE usually handles them)
6. Run `./gradlew build` — must succeed
7. Run smoke test
8. Commit: `"refactor: Move [ClassName] to [TargetPackage]"`

### 4.3 Phase 1 Exit Checklist

- [ ] `net.dodian.uber.game.engine.event.GameEventBus` exists and compiles
- [ ] `net.dodian.uber.game.events.NpcClickEvent` exists (flat, no `events.events` nesting)
- [ ] `net.dodian.uber.game.events.skilling.SkillingEvents` contains all 4 skilling signals
- [ ] `./gradlew build` passes
- [ ] Smoke test passes: NPC dialogue, banking, skilling, login/logout

---

## 5. Phase 2: Engine Config Relocation + Missing Signals
**Duration estimate:** 1-2 days  
**Risk level:** Low  
**Who:** Engine Dev

### 5.1 Tasks

| # | Task | Notes |
|:---|:---|:---|
| 2.1 | Move `game.config.*` to `engine.config.*` | `DotEnvConfig`, `RuntimeConfig`, runtime key constants |
| 2.2 | Update all `config.*` import sites | Via IDE refactor |
| 2.3 | Create missing signal data classes in `game.events` | `ItemClickEvent`, `ItemOnItemEvent`, `ItemOnObjectEvent`, `ItemOnNpcEvent`, `MagicOnNpcEvent`, `MagicOnPlayerEvent`, `MagicOnObjectEvent`, `PlayerLoginEvent`, `PlayerLogoutEvent`, `LevelUpEvent`, `DeathEvent` |
| 2.4 | Wire `PlayerLoginEvent` in login flow | After player data is loaded and world entry completes |
| 2.5 | Wire `PlayerLogoutEvent` in logout flow | Before `PlayerSaveService.queueSave()` |

### 5.2 Phase 2 Exit Checklist

- [ ] `net.dodian.uber.game.engine.config` contains all config classes
- [ ] `net.dodian.uber.game.events` contains all new signal data classes
- [ ] `PlayerLoginEvent` fires during successful login (verified via log statement)
- [ ] `PlayerLogoutEvent` fires before player save (verified via log statement)
- [ ] `./gradlew build` passes
- [ ] Smoke test passes

---

## 6. Phase 3: Skills Core → Systems Layer + ProgressionService
**Duration estimate:** 3-4 days  
**Risk level:** Medium  
**Who:** Engine Dev

### 6.1 Tasks

| # | Task | Notes |
|:---|:---|:---|
| 3.1 | Create `systems.skills` package | Directory only |
| 3.2 | Move `content.skills.core.runtime.GatheringTask` → `systems.skills` | Update all skill plugins |
| 3.3 | Move `content.skills.core.runtime.ActionStopReason` → `systems.skills` | Update all skill plugins |
| 3.4 | Move `content.skills.core.requirements.*` → `systems.skills.requirements` | Update all skill plugins |
| 3.5 | Move `content.skills.core.events.*` → absorbed into `events.skilling` | Merge with existing skilling events |
| 3.6 | Move `content.skills.core.resource.*` → `systems.skills` | All gatherable node classes |
| 3.7 | Move remaining `content.skills.core.runtime.*` → `systems.skills` | Per-skill runtime helpers |
| 3.8 | Create `ProgressionService` in `systems.skills` | Full implementation per Spec 07 |
| 3.9 | Replace all direct XP assignment (`playerXP[id] += x`) with `ProgressionService.addXp()` | Search entire codebase: `grep -r "playerXP\[" src/main/` |
| 3.10 | Wire `LevelUpEvent` from `ProgressionService.addXp()` | Verify level-up interface appears in game |
| 3.11 | Create `ProductionTask` in `systems.skills` | Per Spec 05 |

### 6.2 XP Assignment Audit

Before starting 3.9, run:
```bash
grep -r "playerXP\[" "/Users/alexismaneely/Desktop/ub3r-monorepo/game-server old/src" \
    --include="*.kt" --include="*.java"
grep -r "addXp\|addExperience\|giveXp" \
    "/Users/alexismaneely/Desktop/ub3r-monorepo/game-server old/src" \
    --include="*.kt" --include="*.java"
```

Every result must be replaced with `ProgressionService.addXp()` unless the site is in `ProgressionService` itself.

### 6.3 Phase 3 Exit Checklist

- [ ] `content.skills.core` package no longer exists
- [ ] `systems.skills.GatheringTask` exists and compiles
- [ ] `systems.skills.ProgressionService` exists with `addXp()` and `levelForXp()`
- [ ] `LevelUpEvent` fires when a skill gains a level (verified in-game)
- [ ] All XP gains go through `ProgressionService.addXp()`
- [ ] All 4 skilling event types still fire correctly (verified via log)
- [ ] `./gradlew build` passes
- [ ] Full smoke test passes including skill XP save/load

---

## 7. Phase 4: Content Flattening — NPC Scripts + Systems API
**Duration estimate:** 3-5 days  
**Risk level:** Medium  
**Who:** Content Dev + Migration Dev

### 4.1 NPC Script Flattening Order

NPC scripts are the highest-volume content move. Strategy: use the IDE to move the entire `content/npcs/spawns/` package to `content/npcs/` at once, then update all package declarations.

```bash
# Count the files to move
find "/Users/alexismaneely/Desktop/ub3r-monorepo/game-server old/src/main/kotlin/net/dodian/uber/game/content/npcs" \
    -name "*.kt" | wc -l
```

Steps:
1. IntelliJ refactor: Move `content.npcs.spawns` package to `content.npcs` (entire package at once)
2. IDE updates all import sites automatically
3. Delete the now-empty `spawns/` directory
4. Build and verify NPC interactions for a sampling of 5 different NPC IDs
5. Commit

### 7.2 `ContentActions` API Documentation

`ContentActions` at `systems.api.content.ContentActions` and `ContentInteraction` already exist. Document these in `systems/api/README.md` so content developers know to use them:

- `ContentActions.cancel(player, reason, ...)` — cancel active player action
- `ContentInteraction.canStartDialogue(player)` — check if player can start a new dialogue
- `ContentInteraction.blockingInteractionMessage(player)` — message to send when blocked

### 7.3 NpcIds, ItemIds, ObjectIds Constants

Create the 3 constant files in `content/`:
- `content/NpcIds.kt`
- `content/ItemIds.kt`
- `content/ObjectIds.kt`

Populate from the existing codebase: scan all content files for raw integer IDs used as NPC IDs, item IDs, and object IDs. Convert them to named constants and update the usages.

### 7.4 Phase 4 Exit Checklist

- [ ] `content/npcs/spawns/` directory deleted
- [ ] All NPC scripts in flat `content/npcs/`
- [ ] `NpcIds.kt`, `ItemIds.kt`, `ObjectIds.kt` exist with named constants for all IDs found
- [ ] `ContentActions` API is documented
- [ ] 5+ randomly sampled NPC scripts work in-game
- [ ] `./gradlew build` passes
- [ ] Smoke test passes

---

## 8. Phase 5: UI Flattening + Systems API
**Duration estimate:** 4-6 days  
**Risk level:** High (many interface files, easy to break button IDs)  
**Who:** Content Dev (1 interface per dev working in parallel)

### 8.1 Interface Migration Strategy

Do NOT migrate all interfaces in one commit. Migrate one interface at a time:

1. Choose an interface subdirectory (e.g., `content/interfaces/bank/`)
2. Read all files in it
3. Create `content/ui/BankInterface.kt` following the template in Spec 06, Section 6
4. Verify all button IDs are correct (check against `UpstreamHandler.java` button handling)
5. Transfer all button click logic to the new file
6. Delete the old directory
7. Build and test the specific interface in-game
8. Commit: `"refactor: flatten BankInterface to content/ui/"`

### 8.2 Interface Migration Priority Order

High-traffic interfaces first (most likely to catch issues early):

1. `BankInterface` — used constantly
2. `EquipmentInterface` — slot management
3. `PrayerInterface` — prayer toggles
4. `MagicInterface` — spellbook
5. `CombatInterface` — combat style
6. `SettingsInterface` — game options
7. `TradeInterface` — trade screen
8. `DuelInterface` — duel screen
9. `SmithingInterface` — smithing menu
10. `CraftingInterface` — crafting menu
11. `FletchingInterface` — fletching menu
12. `AppearanceInterface` — makeover mage
13. `EmotesInterface` — emote list
14. `QuestInterface` — quest tab
15. `SkillGuideInterface` — skill guide popup
16. `SkillingMenuInterface` — quantity selection
17. `RewardsInterface` — vote rewards
18. `TravelInterface` — spirit trees/carpets
19. `PartyRoomInterface` — party room
20. (Delete `content/interfaces/dialogue/` — no migration needed)

### 8.3 Button ID Audit Process

Before migrating each interface, run:
```bash
grep -r "buttonId ==" src/main/kotlin/net/dodian/uber/game/content/interfaces/INTERFACE_NAME/ \
    --include="*.kt"
```

Note every button ID and verify it matches the constant name you will define.

### 8.4 Phase 5 Exit Checklist

- [ ] `content/interfaces/` directory is completely deleted
- [ ] `content/ui/` directory exists with 19 named interface files
- [ ] `InterfaceIds.kt`, `SidebarIds.kt`, `ComponentIds.kt` exist with named constants
- [ ] `InterfaceService.kt` exists and wraps all outbound UI packets
- [ ] No `import net.dodian.uber.game.netty.listener.out.*` in any `content/ui/` file
- [ ] All 19 interfaces verified working in-game
- [ ] `./gradlew build` passes
- [ ] Full smoke test passes with banking and trading verified

---

## 9. Phase 6: Skill Content Flattening
**Duration estimate:** 5-7 days  
**Risk level:** Medium  
**Who:** Content Dev (1-2 skills per dev working in parallel)

### 9.1 Per-Skill Migration Template

For each of the 14 non-combat skills currently in `content/skills/`:

1. Create `content/skills/SkillName.kt` (Brain)
2. Create `content/skills/SkillNameData.kt` (Data)
3. Move: Definitions → Data file
4. Move: Service logic → Brain file
5. Move: Plugin registration → Brain `register()` function
6. Merge: All nested `objects/*`, `items/*` sub-packages into Brain
7. Delete: All old subdirectories for this skill
8. Build and test: Gain XP in-game, verify save/load
9. Commit: `"refactor: flatten [SkillName] to Brain/Data"`

### 9.2 Skill Migration Priority Order

1. Woodcutting (simple — reference implementation)
2. Cooking
3. Fishing
4. Mining
5. Prayer
6. Runecrafting
7. Herblore
8. Crafting
9. Fletching
10. Smithing (complex — separate last within production skills)
11. Thieving
12. Slayer
13. Farming (farming scheduler stays in systems — careful)
14. Agility (complex — multiple courses)

### 9.3 Skill Equivalency Verification

For each skill after migration, verify:
- Player gains XP at the correct rate (compare before/after for same action)
- Level-up interface appears at the correct level
- XP saves to `character_stats` table correctly (check DB directly)
- Task cancellation works (walk away mid-skill loop)

### 9.4 Phase 6 Exit Checklist

- [ ] `content/skills/` contains only Brain and Data files — no subdirectories
- [ ] `content/skills/core/` directory deleted entirely
- [ ] All 14 skills have at least a Brain file (Data file optional if no definitions needed)
- [ ] `ProgressionService.addXp()` is the only XP grant mechanism
- [ ] `LevelUpEvent` fires for all skills when a level-up occurs
- [ ] Farming world tick still runs correctly from `systems.world.farming.FarmingScheduler`
- [ ] Agility course timing verified — course completion awards correct XP
- [ ] `./gradlew build` passes
- [ ] Full smoke test passes with all core skills verified

---

## 10. Phase 7: Items, Objects, Commands Flattening
**Duration estimate:** 3-4 days  
**Risk level:** Low  
**Who:** Content Dev + Migration Dev

### 10.1 Commands Permission Consolidation

Because command files are currently stored in `boss/`, `dev/`, `player/`, `social/`, `staff/`, and `travel/`, do NOT just dump everything into the root of `content/commands/`.

1. Create `content/commands/BossCommands.kt` and move all boss command logic into it.
2. Create `content/commands/DevCommands.kt` and move all dev command logic into it.
3. Create `content/commands/PlayerCommands.kt` ... and so on.

Delete the old subdirectories once empty.

### 10.2 Phase 7 Exit Checklist

- [ ] All `content/items/*` subdirectories are deleted; items are flat in `content/items/`.
- [ ] `content/objects/services/` and `content/objects/dsl/` moved to `systems/interaction/`.
- [ ] All `content/commands/*` subdirectories are deleted.
- [ ] Commands are consolidated by permission group (e.g., `PlayerCommands.kt`, `StaffCommands.kt`).
- [ ] Smoke test passes including teleport/admin commands.
- [ ] `./gradlew build` passes cleanly.

---

## 11. Post-Phase Quality Checks (All Phases Complete)

After all 7 phases are complete, run the following final audit:

### 10.1 Prohibited Import Scan

```bash
# No content file imports Netty
grep -r "import io.netty" \
    "src/main/kotlin/net/dodian/uber/game/content" --include="*.kt"
# Any output = FAIL

# No content file imports SQL
grep -r "import java.sql" \
    "src/main/kotlin/net/dodian/uber/game/content" --include="*.kt"
# Any output = FAIL

# No content file imports engine sync
grep -r "import net.dodian.uber.game.engine.sync" \
    "src/main/kotlin/net/dodian/uber/game/content" --include="*.kt"
# Any output = FAIL

# No content file imports persistence SQL repository
grep -r "PlayerSaveSqlRepository" \
    "src/main/kotlin/net/dodian/uber/game/content" --include="*.kt"
# Any output = FAIL

# No content file uses raw walk queue fields
grep -r "walkingQueue\|wQueueReadPtr\|wQueueWritePtr" \
    "src/main/kotlin/net/dodian/uber/game/content" --include="*.kt"
# Any output = FAIL
```

### 10.2 Depth Audit

```bash
# Find any content file at depth 3+
find "src/main/kotlin/net/dodian/uber/game/content" -name "*.kt" | \
    awk -F'/' '{print NF-1 " " $0}' | \
    awk '$1 > 12 {print}'    # adjust depth number for your path length
# Any output = FAIL
```

### 10.3 Nesting Rule Verification (Max 2 Levels Under `content/`)

The content package tree after all phases:
```
content/                   ← allowed
content/npcs/              ← depth 1 ✔
content/ui/                ← depth 1 ✔
content/skills/            ← depth 1 ✔
content/objects/           ← depth 1 ✔
content/items/             ← depth 1 ✔
content/commands/          ← depth 1 ✔
content/dialogue/          ← depth 1 ✔
content/minigames/         ← depth 1 ✔
content/events/            ← depth 1 ✔
content/combat/            ← depth 1 ✔
```

No `content/skills/mining/objects/` (depth 3). No `content/interfaces/bank/buttons/` (depth 3). Zero exceptions.

---

## 12. Regression Test Matrix

After all phases, each item in this matrix must pass in-game:

| Feature | Test Action | Expected Result |
|:---|:---|:---|
| Login | Log in with existing account | Character spawns, XP correct |
| Bank | Click bank booth | Bank opens, items display |
| Bank save | Deposit item, log out, log in | Item is in bank |
| NPC dialogue | Click NPC with dialogue | Dialogue tree appears |
| Dialogue options | Click an option in dialogue | Branch executes |
| Woodcutting | Click tree with axe | XP gained, log received |
| Mining | Click ore rock with pickaxe | XP gained, ore received |
| Fishing | Click fishing spot | XP gained, fish received |
| Cooking | Use raw food on fire | XP gained, cooked food |
| Prayer | Bury dragon bones | XP gained |
| Combat | Attack monster | Damage, XP gained |
| Combat death | Die in combat | Respawn at spawn point |
| Equipment | Equip/unequip item | Stats update |
| Trade | Initiate trade | Trade screen opens |
| Level up | Gain enough XP | Level-up interface displays |
| Commands | `/home` or equivalent | Player teleported |
| Logout | Click logout | Server receives clean disconnect, data saved |
| Reconnect after logout | Log back in | All stats and items intact |

---

## 13. Timeline Summary

| Phase | Description | Duration | Risk |
|:---|:---|:---|:---|
| 0 | Pre-migration hardening | 1 day | Low |
| 1 | Event bus relocation | 2-3 days | Medium |
| 2 | Config relocation + missing signals | 1-2 days | Low |
| 3 | Skills core → systems + ProgressionService | 3-4 days | Medium |
| 4 | NPC flattening + systems API | 3-5 days | Medium |
| 5 | UI flattening + interface consolidation | 4-6 days | High |
| 6 | Skill content flattening (14 skills) | 4-5 days | Medium |
| 7 | Items, Objects, Commands (Perm Group) | 3-4 days | Low |
| — | Final audit + regression test | 1-2 days | Low |
| **Total** | | **~24-34 days** | |

---

## 13. CHANGELOG Convention

Every commit during this migration must follow this format:

```
refactor: [What was moved/renamed/consolidated]

[Body: Brief description of what changed structurally. Never describe new behavior.]

Spec: [Spec number that authorized this change, e.g., "Spec 01, Phase 1, Task 1.3"]
Smoke: [PASS | FAIL — result of smoke test on this branch]
```

### Good Commit Messages
```
refactor: Move GameEventBus to engine.event package

Moved GameEventBus.kt from game.event to game.engine.event.
Updated all import sites (~28 files). No logic changes.

Spec: Spec 01, Phase 1, Task 1.3
Smoke: PASS
```

```
refactor: Flatten content/interfaces/bank/ to content/ui/BankInterface.kt

Consolidated 3 bank interface files into a single BankInterface.kt.
All existing button IDs verified against UpstreamHandler.java.
No new buttons or features added.

Spec: Spec 06, Phase 5
Smoke: PASS
```

### Bad Commit Messages (Reject These in Review)
```
# BAD: Vague
fix: update bank interface

# BAD: Hides added content
refactor: improve Hans dialogue system

# BAD: No spec reference
chore: move stuff around

# BAD: No smoke test result
refactor: restructure mining skill
```

---

## 14. Definition of Done for the Entire Dodian-Flat Migration

- [ ] All 7 phases complete
- [ ] `00_MASTER_RULES_READ_FIRST.md` has been acknowledged by every developer who committed code
- [ ] Prohibited import scan returns zero results for all prohibited patterns
- [ ] Depth audit confirms no content file is > 2 levels deep
- [ ] `./gradlew build` passes cleanly
- [ ] `./gradlew test` passes cleanly
- [ ] All smoke test criteria pass
- [ ] Regression test matrix fully passes
- [ ] MySQL schema is unchanged (compare `SHOW CREATE TABLE characters;` and `character_stats;` against baseline)
- [ ] JVM target remains 17, Kotlin version remains 1.6.21
- [ ] No pathfinding code was touched (verify via `git diff main -- 'src/main/java/**/*Client.java'`)
- [ ] `GameEventBus`, `GameTask`, `GameTaskRuntime`, `InteractionProcessor`, `DialogueService`, `PlayerSaveSqlRepository` are all confirmed functional
- [ ] The `game-server old/docs/dodian-flat-specs/` directory is complete with all 11 files (00–10)
- [ ] **NO NEW CONTENT WAS ADDED** — confirmed by:
  - `git log --all --oneline | grep -Ei "new dialogue|new npc|new item|new feature|new quest|new command"` returns zero results
  - A senior developer has reviewed the final diff and signed off that no player-visible behavior changed
  - The regression test matrix shows zero behavioral differences from pre-migration baseline
