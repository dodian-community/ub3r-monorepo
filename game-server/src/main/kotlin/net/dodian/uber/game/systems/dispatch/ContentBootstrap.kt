package net.dodian.uber.game.systems.dispatch

interface ContentBootstrap {
    val id: String

    fun bootstrap()
}
