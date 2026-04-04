package net.dodian.uber.game.systems.skills

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.interaction.ObjectInteractionPolicy
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
        val binding = SkillPluginRegistry.current().objectBinding(option, objectId) ?: return false
        return try {
            val handled = binding.handler(client, objectId, position, obj)
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.OBJECT,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        } catch (exception: Exception) {
            logger.error(
                "Error handling skill object click option={} objectId={} at {}",
                option,
                objectId,
                position,
                exception,
            )
            SkillPolicyMetrics.record(binding.preset, SkillPolicyRoute.OBJECT, SkillPolicyResult.POLICY_REJECT)
            false
        }
    }

    @JvmStatic
    fun resolveObjectPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ObjectInteractionPolicy? {
        val binding = SkillPluginRegistry.current().objectBinding(option, objectId) ?: return null
        return try {
            binding.objectPolicy()
        } catch (exception: Exception) {
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
        val binding = SkillPluginRegistry.current().npcBinding(option, npc.id) ?: return false
        return try {
            val handled = binding.handler(client, npc)
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.NPC,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        } catch (exception: Exception) {
            logger.error(
                "Error handling skill npc click option={} npcId={}",
                option,
                npc.id,
                exception,
            )
            SkillPolicyMetrics.record(binding.preset, SkillPolicyRoute.NPC, SkillPolicyResult.POLICY_REJECT)
            false
        }
    }

    @JvmStatic
    fun tryHandleItemOnItem(client: Client, itemUsed: Int, otherItem: Int): Boolean {
        val binding = SkillPluginRegistry.current().itemOnItemBinding(itemUsed, otherItem) ?: return false
        return try {
            val handled = binding.handler(client, itemUsed, otherItem)
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.ITEM_ON_ITEM,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        } catch (exception: Exception) {
            logger.error(
                "Error handling skill item-on-item itemUsed={} otherItem={}",
                itemUsed,
                otherItem,
                exception,
            )
            SkillPolicyMetrics.record(binding.preset, SkillPolicyRoute.ITEM_ON_ITEM, SkillPolicyResult.POLICY_REJECT)
            false
        }
    }

    @JvmStatic
    fun tryHandleButton(client: Client, rawButtonId: Int, opIndex: Int): Boolean {
        val binding = SkillPluginRegistry.current().buttonBinding(rawButtonId, opIndex) ?: return false
        return try {
            val handled = binding.handler(client, rawButtonId, opIndex)
            SkillPolicyMetrics.record(
                binding.preset,
                SkillPolicyRoute.BUTTON,
                if (handled) SkillPolicyResult.HANDLED else SkillPolicyResult.POLICY_REJECT,
            )
            handled
        } catch (exception: Exception) {
            logger.error(
                "Error handling skill button rawButtonId={} opIndex={}",
                rawButtonId,
                opIndex,
                exception,
            )
            SkillPolicyMetrics.record(binding.preset, SkillPolicyRoute.BUTTON, SkillPolicyResult.POLICY_REJECT)
            false
        }
    }
}
