package net.dodian.uber.game.runtime.zone

import net.dodian.uber.game.model.entity.player.Client

internal class ZoneFlushService(
    private val subscriberIndex: ZoneSubscriberIndex = ZoneSubscriberIndex(),
) {
    fun flush(
        deltas: List<ZoneDelta>,
        activePlayers: List<Client>,
    ): ZoneFlushStats {
        if (deltas.isEmpty()) {
            return ZoneFlushStats.EMPTY
        }
        var candidateViewers = 0
        var deliveries = 0
        deltas.forEach { delta ->
            val viewers = subscriberIndex.viewersFor(delta, activePlayers)
            candidateViewers += viewers.size
            viewers.forEach {
                delta.deliver(it)
                deliveries++
            }
        }
        return ZoneFlushStats(deltas.size, candidateViewers, deliveries)
    }
}
