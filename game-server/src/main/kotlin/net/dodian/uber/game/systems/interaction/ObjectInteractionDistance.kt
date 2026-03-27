package net.dodian.uber.game.systems.interaction

import net.dodian.cache.`object`.GameObjectData
import net.dodian.cache.`object`.GameObjectDef
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.`object`.GlobalObject
import net.dodian.uber.game.model.`object`.Object as GameObject
import net.dodian.utilities.Misc
import kotlin.math.abs

/**
 * Shared distance resolution for object interactions.
 *
 * This was lifted from the legacy Event(600) object interaction handlers to preserve 317 behavior.
 */
object ObjectInteractionDistance {
    enum class DistanceMode {
        CLICK,
        MINING,
        ITEM_ON_OBJECT,
        MAGIC,
        POLICY_NEAREST_BOUNDARY_CARDINAL,
        POLICY_NEAREST_BOUNDARY_ANY,
    }

    @JvmStatic
    fun resolveDistancePosition(
        client: Client,
        walkTo: Position,
        objectId: Int,
        objectData: GameObjectData?,
        def: GameObjectDef?,
        mode: DistanceMode,
    ): Position? {
        var objectPosition: Position? = null

        if (mode == DistanceMode.POLICY_NEAREST_BOUNDARY_CARDINAL) {
            return resolveNearestBoundaryDistancePosition(client, walkTo, objectData, def, objectId, true)
        }
        if (mode == DistanceMode.POLICY_NEAREST_BOUNDARY_ANY) {
            return resolveNearestBoundaryDistancePosition(client, walkTo, objectData, def, objectId, false)
        }

        val objectAtTile = GameObject(objectId, walkTo.x, walkTo.y, walkTo.z, 10)
        if (def != null && !GlobalObject.hasGlobalObject(objectAtTile)) {
            if (objectData != null) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.x,
                    walkTo.y,
                    client.position.x,
                    client.position.y,
                    objectData.getSizeX(def.face),
                    objectData.getSizeY(def.face),
                    client.position.z,
                )
            }
        } else {
            if (GlobalObject.hasGlobalObject(objectAtTile)) {
                if (objectData != null) {
                    objectPosition = Misc.goodDistanceObject(
                        walkTo.x,
                        walkTo.y,
                        client.position.x,
                        client.position.y,
                        objectData.getSizeX(objectAtTile.face),
                        objectData.getSizeY(objectAtTile.type),
                        objectAtTile.z,
                    )
                }
            } else if (objectData != null) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.x,
                    walkTo.y,
                    client.position.x,
                    client.position.y,
                    objectData.sizeX,
                    objectData.sizeY,
                    client.position.z,
                )
            }
        }

        if (mode == DistanceMode.CLICK || mode == DistanceMode.MAGIC) {
            if (objectId == 23131 && objectData != null) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.x,
                    3552,
                    client.position.x,
                    client.position.y,
                    objectData.sizeX,
                    objectData.sizeY,
                    client.position.z,
                )
            }
            if (objectId == 16466) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.x,
                    2972,
                    client.position.x,
                    client.position.y,
                    1,
                    1,
                    client.position.z,
                )
            }
            if (objectId == 11643) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.x,
                    walkTo.y,
                    client.position.x,
                    client.position.y,
                    2,
                    client.position.z,
                )
            }
        }

        if (mode == DistanceMode.ITEM_ON_OBJECT) {
            if (objectId == 23131 && objectData != null) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.x,
                    3552,
                    client.position.x,
                    client.position.y,
                    objectData.sizeX,
                    objectData.sizeY,
                    client.position.z,
                )
            }
            if (objectId == 16466) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.x,
                    2972,
                    client.position.x,
                    client.position.y,
                    1,
                    3,
                    client.position.z,
                )
            }
        }

        return objectPosition
    }

    private fun resolveNearestBoundaryDistancePosition(
        client: Client,
        walkTo: Position,
        objectData: GameObjectData?,
        def: GameObjectDef?,
        objectId: Int,
        cardinalOnly: Boolean,
    ): Position? {
        if (client.position.z != walkTo.z) {
            return null
        }

        val footprint = resolveObjectFootprint(walkTo, objectData, def, objectId)
        val nearestBoundaryTile = resolveNearestBoundaryTile(client.position, footprint) ?: return null
        if (cardinalOnly && isCardinalAdjacent(client.position, nearestBoundaryTile)) {
            return nearestBoundaryTile
        }
        if (!cardinalOnly && isAdjacent(client.position, nearestBoundaryTile)) {
            return nearestBoundaryTile
        }
        return null
    }

    private fun resolveObjectFootprint(
        walkTo: Position,
        objectData: GameObjectData?,
        def: GameObjectDef?,
        objectId: Int,
    ): Footprint {
        val minX = walkTo.x
        val minY = walkTo.y
        var maxX = walkTo.x
        var maxY = walkTo.y

        if (objectData != null) {
            var sizeX: Int = objectData.sizeX
            var sizeY: Int = objectData.sizeY

            val globalObject = GlobalObject.getGlobalObject(walkTo.x, walkTo.y)
            if (globalObject != null && globalObject.id == objectId) {
                val rotation = globalObject.face
                sizeX = objectData.getSizeX(rotation)
                sizeY = objectData.getSizeY(rotation)
            } else if (def != null) {
                val rotation = def.face
                sizeX = objectData.getSizeX(rotation)
                sizeY = objectData.getSizeY(rotation)
            }

            sizeX = maxOf(1, sizeX)
            sizeY = maxOf(1, sizeY)
            maxX = minX + sizeX - 1
            maxY = minY + sizeY - 1
        }

        return Footprint(minX, minY, maxX, maxY, walkTo.z)
    }

    private fun resolveNearestBoundaryTile(player: Position, footprint: Footprint): Position {
        var nearestX = clamp(player.x, footprint.minX, footprint.maxX)
        var nearestY = clamp(player.y, footprint.minY, footprint.maxY)

        if (nearestX > footprint.minX && nearestX < footprint.maxX &&
            nearestY > footprint.minY && nearestY < footprint.maxY
        ) {
            val toWest = nearestX - footprint.minX
            val toEast = footprint.maxX - nearestX
            val toSouth = nearestY - footprint.minY
            val toNorth = footprint.maxY - nearestY

            val minDelta = minOf(minOf(toWest, toEast), minOf(toSouth, toNorth))
            if (minDelta == toWest) {
                nearestX = footprint.minX
            } else if (minDelta == toEast) {
                nearestX = footprint.maxX
            } else if (minDelta == toSouth) {
                nearestY = footprint.minY
            } else {
                nearestY = footprint.maxY
            }
        }

        return Position(nearestX, nearestY, footprint.z)
    }

    private fun isCardinalAdjacent(player: Position, tile: Position): Boolean {
        if (player.z != tile.z) {
            return false
        }
        val deltaX = abs(player.x - tile.x)
        val deltaY = abs(player.y - tile.y)
        return (deltaX + deltaY) == 1
    }

    private fun isAdjacent(player: Position, tile: Position): Boolean {
        if (player.z != tile.z) {
            return false
        }
        val deltaX = abs(player.x - tile.x)
        val deltaY = abs(player.y - tile.y)
        return maxOf(deltaX, deltaY) == 1
    }

    private fun clamp(value: Int, min: Int, max: Int): Int = maxOf(min, minOf(max, value))

    private data class Footprint(
        val minX: Int,
        val minY: Int,
        val maxX: Int,
        val maxY: Int,
        val z: Int,
    )
}
