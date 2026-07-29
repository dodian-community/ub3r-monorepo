package net.dodian.uber.game.social

import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object WorldAnnouncementService {
    @JvmStatic fun broadcast(message: String) = PlayerRegistry.players.filterIsInstance<Client>()
        .filter { it.isActive && !it.disconnected && it.position.x > 0 && it.position.y > 0 }
        .forEach { it.send(SendMessage(message)) }

    @JvmStatic fun broadcastAll(message: String) = PlayerRegistry.players.filterIsInstance<Client>()
        .filter { it.isActive }.forEach { it.send(SendMessage(message)) }

    @JvmStatic fun broadcastWilderness(message: String) = PlayerRegistry.players.filterIsInstance<Client>()
        .filter { it.isActive && (it.inWildy() || it.inEdgeville()) }.forEach { it.send(SendMessage(message)) }

    @JvmStatic fun broadcastArea(message: String, area: String) = PlayerRegistry.players.filterIsInstance<Client>()
        .filter { it.isActive && it.positionName.contains(area) }
        .forEach { it.send(SendMessage("<col=FFFF00>[Area]<col=000000> $message")) }
}
