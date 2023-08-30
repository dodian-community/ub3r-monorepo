package net.dodian.uber.game.login

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.io.player.PlayerSerializer
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.session.GameSession

private val logger = InlineLogger()

class PlayerSaveWorker(
    private val serializer: PlayerSerializer,
    private val session: GameSession,
    private val player: Player
) : Runnable {
    override fun run() {

    }
}