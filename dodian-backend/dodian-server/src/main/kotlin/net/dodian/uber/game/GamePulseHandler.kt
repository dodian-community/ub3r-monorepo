package net.dodian.uber.game

import net.dodian.uber.services.GameService

class GamePulseHandler(private val service: GameService) : Runnable {

    override fun run() {
        service.pulse()
    }
}