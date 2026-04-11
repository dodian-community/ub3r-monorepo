# Trading & Dueling State Machines

## Overview
Trading and Dueling are the two most complex social interactions in the game because they require synchronizing the state of two independent `Client` objects. If they fall out of sync, players can duplicate items or get stuck.

## The Shared State Machine
Both systems use a similar flow, managed by boolean flags inside `Client.java`.

### 1. The Request Phase
- **Action**: Player A uses a player-interaction option (Trade or Duel) on Player B.
- **Intent**: `PacketInteractionRequestService` handles the request.
- **Validation**: The server checks distance, line-of-sight, and if the other player is already busy (`inTrade`, `inDuel`, `duelFight`, `IsBanking`).
- **Notification**: Player B receives a message ("Player A wishes to trade with you.").

### 2. The Offer Phase (Screen 1)
- If Player B accepts the request, both players are flagged `inTrade = true` or `inDuel = true`.
- The server opens the Offer Interface for both players.
- **Action**: Players add items to the screen. 
- **Security**: Items are **NOT** removed from the player's inventory yet. They are copied to an "offer array" (`offeredItems`). The server strictly checks that the player actually has the item before adding it to the array.

### 3. The First Confirmation
- **Action**: Player A clicks "Accept".
- **State**: `tradeConfirmed = true` (or `duelConfirmed = true`).
- The server checks if Player B has also clicked Accept. If so, it moves both players to Screen 2.

### 4. The Final Confirmation (Screen 2)
- **Action**: Both players review the final trade window.
- **Action**: Both players click "Accept" again.
- **State**: `tradeConfirmed2 = true`.
- **Execution**: ONLY when both players have confirmed on Screen 2 does the server actually execute the item transfer. 
    1. It removes the items from Player A's inventory.
    2. It adds Player B's offered items to Player A's inventory.
    3. It repeats for Player B.
    4. It clears all flags (`inTrade = false`).

## Dueling Specifics
Dueling adds two extra complexities:

1. **Rules**: On Screen 1, players can toggle rules (No Magic, No Food, Obstacles). These are synced between both clients (`toggleDuelRule()`).
2. **The Fight Phase**: After Screen 2, instead of transferring items, the server teleports both players to the Duel Arena and sets `duelFight = true`.

### The Duel Fight
- During `duelFight`, standard combat rules apply, but the `PlayerAttackCombat` system respects the toggled rules (e.g., if No Magic is checked, spell casting is blocked).
- When a player dies in a duel, `PlayerDeathTickService` intercepts the death. Instead of dropping items on the floor, it:
    1. Restores the loser's stats and teleports them out.
    2. Calculates the winner.
    3. Awards the staked items to the winner's inventory.
    4. Resets `duelFight = false` for both.

## Audit Logging
Because these systems are prime targets for real-world trading (RWT) and duplication glitches, every finalized trade and duel is logged using `TradeLog.kt` and `DuelLog.kt`. These logs are written asynchronously to the database and/or file system for administrative review.