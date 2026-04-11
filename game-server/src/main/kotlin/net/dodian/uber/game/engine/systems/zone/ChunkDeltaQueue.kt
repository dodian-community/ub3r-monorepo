package net.dodian.uber.game.engine.systems.zone

internal class ChunkDeltaQueue {
    private val deltas = ArrayList<ZoneDelta>()

    fun add(delta: ZoneDelta) {
        deltas += delta
    }

    fun drain(): List<ZoneDelta> {
        if (deltas.isEmpty()) {
            return emptyList()
        }
        val snapshot = ArrayList(deltas)
        deltas.clear()
        return snapshot
    }
}
