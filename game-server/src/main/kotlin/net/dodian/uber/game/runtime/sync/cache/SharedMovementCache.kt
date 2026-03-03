package net.dodian.uber.game.runtime.sync.cache

import java.util.IdentityHashMap
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Player

class SharedMovementCache {
    private val playerDirections = IdentityHashMap<Player, IntArray>()
    private val npcDirections = IdentityHashMap<Npc, IntArray>()

    fun freezePlayer(player: Player) {
        playerDirections[player] = intArrayOf(player.primaryDirection, player.secondaryDirection)
    }

    fun freezeNpc(npc: Npc) {
        npcDirections[npc] = intArrayOf(npc.direction)
    }
}
