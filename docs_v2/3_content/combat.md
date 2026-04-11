# Combat System

## Overview
The combat system manages all offensive and defensive interactions. It is designed around a stateful target system and evaluates logic on a strict 600ms tick cycle.

## Entry and State
Combat begins when a player selects the "Attack" option on an entity (or casts a spell).
1.  **Initiation**: `CombatStartService.beginAttackNow()` is called.
2.  **Target Lock**: The player's `target` (a reference to the `Entity`) and `combatTargetState` are set.
3.  **Persistence**: The player remains in combat state until the target dies, moves out of range, or the player initiates a different action (like walking away).

## The Combat Tick (`CombatRuntimeService`)
During the `PlayerMainPhase` of the game loop, `CombatRuntimeService.process()` evaluates every player currently in combat.

1.  **Validation**: Is the target still alive and online?
2.  **Pathfinding**: Is the player within the required range? (1 tile for Melee, up to 10 for Magic/Ranged). If not, `FollowRouting` updates their walking queue.
3.  **Timing**: Has enough time passed since the last attack? It checks `currentCycle >= player.nextAttackCycle`.
4.  **Execution**: If all checks pass, it calls `Client.attackTarget()`.

## Combat Styles
`Client.attackTarget()` dispatches the attack to one of three specialized singletons:

### `MeleeCombat.kt`
- Calculates max hit based on Strength level and equipment bonuses.
- Calculates accuracy based on Attack level vs. target's Defence level.
- Plays weapon animations.

### `RangedCombat.kt`
- Verifies ammunition (arrows/bolts) and removes them from the equipment slot.
- Calculates distance and creates a `Projectile` object sent to the client.
- Calculates damage based on Ranged level and ammo type.

### `MagicCombat.kt`
- Verifies runes in the inventory and consumes them.
- Creates magical projectiles and target graphics (e.g., Ice Barrage freeze block).
- Applies special effects (freezing, lowering stats).

## The Hit Queue (`CombatHitQueueService`)
Damage is **not** applied instantly. If you shoot an arrow, the damage shouldn't appear until the arrow visually hits the target.

When an attack lands, a `CombatHit` is created and given a delay (e.g., 0 ticks for melee, 2-3 ticks for ranged depending on distance). It is added to the `CombatHitQueueService`.

During the `CombatHitQueueService.process()` phase of the game loop, hits whose delay has expired are applied:
1.  Target's HP is reduced.
2.  `UpdateFlag.HIT` (or `HIT2`) is flagged for the target so the client sees the red splat.
3.  If HP reaches 0, the Death Sequence is initiated.

## Death Sequence (`PlayerDeathTickService`)
When an entity dies, it doesn't disappear instantly.
1.  **Animation**: The death animation plays.
2.  **Wait**: A short delay (usually 3-4 ticks) occurs to match the animation length.
3.  **Drop Loot**: The killer is determined, and items are dropped on the ground.
4.  **Respawn**: The player is teleported to the spawn point, and stats are restored. (NPCs are hidden and scheduled for respawn via `NpcTimerScheduler`).