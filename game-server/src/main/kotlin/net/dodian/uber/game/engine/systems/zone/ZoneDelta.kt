package net.dodian.uber.game.engine.systems.zone

import net.dodian.uber.game.model.entity.player.Client

abstract class ZoneDelta {
    abstract fun appliesTo(viewer: Client): Boolean

    abstract fun deliver(viewer: Client)

    abstract val minChunkX: Int
    abstract val maxChunkX: Int
    abstract val minChunkY: Int
    abstract val maxChunkY: Int
}