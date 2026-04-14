# Content Runtime Playbook

Use this as the copy/paste guide for new content modules.

## One import surface

For day-to-day content code, start with:

```kotlin
import net.dodian.uber.game.api.content.ContentPredef.*
```

This gives you event hooks, scheduling helpers, and common player actions from one place.

## Event listener example

```kotlin
on(MyContentEvent::class.java) { event ->
    message(event.player, "Event handled")
    true
}
```

## Player/world coroutine task examples

```kotlin
player(player) {
    delayTicks(1)
    repeatEvery(intervalTicks = 2) {
        message(player, "tick")
        !player.disconnected
    }
}
```

```kotlin
worldCountdown(totalTicks = 5, onTick = { remaining ->
    println("remaining=$remaining")
}) {
    println("done")
}
```

## Dialogue flow example (DialogueFactory)

```kotlin
dialogue(player) {
    npcChat(npc.id, 588, "Hello adventurer.")
    options(
        title = "Select an Option",
        DialogueOption("Open bank") {
            action { openBank(it) }
            finish()
        },
        DialogueOption("Nevermind") {
            finish()
        },
    )
}
```

For NPC-heavy scripted flows, prefer the sequential DSL in `NpcDialogueDsl`.

## Shop/bank/teleport action examples

```kotlin
openShop(player, shopId = 12)
openBank(player)
teleport(player, x = 3222, y = 3218, z = 0)
message(player, "Welcome!")
```

## Rules for new modules

- Keep plugin ownership strict (`SkillPlugin`/`NpcPlugin` route declarations).
- Prefer `ContentPredef` + `ContentActions` + `ContentScheduling` APIs over direct runtime internals.
- Route new dialogue via `NpcDialogueDsl`/`DialogueFactory`; avoid direct `showNPCChat`/`showPlayerChat` in new code.

