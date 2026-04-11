package net.dodian.utilities

import net.dodian.cache.`object`.GameObjectDef
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.engine.util.Misc as EngineMisc

object Misc {
    @JvmStatic
    fun random(range: Int): Int = EngineMisc.random(range)

    @JvmStatic
    fun chance(range: Int): Int = EngineMisc.chance(range)

    @JvmStatic
    fun format(num: Int): String = EngineMisc.format(num)

    @JvmStatic
    fun goodDistanceObject(
        objectX: Int,
        objectY: Int,
        playerX: Int,
        playerY: Int,
        objectXSize: Int,
        objectYSize: Int,
        z: Int,
    ): Position? = EngineMisc.goodDistanceObject(objectX, objectY, playerX, playerY, objectXSize, objectYSize, z)

    @JvmStatic
    fun goodDistanceObject(
        objectX: Int,
        objectY: Int,
        playerX: Int,
        playerY: Int,
        distance: Int,
        z: Int,
    ): Position? = EngineMisc.goodDistanceObject(objectX, objectY, playerX, playerY, distance, z)

    @JvmStatic
    fun delta(
        a: Position,
        b: Position,
    ): Position = EngineMisc.delta(a, b)

    @JvmStatic
    fun getObject(
        objectId: Int,
        x: Int,
        y: Int,
        h: Int,
    ): GameObjectDef? = EngineMisc.getObject(objectId, x, y, h)
}
