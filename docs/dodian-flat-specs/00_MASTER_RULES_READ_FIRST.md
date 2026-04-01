# 00: MASTER RULES — READ THIS BEFORE TOUCHING ANYTHING
### Dodian-Flat Architecture — Non-Negotiable Laws

---

> ⛔ **THIS FILE IS THE LAW. ALL OTHER SPECS DERIVE FROM IT.**
> If anything in Specs 01–10 appears to conflict with this document, this document wins.
> Every developer on this project must read and sign off on this file before making any commit.

---

## LAW 1 — NO NEW CONTENT. ZERO EXCEPTIONS.

This is the single most important rule of the entire migration.

**The scope of this entire project is:**
- Moving files to new packages
- Renaming packages and directories
- Consolidating multiple files into one
- Adding Kotlin wrapper functions over existing Java code
- Adding type-safe constants over existing raw integers
- Moving infrastructure classes to their correct architectural layer
- Creating plumbing (event signals, attribute bridges, DSL helpers) that existing content will use

**The scope explicitly excludes:**
- ❌ Adding dialogue to an NPC that does not currently have dialogue
- ❌ Changing what an NPC currently says or does
- ❌ Adding a new skilling mechanic that does not currently exist
- ❌ Adding new items, new drop tables, new reward systems, new quests
- ❌ Changing combat formulas or damage values
- ❌ Adding new game modes, new areas, or new features players can see or interact with
- ❌ Changing the rules of any existing minigame
- ❌ Adding new commands that players or staff can use
- ❌ Changing loot tables, XP rates, or level requirements

### About the Code Examples in These Specs

Every code example in Specs 01–10 uses **existing content as a pattern illustration only**. When a spec shows `Hans.kt` with dialogue, that is showing you **the pattern for migrating Hans's existing dialogue** — not adding new things for Hans to say. When a spec shows `BankInterface.kt`, it is showing you the pattern for migrating the existing bank interface code — not adding new bank features.

**Any developer who reads an example and then adds new functionality to that NPC, interface, or feature has violated Law 1.**

The rule of thumb: if a player can see, feel, or measure the difference before and after your commit, you have done it wrong.

### What "Backend Improvement" Means (Allowed)

Backend improvements that do not affect player experience are allowed:
- The bank now opens via `BankInterface.open(player)` instead of `player.send(ShowInterface(5292))` — **allowed** (same interface, same behavior, better code)
- NPC Hans now fires his existing dialogue via `GameEventBus.on<NpcClickEvent>` instead of a direct Java dispatch — **allowed** (same dialogue, better wiring)
- Mining XP is now granted via `ProgressionService.addXp()` instead of `client.playerXP[14] += 35` — **allowed** (same XP, same value, better implementation)
- The bank `ButtonClickEvent` handler now uses `BankInterface.BTN_DEPOSIT_INVENTORY` instead of the raw integer `47` — **allowed** (named constant, same behavior)

---

## LAW 2 — NO MySQL SCHEMA CHANGES. ZERO EXCEPTIONS.

The `characters` and `character_stats` tables are **permanently frozen** for this migration.

- ❌ Do not add new columns
- ❌ Do not rename existing columns
- ❌ Do not change column types
- ❌ Do not add new tables
- ❌ Do not drop tables or columns
- ❌ Do not change indexes or keys

If something you are implementing requires a new column, stop and seek approval before proceeding. The effects columns and JSON overflow system (Spec 03) exist precisely to accommodate new per-player variables without schema changes.

**Verification:** After every phase, run `SHOW CREATE TABLE characters;` and `SHOW CREATE TABLE character_stats;` and compare to the baseline snapshot. Any difference is a violation.

### Baseline Schema Snapshot

Before starting any work on this migration, create a baseline:
```bash
mysql -u root -p game_db -e "SHOW CREATE TABLE characters\G" > /tmp/schema_baseline_characters.txt
mysql -u root -p game_db -e "SHOW CREATE TABLE character_stats\G" > /tmp/schema_baseline_character_stats.txt
```

Store these files in version control. Compare against them after every migration phase.

---

## LAW 3 — NO GAMEPLAY LOGIC CHANGES. ZERO EXCEPTIONS.

### What Is "Gameplay Logic"

