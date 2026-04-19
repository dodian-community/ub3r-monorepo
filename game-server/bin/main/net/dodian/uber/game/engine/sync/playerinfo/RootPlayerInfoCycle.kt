package net.dodian.uber.game.engine.sync.playerinfo

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.sync.viewport.ViewportIndex

data class RootPlayerInfoCycle(
    val viewers: List<Client>,
    val viewportIndex: ViewportIndex?,
)
