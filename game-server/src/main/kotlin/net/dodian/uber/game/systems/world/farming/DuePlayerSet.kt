package net.dodian.uber.game.systems.world.farming

import java.util.Collections
import java.util.IdentityHashMap
import net.dodian.uber.game.model.entity.player.Client

class DuePlayerSet {
    private val players = Collections.newSetFromMap(IdentityHashMap<Client, Boolean>())

    fun add(player: Client) {
        players += player
    }

    fun remove(player: Client) {
        players -= player
    }

    fun drain(): List<Client> {
        val copy = players.toList()
        players.clear()
        return copy
    }

    fun isEmpty(): Boolean = players.isEmpty()
}
