package net.dodian.uber.game.content.npcs.attack

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object Farmer3086AttackNpc : NpcAttackContent {
    override val npcIds: IntArray = intArrayOf(3086)

    override fun onAttack(client: Client, npc: Npc, npcIndex: Int): Boolean {
        client.send(SendMessage("You can't attack a Farmer!"))
        client.resetWalkingQueue()
        client.setWalkToTask(null)
        return true
    }
}

