package net.dodian.uber.game.engine.systems.skills

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.api.content.ContentErrorPolicy
import net.dodian.uber.game.engine.systems.interaction.ObjectInteractionPolicy
import net.dodian.uber.game.api.plugin.PluginRegistry
import net.dodian.uber.game.api.plugin.skills.objectPolicy
import net.dodian.uber.game.api.plugin.skills.SkillButtonInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemOnItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemOnObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemGridInteraction
import net.dodian.uber.game.api.plugin.skills.SkillMagicOnObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef
import net.dodian.uber.game.api.plugin.skills.SkillNpcRef
import net.dodian.uber.game.engine.config.gameWorldId
import net.dodian.uber.game.engine.loop.GameThreadContext
import org.slf4j.LoggerFactory

object SkillInteractionDispatcher {
    private val logger = LoggerFactory.getLogger(SkillInteractionDispatcher::class.java)

    @JvmStatic
    fun tryHandleObjectClick(
        client: Client,
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): Boolean {
        validateThread("object")
        val binding = PluginRegistry.currentSkills().objectBinding(option, objectId) ?: run {
            if (gameWorldId == 2) logger.debug("[W2-DISPATCH] SkillDispatch no binding option={} objectId={}", option, objectId)
            return false
        }
        return ContentErrorPolicy.runBoolean(client, "skill.object.click", bindingKey = "skill.object:$option:$objectId") {
            val handled = binding.handler(SkillObjectInteraction(client.asSkillPlayer(), option, objectRef(objectId, position, obj)))
            if (gameWorldId == 2) logger.debug("[W2-DISPATCH] SkillDispatch handled={} option={} objectId={}", handled, option, objectId)
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.OBJECT,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        }
    }

    @JvmStatic
    fun resolveObjectPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ObjectInteractionPolicy? {
        val binding = PluginRegistry.currentSkills().objectBinding(option, objectId) ?: return null
        return try {
            binding.objectPolicy()
        } catch (exception: RuntimeException) {
            logger.error(
                "Error resolving skill object policy option={} objectId={} at {}",
                option,
                objectId,
                position,
                exception,
            )
            null
        }
    }

    @JvmStatic
    fun tryHandleNpcClick(client: Client, option: Int, npc: Npc): Boolean {
        validateThread("npc")
        val binding = PluginRegistry.currentSkills().npcBinding(option, npc.id) ?: return false
        return ContentErrorPolicy.runBoolean(client, "skill.npc.click", bindingKey = "skill.npc:$option:${npc.id}") {
            val handled = binding.handler(SkillNpcInteraction(client.asSkillPlayer(), option, SkillNpcRef(npc.id, npc.slot, npc.position.toSkillPosition())))
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.NPC,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        }
    }

    @JvmStatic
    fun tryHandleItemOnItem(client: Client, itemUsed: Int, otherItem: Int): Boolean {
        validateThread("item-on-item")
        val binding = PluginRegistry.currentSkills().itemOnItemBinding(itemUsed, otherItem) ?: return false
        return ContentErrorPolicy.runBoolean(client, "skill.item.on.item", bindingKey = "skill.item-on-item:${minOf(itemUsed, otherItem)}:${maxOf(itemUsed, otherItem)}") {
            val handled = binding.handler(SkillItemOnItemInteraction(client.asSkillPlayer(), itemUsed, otherItem))
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.ITEM_ON_ITEM,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        }
    }

    @JvmStatic
    fun tryHandleItemClick(client: Client, option: Int, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        validateThread("item")
        val binding = PluginRegistry.currentSkills().itemBinding(option, itemId)
            ?: PluginRegistry.currentSkillSupport().itemBinding(option, itemId)
            ?: return false
        return ContentErrorPolicy.runBoolean(client, "skill.item.click", bindingKey = "skill.item:$option:$itemId") {
            val handled = binding.handler(SkillItemInteraction(client.asSkillPlayer(), option, itemId, itemSlot, interfaceId))
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.ITEM,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        }
    }

