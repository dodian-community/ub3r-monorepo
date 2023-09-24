package net.dodian.uber.game.modelkt.area.update

import net.dodian.uber.game.modelkt.area.EntityUpdateType
import net.dodian.uber.game.modelkt.area.EntityUpdateType.*
import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.modelkt.area.Region
import net.dodian.uber.game.modelkt.entity.Entity
import net.dodian.uber.net.protocol.packets.server.region.RegionUpdateMessage

@Suppress("MemberVisibilityCanBePrivate")
abstract class UpdateOperation<E : Entity>(
    protected val entity: E,
    protected val region: Region,
    protected val updateType: EntityUpdateType
) {
    protected abstract fun remove(offset: Int): RegionUpdateMessage
    protected abstract fun add(offset: Int): RegionUpdateMessage

    fun inverse(): RegionUpdateMessage {
        val offset = positionOffset(entity.position)

        return when (updateType) {
            ADD -> remove(offset)
            REMOVE -> add(offset)
        }
    }

    fun toMessage(): RegionUpdateMessage {
        val offset = positionOffset(entity.position)

        return when (updateType) {
            ADD -> add(offset)
            REMOVE -> remove(offset)
        }
    }

    private fun positionOffset(position: Position): Int {
        val coordinates = region.coordinates
        val dx = position.x - coordinates.absoluteX
        val dy = position.y - coordinates.absoluteY

        if ((dx < 0 || dx >= Region.SIZE) || (dy < 0 || dy >= Region.SIZE))
            error("$position not in expected Region of $region.")

        return dx shl 4 or dy
    }
}