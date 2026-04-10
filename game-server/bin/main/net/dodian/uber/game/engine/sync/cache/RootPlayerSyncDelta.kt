package net.dodian.uber.game.engine.sync.cache

import net.dodian.uber.game.model.entity.player.Player

data class RootPlayerSyncDelta(
    val player: Player,
    val phases: Set<String>,
)
