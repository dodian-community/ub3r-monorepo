package net.dodian.uber.game.content.npcs.attack

import net.dodian.uber.game.Server
import net.dodian.uber.game.combat.getAttackStyle
import net.dodian.uber.game.event.Event
import net.dodian.uber.game.event.EventManager
import net.dodian.uber.game.model.WalkToTask
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory


object NpcAttackDispatcher {
    private val logger = LoggerFactory.getLogger(NpcAttackDispatcher::class.java)

    @JvmStatic
    fun handle(client: Client, npcIndex: Int) {
        logger.debug("NpcAttackDispatcher: npcIndex {}", npcIndex)

        if (client.magicId >= 0) client.magicId = -1
        if (client.deathStage >= 1) return

        val npc = Server.npcManager.getNpc(npcIndex) ?: return
        if (client.randomed || client.UsingAgility) return

        val content = NpcAttackContentRegistry.get(npc.id)
        if (content != null) {
            val handled = try {
                content.onAttack(client, npc, npcIndex)
            } catch (e: Exception) {
                logger.error("Error in onAttack for npcId={} via {}", npc.id, content::class.java.name, e)
                false
            }
            if (handled) return
        }

        attackLikeLegacy(client, npc, npcIndex)
    }

    @JvmStatic
    fun attackLikeLegacy(client: Client, npc: Npc, npcIndex: Int) {
        val rangedAttack = client.getAttackStyle() != 0
        if ((rangedAttack && client.goodDistanceEntity(npc, 5)) || client.goodDistanceEntity(npc, 1)) {
            client.resetWalkingQueue()
            client.startAttack(npc)
            return
        }

        if ((rangedAttack && !client.goodDistanceEntity(npc, 5)) || (!rangedAttack && !client.goodDistanceEntity(npc, 1))) {
            val task = WalkToTask(WalkToTask.Action.ATTACK_NPC, npcIndex, npc.position)
            client.setWalkToTask(task)
            EventManager.getInstance().registerEvent(object : Event(600) {
                override fun execute() {
                    if (client.disconnected || client.walkToTask != task) {
                        stop()
                        return
                    }
                    if ((client.getAttackStyle() != 0 && client.goodDistanceEntity(npc, 5)) || client.goodDistanceEntity(npc, 1)) {
                        client.resetWalkingQueue()
                        client.startAttack(npc)
                        client.setWalkToTask(null)
                        stop()
                    }
                }
            })
        }
    }
}
