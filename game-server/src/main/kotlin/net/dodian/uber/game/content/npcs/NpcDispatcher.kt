package net.dodian.uber.game.content.npcs

import net.dodian.uber.game.Server
import net.dodian.uber.game.combat.getAttackStyle
import net.dodian.uber.game.event.Event
import net.dodian.uber.game.event.EventManager
import net.dodian.uber.game.model.WalkToTask
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

object NpcDispatcher {
    private val logger = LoggerFactory.getLogger(NpcDispatcher::class.java)

    @JvmStatic
    fun tryHandle(client: Client, option: Int, npc: Npc, npcIndex: Int): Boolean {
        val content = NpcContentRegistry.get(npc.id) ?: return false
        return try {
            when (option) {
                1 -> content.onFirstClick(client, npc, npcIndex)
                2 -> content.onSecondClick(client, npc, npcIndex)
                3 -> content.onThirdClick(client, npc, npcIndex)
                4 -> content.onFourthClick(client, npc, npcIndex)
                5 -> content.onFifthClick(client, npc, npcIndex)
                else -> false
            }
        } catch (e: Exception) {
            logger.error("Error handling npc click (option={}, npcId={}) via {}", option, npc.id, content::class.java.name, e)
            false
        }
    }

    @JvmStatic
    fun handleAttack(client: Client, npcIndex: Int) {
        logger.debug("NpcDispatcher: attack npcIndex {}", npcIndex)

        if (client.magicId >= 0) client.magicId = -1
        if (client.deathStage >= 1) return

        val npc = Server.npcManager.getNpc(npcIndex) ?: return
        if (client.randomed || client.UsingAgility) return

        val content = NpcContentRegistry.get(npc.id)
        if (content != null) {
            val handled = try {
                content.onAttack(client, npc, npcIndex)
            } catch (e: Exception) {
                logger.error("Error handling npc attack (npcId={}) via {}", npc.id, content::class.java.name, e)
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

