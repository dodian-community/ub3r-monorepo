package net.dodian.uber.game.systems.skills

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.api.content.ContentObjectInteractionPolicy
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
            binding.handler(client, objectId, position, obj)
        } catch (exception: Exception) {
            logger.error(
                "Error handling skill object click option={} objectId={} at {}",
                option,
                objectId,
                position,
                exception,
            )
            false
        }
    }

    @JvmStatic
    fun resolveObjectPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ContentObjectInteractionPolicy? {
        val binding = SkillPluginRegistry.current().objectBinding(option, objectId) ?: return null
        return try {
            binding.policyResolver?.invoke(option, objectId, position, obj)
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
            binding.handler(client, npc)
        } catch (exception: Exception) {
            logger.error(
                "Error handling skill npc click option={} npcId={}",
                option,
                npc.id,
                exception,
            )
            false
        }
    }

    @JvmStatic
    fun tryHandleItemOnItem(client: Client, itemUsed: Int, otherItem: Int): Boolean {
        val binding = SkillPluginRegistry.current().itemOnItemBinding(itemUsed, otherItem) ?: return false
        return try {
            binding.handler(client, itemUsed, otherItem)
        } catch (exception: Exception) {
            logger.error(
                "Error handling skill item-on-item itemUsed={} otherItem={}",
                itemUsed,
                otherItem,
                exception,
            )
            false
        }
    }

    @JvmStatic
    fun tryHandleButton(client: Client, rawButtonId: Int, opIndex: Int): Boolean {
        val binding = SkillPluginRegistry.current().buttonBinding(rawButtonId, opIndex) ?: return false
        return try {
            binding.handler(client, rawButtonId, opIndex)
        } catch (exception: Exception) {
            logger.error(
                "Error handling skill button rawButtonId={} opIndex={}",
                rawButtonId,
                opIndex,
                exception,
            )
            false
        }
    }
}
