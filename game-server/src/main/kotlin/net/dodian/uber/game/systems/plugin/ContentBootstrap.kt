package net.dodian.uber.game.systems.plugin

interface ContentBootstrap {
    val id: String

    fun bootstrap()
}
