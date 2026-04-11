# Kotlin Restructure Plan (DDD & Dev-Friendly)

This document outlines the "Gold Standard" package structure for the Ub3r Kotlin directory. It moves from a technical-layered approach to a **Domain-Driven Design (DDD)** that prioritizes content discoverability.

---

## 🏛️ Proposed Root: `net.dodian.uber.game`

We will divide the codebase into two primary top-level packages:
1.  **`domain`**: The "What" (Gameplay, Entities, Skills). This is where content devs spend 90% of their time.
2.  **`infra`**: The "How" (Networking, Database, Game Loop). This is the internal engine.

---

## 🧩 1. The Gameplay Domains (`domain.*`)

| New Package | Content | Purpose |
| :--- | :--- | :--- |
| **`domain.skill`** | `Woodcutting`, `Mining`, `ProgressionService` | All skilling logic and plugins. |
| **`domain.combat`** | `MeleeCombat`, `SpecialAttacks`, `HitQueue` | Combat styles, formulas, and death sequences. |
| **`domain.entity.player`**| `PlayerRegistry`, `Appearance`, `Stats` | Player-specific state and management. |
| **`domain.entity.npc`** | `NpcManager`, `NpcModule`, `NpcAI` | NPC definitions, spawns, and behavior. |
| **`domain.world`** | `ObjectContent`, `GroundItems`, `Chunks` | Physical world objects, spatial partitioning, and regions. |
| **`domain.social`** | `Chat`, `Friends`, `Trading`, `Dueling` | Player-to-player interactions. |
| **`domain.interaction`** | `Intents`, `Tasks`, `DistanceChecks` | The generic "Action Engine" (formerly `dispatch`). |
| **`domain.activity`** | `Quests`, `Minigames`, `BossLogs` | High-level player progression and events. |

---

## ⚙️ 2. The Infrastructure (`infra.*`)

| New Package | Content | Purpose |
| :--- | :--- | :--- |
| **`infra.network`** | `Netty`, `Packets`, `ISAAC`, `Login` | Protocol handling and I/O. |
| **`infra.database`** | `SQL`, `Repositories`, `Saving/Loading` | Persistence and world data syncing. |
| **`infra.heartbeat`** | `GameLoopService`, `TickScheduler` | The 600ms heartbeat and execution phases. |
| **`infra.event`** | `GameEventBus`, `Listeners` | The internal messaging system. |
| **`infra.task`** | `GameTask`, `Coroutines`, `Wait` | The coroutine execution engine. |

---

## 🔄 Migration Table (Current -> Proposed)

| Current Package | Proposed Package | Action |
| :--- | :--- | :--- |
| `systems.skills` | `domain.skill` | Consolidate logic and data. |
| `systems.combat` | `domain.combat` | Move from systems to domain. |
| `systems.dispatch.objects` | `domain.world.object` | Rename technical "Dispatch" to "World". |
| `systems.dispatch.npcs` | `domain.entity.npc.interaction` | Group with NPC domain. |
| `systems.interaction` | `domain.interaction` | Keep core action engine separate. |
| `systems.ui.dialogue` | `domain.social.dialogue` | Group with social interactions. |
| `content.npcs` | `domain.entity.npc.content` | Move definitions into the NPC domain. |
| `model.chunk` | `domain.world.spatial` | Move spatial logic into World domain. |
| `persistence.*` | `infra.database` | Move to infra. |
| `engine.loop` | `infra.heartbeat` | Move to infra. |

---

## 🛠️ Step-by-Step Implementation (No Major Rewrites)

1.  **Phase 1: Create the `domain` and `infra` roots.**
2.  **Phase 2: Move "Leaf" packages first.** (e.g., Move `systems.skills.plugin` to `domain.skill.plugin`).
3.  **Phase 3: Update `ksp-processor`**.
    - **CRITICAL**: The `PluginModuleIndexSymbolProcessor` uses package strings to find content. You must update the strings in the processor to match the new `domain.*` paths.
    - *Example*: Change `packageName.startsWith("net.dodian.uber.game.content")` to `packageName.startsWith("net.dodian.uber.game.domain")`.
4.  **Phase 4: Run a full project "Optimize Imports"**. Modern IDEs like IntelliJ will handle the renaming of imports across the project in seconds.
5.  **Phase 5: Re-generate Index**. Run `./gradlew clean build` to allow KSP to build the new `GeneratedPluginModuleIndex`.

---

## ✨ Why this is Better
- **Intuitive**: A new dev wants to fix a tree? They look in `domain.world.object` or `domain.skill`.
- **Clean Boundaries**: If the server is lagging, the lead dev looks in `infra.heartbeat`. If an NPC isn't dropping loot, they look in `domain.entity.npc`.
- **Scalable**: As new minigames or skills are added, the structure doesn't get "flatter" or "messier"—it just gets more entries in the relevant domain.
