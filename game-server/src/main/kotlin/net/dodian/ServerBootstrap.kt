package net.dodian

import net.dodian.services.impl.GameService
import net.dodian.services.impl.LoginService
import net.dodian.services.impl.RsaService
import net.dodian.uber.game.session.PlayerManager
import net.dodian.uber.net.startChannel

val context = ServerContext()

fun main() {
    context.registerHandler(PlayerManager())

    context.registerServices(
        GameService(),
        LoginService(),
        RsaService()
    )

    startChannel()
}