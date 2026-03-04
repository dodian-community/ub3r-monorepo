package net.dodian.uber.game.runtime.sync

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.sync.viewport.ViewportSnapshot

data class ViewerSyncContext(
    val viewer: Client,
    val snapshot: ViewportSnapshot?,
)
