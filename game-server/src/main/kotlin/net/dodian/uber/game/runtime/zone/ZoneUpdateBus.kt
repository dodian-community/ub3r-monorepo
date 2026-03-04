package net.dodian.uber.game.runtime.zone

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.netty.listener.out.CreateGroundItem
import net.dodian.uber.game.netty.listener.out.RemoveGroundItem

object ZoneUpdateBus {
    private val queue = ChunkDeltaQueue()
    private val flushService = ZoneFlushService()
    private val lock = Any()

    @JvmStatic
    fun queueGroundItemCreate(
        itemId: Int,
        amount: Int,
        position: Position,
        onlyDbId: Int? = null,
        excludeDbId: Int? = null,
    ) {
        enqueue(
            PacketZoneDelta(
                position = position,
                radius = 104,
                onlyDbId = onlyDbId,
                excludeDbId = excludeDbId,
            ) { viewer ->
                viewer.send(CreateGroundItem(GameItem(itemId, amount), position))
            },
        )
    }

    @JvmStatic
    fun queueGroundItemRemove(
        itemId: Int,
        amount: Int,
        position: Position,
    ) {
        enqueue(
            PacketZoneDelta(position, 104) { viewer ->
                viewer.send(RemoveGroundItem(GameItem(itemId, amount), position))
            },
        )
    }

    @JvmStatic
    fun queueGlobalObjectNew(
        position: Position,
        objectId: Int,
        face: Int,
        type: Int,
    ) {
        enqueue(
            PacketZoneDelta(position, 60) { viewer ->
                viewer.ReplaceObject2(position, objectId, face, type)
            },
        )
    }

    @JvmStatic
    fun queueGlobalObjectOld(
        position: Position,
        oldObjectId: Int,
        face: Int,
        type: Int,
    ) {
        enqueue(
            PacketZoneDelta(position, 60) { viewer ->
                viewer.ReplaceObject(position.x, position.y, oldObjectId, face, type)
            },
        )
    }

    @JvmStatic
    fun queuePersonalObject(
        dbId: Int,
        position: Position,
        objectId: Int,
        face: Int,
        type: Int,
    ) {
        enqueue(
            PacketZoneDelta(position, 60, onlyDbId = dbId) { viewer ->
                viewer.ReplaceObject2(position, objectId, face, type)
            },
        )
    }

    @JvmStatic
    fun flush(activePlayers: List<Client>) {
        val deltas =
            synchronized(lock) {
                queue.drain()
            }
        if (deltas.isEmpty()) {
            return
        }
        flushService.flush(deltas, activePlayers)
    }

    private fun enqueue(delta: ZoneDelta) {
        synchronized(lock) {
            queue.add(delta)
        }
    }

    private class PacketZoneDelta(
        private val position: Position,
        private val radius: Int,
        private val onlyDbId: Int? = null,
        private val excludeDbId: Int? = null,
        private val sender: (Client) -> Unit,
    ) : ZoneDelta() {
        override fun appliesTo(viewer: Client): Boolean {
            if (!viewer.isActive || viewer.disconnected) {
                return false
            }
            if (onlyDbId != null && viewer.dbId != onlyDbId) {
                return false
            }
            if (excludeDbId != null && viewer.dbId == excludeDbId) {
                return false
            }
            if (viewer.position.z != position.z) {
                return false
            }
            return viewer.GoodDistance(viewer.position.x, viewer.position.y, position.x, position.y, radius)
        }

        override fun deliver(viewer: Client) {
            sender(viewer)
        }
    }
}
