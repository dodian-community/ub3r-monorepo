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
import net.dodian.uber.game.api.plugin.skills.SkillItemOnNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillMagicOnItemInteraction
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
        return executeSkillRoute(client, "skill.object.click", "skill.object:$option:$objectId", SkillPolicyRoute.OBJECT) {
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
        return executeSkillRoute(client, "skill.npc.click", "skill.npc:$option:${npc.id}", SkillPolicyRoute.NPC) {
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
        return executeSkillRoute(client, "skill.item.on.item", "skill.item-on-item:${minOf(itemUsed, otherItem)}:${maxOf(itemUsed, otherItem)}", SkillPolicyRoute.ITEM_ON_ITEM) {
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
        return executeSkillRoute(client, "skill.item.click", "skill.item:$option:$itemId", SkillPolicyRoute.ITEM) {
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
        return executeSkillRoute(client, "skill.item.on.object", "skill.item-on-object:$objectId:$itemId", SkillPolicyRoute.ITEM_ON_OBJECT) {
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
        return executeSkillRoute(client, "skill.magic.on.object", "skill.magic-on-object:$objectId:$spellId", SkillPolicyRoute.MAGIC_ON_OBJECT) {
            val handled = binding.handler(SkillMagicOnObjectInteraction(client.asSkillPlayer(), objectRef(objectId, position, obj), spellId))
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.MAGIC_ON_OBJECT,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        }
    }

    @JvmStatic
    fun tryHandleItemOnNpc(client: Client, npc: Npc, itemId: Int, itemSlot: Int): Boolean {
        validateThread("item-on-npc")
        val binding = PluginRegistry.currentSkills().itemOnNpcBinding(npc.id, itemId) ?: return false
        return executeSkillRoute(client, "skill.item.on.npc", "skill.item-on-npc:${npc.id}:$itemId", SkillPolicyRoute.ITEM_ON_NPC) {
            val handled = binding.handler(SkillItemOnNpcInteraction(client.asSkillPlayer(), SkillNpcRef(npc.id, npc.slot, npc.position.toSkillPosition()), itemId, itemSlot))
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.ITEM_ON_NPC,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        }
    }

    @JvmStatic
    fun tryHandleMagicOnItem(client: Client, itemId: Int, itemSlot: Int, spellId: Int): Boolean {
        validateThread("magic-on-item")
        val binding = PluginRegistry.currentSkills().magicOnItemBinding(itemId, spellId) ?: return false
        return executeSkillRoute(client, "skill.magic.on.item", "skill.magic-on-item:$itemId:$spellId", SkillPolicyRoute.MAGIC_ON_ITEM) {
            val handled = binding.handler(SkillMagicOnItemInteraction(client.asSkillPlayer(), itemId, itemSlot, spellId))
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.MAGIC_ON_ITEM,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        }
    }

    /** Renders a plugin-owned [net.dodian.uber.game.api.plugin.skills.SkillPlayerOptionMenuBinding]'s
     * N-option menu (or its [guard][net.dodian.uber.game.api.plugin.skills.SkillPlayerOptionMenuBinding.guard]
     * message) when its dialogue id opens - see
     * [net.dodian.uber.game.engine.systems.dialogue.core.DialogueRenderRegistry]. */
    @JvmStatic
    fun tryRenderPlayerOptionMenu(client: Client, dialogueId: Int): Boolean {
        val binding = PluginRegistry.currentSkills().playerOptionMenuBinding(dialogueId) ?: return false
        val guardMessage = binding.guard?.invoke(client.asSkillPlayer())
        if (guardMessage != null) {
            client.sendMessage(guardMessage)
            net.dodian.uber.game.engine.systems.dialogue.DialogueService.setDialogueSent(client, true)
            return true
        }
        net.dodian.uber.game.ui.dialogue.DialogueUi.showPlayerOption(
            client,
            (listOf(binding.title) + binding.options).toTypedArray(),
        )
        net.dodian.uber.game.engine.systems.dialogue.DialogueService.setDialogueSent(client, true)
        return true
    }

    /** Handles the option-button response for a plugin-owned player-option menu - see
     * [net.dodian.uber.game.engine.systems.dialogue.DialogueOptionService]. */
    @JvmStatic
    fun tryHandlePlayerOptionMenu(client: Client, dialogueId: Int, button: Int): Boolean {
        validateThread("player-option-menu")
        val binding = PluginRegistry.currentSkills().playerOptionMenuBinding(dialogueId) ?: return false
        if (button in 1..binding.options.size) {
            executeSkillRoute(client, "skill.player.option.menu", "skill.player-option-menu:$dialogueId", SkillPolicyRoute.PLAYER_OPTION_MENU) {
                binding.handler(client.asSkillPlayer(), button)
                true
            }
        }
        client.send(net.dodian.uber.game.netty.listener.out.RemoveInterfaces())
        return true
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
        return executeSkillRoute(client, "skill.button.click", "skill.button:$rawButtonId:$opIndex:${client.activeInterfaceId}", SkillPolicyRoute.BUTTON) {
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
        return executeSkillRoute(client, "skill.item.grid", "skill.item-grid:$interfaceId", SkillPolicyRoute.ITEM) {
            binding.handler(SkillItemGridInteraction(client.asSkillPlayer(), interfaceId, itemId, slot, amount))
        }
    }

    /** Renders a plugin-owned [SkillConfirmDialogueBinding]'s Yes/No text when its dialogue id
     * opens - see [net.dodian.uber.game.engine.systems.dialogue.core.DialogueRenderRegistry]. */
    @JvmStatic
    fun tryRenderConfirmDialogue(client: Client, dialogueId: Int): Boolean {
        val binding = PluginRegistry.currentSkills().confirmDialogueBinding(dialogueId) ?: return false
        net.dodian.uber.game.ui.dialogue.DialogueUi.showPlayerOption(
            client,
            arrayOf(binding.message, binding.yesLabel, binding.noLabel),
        )
        net.dodian.uber.game.engine.systems.dialogue.DialogueService.setDialogueSent(client, true)
        return true
    }

    /** Handles the Yes/No button response for a plugin-owned confirm dialogue - see
     * [net.dodian.uber.game.engine.systems.dialogue.DialogueOptionService]. */
    @JvmStatic
    fun tryHandleConfirmDialogue(client: Client, dialogueId: Int, button: Int): Boolean {
        validateThread("confirm-dialogue")
        val binding = PluginRegistry.currentSkills().confirmDialogueBinding(dialogueId) ?: return false
        if (button == 1) {
            executeSkillRoute(client, "skill.confirm.dialogue", "skill.confirm-dialogue:$dialogueId", SkillPolicyRoute.CONFIRM_DIALOGUE) {
                binding.handler(client.asSkillPlayer())
                true
            }
        }
        client.send(net.dodian.uber.game.netty.listener.out.RemoveInterfaces())
        return true
    }

    private fun executeSkillRoute(
        client: Client,
        scope: String,
        bindingKey: String,
        route: SkillPolicyRoute,
        action: () -> Boolean,
    ): Boolean {
        val started = System.nanoTime()
        val result = ContentErrorPolicy.runBooleanResult(client, scope, bindingKey = bindingKey, action = action)
        SkillBetaTelemetry.record(client, bindingKey, route, result, System.nanoTime() - started)
        return result.handled
    }

    private fun validateThread(route: String) {
        GameThreadContext.validateGameThread("content.skill.$route")
    }
}
