# Skill Plugin Template

Use this as the default pattern when adding a new skill or extending an existing one.

## Goals

- one clear entry point per skill: `object <Skill>NameSkillPlugin : SkillPlugin`
- route ownership declared in one place
- orchestration kept in shared runtime systems, not ad-hoc loops inside plugin files
- bridge helpers used when wrapping existing `ObjectContent` / `ItemContent`
- explicit `PolicyPreset` on every route binding
- concise authoring via one content-facing import surface where practical

## Preferred file shape

```kotlin
package net.dodian.uber.game.skill.example

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.item.ItemContent
import net.dodian.uber.game.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.bindItemContentClick
import net.dodian.uber.game.api.plugin.skills.bindObjectContentClick
import net.dodian.uber.game.api.plugin.skills.bindObjectContentMagic
import net.dodian.uber.game.api.plugin.skills.bindObjectContentUseItem
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset

object ExampleSkill {
    @JvmStatic
    fun start(client: Client, request: ExampleRequest): Boolean {
        // domain logic only
        return true
    }
}

private class ExampleObjectContent : ObjectContent {
    override val objectIds: IntArray = intArrayOf(1234)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return ExampleSkill.start(client, ExampleRequest(objectId, position))
    }
}

private class ExampleItemContent : ItemContent {
    override val itemIds: IntArray = intArrayOf(5678)

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        return true
    }
}

object ExampleSkillPlugin : SkillPlugin {
    override val definition =
        skillPlugin(name = "Example", skill = Skill.EXAMPLE) {
            val exampleObjects = ExampleObjectContent()
            val exampleItems = ExampleItemContent()

            bindObjectContentClick(
                preset = PolicyPreset.GATHERING,
                option = 1,
                content = exampleObjects,
            )
            bindObjectContentUseItem(
                preset = PolicyPreset.GATHERING,
                content = exampleObjects,
                itemIds = exampleItems.itemIds,
            )
            bindObjectContentMagic(
                preset = PolicyPreset.GATHERING,
                content = exampleObjects,
                spellIds = intArrayOf(1179),
            )
            bindItemContentClick(
                preset = PolicyPreset.GATHERING,
                option = 1,
                content = exampleItems,
            )
        }
}
```

## Route selection guide

### Single import surface for content helpers

For general content helpers (events/actions/scheduling), prefer:

- `import net.dodian.uber.game.api.content.ContentPredef.*`

This keeps module code concise while preserving strict plugin route ownership.

### Use direct plugin DSL when the skill is already plugin-native

Prefer these when the handler is simple and does not need a compatibility wrapper:

- `objectClick(...)`
- `npcClick(...)`
- `itemOnItem(...)`
- `itemClick(...)`
- `itemOnObject(...)`
- `magicOnObject(...)`
- `button(...)`

### Use bridge helpers when wrapping existing content objects

Prefer these while migrating old skills or when a content object is still shared elsewhere:

- `bindObjectContentClick(...)`
- `bindObjectContentMagic(...)`
- `bindObjectContentUseItem(...)`
- `bindItemContentClick(...)`

## Conventions

### 1. Keep exported surface small and plugin-owned

Expose only:

- the skill domain object (`ExampleSkill`)
- the plugin (`ExampleSkillPlugin`)

Keep wrapper content objects `internal` unless another package truly needs them.

Prefer private wrapper classes instantiated inside `*SkillPlugin` (plugin-owned instances). Avoid singleton `object ... : ObjectContent` wrappers in skill modules.

### 2. Put behavior in the domain object, not the plugin body

Good:

```kotlin
objectClick(preset = PolicyPreset.GATHERING, option = 1, 1234) { client, objectId, position, obj ->
    ExampleSkill.start(client, ExampleRequest(objectId, position))
}
```

Avoid large inline lambdas with lots of orchestration.

### 3. Always declare `preset = PolicyPreset...`

This is required for consistent routing and audit checks.

### 4. Avoid legacy ownership split

Do not introduce new skill behavior that only lives in:

- `ObjectContentRegistry`
- `ItemContentRegistry`
- direct packet listeners
- ad-hoc branches in `InteractionProcessor`

If it is skill-owned, route it through the skill plugin system.

### 4.1 Dialogue routing for new modules

For new content modules, route dialogue through:

- `NpcDialogueDsl` for NPC option flows
- `DialogueFactory`/`DialogueService` for general dialogue chains

Avoid introducing new direct `Client.showNPCChat(...)` / `Client.showPlayerChat(...)` usage.

### 5. Prefer shared runtime actions

Use the runtime helpers already in place for loops/cycles:

- gathering/production queues
- `ContentActions`
- skill runtime action helpers
- progression/random event services

Do not build new `while (true)` loops or ad-hoc player action schedulers inside plugins.

## Current migration rule of thumb

For existing mixed-mode skills:

1. keep legacy wrapper behavior temporarily if needed, but prefer class wrappers over singleton objects
2. register those wrappers through the skill plugin bridge helpers
3. once stable, inline or remove wrappers if they no longer add value

## Magic-on-object guidance

`magicOnObject(...)` is now a first-class skill plugin route.

Use it directly for plugin-native skills, or `bindObjectContentMagic(...)` while migrating existing `ObjectContent` wrappers. Prefer explicit spell ids over wildcard spell ownership unless the whole object is genuinely owned by one skill for all spells.
