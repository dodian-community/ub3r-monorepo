package net.dodian.uber.game.session

import net.dodian.uber.game.model.entity.player.Player

class PlayerManager(
    val players: MutableList<Player> = mutableListOf()
) {

    fun register(player: Player) {
        players.add(player)
    }

    fun findPlayer(name: String): Player? {
        return players.firstOrNull { it.playerName == name }
    }

    fun isPlayerOnline(name: String) = players.any { it.playerName == name }
    fun isPlayerOnline(player: Player) = isPlayerOnline(player.playerName)

    val isFull get() = players.size >= 2_000
}