package net.dodian.uber.game.runtime.zone

import net.dodian.uber.game.model.entity.player.Client

internal class ZoneFlushService(
    private val subscriberIndex: ZoneSubscriberIndex = ZoneSubscriberIndex(),
) {
    fun flush(
        deltas: List<ZoneDelta>,
        activePlayers: List<Client>,
    ) {
        deltas.forEach { delta ->
            subscriberIndex.viewersFor(delta, activePlayers).forEach(delta::deliver)
        }
    }
}