Gameplay logic is any code that changes:
- What actions a player can perform
- What happens when a player performs an action
- When an action succeeds or fails
- What rewards or penalties a player receives
- How much XP, items, or currency a player gains or loses
- Combat hit rates, damage ranges, or formulas
- Timer intervals for skilling, spawning, or world events
- The text of NPC dialogue or interface messages

### The Only Allowed Changes During Migration

Only the following categories of change are allowed:

| Category | Allowed Change |
|:---|:---|
| **Package** | Moving a `.kt` file to a different directory, updating `package` declaration |
| **Import** | Updating `import` statements to match moved classes |
| **Refactor** | Extracting a Kotlin extension function that wraps an existing Java method |
| **Naming** | Replacing a raw integer with a named constant that has the same value |
| **Consolidation** | Merging multiple files into one file with no logic changes |
| **Tier correction** | Moving infrastructure code from `content.*` to `systems.*` or `engine.*` |
| **Plumbing** | Creating a new event signal, attribute bridge, or DSL helper |
| **Documentation** | Adding KDoc comments that describe existing behavior |
| **Test** | Adding a test that confirms existing behavior is preserved |

---

## LAW 4 — THE SERVER MUST BUILD AND BOOT AFTER EVERY COMMIT.

No commit may leave the codebase in a state where:
- `./gradlew build` fails  
- The server crashes on startup  
- A player cannot log in and out  

Use feature branches. Merge only when green.

---

## LAW 5 — NO PATHFINDING CHANGES.

The walk queue system in `Client.java` (`walkingQueue`, `wQueueReadPtr`, `wQueueWritePtr`, `primaryDirection`, `secondaryDirection`) is **completely frozen**. Do not touch it.

- ❌ Do not modify, wrap, extend, or call walk-queue methods from Kotlin
- ❌ Do not create a `PathfindingService`, `Router`, or `WalkEngine` class
- ❌ Do not move walk-queue code to a different class
- ✅ `player.teleportTo(position)` is the only movement allowed from content code

---

## LAW 6 — NO JAVA FILE MODIFICATIONS (WITH EXCEPTIONS).

The following Java directories/files must not be modified:
- `src/main/java/net/dodian/uber/game/netty/` — All Netty files frozen
- `src/main/java/net/dodian/uber/game/model/entity/player/Client.java` — Core player class frozen
- `src/main/java/net/dodian/utilities/Misc.java` — Utility class frozen
- All `src/main/java/*.java` packet listener files

Allowed Java modifications (by Engine Dev only, with review):
- Setting `player.pendingInteraction` to a new `InteractionIntent` subclass when wiring a new interaction type from a packet handler — this is in `UpstreamHandler.java` only, one line per new interaction type, no logic changes
- Adding `@JvmStatic` annotations to Kotlin companion objects where needed for Java interop — low risk

---

## LAW 7 — ONE FEATURE BRANCH PER PHASE.

Each phase from Spec 10 must live in its own Git feature branch. No phase may be merged to `develop` until:
1. `./gradlew build` passes on the branch
2. All items in the phase's exit checklist are checked off
3. The smoke test (Spec 10, Section 3.1) passes
4. At least one other developer has reviewed the PR

Branch naming convention:
```
feature/dodian-flat-phase-1-event-bus
feature/dodian-flat-phase-2-config-signals
feature/dodian-flat-phase-3-systems-skills
feature/dodian-flat-phase-4-npc-flattening
feature/dodian-flat-phase-5-ui-flattening
feature/dodian-flat-phase-6-skill-flattening
```

---

## LAW 8 — VERIFY BEFORE DELETE.

When flattening a directory, follow this order:
1. Create the target file(s) in the new location
2. Verify the new file compiles and all existing callers use it
3. Run `./gradlew build`
4. Run the smoke test
5. Only then: delete the old directory

Never delete a directory and then create the replacement in the same commit. Split it into at minimum two commits — or better, two PRs.

---

## LAW 9 — EXAMPLES IN SPECS ARE PATTERN ILLUSTRATIONS, NOT REQUIREMENTS.

Code examples that show specific NPCs, items, objects, or interfaces are illustrating the **pattern** you must follow for whatever content you are migrating. They are not instructions to:
- Create Hans's dialogue from scratch if it doesn't exist
- Add new BankInterface buttons that don't currently exist
- Create new constants for IDs that don't currently appear in the codebase

