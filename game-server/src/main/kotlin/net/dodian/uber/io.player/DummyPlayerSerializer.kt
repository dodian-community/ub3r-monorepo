package net.dodian.uber.io.player

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.modelkt.entity.Player
import net.dodian.uber.net.codec.login.STATUS_ACCOUNT_ONLINE
import net.dodian.uber.net.codec.login.STATUS_OK
import net.dodian.uber.net.codec.login.STATUS_SERVER_FULL
import net.dodian.utilities.security.PlayerCredentials

private val logger = InlineLogger()

class DummyPlayerSerializer : PlayerSerializer() {

    override fun loadPlayer(credentials: PlayerCredentials): PlayerLoaderResponse {
        var status = STATUS_OK

        val player = Player(credentials)

        player.playerRights = 0
        player.premium = true

        when (credentials.password) {
            "admin" -> player.playerRights = 3
            "mod" -> player.playerRights = 2
            "!prem" -> player.premium = false
            "full" -> status = STATUS_SERVER_FULL
            "on" -> status = STATUS_ACCOUNT_ONLINE
        }

        return PlayerLoaderResponse(status, player)
    }

    override fun savePlayer(player: Player) {
        
    }
}