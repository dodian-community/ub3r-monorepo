package net.dodian.uber.game.engine.sync.playerinfo.state

import net.dodian.uber.game.engine.sync.player.fragments.PlayerAddLocalFragmentCache
import net.dodian.uber.game.engine.sync.player.fragments.PlayerBlockFragmentCache
import net.dodian.uber.game.engine.sync.player.fragments.PlayerMovementFragmentCache

data class PlayerInfoFragmentCache(
    val movement: PlayerMovementFragmentCache = PlayerMovementFragmentCache(),
    val blocks: PlayerBlockFragmentCache = PlayerBlockFragmentCache(),
    val addLocal: PlayerAddLocalFragmentCache = PlayerAddLocalFragmentCache(),
)
