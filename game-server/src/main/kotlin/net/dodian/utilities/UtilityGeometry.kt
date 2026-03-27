package net.dodian.utilities

import net.dodian.cache.`object`.CacheObject
import net.dodian.cache.`object`.GameObjectDef
import net.dodian.cache.`object`.ObjectLoader
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.`object`.DoorHandler

object UtilityGeometry {
    @JvmStatic
    fun goodDistance(
        objectX: Int,
        objectY: Int,
        playerX: Int,
        playerY: Int,
        distance: Int,
    ): Boolean {
        val deltaX = objectX - playerX
        val deltaY = objectY - playerY
        val trueDistance = kotlin.math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toInt()
        return trueDistance <= distance
    }

    @JvmStatic
    fun goodDistanceObject(
        objectX: Int,
        objectY: Int,
        playerX: Int,
        playerY: Int,
        objectXSize: Int,
        objectYSize: Int,
        z: Int,
    ): Position? {
        if (objectXSize < 1 && objectYSize < 1) {
            if (playerX == objectX && playerY == objectY) {
                return Position(objectX, objectY, z)
            }
            return null
        }
        if (objectXSize == 1 && objectYSize == 1) {
            if (goodDistance(playerX, playerY, objectX, objectY, 1)) {
                return Position(objectX, objectY, z)
            }
        }
        val maxObjX = objectX + objectXSize
        val maxObjY = objectY + objectYSize
        val playerPos = Position(playerX, playerY, z)
        for (x in objectX..maxObjX) {
            for (y in objectY..maxObjY) {
                val pos = Position(x, y, z)
                if (goodDistance(pos.x, pos.y, playerPos.x, playerPos.y, 1) && pos.isPerpendicularTo(playerPos)) {
                    return pos
                }
            }
        }
        return null
    }

    @JvmStatic
    fun goodDistanceObject(
        objectX: Int,
        objectY: Int,
        playerX: Int,
        playerY: Int,
        distance: Int,
        z: Int,
    ): Position? {
        if (goodDistance(playerX, playerY, objectX, objectY, distance)) {
            return Position(objectX, objectY, z)
        }
        return null
    }

    @JvmStatic
    fun delta(
        a: Position,
        b: Position,
    ): Position = Position(b.x - a.x, b.y - a.y)

    @JvmStatic
    fun getDistance(
        coordX1: Int,
        coordY1: Int,
        coordX2: Int,
        coordY2: Int,
    ): Int {
        val deltaX = coordX2 - coordX1
        val deltaY = coordY2 - coordY1
        return kotlin.math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toInt()
    }

    @JvmStatic
    fun getObject(
        objectId: Int,
        x: Int,
        y: Int,
        h: Int,
    ): GameObjectDef? {
        val cached: CacheObject? = ObjectLoader.`object`(objectId, x, y, h)
        if (cached != null) {
            return cached.def
        }
        for (i in DoorHandler.doorId.indices) {
            if (DoorHandler.doorId[i] == objectId && DoorHandler.doorX[i] == x && DoorHandler.doorY[i] == y) {
                return GameObjectDef(objectId, 2, 0, Position(x, y))
            }
        }
        for (obj in Server.objects) {
            if (obj.id == objectId && obj.x == x && obj.y == y) {
                return GameObjectDef(objectId, obj.type, 0, Position(x, y))
            }
        }
        return null
    }
}
