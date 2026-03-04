package net.dodian.uber.game.runtime.metrics

import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory

class GcStallTracker(
    private val beans: List<GarbageCollectorMXBean> = ManagementFactory.getGarbageCollectorMXBeans(),
) {
    data class Snapshot(
        val collectionCount: Long,
        val collectionTimeMs: Long,
    )

    fun snapshot(): Snapshot {
        var count = 0L
        var timeMs = 0L
        for (bean in beans) {
            val c = bean.collectionCount
            val t = bean.collectionTime
            if (c > 0) count += c
            if (t > 0) timeMs += t
        }
        return Snapshot(count, timeMs)
    }

    fun delta(before: Snapshot, after: Snapshot): Snapshot =
        Snapshot(
            collectionCount = (after.collectionCount - before.collectionCount).coerceAtLeast(0),
            collectionTimeMs = (after.collectionTimeMs - before.collectionTimeMs).coerceAtLeast(0),
        )
}

