# Game Event Bus

## Overview
The `GameEventBus` provides a decoupled, publish-subscribe architecture for the server. Instead of the Woodcutting system needing to explicitly call the Achievements system, the Quest system, and the Highscores system whenever a log is chopped, it simply publishes an event.

## 1. Defining Events
All events implement the marker interface `GameEvent`. They are typically defined as immutable data classes.
```kotlin
// Example Event
data class PlayerDeathEvent(val victim: Client, val killer: Entity?) : GameEvent
```

## 2. Publishing Events
To notify the server that something happened, use `GameEventBus.post()`.
```kotlin
// Inside PlayerDeathTickService.kt
GameEventBus.post(PlayerDeathEvent(player, killer))
```

## 3. Subscribing to Events (Listeners)
Any class can listen to an event. Listeners are usually registered during startup in a `ContentBootstrap` or `EventBootstrap` object (which is picked up by the KSP processor).

```kotlin
// Example Listener
GameEventBus.on<PlayerDeathEvent> { event ->
    if (event.killer is Client) {
        event.killer.sendMessage("You have defeated ${event.victim.playerName}!")
    }
    true // Return true to indicate the event was "handled"
}
```

### Conditional Listeners
You can pass a condition block to avoid running the main action if it's not relevant.
```kotlin
GameEventBus.on<PlayerLevelUpEvent>(
    condition = { event -> event.level == 99 }
) { event ->
    Server.yell("${event.player.playerName} just reached 99 in ${event.skill.name}!")
    true
}
```

## 4. Returnable Events
Sometimes, a system needs to ask other systems for data, rather than just notifying them. For this, `GameEventBus.postAndReturn` is used.

**Example: Damage Modifiers**
When calculating a melee hit, the combat system asks: "Does anyone have a modifier for this damage?"
```kotlin
val event = CombatDamageModifierEvent(attacker, defender, baseDamage)
// Returns a List<Float> of all modifiers applied by listeners
val modifiers = GameEventBus.postAndReturn<CombatDamageModifierEvent, Float>(event)

var finalDamage = baseDamage
for (mod in modifiers) {
    finalDamage *= mod
}
```
A listener (like the Salve Amulet plugin) might respond:
```kotlin
GameEventBus.onReturnable<CombatDamageModifierEvent, Float>(
    condition = { it.attacker.hasAmulet(SALVE) && it.defender.isUndead() }
) { event ->
    return@onReturnable 1.15f // +15% damage
}
```

## 5. Event Filters
Filters allow you to globally block an event from reaching its listeners based on a condition.
```kotlin
GameEventBus.addFilter<ChatEvent> { event -> 
    !event.player.isMuted // If false, the event is swallowed and not broadcasted
}
```