package net.dodian.uber.game.runtime.zone

import net.dodian.uber.game.model.entity.player.Client

internal class ZoneSubscriberIndex {
    fun viewersFor(
        delta: ZoneDelta,
        activePlayers: List<Client>,
    ): List<Client> = activePlayers.filter(delta::appliesTo)
}
