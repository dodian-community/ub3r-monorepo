package net.dodian.uber.game.content.npcs.action1

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

object NpcAction1Dispatcher {
    private val logger = LoggerFactory.getLogger(NpcAction1Dispatcher::class.java)

    @JvmStatic
    fun tryHandle(client: Client, npc: Npc, npcIndex: Int): Boolean {
        val content = NpcAction1Registry.get(npc.id) ?: return false
        return try {
            content.onClick1(client, npc, npcIndex)
        } catch (e: Exception) {
            logger.error("Error handling npc click1 for npcId={} via {}", npc.id, content::class.java.name, e)
            false
        }
    }
}