If the codebase does not currently have a `Hans.kt` NPC script file, you do not need to create one during this migration. The example is there to show you what the migrated version of an existing script would look like.

---

## LAW 10 — ALL IDS MUST BE VERIFIED AGAINST THE ACTUAL CLIENT BINARY.

Any time a spec shows an interface ID, component ID, button ID, NPC ID, item ID, or object ID, those values must be verified against:
1. The existing Java code (search `UpstreamHandler.java`, existing Kotlin files, or existing Java interface files)
2. The 317 client binary / cache data if available
3. The `GameObjectDef`, `ItemDef`, or `NpcDef` cache loader for object/item/NPC IDs

**Never commit a named constant with an ID value that has not been verified.** A wrong constant is worse than a raw integer — it teaches everyone the wrong value.

---

## The Review Checklist (For Every PR)

The PR author fills this out. The reviewer verifies each item.

```markdown
### Pre-Merge Review Checklist

**LAW 1 — No New Content:**
- [ ] No NPC dialogue was added or changed
- [ ] No item, object, or NPC behavior was added or changed
- [ ] No XP rates, drop tables, or level requirements were changed
- [ ] No new player-visible features were added

**LAW 2 — No Schema Changes:**
- [ ] Running `SHOW CREATE TABLE characters` matches baseline
- [ ] Running `SHOW CREATE TABLE character_stats` matches baseline

**LAW 3 — No Logic Changes:**
- [ ] All values in named constants match the values they replace
- [ ] All migrated methods produce identical output to the methods they replace

**LAW 4 — Server Must Build:**
- [ ] `./gradlew build` passes on this branch
- [ ] Smoke test passes: login, dialogue, bank, skilling, logout, re-login

**LAW 5 — No Pathfinding:**
- [ ] No Kotlin file touches `walkingQueue`, `wQueueReadPtr`, `wQueueWritePtr`
- [ ] No new pathfinding class was created

**LAW 6 — No Java Modifications:**
- [ ] `git diff --name-only HEAD~1` shows no `.java` files (or Engine Dev reviewed exception)

**Import Violations (grep must return empty for each):**
- [ ] `grep -r "import io.netty" src/main/kotlin/net/dodian/uber/game/content`
- [ ] `grep -r "import java.sql" src/main/kotlin/net/dodian/uber/game/content`
- [ ] `grep -r "import net.dodian.uber.game.engine.sync" src/main/kotlin/net/dodian/uber/game/content`
- [ ] `grep -r "PlayerSaveSqlRepository" src/main/kotlin/net/dodian/uber/game/content`
- [ ] `grep -r "walkingQueue\|wQueueReadPtr\|wQueueWritePtr" src/main/kotlin/net/dodian/uber/game/content`

**Depth Rule:**
- [ ] No content file is more than 2 directories deep under `content/`

**Reviewer sign-off:**
- [ ] Reviewed by: _____________ Date: _____________
```

---

## Quick Reference Card (Print This)

| ✅ ALLOWED | ❌ FORBIDDEN |
|:---|:---|
| Moving `.kt` files between packages | Changing what any NPC says or does |
| Renaming packages | Adding new dialogue, items, or features |
| Creating named constants for existing IDs | Changing XP rates, damage values, timers |
| Wrapping Java methods in Kotlin extensions | Modifying any Java file (except Engine Dev exceptions) |
| Consolidating multiple files into one | Adding new MySQL columns or tables |
| Moving infrastructure to the correct tier | Touching walk-queue code |
| Creating event signals, bridge keys, DSL helpers | Adding new commands players can use |
| Adding KDoc comments | Breaking the build |
| Adding tests for existing behavior | Deleting a file before verifying its replacement works |

---

## Who Owns What

| Area | Owner Role |
|:---|:---|
| `engine.*`, `systems.*`, Java files | **Engine Dev** — must have senior approval before merging |
| `content/npcs/`, `content/objects/`, `content/items/` | **Content Dev** — move only, verify behavior preserved |
| `content/ui/`, `content/commands/` | **Content Dev** — move only |
| `content/skills/` | **Content Dev + Engine Dev** — moving skills core requires Engine Dev review |
| `persistence.*` | **Engine Dev only** — do not touch without explicit task |
| All specs in `docs/dodian-flat-specs/` | **Tech Lead only** — specs are documents of record |
