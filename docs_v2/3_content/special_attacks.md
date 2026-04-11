# Combat Specials & Weapon Systems

## Overview
Combat in Ub3r is driven by a combination of the `CombatRuntimeService` and weapon-specific logic. For modern weapons and special attacks, the server uses a "Combat Style" approach.

## 1. Special Attacks
Special attacks are triggered by the "Special Attack Bar" on the client (Interface Button clicks).
- **Trigger**: `ClickingButtonsListener` (opcode 185) detects the special bar button.
- **State**: The player's `specialAmount` is checked.
- **Execution**: If the special is toggled, the next attack cycle in `CombatRuntimeService` will execute the "Special" logic.

### Special Attack Patterns
- **Multi-Hit**: (e.g., Dragon Dagger) Uses `HIT` and `HIT2` masks in the same tick.
- **Graphic Overlays**: Special attacks often trigger a GFX on both the player and the target (e.g., Abyssal Whip).
- **Drain Logic**: Special energy is subtracted immediately upon the attack being "landed" in the code, regardless of when the hit-splat appears.

## 2. Weapon Speed & Accuracy
- **Weapon Speed**: Defined in `GAME_ITEM_DEFINITIONS`. This value is used to calculate the `nextAttackCycle`.
- **Accuracy Formula**: The server uses a modernized version of the 317 combat formulas, taking into account:
    - Attacker's skill level (Attack/Ranged/Magic).
    - Attacker's equipment bonuses.
    - Defender's matching defensive bonus (e.g., Slash Defence vs. Slash Attack).
    - Prayer modifiers.

## 3. Auto-Retaliate
Auto-retaliate logic is handled during the `npcMainPhase` and `playerMainPhase`. If an entity receives a hit and their `target` is null, the system automatically calls `CombatStartService.beginAttackNow()` against the attacker, provided the "Auto-Retaliate" toggle is enabled in the client's UI settings.
