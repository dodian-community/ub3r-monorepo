package net.dodian.uber.game.runtime.zone

import net.dodian.uber.game.model.entity.player.Client

internal sealed class ZoneDelta {
    abstract fun appliesTo(viewer: Client): Boolean

    abstract fun deliver(viewer: Client)
}
