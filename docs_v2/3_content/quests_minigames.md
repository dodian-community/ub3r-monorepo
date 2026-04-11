# Quests, Minigames, and Boss Logs

## Overview
Ub3r uses a hybrid approach for player progression tracking. While modern skills use Kotlin DSLs, the Quest and Achievement systems still rely on a legacy array-based model for state, with Kotlin-based UI rendering.

## 1. The Quest System
Player quest progress is stored in a fixed-size integer array: `client.quests[]`.
- **Index**: Corresponds to the quest ID defined in the `QuestSend` enum.
- **Value**: Represents the "Stage" of the quest.
    - `0`: Not started (shown as @red@ in the quest tab).
    - `1 to (End-1)`: In progress (shown as @yel@).
    - `End`: Completed (shown as @gre@).

### UI Logic (`QuestSend.kt`)
The quest tab and the quest info interfaces are rendered using the `QuestSend` singleton. It maps quest IDs to interface line IDs (e.g., `PLAGUE_DOCKS` maps to line `7332`).

### Scripting Quests
Currently, quest logic (checking stages, giving rewards) is often mixed into `NpcModule` or `ObjectContent` plugins. 
*Recommendation*: Move complex quest flows into a dedicated `QuestService` or use the `GameEventBus` to track progress.

## 2. Monster and Boss Logs
Ub3r tracks every NPC kill for every player to provide a "Monster Log" and "Boss Log" feature.
- **Storage**: Two parallel arrays in the `Client` class: `monsterName[]` and `monsterCount[]` (and similar for bosses).
- **Persistence**: These counts are saved to the `GAME_PLAYER_BOSS_LOGS` and `GAME_PLAYER_MONSTER_LOGS` database tables.
- **Viewing**: Players can view their logs via the Quest tab, which triggers the `QuestSend.showMonsterLog()` UI logic.

## 3. Minigames
### Casino / Gambling
Located in `net.dodian.uber.game.content.minigames.casino`.
- Handles games of chance like Slot Machines or Dice.
- Uses standard Java/Kotlin random number generation.
- Logic is encapsulated in `CasinoService.kt` and triggered via object interactions or dialogues.

### Plunder
Mentioned in the `GameLoopService`, the Plunder system is a world-wide event or minigame that executes logic during the `WorldMaintenancePhase`. It typically involves global object spawns or timed resource events.
