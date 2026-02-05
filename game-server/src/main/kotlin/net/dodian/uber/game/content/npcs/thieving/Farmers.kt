package net.dodian.uber.game.content.npcs.thieving

import net.dodian.uber.game.content.npcs.NpcContent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.thieving.Thieving
import net.dodian.uber.game.netty.listener.out.SendMessage

object Farmers : NpcContent {
    override val npcIds: IntArray = intArrayOf(3086, 3257)

    override fun onSecondClick(client: Client, npc: Npc, npcIndex: Int): Boolean {
        Thieving.attemptSteal(client, npc.id, npc.position)
        return true
    }

    override fun onAttack(client: Client, npc: Npc, npcIndex: Int): Boolean {
        client.send(SendMessage("You can't attack a Farmer!"))
        client.resetWalkingQueue()
        client.setWalkToTask(null)
        return true
    }
}

