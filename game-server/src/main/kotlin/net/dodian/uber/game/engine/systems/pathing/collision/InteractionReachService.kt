package net.dodian.uber.game.engine.systems.pathing.collision

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.objects.WorldObject

/**
 * Service to determine if a player has reached an object for interaction.
 *
 * This logic is ported from Luna (RS2 #377 client logic) and handles:
 * 1. Straight walls (Types 0-3)
 * 2. Diagonal walls (Type 9)
 * 3. Wall decorations (Types 4-8)
 * 4. Multi-tile interactable objects (Types 10-11, 12-21)
 * 5. Interaction face masks (Opcode 69)
 */
object InteractionReachService {

    fun reachedObject(start: Position, worldObject: WorldObject): Boolean {
        val definition = GameObjectData.forId(worldObject.id)
        val type = worldObject.type
        val rotation = worldObject.face and 0x3

        if (type == 10 || type == 11 || type == 22) {
            val unrotatedSizeX = definition.sizeX
            val unrotatedSizeY = definition.sizeY
            val sizeX: Int
            val sizeY: Int
            if (rotation == 0 || rotation == 2) {
                sizeX = unrotatedSizeX
                sizeY = unrotatedSizeY
            } else {
                sizeX = unrotatedSizeY
                sizeY = unrotatedSizeX
            }

            var packedDirections = definition.interactionFaceMask
            if (rotation != 0) {
                // Bitwise rotate based on face (0=WEST, 1=NORTH, 2=EAST, 3=SOUTH)
                // Note: Standard RS2 opcode 69 is NESW (1, 2, 4, 8)
                packedDirections = ((packedDirections shl rotation) and 0xF) or (packedDirections shr (4 - rotation))
            }

            return reachedFacingEntity(
                startX = start.x,
                startY = start.y,
                z = start.z,
                endX = worldObject.x,
                endY = worldObject.y,
                sizeX = sizeX,
                sizeY = sizeY,
                packedDirections = packedDirections
            )
        } else {
            if (type in 0..3 || type == 9) {
                return reachedWall(start, worldObject)
            }
            if (type in 4..8) {
                return reachedDecoration(start, worldObject)
            }
            return false
        }
    }

    private fun reachedFacingEntity(
        startX: Int,
        startY: Int,
        z: Int,
        endX: Int,
        endY: Int,
        sizeX: Int,
        sizeY: Int,
        packedDirections: Int
    ): Boolean {
        val radiusX = (endX + sizeX) - 1
        val radiusY = (endY + sizeY) - 1

        // Inside the object footprint
        if (startX in endX..radiusX && startY in endY..radiusY) {
            return true
        }

        // WEST face (looking from West to East)
        if (startX == endX - 1 && startY in endY..radiusY) {
            if ((CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_EAST) == 0 && (packedDirections and 0x8) == 0) {
                return true
            }
        }
        // EAST face (looking from East to West)
        if (startX == radiusX + 1 && startY in endY..radiusY) {
            if ((CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_WEST) == 0 && (packedDirections and 0x2) == 0) {
                return true
            }
        }
        // SOUTH face (looking from South to North)
        if (startY == endY - 1 && startX in endX..radiusX) {
            if ((CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_NORTH) == 0 && (packedDirections and 0x4) == 0) {
                return true
            }
        }
        // NORTH face (looking from North to South)
        if (startY == radiusY + 1 && startX in endX..radiusX) {
            if ((CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_SOUTH) == 0 && (packedDirections and 0x1) == 0) {
                return true
            }
        }

        return false
    }

