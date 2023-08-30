package net.dodian.services

import net.dodian.services.impl.GameService
import net.dodian.services.impl.LoginService
import net.dodian.services.impl.RsaService

class ServiceManager(
    val gameService: GameService,
    val loginService: LoginService,
    val rsaService: RsaService
) {
}