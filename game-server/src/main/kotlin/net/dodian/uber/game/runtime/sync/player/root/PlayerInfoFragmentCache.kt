package net.dodian.uber.game.runtime.sync.player.root

import net.dodian.uber.game.runtime.sync.player.fragments.PlayerAddLocalFragmentCache
import net.dodian.uber.game.runtime.sync.player.fragments.PlayerBlockFragmentCache
import net.dodian.uber.game.runtime.sync.player.fragments.PlayerMovementFragmentCache

data class PlayerInfoFragmentCache(
    val movement: PlayerMovementFragmentCache = PlayerMovementFragmentCache(),
    val blocks: PlayerBlockFragmentCache = PlayerBlockFragmentCache(),
    val addLocal: PlayerAddLocalFragmentCache = PlayerAddLocalFragmentCache(),
)
