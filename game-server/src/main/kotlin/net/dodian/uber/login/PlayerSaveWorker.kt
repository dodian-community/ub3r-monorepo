package net.dodian.uber.login

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.modelkt.entity.Player
import net.dodian.uber.io.player.PlayerSerializer
import net.dodian.uber.session.GameSession

private val logger = InlineLogger()

class PlayerSaveWorker(
    private val serializer: PlayerSerializer,
    private val session: GameSession,
    private val player: Player
) : Runnable {

    override fun run() {
        try {
            serializer.savePlayer(player)
            session.handlePlayerSaverResponse(true)
        } catch (exception: Exception) {
            logger.error(exception) { "Unable to save player's game..." }
            session.handlePlayerSaverResponse(false)
        }
    }
}