package net.dodian.uber.service

import net.dodian.uber.game.session.LoginSession
import net.dodian.uber.net.codec.login.LoginRequest
import net.dodian.uber.net.codec.login.STATUS_GAME_UPDATED
import net.dodian.uber.net.codec.login.STATUS_OK

class LoginService {

    fun submitLoadRequest(session: LoginSession, request: LoginRequest) {
        var response = STATUS_OK

        if (requiresUpdate(request))
            response = STATUS_GAME_UPDATED

        if (response == STATUS_OK) {

        } else {

        }
    }

    private fun requiresUpdate(request: LoginRequest): Boolean {
        val release = 1
        if (release != request.releaseNumber)
            return true

        // TODO: Implement crc check?
        return true
    }
}