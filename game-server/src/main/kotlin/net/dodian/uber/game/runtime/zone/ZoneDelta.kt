package net.dodian.uber.game.runtime.zone

import net.dodian.uber.game.model.entity.player.Client

abstract class ZoneDelta {
    abstract fun appliesTo(viewer: Client): Boolean

    abstract fun deliver(viewer: Client)

    abstract fun candidateChunkKeys(): LongArray
}
