package net.dodian.uber.game.content.npcs.action2

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.thieving.Thieving

object Farmer3086 : NpcAction2Content {
    override val npcIds: IntArray = intArrayOf(3086)

    override fun onClick2(client: Client, npc: Npc, npcIndex: Int): Boolean {
        Thieving.attemptSteal(client, npc.id, npc.position)
        return true
    }
}

