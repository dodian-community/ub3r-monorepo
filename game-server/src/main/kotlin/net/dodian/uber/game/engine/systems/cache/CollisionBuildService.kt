package net.dodian.uber.game.engine.systems.cache

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.engine.systems.pathing.collision.CollisionDirection
import net.dodian.uber.game.engine.systems.pathing.collision.CollisionManager

class CollisionBuildService(
    private val collision: CollisionManager,
) {
    enum class FootprintMode {
        ROTATED,
        LUNA_UNROTATED_INTERACTABLE,
    }

    fun clear() {
        collision.clear()
    }

    fun rebuild(table: MapIndexTable) {
        collision.clear()
        applyTerrain(table)
        applyObjects(table)
    }

    fun applyTerrain(table: MapIndexTable) {
        for ((_, grid) in table.tileGrids) {
            applyTerrain(grid)
        }
    }

    fun applyTerrain(grid: DecodedMapTileGrid) {
        for (plane in 0 until 4) {
            for (x in 0 until 64) {
                for (y in 0 until 64) {
                    val tile = grid.getTile(x, y, plane)
                    if (!tile.isBlocked()) {
                        continue
                    }

                    var effectivePlane = plane
                    if (grid.getTile(x, y, 1).isBridge()) {
                        effectivePlane--
                    }
                    if (effectivePlane < 0) {
                        continue
                    }

                    val globalX = (grid.regionId shr 8) * 64 + x
                    val globalY = (grid.regionId and 0xFF) * 64 + y
                    collision.flagSolid(globalX, globalY, effectivePlane)
                }
            }
        }
    }

    fun applyObjects(table: MapIndexTable) {
        applyObjects(table.objects)
    }

    fun applyObjects(objects: List<DecodedMapObject>) {
        for (obj in objects) {
            val definition = GameObjectData.forId(obj.objectId)
            applyObject(
                id = obj.objectId,
                x = obj.x,
                y = obj.y,
                z = obj.plane,
                type = obj.type,
                rotation = obj.rotation,
                sizeX = definition.sizeX,
                sizeY = definition.sizeY,
                solid = definition.isSolid(),
                walkable = definition.isWalkable(),
                hasActions = definition.hasActions(),
                objectName = definition.name,
                blockWalk = definition.blockWalk(),
                blockRange = definition.blockRange(),
                breakRouteFinding = definition.breakRouteFinding(),
            )
        }
    }

    fun applyObjectData(obj: DecodedMapObject, definition: GameObjectData) {
        applyObject(
            id = obj.objectId,
            x = obj.x,
            y = obj.y,
            z = obj.plane,
            type = obj.type,
            rotation = obj.rotation,
            sizeX = definition.sizeX,
            sizeY = definition.sizeY,
            solid = definition.isSolid(),
            walkable = definition.isWalkable(),
            hasActions = definition.hasActions(),
            objectName = definition.name,
            blockWalk = definition.blockWalk(),
            blockRange = definition.blockRange(),
            breakRouteFinding = definition.breakRouteFinding(),
        )
    }

    fun applyTerrainAndObjects(grid: DecodedMapTileGrid?, objects: List<DecodedMapObject>) {
        if (grid != null) {
            applyTerrain(grid)
        }
        applyObjects(objects)
    }

    fun applyObject(
        id: Int,
        x: Int,
        y: Int,
        z: Int,
        type: Int,
        rotation: Int,
        sizeX: Int,
        sizeY: Int,
        solid: Boolean,
        walkable: Boolean,
        hasActions: Boolean = true,
        objectName: String? = null,
        blockWalk: Int = if (solid) 2 else 0,
        blockRange: Boolean = blockWalk != 0,
        breakRouteFinding: Boolean = false,
    ) = updateObjectCollision(
        remove = false,
        id = id,
        x = x,
        y = y,
        z = z,
        type = type,
        rotation = rotation,
        sizeX = sizeX,
        sizeY = sizeY,
        solid = solid,
        walkable = walkable,
        hasActions = hasActions,
        objectName = objectName,
        blockWalk = blockWalk,
        blockRange = blockRange,
        breakRouteFinding = breakRouteFinding,
    )

    fun removeObject(
        id: Int,
        x: Int,
        y: Int,
        z: Int,
        type: Int,
        rotation: Int,
        sizeX: Int,
        sizeY: Int,
        solid: Boolean,
        walkable: Boolean,
        hasActions: Boolean = true,
        objectName: String? = null,
        blockWalk: Int = if (solid) 2 else 0,
        blockRange: Boolean = blockWalk != 0,
        breakRouteFinding: Boolean = false,
    ) = updateObjectCollision(
        remove = true,
        id = id,
        x = x,
        y = y,
        z = z,
        type = type,
        rotation = rotation,
        sizeX = sizeX,
        sizeY = sizeY,
        solid = solid,
        walkable = walkable,
        hasActions = hasActions,
        objectName = objectName,
        blockWalk = blockWalk,
        blockRange = blockRange,
        breakRouteFinding = breakRouteFinding,
    )

    @Suppress("UNUSED_PARAMETER")
    private fun updateObjectCollision(
        remove: Boolean,
        id: Int,
        x: Int,
        y: Int,
        z: Int,
        type: Int,
        rotation: Int,
        sizeX: Int,
        sizeY: Int,
        solid: Boolean,
        walkable: Boolean,
        hasActions: Boolean,
        objectName: String?,
        blockWalk: Int,
        blockRange: Boolean,
        breakRouteFinding: Boolean,
    ) {
        if (!isTypeWalkBlocking(type, blockWalk, objectName)) {
            return
        }

        val normalizedRotation = rotation and 0x3
        val (width, height) = resolveFootprint(type, normalizedRotation, sizeX, sizeY, LIVE_FOOTPRINT_MODE)

        when (type) {
            0 -> applyWall(remove, x, y, z, CollisionDirection.WNES[normalizedRotation], blockRange)
            1, 3 -> applyDiagonalWall(remove, x, y, z, CollisionDirection.WNES_DIAGONAL[normalizedRotation], blockRange)
            2 -> applyLargeCorner(remove, x, y, z, CollisionDirection.WNES_DIAGONAL[normalizedRotation], blockRange)
            else -> {
                for (dx in 0 until width) {
                    for (dy in 0 until height) {
                        applySolid(remove, x + dx, y + dy, z, blockRange)
                        if (breakRouteFinding && type in 9..21) {
                            applyRouteBlocker(remove, x + dx, y + dy, z)
                        }
                    }
                }
            }
        }
    }

    private fun applyWall(remove: Boolean, x: Int, y: Int, z: Int, direction: CollisionDirection, blockRange: Boolean) {
        if (remove) {
            collision.clearWall(x, y, z, direction, blockRange)
        } else {
            collision.wall(x, y, z, direction, blockRange)
        }
    }

    private fun applyDiagonalWall(remove: Boolean, x: Int, y: Int, z: Int, direction: CollisionDirection, blockRange: Boolean) {
        if (remove) {
            collision.clearWall(x, y, z, direction, blockRange)
        } else {
            collision.wall(x, y, z, direction, blockRange)
        }
    }

    private fun applyLargeCorner(remove: Boolean, x: Int, y: Int, z: Int, direction: CollisionDirection, blockRange: Boolean) {
        if (remove) {
            collision.clearLargeCornerWall(x, y, z, direction, blockRange)
        } else {
            collision.largeCornerWall(x, y, z, direction, blockRange)
        }
    }

    private fun applySolid(remove: Boolean, x: Int, y: Int, z: Int, blockRange: Boolean) {
        if (remove) {
            collision.clearSolid(x, y, z, blockRange)
        } else {
            collision.flagSolid(x, y, z, blockRange)
        }
    }

    private fun applyRouteBlocker(remove: Boolean, x: Int, y: Int, z: Int) {
        if (remove) {
            collision.clearRouteBlocker(x, y, z)
        } else {
            collision.flagRouteBlocker(x, y, z)
        }
    }

    companion object {
        @Volatile
        var LIVE_FOOTPRINT_MODE: FootprintMode = FootprintMode.ROTATED

        @JvmStatic
        fun resolveFootprint(type: Int, normalizedRotation: Int, sizeX: Int, sizeY: Int, mode: FootprintMode): Pair<Int, Int> {
            val swapForRotation = normalizedRotation == 1 || normalizedRotation == 3
            val swap =
                when (mode) {
                    FootprintMode.ROTATED -> swapForRotation
                    FootprintMode.LUNA_UNROTATED_INTERACTABLE -> swapForRotation && type !in 9..21
                }
            return if (swap) sizeY to sizeX else sizeX to sizeY
        }

        @JvmStatic
        fun isTypeWalkBlocking(type: Int, blockWalk: Int, objectName: String? = null): Boolean {
            if (type in 10..11 && isUnnamedDefinitionName(objectName)) {
                return false
            }
            return when (type) {
                in 4..8 -> false
                22 -> blockWalk == 1
                else -> blockWalk != 0
            }
        }

        private fun isUnnamedDefinitionName(objectName: String?): Boolean {
            val normalized = objectName?.trim()?.lowercase() ?: return false
            return normalized == "null"
        }

        /**
         * Legacy compatibility shim. New callers should use [isTypeWalkBlocking].
         */
        @JvmStatic
        fun isTypeUnwalkable(type: Int, solid: Boolean, walkable: Boolean, hasActions: Boolean): Boolean =
            isTypeWalkBlocking(type, if (solid) 2 else 0, objectName = null)

        @JvmStatic
        fun shouldApplyRouteBlocking(type: Int, breakRouteFinding: Boolean): Boolean =
            breakRouteFinding && type in 9..21

        @JvmStatic
        fun shouldApplyProjectileBlocking(blockRange: Boolean): Boolean = blockRange

        @JvmStatic
        fun occupiesTile(
            objectX: Int,
            objectY: Int,
            tileX: Int,
            tileY: Int,
            type: Int,
            rotation: Int,
            sizeX: Int,
            sizeY: Int,
            mode: FootprintMode,
        ): Boolean {
            val (width, height) = resolveFootprint(type, rotation and 0x3, sizeX, sizeY, mode)
            return tileX in objectX until (objectX + width) && tileY in objectY until (objectY + height)
        }
    }
}
