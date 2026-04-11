# Database & Save/Load Flows

## Overview
Ub3r uses an asynchronous, snapshot-based persistence architecture. This guarantees that slow database queries or network latency between the server and the MySQL database will *never* cause the game loop to lag (drop ticks).

## 1. The Login Load Flow
When a player connects, their data must be loaded *before* they enter the game world.
1.  `LoginProcessorHandler` (on a Netty thread) calls `AccountPersistenceService.submitLoginLoad()`.
2.  This requests a thread from the HikariCP connection pool.
3.  The database thread executes `SELECT` statements across `GAME_PLAYER_ACCOUNTS`, `GAME_PLAYER_ITEMS`, `GAME_PLAYER_BANK`, and `GAME_PLAYER_SKILLS`.
4.  Once all data is retrieved, it is mapped onto the `Client` object.
5.  The Netty thread is notified, and the player is passed to the Game Thread for final insertion into the world.

## 2. The Asynchronous Save Flow
Saving is triggered periodically (e.g., every 15 minutes) or when a player logs out. Because writing to 5 different tables takes time, the server cannot do this on the Game Thread.

### Step 1: Snapshotting (Game Thread)
You cannot read from the `Client` object while a background thread is saving it, because the player might drop an item or gain XP while the save is happening, causing data corruption.

To solve this, `PlayerSaveService` creates a `PlayerSaveSnapshot` on the Game Thread. This is an **immutable, deep copy** of the player's critical state (Inventory arrays, Bank arrays, Skills, Location). It takes less than 1 millisecond.

### Step 2: Queueing
The immutable snapshot is wrapped in a `PlayerSaveEnvelope` and sent to the `DbAsyncRepository` worker queue. The Game Thread immediately resumes processing the next player.

### Step 3: SQL Execution (Database Thread)
A HikariCP worker thread picks up the envelope. `PlayerSaveSqlRepository` reads the snapshot and generates a batch of `UPDATE` and `INSERT` statements. These are executed against the MySQL database.

## 3. Disconnection & Cleanup
If a player logs out or their TCP connection drops:
1.  They are flagged for logout.
2.  The Game Thread generates a final `PlayerSaveSnapshot` and sends it to the queue.
3.  The player is **not** immediately removed from the `PlayerRegistry`. Their world slot remains occupied, and they cannot log back in yet.
4.  Only when the database thread confirms that the final save was successful is the player fully removed from the registry, freeing up the slot. This prevents item duplication (where a player drops an item, logs out, logs back in before the save finishes, and still has the item).