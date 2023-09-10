package net.dodian.uber.game.session

import net.dodian.uber.context
import net.dodian.uber.event.impl.PlayerSessionEvent
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.mob.list.PlayerList
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class PlayerManager(
    val players: PlayerList
) {

    data class LoginPlayerRequest(
        val player: Player,
        val session: LoginSession
    )

    val newPlayers: Queue<LoginPlayerRequest> = ConcurrentLinkedQueue()
    val oldPlayers: Queue<Player> = ConcurrentLinkedQueue()

    fun registerPlayer(player: Player, session: LoginSession) {
        newPlayers.add(LoginPlayerRequest(player, session))
    }

    fun unregisterPlayer(player: Player) {
        oldPlayers.add(player)
    }

    fun register(player: Player) {
        context.eventBus.publish(player, PlayerSessionEvent.Login)
        players.add(player)
    }

    fun findPlayer(name: String): Player? {
        return players.firstOrNull { it.playerName == name }
    }

    fun isPlayerOnline(name: String) = players.any { it.playerName == name }
    fun isPlayerOnline(player: Player) = isPlayerOnline(player.playerName)

    val isFull get() = players.size >= 2_000
}