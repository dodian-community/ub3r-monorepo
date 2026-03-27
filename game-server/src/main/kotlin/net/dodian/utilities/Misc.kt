package net.dodian.utilities

import net.dodian.cache.object.GameObjectDef
import net.dodian.uber.game.model.Position

object Misc {
    @JvmStatic
    fun random(range: Int): Int = UtilityRandoms.random(range)

    @JvmStatic
    fun chance(range: Int): Int = UtilityRandoms.chance(range)

    @JvmStatic
    fun format(num: Int): String = UtilityFormatting.format(num)

    @JvmStatic
    fun goodDistanceObject(
        objectX: Int,
        objectY: Int,
        playerX: Int,
        playerY: Int,
        objectXSize: Int,
        objectYSize: Int,
        z: Int,
    ): Position? = UtilityGeometry.goodDistanceObject(objectX, objectY, playerX, playerY, objectXSize, objectYSize, z)

    @JvmStatic
    fun goodDistanceObject(
        objectX: Int,
        objectY: Int,
        playerX: Int,
        playerY: Int,
        distance: Int,
        z: Int,
    ): Position? = UtilityGeometry.goodDistanceObject(objectX, objectY, playerX, playerY, distance, z)

    @JvmStatic
    fun delta(
        a: Position,
        b: Position,
    ): Position = UtilityGeometry.delta(a, b)

    @JvmStatic
    fun getObject(
        objectId: Int,
        x: Int,
        y: Int,
        h: Int,
    ): GameObjectDef? = UtilityGeometry.getObject(objectId, x, y, h)
}