    private fun reachedWall(start: Position, wall: WorldObject): Boolean {
        val startX = start.x
        val startY = start.y
        val z = start.z
        val endX = wall.x
        val endY = wall.y
        val rotation = wall.face and 0x3

        if (startX == endX && startY == endY) return true

        if (wall.type == 0) {
            when (rotation) {
                0 -> { // WEST
                    if (startX == endX - 1 && startY == endY) return true
                    if (startX == endX && startY == endY + 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_SOUTH) == 0) return true
                    if (startX == endX && startY == endY - 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_NORTH) == 0) return true
                }
                1 -> { // NORTH
                    if (startX == endX && startY == endY + 1) return true
                    if (startX == endX - 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_EAST) == 0) return true
                    if (startX == endX + 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_WEST) == 0) return true
                }
                2 -> { // EAST
                    if (startX == endX + 1 && startY == endY) return true
                    if (startX == endX && startY == endY + 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_SOUTH) == 0) return true
                    if (startX == endX && startY == endY - 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_NORTH) == 0) return true
                }
                3 -> { // SOUTH
                    if (startX == endX && startY == endY - 1) return true
                    if (startX == endX - 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_EAST) == 0) return true
                    if (startX == endX + 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_WEST) == 0) return true
                }
            }
        } else if (wall.type == 2) {
            when (rotation) {
                0 -> {
                    if (startX == endX - 1 && startY == endY) return true
                    if (startX == endX && startY == endY + 1) return true
                    if (startX == endX + 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_WEST) == 0) return true
                    if (startX == endX && startY == endY - 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_NORTH) == 0) return true
                }
                1 -> {
                    if (startX == endX - 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_EAST) == 0) return true
                    if (startX == endX && startY == endY + 1) return true
                    if (startX == endX + 1 && startY == endY) return true
                    if (startX == endX && startY == endY - 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_NORTH) == 0) return true
                }
                2 -> {
                    if (startX == endX - 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_EAST) == 0) return true
                    if (startX == endX && startY == endY + 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_SOUTH) == 0) return true
                    if (startX == endX + 1 && startY == endY) return true
                    if (startX == endX && startY == endY - 1) return true
                }
                3 -> {
                    if (startX == endX - 1 && startY == endY) return true
                    if (startX == endX && startY == endY + 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_SOUTH) == 0) return true
                    if (startX == endX + 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_WEST) == 0) return true
                    if (startX == endX && startY == endY - 1) return true
                }
            }
        } else if (wall.type == 9) {
            if (startX == endX && startY == endY + 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_SOUTH) == 0) return true
            if (startX == endX && startY == endY - 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_NORTH) == 0) return true
            if (startX == endX - 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_EAST) == 0) return true
            if (startX == endX + 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_WEST) == 0) return true
        }

        return false
    }

    private fun reachedDecoration(start: Position, decoration: WorldObject): Boolean {
        val startX = start.x
        val startY = start.y
        val z = start.z
        val endX = decoration.x
        val endY = decoration.y
        val type = decoration.type
        var rotation = decoration.face and 0x3

        if (startX == endX && startY == endY) return true

        if (type == 6 || type == 7) {
            if (type == 7) rotation = (rotation + 2) and 3
            when (rotation) {
                0 -> {
                    if (startX == endX + 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_WEST) == 0) return true
                    if (startX == endX && startY == endY - 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_NORTH) == 0) return true
                }
                1 -> {
                    if (startX == endX - 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_EAST) == 0) return true
                    if (startX == endX && startY == endY - 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_NORTH) == 0) return true
                }
                2 -> {
                    if (startX == endX - 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_EAST) == 0) return true
                    if (startX == endX && startY == endY + 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_SOUTH) == 0) return true
                }
                3 -> {
                    if (startX == endX + 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_WEST) == 0) return true
                    if (startX == endX && startY == endY + 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_SOUTH) == 0) return true
                }
            }
        }
        if (type == 8) {
            if (startX == endX && startY == endY + 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_SOUTH) == 0) return true
            if (startX == endX && startY == endY - 1 && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_NORTH) == 0) return true
            if (startX == endX - 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_EAST) == 0) return true
            if (startX == endX + 1 && startY == endY && (CollisionManager.global().getFlags(startX, startY, z) and CollisionFlag.MOB_WEST) == 0) return true
        }
        return false
    }
}
