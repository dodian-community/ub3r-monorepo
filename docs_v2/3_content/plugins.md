# Content Plugins & Dispatch

## Overview
Instead of writing massive `if-else` or `switch` statements to handle every object click in the game, the Ub3r server uses a decoupled "Plugin & Dispatch" pattern.

## The Dispatcher Pattern
When a player clicks an object, the server doesn't immediately execute the logic for that specific object. Instead, it follows a generic pipeline:

1.  **Packet Listener**: Receives the opcode (e.g., `ObjectInteractionListener`).
2.  **Intent Creation**: Converts the raw packet data into an `ObjectClickIntent`.
3.  **Task Scheduling**: Wraps the intent in an `InteractionQueueTask` and schedules it. This handles the pathfinding and distance checking.
4.  **Processor**: Once the player is adjacent to the object, `InteractionProcessor.processObjectClick()` is called.
5.  **Resolution**: The processor asks the `ObjectContentRegistry`: "Do you have a module registered for Object ID X at Position Y?"
6.  **Execution**: If a module is found, its `click()` method is executed.

## Types of Plugins
The server supports various content plugins, all discovered at compile-time by the KSP processor:

-   `ObjectContent`: Handles clicks on static or dynamic world objects (e.g., Bank Booths, Ladders).
-   `NpcModule`: Handles clicks on NPCs (e.g., Bankers, Quest Guides).
-   `ItemContent`: Handles clicks on items in the inventory (e.g., Food, Potions).
-   `CommandContent`: Handles player and admin commands typed in the chat.
-   `SkillPlugin`: A specialized DSL for defining complex skill behaviors (see `skills.md`).
-   `ShopPlugin`: Handles opening and interacting with specific shops.

## Creating a Basic Plugin

To create a new piece of content, simply create an `object` that implements the relevant interface and register it (which is handled automatically by KSP).

### Example: A Custom Object
```kotlin
package net.dodian.uber.game.content.objects

import net.dodian.uber.game.systems.dispatch.objects.ObjectContent
import net.dodian.uber.game.model.entity.player.Client

object MyCustomAltar : ObjectContent {
    override val objectIds = intArrayOf(409) // The Altar ID

    override fun click(client: Client, option: Int, objectId: Int, position: Position): Boolean {
        if (option == 1) {
            client.sendMessage("You pray at the altar.")
            client.animate(645)
            // Restore prayer logic here...
            return true // Indicate we handled the click
        }
        return false // We didn't handle this option
    }
}
```
**No other code changes are required!** Simply compile the server, and the KSP processor will add `MyCustomAltar` to the `GeneratedPluginModuleIndex`.

## Fallback Mechanisms
What happens if `ObjectContentRegistry` doesn't find a registered module?
The `InteractionProcessor` will often fall back to legacy methods in `Client.java` (like `objectClick1()`). As the server continues to modernize, the goal is to migrate all logic out of `Client.java` and into dedicated Kotlin plugins.