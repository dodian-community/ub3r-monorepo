# Task Coroutine System (RSPS Quick Reference)

This is the coroutine-style task layer for Dodian. It runs on the same game tick runtime (`GameTaskRuntime`), so there is no separate scheduler to maintain. Think of it like old world-task scripting, but with clean `delay(...)` and `stop()` in Kotlin.

## Core API Map

Main entrypoints:

- `worldTaskCoroutine { ... }`
- `playerTaskCoroutine(player) { ... }`
- `npcTaskCoroutine(npc) { ... }`

Inside task contexts:

- `delay(ticks)`
- `stop()`
- `repeatEvery(intervalTicks, initialDelayTicks = 0) { ... }`
- `gameClock()`

Aliases also exist:

- `runWorldTaskCoroutine(...)`
- `runPlayerTaskCoroutine(...)`
- `runNpcTaskCoroutine(...)`

## API Reference

`fun worldTaskCoroutine(priority: TaskPriority = TaskPriority.STANDARD, block: suspend WorldTaskContext.() -> Unit): TaskHandle`

- Schedules a coroutine on the world task set.
- Use for global tasks not tied to one specific player/NPC.

`fun playerTaskCoroutine(player: Client, priority: TaskPriority = TaskPriority.STANDARD, block: suspend PlayerTaskContext.() -> Unit): TaskHandle`

- Schedules a coroutine on one player’s task set.
- Best for player-local scripted flows.

`fun npcTaskCoroutine(npc: Npc, priority: TaskPriority = TaskPriority.STANDARD, block: suspend NpcTaskContext.() -> Unit): TaskHandle`

- Schedules a coroutine on one NPC’s task set.
- Best for npc-local timed behavior.

`suspend fun delay(ticks: Int)`

- Waits game ticks (`600ms` per tick).
- `delay(0)` is immediate (no suspend).
- Must be `ticks >= 0`.

`suspend fun stop(): Nothing`

- Ends the current coroutine task immediately.
- Code after `stop()` does not run.

`suspend fun repeatEvery(intervalTicks: Int, initialDelayTicks: Int = 0, block: suspend Context.() -> Boolean)`

- Repeats the block every interval while the block returns `true`.
- Stops when block returns `false`.

`fun gameClock(): Long`

- Returns current game cycle/tick counter.

## Examples

### World task sequence

```kotlin
worldTaskCoroutine {
    println("[${gameClock()}] world start")
    delay(3)
    println("[${gameClock()}] world step 2")
    delay(7)
    println("[${gameClock()}] world stop")
    stop()
}
```

### Player task sequence

```kotlin
playerTaskCoroutine(player) {
    player.send(SendMessage("Starting action..."))
    delay(2)
    player.send(SendMessage("Still going..."))
    delay(2)
    stop()
}
```

### NPC task sequence

```kotlin
npcTaskCoroutine(npc) {
    npc.setText("I am waking up...")
    delay(2)
    npc.setText("Now active.")
    delay(2)
    stop()
}
```

### Repeating loop

```kotlin
worldTaskCoroutine {
    var count = 0
    repeatEvery(intervalTicks = 1, initialDelayTicks = 1) {
        println("tick=${gameClock()} count=$count")
        count++
        count < 5
    }
}
```

## Rules 

- Ticks are game ticks (`600ms`), not milliseconds.
- `delay(0)` is valid and immediate.
- `stop()` is a hard stop for that coroutine.
- Do not do blocking DB/file/network I/O inside these task blocks.
- Don’t build a custom `WorldTasksManager`; use this facade over `GameTaskRuntime`.


### 1) Interaction chase loop (like `InteractionTaskScheduler`)

Current pattern is effectively “keep executing every tick until done/cancelled”. Coroutine version:

```kotlin
playerTaskCoroutine(player) {
    while (true) {
        val keepGoing = interaction.execute()
        if (!keepGoing) stop()
        delay(1)
    }
}
```

Use this for click-to-object/NPC chase flows that need per-tick retry until range/path conditions are met.

### 2) Skilling cycle loop (like `GatheringTask`)

Current gathering logic checks requirements, performs one cycle, waits, repeats. Coroutine version:

```kotlin
playerTaskCoroutine(player) {
    while (true) {
        if (!requirementsMet(player, node)) stop()
        performGatheringCycle(player, node)
        delay(cycleDelayTicks)
    }
}
```

Use this for woodcutting/fishing/mining style loops with clear stop conditions.

### 3) Delayed follow-up action (like scheduler `runLater*`)

If you just need “do X, wait N ticks, do Y”:

```kotlin
playerTaskCoroutine(player) {
    startAnimation(player)
    delay(2)
    applyResult(player)
    stop()
}
```

Use this instead of wiring one-off delay callbacks for simple linear flows.

### 4) Tick-based repeating checks (replace manual repeating boilerplate)

```kotlin
worldTaskCoroutine {
    repeatEvery(intervalTicks = 1) {
        processOneWorldStep()
        shouldKeepRunning()
    }
}
```

Use this for short-lived periodic world checks where you want stop logic inline.
