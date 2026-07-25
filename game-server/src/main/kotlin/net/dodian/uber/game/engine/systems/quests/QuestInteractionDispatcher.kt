package net.dodian.uber.game.engine.systems.quests

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.api.content.ContentErrorPolicy
import net.dodian.uber.game.api.plugin.PluginRegistry
import net.dodian.uber.game.api.plugin.quests.QuestButtonInteraction
import net.dodian.uber.game.api.plugin.quests.QuestItemInteraction
import net.dodian.uber.game.api.plugin.quests.QuestItemOnItemInteraction
import net.dodian.uber.game.api.plugin.quests.QuestItemOnObjectInteraction
import net.dodian.uber.game.api.plugin.quests.QuestMagicOnObjectInteraction
import net.dodian.uber.game.api.plugin.quests.QuestNpcInteraction
import net.dodian.uber.game.api.plugin.quests.QuestObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillNpcRef
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef
import net.dodian.uber.game.engine.loop.GameThreadContext
import net.dodian.uber.game.engine.systems.skills.toSkillPosition

/** Packet -> quest-binding dispatch. Structurally mirrors `SkillInteractionDispatcher`, minus the object/policy resolution skills need for gathering-node walk-up behavior. */
object QuestInteractionDispatcher {
    @JvmStatic
    fun tryHandleObjectClick(client: Client, option: Int, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        validateThread("object")
        val binding = PluginRegistry.currentQuests().objectBinding(option, objectId) ?: return false
        return ContentErrorPolicy.runBoolean(client, "quest.object.click", bindingKey = "quest.object:$option:$objectId") {
            binding.handler(QuestObjectInteraction(client.asQuestPlayer(), option, objectRef(objectId, position, obj)))
        }
    }

    @JvmStatic
    fun hasObjectBinding(option: Int, objectId: Int): Boolean = PluginRegistry.currentQuests().objectBinding(option, objectId) != null

    @JvmStatic
    fun tryHandleNpcClick(client: Client, option: Int, npc: Npc): Boolean {
        validateThread("npc")
        val binding = PluginRegistry.currentQuests().npcBinding(option, npc.id) ?: return false
        return ContentErrorPolicy.runBoolean(client, "quest.npc.click", bindingKey = "quest.npc:$option:${npc.id}") {
            binding.handler(QuestNpcInteraction(client.asQuestPlayer(), option, SkillNpcRef(npc.id, npc.slot, npc.position.toSkillPosition())))
        }
    }

    @JvmStatic
    fun tryHandleItemOnItem(client: Client, itemUsed: Int, otherItem: Int): Boolean {
        validateThread("item-on-item")
        val binding = PluginRegistry.currentQuests().itemOnItemBinding(itemUsed, otherItem) ?: return false
        return ContentErrorPolicy.runBoolean(client, "quest.item.on.item", bindingKey = "quest.item-on-item:${minOf(itemUsed, otherItem)}:${maxOf(itemUsed, otherItem)}") {
            binding.handler(QuestItemOnItemInteraction(client.asQuestPlayer(), itemUsed, otherItem))
        }
    }

    @JvmStatic
    fun tryHandleItemClick(client: Client, option: Int, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        validateThread("item")
        val binding = PluginRegistry.currentQuests().itemBinding(option, itemId) ?: return false
        return ContentErrorPolicy.runBoolean(client, "quest.item.click", bindingKey = "quest.item:$option:$itemId") {
            binding.handler(QuestItemInteraction(client.asQuestPlayer(), option, itemId, itemSlot, interfaceId))
        }
    }

    @JvmStatic
    fun tryHandleItemOnObject(client: Client, objectId: Int, position: Position, obj: GameObjectData?, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        validateThread("item-on-object")
        val binding = PluginRegistry.currentQuests().itemOnObjectBinding(objectId, itemId) ?: return false
        return ContentErrorPolicy.runBoolean(client, "quest.item.on.object", bindingKey = "quest.item-on-object:$objectId:$itemId") {
            binding.handler(QuestItemOnObjectInteraction(client.asQuestPlayer(), objectRef(objectId, position, obj), itemId, itemSlot, interfaceId))
        }
    }

    @JvmStatic
    fun hasItemOnObjectBinding(objectId: Int, itemId: Int): Boolean = PluginRegistry.currentQuests().itemOnObjectBinding(objectId, itemId) != null

    @JvmStatic
    fun tryHandleMagicOnObject(client: Client, objectId: Int, position: Position, obj: GameObjectData?, spellId: Int): Boolean {
        validateThread("magic-on-object")
        val binding = PluginRegistry.currentQuests().magicOnObjectBinding(objectId, spellId) ?: return false
        return ContentErrorPolicy.runBoolean(client, "quest.magic.on.object", bindingKey = "quest.magic-on-object:$objectId:$spellId") {
            binding.handler(QuestMagicOnObjectInteraction(client.asQuestPlayer(), objectRef(objectId, position, obj), spellId))
        }
    }

    @JvmStatic
    fun hasMagicOnObjectBinding(objectId: Int, spellId: Int): Boolean = PluginRegistry.currentQuests().magicOnObjectBinding(objectId, spellId) != null

    @JvmStatic
    fun tryHandleButton(client: Client, rawButtonId: Int, opIndex: Int): Boolean {
        validateThread("button")
        val binding = PluginRegistry.currentQuests().buttonBinding(rawButtonId, opIndex, client.activeInterfaceId) ?: return false
        return ContentErrorPolicy.runBoolean(client, "quest.button.click", bindingKey = "quest.button:$rawButtonId:$opIndex:${client.activeInterfaceId}") {
            binding.handler(QuestButtonInteraction(client.asQuestPlayer(), rawButtonId, opIndex, client.activeInterfaceId))
        }
    }

    private fun objectRef(id: Int, position: Position, definition: GameObjectData?) = SkillObjectRef(
        id = id,
        position = position.toSkillPosition(),
        sizeX = definition?.sizeX ?: 1,
        sizeY = definition?.sizeY ?: 1,
    )

    private fun validateThread(route: String) {
        GameThreadContext.validateGameThread("content.quest.$route")
    }
}
