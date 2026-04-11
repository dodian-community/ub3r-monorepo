# Coroutines & Game Tasks (`GameTask`)

## Overview
Most actions in RuneScape take time. Chopping a tree takes multiple swings, crafting an inventory of bows takes several seconds. If the server blocked the main thread for 10 seconds while a player fletched, no one else could play.

To solve this without creating a tangled mess of callbacks and state machines, Ub3r uses **Kotlin Coroutines** wrapped in a `GameTask` architecture.

## How `GameTask` Works
A `GameTask` is a suspendable piece of logic. It executes on the Game Thread, but it can `suspend` (pause) its execution, handing control back to the Game Thread to process other players. On a future tick, the Game Thread will `resume` the task exactly where it left off.

### Example: Fletching Task
```kotlin
class FletchBowsTask(val client: Client, val logId: Int, val amount: Int) : GameTask(client) {
    override suspend fun execute() {
        var remaining = amount
        while (remaining > 0) {
            // Check preconditions every loop!
            if (!client.playerHasItem(logId)) {
                client.sendMessage("You have run out of logs.")
                stop() // Exits the coroutine entirely
            }

            client.animate(1248)
            client.inventory.remove(logId, 1)
            client.inventory.add(839, 1) // Add unstrung bow
            remaining--

            // PAUSE execution for 3 game ticks (1.8 seconds)
            wait(3) 
            
            // 3 ticks later, the code resumes right here
        }
    }
}
```

## Suspension Functions
The `GameTask` provides several methods to control suspension:

*   `wait(ticks: Int)`: Suspends the coroutine for `X` game ticks.
*   `wait(minTicks: Int, maxTicks: Int)`: Suspends for a random number of ticks between min and max (useful for skilling success rates).
*   `waitUntil { condition }`: Suspends execution, waking up every tick to evaluate the condition. If true, execution continues.
*   `waitUntilCycle(targetCycle: Long)`: Suspends until the global `GameCycleClock` reaches the target.

## Task Lifecycle & Safety
1.  **Thread Safety**: Coroutines in this system use a custom dispatcher that *forces* them to resume only on the Game Thread. You never have to worry about `ConcurrentModificationException` when modifying player inventory inside a `GameTask`.
2.  **Cancellation**: Tasks are bound to the `Client`. If the player disconnects, logs out, or walks away (interrupting the action), the `TaskCoroutineFacade` cancels the coroutine automatically. 
3.  **State Verification**: Because time passes during a `wait(3)`, the game world might change. A tree might deplete, or the player might drop their axe. **You must always re-verify state after a `wait()` call**.

## Interaction Scheduling
When a player clicks a tree from 5 tiles away, they don't start chopping instantly.
1.  The `PacketObjectService` creates an `ObjectClickIntent`.
2.  The `InteractionTaskScheduler` wraps this in an `InteractionQueueTask`.
3.  The task uses `waitUntil` to pause until the player's pathfinding brings them within 1 tile of the tree.
4.  Once adjacent, the task resumes and triggers the `WoodcuttingPlugin`.