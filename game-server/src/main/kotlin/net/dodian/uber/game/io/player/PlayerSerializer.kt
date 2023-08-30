package net.dodian.uber.game.io.player

import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.session.PlayerCredentials
import net.dodian.uber.game.session.PlayerLoaderResponse

abstract class PlayerSerializer {
    abstract fun loadPlayer(credentials: PlayerCredentials): PlayerLoaderResponse
    abstract fun savePlayer(player: Player)
}