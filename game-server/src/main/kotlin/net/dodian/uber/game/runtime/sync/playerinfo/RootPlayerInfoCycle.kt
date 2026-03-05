package net.dodian.uber.game.runtime.sync.playerinfo

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.sync.viewport.ViewportIndex

data class RootPlayerInfoCycle(
    val viewers: List<Client>,
    val viewportIndex: ViewportIndex?,
)
