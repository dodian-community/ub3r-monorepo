# Skill Architecture

## Overview
The Skilling system uses a specialized DSL (Domain Specific Language) that builds upon the core Plugin architecture. It allows developers to define everything about a skill—from the object clicks that start it, to the coroutines that run it, to the experience awarded at the end—in a highly readable format.

## Anatomy of a `SkillPlugin`
A Skill Plugin is declared using the `skillPlugin` builder function.

### Example: Thieving
```kotlin
val ThievingPlugin = skillPlugin("Thieving", Skill.THIEVING) {
    
    // 1. Bind an interaction (Clicking an NPC)
    npcClick(PolicyPreset.SKILLING, option = 2, 1, 2, 3) { client, npc -> // IDs 1, 2, 3 (Man/Woman)
        
        // 2. Requirement Checks
        if (!client.hasLevel(Skill.THIEVING, 1)) {
            client.sendMessage("You need a Thieving level of 1 to pickpocket this.")
            return@npcClick true
        }

        // 3. Start the action (Coroutine)
        client.startTask(PickpocketAction(client, npc))
        true
    }

    // You can bind multiple interactions in one plugin!
    objectClick(PolicyPreset.SKILLING, option = 1, 11730) { client, objId, pos, obj -> // Stall
        client.startTask(StealFromStallAction(client, objId, pos))
        true
    }
}
```

## The Progression Service
You should **never** manually add experience to `player.playerXP` or check for level-ups yourself. Always use the `SkillProgressionService`.

```kotlin
// Awards 50 base experience (this will be multiplied by server XP rates)
SkillProgressionService.awardExperience(client, Skill.THIEVING, 50.0)
```
The `ProgressionService` handles:
- Applying the global server XP multiplier (e.g., 10x).
- Applying double XP weekend bonuses.
- Checking if the new total XP crosses a level boundary.
- Triggering the Level-Up fireworks (`graphics 199`).
- Opening the Level-Up dialogue interface.
- Broadcasting the `PlayerLevelUpEvent` to the Event Bus.

## Coroutine Actions (`GameTask`)
The actual "work" of skilling (the swinging of the pickaxe, the waiting, the success calculation) is handled by a `GameTask` (see `task_system.md`).

### Common Pattern
1.  **Validate**: Check levels, tools, and inventory space.
2.  **Animate**: Send the skilling animation (e.g., `client.animate(624)`).
3.  **Wait**: Pause the coroutine for the duration of the action (`wait(3)`).
4.  **Calculate**: Determine success based on player level vs. target requirement.
5.  **Reward**: Give items and call `awardExperience()`.
6.  **Repeat**: If it's a continuous action (like Woodcutting), loop back to step 1.

## Skill Interfaces
Level-up interfaces and skill guides are managed via the `domain.interface` packages (or legacy `Client.java` methods if not yet migrated). When creating a new skill, ensure the corresponding UI components (like the skill tab tooltips) are mapped correctly.