    @JvmStatic
    fun tryHandleItemOnObject(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean {
        validateThread("item-on-object")
        val binding = PluginRegistry.currentSkills().itemOnObjectBinding(objectId, itemId) ?: return false
        return ContentErrorPolicy.runBoolean(client, "skill.item.on.object", bindingKey = "skill.item-on-object:$objectId:$itemId") {
            val handled = binding.handler(SkillItemOnObjectInteraction(client.asSkillPlayer(), objectRef(objectId, position, obj), itemId, itemSlot, interfaceId))
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.ITEM_ON_OBJECT,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        }
    }

    @JvmStatic
    fun tryHandleMagicOnObject(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        spellId: Int,
    ): Boolean {
        validateThread("magic-on-object")
        val binding = PluginRegistry.currentSkills().magicOnObjectBinding(objectId, spellId) ?: return false
        return ContentErrorPolicy.runBoolean(client, "skill.magic.on.object", bindingKey = "skill.magic-on-object:$objectId:$spellId") {
            val handled = binding.handler(SkillMagicOnObjectInteraction(client.asSkillPlayer(), objectRef(objectId, position, obj), spellId))
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.MAGIC_ON_OBJECT,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        }
    }

    private fun objectRef(id: Int, position: Position, definition: GameObjectData?) = SkillObjectRef(
        id = id,
        position = position.toSkillPosition(),
        sizeX = definition?.sizeX ?: 1,
        sizeY = definition?.sizeY ?: 1,
    )

    @JvmStatic
    fun resolveItemOnObjectPolicy(
        objectId: Int,
        itemId: Int,
    ): ObjectInteractionPolicy? {
        val binding = PluginRegistry.currentSkills().itemOnObjectBinding(objectId, itemId) ?: return null
        return try {
            binding.objectPolicy()
        } catch (exception: RuntimeException) {
            logger.error(
                "Error resolving skill item-on-object policy objectId={} itemId={}",
                objectId,
                itemId,
                exception,
            )
            null
        }
    }

    @JvmStatic
    fun resolveMagicOnObjectPolicy(
        objectId: Int,
        spellId: Int,
    ): ObjectInteractionPolicy? {
        val binding = PluginRegistry.currentSkills().magicOnObjectBinding(objectId, spellId) ?: return null
        return try {
            binding.objectPolicy()
        } catch (exception: RuntimeException) {
            logger.error(
                "Error resolving skill magic-on-object policy objectId={} spellId={}",
                objectId,
                spellId,
                exception,
            )
            null
        }
    }

    @JvmStatic
    fun tryHandleButton(client: Client, rawButtonId: Int, opIndex: Int): Boolean {
        validateThread("button")
        val binding = PluginRegistry.currentSkills().buttonBinding(rawButtonId, opIndex, client.activeInterfaceId)
            ?: PluginRegistry.currentSkillSupport().buttonBinding(rawButtonId, opIndex, client.activeInterfaceId)
            ?: return false
        return ContentErrorPolicy.runBoolean(client, "skill.button.click", bindingKey = "skill.button:$rawButtonId:$opIndex:${client.activeInterfaceId}") {
            val handled = binding.handler(SkillButtonInteraction(client.asSkillPlayer(), rawButtonId, opIndex, client.activeInterfaceId))
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.BUTTON,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        }
    }

    @JvmStatic
    fun tryHandleItemGrid(client: Client, interfaceId: Int, itemId: Int, slot: Int, amount: Int): Boolean {
        validateThread("item-grid")
        val binding = PluginRegistry.currentSkills().itemGridBinding(interfaceId)
            ?: PluginRegistry.currentSkillSupport().itemGridBinding(interfaceId)
            ?: return false
        return ContentErrorPolicy.runBoolean(client, "skill.item.grid", bindingKey = "skill.item-grid:$interfaceId") {
            binding.handler(SkillItemGridInteraction(client.asSkillPlayer(), interfaceId, itemId, slot, amount))
        }
    }

    private fun validateThread(route: String) {
        GameThreadContext.validateGameThread("content.skill.$route")
    }
}
