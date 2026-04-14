package net.dodian.uber.game.api.plugin.skills

import net.dodian.uber.game.item.ItemContent
import net.dodian.uber.game.objects.ObjectContent
import net.dodian.uber.game.engine.systems.action.PolicyPreset

fun SkillPluginBuilder.bindObjectContentClick(
    preset: PolicyPreset,
    option: Int,
    content: ObjectContent,
) {
    objectClick(preset = preset, option = option, *content.objectIds) { client, objectId, position, obj ->
        when (option) {
            1 -> content.onFirstClick(client, objectId, position, obj)
            2 -> content.onSecondClick(client, objectId, position, obj)
            3 -> content.onThirdClick(client, objectId, position, obj)
            4 -> content.onFourthClick(client, objectId, position, obj)
            5 -> content.onFifthClick(client, objectId, position, obj)
            else -> false
        }
    }
}

fun SkillPluginBuilder.bindObjectContentUseItem(
    preset: PolicyPreset,
    content: ObjectContent,
    itemIds: IntArray = intArrayOf(-1),
) {
    itemOnObject(preset = preset, *content.objectIds, itemIds = itemIds) { client, objectId, position, obj, itemId, itemSlot, interfaceId ->
        content.onUseItem(
            client = client,
            objectId = objectId,
            position = position,
            obj = obj,
            itemId = itemId,
            itemSlot = itemSlot,
            interfaceId = interfaceId,
        )
    }
}

fun SkillPluginBuilder.bindObjectContentMagic(
    preset: PolicyPreset,
    content: ObjectContent,
    spellIds: IntArray = intArrayOf(-1),
) {
    magicOnObject(preset, *content.objectIds, spellIds = spellIds) { client, objectId, position, obj, spellId ->
        content.onMagic(
            client = client,
            objectId = objectId,
            position = position,
            obj = obj,
            spellId = spellId,
        )
    }
}

fun SkillPluginBuilder.bindItemContentClick(
    preset: PolicyPreset,
    option: Int,
    content: ItemContent,
) {
    itemClick(preset = preset, option = option, *content.itemIds) { client, itemId, itemSlot, interfaceId ->
        when (option) {
            1 -> content.onFirstClick(client, itemId, itemSlot, interfaceId)
            2 -> content.onSecondClick(client, itemId, itemSlot, interfaceId)
            3 -> content.onThirdClick(client, itemId, itemSlot, interfaceId)
            else -> false
        }
    }
}
