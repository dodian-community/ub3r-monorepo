package net.dodian

import net.dodian.services.impl.GameService
import net.dodian.services.impl.LoginService
import net.dodian.services.impl.RsaService
import net.dodian.uber.net.startChannel

lateinit var serverContext: ServerContext

fun main() {
    serverContext = ServerContext(
        gameService = GameService(),
        loginService = LoginService(),
        rsaService = RsaService()
    )

    startChannel()
}