package net.dodian.uber.game

import com.github.michaelbull.logging.InlineLogger
import net.dodian.services.impl.GameService

private val logger = InlineLogger()

class GamePulseHandler(private val service: GameService) : Runnable {

    override fun run() {
        try {
            service.pulse()
        } catch (throwable: Throwable) {
            logger.error(throwable) { "Exception occurred during pulse!" }
        }
    }
}