package net.dodian.uber.game.content.npcs.action2

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

object NpcAction2Dispatcher {
    private val logger = LoggerFactory.getLogger(NpcAction2Dispatcher::class.java)

    @JvmStatic
    fun tryHandle(client: Client, npc: Npc, npcIndex: Int): Boolean {
        val content = NpcAction2Registry.get(npc.id) ?: return false
        return try {
            content.onClick2(client, npc, npcIndex)
        } catch (e: Exception) {
            logger.error("Error handling npc click2 for npcId={} via {}", npc.id, content::class.java.name, e)
            false
        }
    }
}

