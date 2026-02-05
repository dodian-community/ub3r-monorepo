package net.dodian.uber.game.content.npcs.yanille.monks

import net.dodian.uber.game.content.npcs.NpcContent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object Monk : NpcContent {
    override val npcIds: IntArray = intArrayOf(555)

    override fun onFirstClick(client: Client, npc: Npc, npcIndex: Int): Boolean {
        client.quests[0]++
        client.send(
            SendMessage(
                if (client.playerRights > 1) {
                    "Set your quest to: ${client.quests[0]}"
                } else {
                    "Suddenly the monk had an urge to dissapear!"
                }
            )
        )
        return true
    }
}
