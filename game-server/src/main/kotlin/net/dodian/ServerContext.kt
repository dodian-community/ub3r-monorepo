package net.dodian

import net.dodian.services.ServiceManager
import net.dodian.services.impl.GameService
import net.dodian.services.impl.LoginService
import net.dodian.services.impl.RsaService

class ServerContext(
    val serviceManager: ServiceManager
) {
}