@file:Suppress("unused")

package net.dodian.uber.game.engine.util

import net.dodian.cache.`object`.GameObjectDef
import net.dodian.uber.game.model.Position
import net.dodian.utilities.Formatting
import net.dodian.utilities.Geometry
import net.dodian.utilities.Randoms

object Misc {
    @JvmStatic
    fun random(range: Int): Int = Randoms.random(range)

    @JvmStatic
    fun chance(range: Int): Int = Randoms.chance(range)

    @JvmStatic
    fun format(num: Int): String = Formatting.format(num)

    @JvmStatic
    fun goodDistanceObject(
        objectX: Int,
        objectY: Int,
        playerX: Int,
        playerY: Int,
        objectXSize: Int,
        objectYSize: Int,
        z: Int,
    ): Position? = Geometry.goodDistanceObject(objectX, objectY, playerX, playerY, objectXSize, objectYSize, z)

    @JvmStatic
    fun goodDistanceObject(
        objectX: Int,
        objectY: Int,
        playerX: Int,
        playerY: Int,
        distance: Int,
        z: Int,
    ): Position? = Geometry.goodDistanceObject(objectX, objectY, playerX, playerY, distance, z)

    @JvmStatic
    fun delta(
        a: Position,
        b: Position,
    ): Position = Geometry.delta(a, b)

    @JvmStatic
    fun getObject(
        objectId: Int,
        x: Int,
        y: Int,
        h: Int,
    ): GameObjectDef? = Geometry.getObject(objectId, x, y, h)
}

