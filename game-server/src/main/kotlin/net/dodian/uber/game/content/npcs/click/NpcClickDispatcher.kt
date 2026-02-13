package net.dodian.uber.game.content.npcs.click

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

object NpcClickDispatcher {
    private val logger = LoggerFactory.getLogger(NpcClickDispatcher::class.java)

    @JvmStatic
    fun tryHandle(client: Client, option: Int, npc: Npc): Boolean {
        val content = NpcClickContentRegistry.get(npc.id) ?: return false
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
                content::class.java.name,
                e
            )
            false
        }
    }
}

