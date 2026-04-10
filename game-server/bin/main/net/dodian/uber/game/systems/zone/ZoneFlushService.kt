package net.dodian.uber.game.systems.zone

import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

internal class ZoneFlushService(
    private val subscriberIndex: ZoneSubscriberIndex = ZoneSubscriberIndex(),
) {
    private val logger = LoggerFactory.getLogger(ZoneFlushService::class.java)

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
            viewers.forEach { viewer ->
                try {
                    delta.deliver(viewer)
                    deliveries++
                } catch (throwable: Throwable) {
                    logger.error(
                        "Zone delta delivery failed player={} slot={} delta={}",
                        viewer.playerName,
                        viewer.slot,
                        delta.javaClass.simpleName,
                        throwable,
                    )
                    viewer.disconnected = true
                }
            }
        }
        return ZoneFlushStats(deltas.size, candidateViewers, deliveries)
    }
}
