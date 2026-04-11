package net.dodian.uber.game.systems.cache

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.systems.pathing.collision.CollisionDirection
import net.dodian.uber.game.systems.pathing.collision.CollisionManager

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
    ) {
        if (!isUnwalkableObjectType(type, solid, walkable, hasActions)) {
            return
        }

        val normalizedRotation = rotation and 0x3
        val (width, height) = resolveFootprint(type, normalizedRotation, sizeX, sizeY, LIVE_FOOTPRINT_MODE)

        when (type) {
            0 -> applyWall(remove, x, y, z, CollisionDirection.WNES[normalizedRotation])
            1, 3 -> applyDiagonalWall(remove, x, y, z, CollisionDirection.WNES_DIAGONAL[normalizedRotation])
            2 -> applyLargeCorner(remove, x, y, z, CollisionDirection.WNES_DIAGONAL[normalizedRotation])
            22 -> {
                if (hasActions) {
                    applySolid(remove, x, y, z)
                }
            }
            else -> {
                for (dx in 0 until width) {
                    for (dy in 0 until height) {
                        applySolid(remove, x + dx, y + dy, z)
                    }
                }
            }
        }
    }

    /**
     * Luna-style object collision classification is type-first.
     *
     * Wall and wall-roof ranges are always unwalkable by type; default
     * footprint objects still honor solidity/walkable metadata.
     */
    private fun isUnwalkableObjectType(type: Int, solid: Boolean, walkable: Boolean, hasActions: Boolean): Boolean =
        isTypeUnwalkable(type, solid, walkable, hasActions)

    private fun applyWall(remove: Boolean, x: Int, y: Int, z: Int, direction: CollisionDirection) {
        if (remove) {
            collision.clearWall(x, y, z, direction)
        } else {
            collision.wall(x, y, z, direction)
        }
    }

    private fun applyDiagonalWall(remove: Boolean, x: Int, y: Int, z: Int, direction: CollisionDirection) {
        if (remove) {
            collision.clearWall(x, y, z, direction)
        } else {
            collision.wall(x, y, z, direction)
        }
    }

    private fun applyLargeCorner(remove: Boolean, x: Int, y: Int, z: Int, direction: CollisionDirection) {
        if (remove) {
            collision.clearLargeCornerWall(x, y, z, direction)
        } else {
            collision.largeCornerWall(x, y, z, direction)
        }
    }

    private fun applySolid(remove: Boolean, x: Int, y: Int, z: Int) {
        if (remove) {
            collision.clearSolid(x, y, z)
        } else {
            collision.flagSolid(x, y, z)
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
        fun isTypeUnwalkable(type: Int, solid: Boolean, walkable: Boolean, hasActions: Boolean): Boolean =
            when (type) {
                0, 1, 2, 3 -> true
                in 4..8 -> false
                in 12..21 -> solid
                22 -> solid && hasActions
                9 -> solid
                10, 11 -> solid
                else -> solid && !walkable
            }

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
