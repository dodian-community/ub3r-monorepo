package net.dodian.uber.game.runtime.interaction

import net.dodian.uber.game.Server
import net.dodian.uber.game.combat.getAttackStyle
import net.dodian.uber.game.content.npcs.spawns.NpcContentDispatcher
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.netty.listener.out.SendMessage

object InteractionProcessor {
    @JvmStatic
    fun process(player: Client) {
        val intent = player.pendingInteraction ?: return
        if (PlayerHandler.cycle < player.interactionEarliestCycle) {
            return
        }
        when (intent) {
            is NpcInteractionIntent -> processNpcInteraction(player, intent)
        }
    }

    private fun processNpcInteraction(player: Client, intent: NpcInteractionIntent) {
        val npc = Server.npcManager.getNpc(intent.npcIndex)
        if (npc == null) {
            clear(player)
            return
        }

        if (player.randomed || player.UsingAgility) {
            clear(player)
            return
        }

        val range =
            if (intent.option == NPC_ATTACK_OPTION && player.getAttackStyle() != 0) {
                5
            } else {
                1
            }

        if (!player.goodDistanceEntity(npc, range) || npc.position.withinDistance(player.position, 0)) {
            return
        }

        player.activeInteraction = ActiveInteraction(intent, player.lastProcessedCycle)
        when (intent.option) {
            1 -> handleNpcClick1(player, npc)
            2 -> handleNpcClick2(player, npc)
            3 -> handleNpcClick3(player, npc)
            4 -> handleNpcClick4(player, npc)
            NPC_ATTACK_OPTION -> handleNpcAttack(player, npc)
        }
        clear(player)
    }

    private fun handleNpcClick1(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc) {
        if (!npc.isAlive) {
            player.send(SendMessage("That monster has been killed!"))
            return
        }
        player.resetAction()
        player.faceNpc(npc.slot)
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        player.startFishing(npc.id, 1)
        NpcContentDispatcher.tryHandleClick(player, 1, npc)
    }

    private fun handleNpcClick2(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc) {
        if (!npc.isAlive) {
            player.send(SendMessage("That monster has been killed!"))
            return
        }
        player.resetAction()
        player.faceNpc(npc.slot)
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        player.startFishing(npc.id, 2)
        NpcContentDispatcher.tryHandleClick(player, 2, npc)
    }

    private fun handleNpcClick3(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc) {
        if (player.isBusy) {
            return
        }
        player.resetAction()
        player.faceNpc(npc.slot)
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        NpcContentDispatcher.tryHandleClick(player, 3, npc)
    }

    private fun handleNpcClick4(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc) {
        if (player.isBusy) {
            return
        }
        player.skillX = npc.position.x
        player.setSkillY(npc.position.y)
        NpcContentDispatcher.tryHandleClick(player, 4, npc)
    }

    private fun handleNpcAttack(player: Client, npc: net.dodian.uber.game.model.entity.npc.Npc) {
        if (player.magicId >= 0) {
            player.magicId = -1
        }
        if (player.deathStage >= 1) {
            return
        }
        if (NpcContentDispatcher.tryHandleAttack(player, npc)) {
            return
        }
        player.resetWalkingQueue()
        player.startAttack(npc)
    }

    private fun clear(player: Client) {
        player.pendingInteraction = null
        player.activeInteraction = null
        player.interactionEarliestCycle = 0
    }

    private const val NPC_ATTACK_OPTION = 5
}
