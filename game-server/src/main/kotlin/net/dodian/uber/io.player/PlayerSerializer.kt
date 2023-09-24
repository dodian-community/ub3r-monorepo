package net.dodian.uber.io.player

import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.utilities.security.PlayerCredentials

abstract class PlayerSerializer {
    abstract fun loadPlayer(credentials: PlayerCredentials): PlayerLoaderResponse
    abstract fun savePlayer(player: Player): Boolean
}