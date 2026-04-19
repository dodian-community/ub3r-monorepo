package net.dodian.utilities

import net.dodian.uber.game.engine.util.Utils as EngineUtils

object Utils {
    @JvmField val playerNameXlateTable: CharArray = EngineUtils.playerNameXlateTable
    @JvmField val directionDeltaX: ByteArray = EngineUtils.directionDeltaX
    @JvmField val directionDeltaY: ByteArray = EngineUtils.directionDeltaY
    @JvmField val xlateDirectionToClient: ByteArray = EngineUtils.xlateDirectionToClient
    @JvmField val xlateTable: CharArray = EngineUtils.xlateTable

    @JvmStatic
    fun longToPlayerName(l: Long): String = EngineUtils.longToPlayerName(l)

    @JvmStatic
    fun println(str: String) = EngineUtils.println(str)

    @JvmStatic
    fun HexToInt(
        data: ByteArray,
        offset: Int,
        len: Int,
    ): Int = EngineUtils.HexToInt(data, offset, len)

    @JvmStatic
    fun random(range: Int): Int = EngineUtils.random(range)

    @JvmStatic
    fun dRandom2(range: Double): Double = EngineUtils.dRandom2(range)

    @JvmStatic
    fun playerNameToInt64(s: String): Long = EngineUtils.playerNameToInt64(s)

    @JvmStatic
    fun playerNameToLong(s: String): Long = EngineUtils.playerNameToLong(s)

    @JvmStatic
    fun textUnpack(
        packedData: ByteArray,
        size: Int,
    ): String = EngineUtils.textUnpack(packedData, size)

    @JvmStatic
    fun direction(
        srcX: Int,
        srcY: Int,
        destX: Int,
        destY: Int,
    ): Int = EngineUtils.direction(srcX, srcY, destX, destY)

    @JvmStatic
    fun format(num: Int): String = EngineUtils.format(num)

    @JvmStatic
    fun getDistance(
        coordX1: Int,
        coordY1: Int,
        coordX2: Int,
        coordY2: Int,
    ): Int = EngineUtils.getDistance(coordX1, coordY1, coordX2, coordY2)

    @JvmStatic
    fun println_debug(message: String) = EngineUtils.println_debug(message)

    @JvmStatic
    fun capitalize(str: String?): String? = EngineUtils.capitalize(str)
}
