package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

object NpcContentDispatcher {
    private val logger = LoggerFactory.getLogger(NpcContentDispatcher::class.java)

    @JvmStatic
    fun tryHandleClick(client: Client, option: Int, npc: Npc): Boolean {
        val content = NpcContentRegistry.get(npc.id) ?: return false
        return try {
            when (option) {
                1 -> content.onFirstClick(client, npc)
                2 -> content.onSecondClick(client, npc)
                3 -> content.onThirdClick(client, npc)
                4 -> content.onFourthClick(client, npc)
                else -> false
            }
        } catch (e: Exception) {
            logger.error(
                "Error handling npc click (option={}, npcId={}) via {}",
                option,
                npc.id,
                content.name,
                e,
            )
            false
        }
    }

    @JvmStatic
    fun tryHandleAttack(client: Client, npc: Npc): Boolean {
        val content = NpcContentRegistry.get(npc.id) ?: return false
        return try {
            content.onAttack(client, npc)
        } catch (e: Exception) {
            logger.error(
                "Error handling npc attack (npcId={}) via {}",
                npc.id,
                content.name,
                e,
            )
            false
        }
    }
}
