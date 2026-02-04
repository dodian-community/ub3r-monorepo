package net.dodian.uber.game.content.npcs.action1

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

/**
 * Shopkeeper (npcId=506).
 *
 * Mystic client appears to send this interaction via first-click.
 * Legacy used click2 (npcId 506/527) to set WanneShop=3.
 */
object Shopkeeper506Trade : NpcAction1Content {
    override val npcIds: IntArray = intArrayOf(506)

    override fun onClick1(client: Client, npc: Npc, npcIndex: Int): Boolean {
        client.WanneShop = 3
        return true
    }
}

