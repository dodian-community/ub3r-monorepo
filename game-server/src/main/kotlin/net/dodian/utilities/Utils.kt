package net.dodian.utilities

object Utils {
    @JvmField val playerNameXlateTable: CharArray = UtilityNames.playerNameXlateTable
    @JvmField val directionDeltaX: ByteArray = UtilityDirection.directionDeltaX
    @JvmField val directionDeltaY: ByteArray = UtilityDirection.directionDeltaY
    @JvmField val xlateDirectionToClient: ByteArray = UtilityDirection.xlateDirectionToClient
    @JvmField val xlateTable: CharArray = UtilityText.xlateTable

    @JvmStatic
    fun longToPlayerName(l: Long): String = UtilityNames.longToPlayerName(l)

    @JvmStatic
    fun println(str: String) = UtilityFormatting.println(str)

    @JvmStatic
    fun HexToInt(
        data: ByteArray,
        offset: Int,
        len: Int,
    ): Int = UtilityText.hexToInt(data, offset, len)

    @JvmStatic
    fun random(range: Int): Int = UtilityRandoms.random(range)

    @JvmStatic
    fun dRandom2(range: Double): Double = UtilityRandoms.dRandom2(range)

    @JvmStatic
    fun playerNameToInt64(s: String): Long = UtilityNames.playerNameToInt64(s)

    @JvmStatic
    fun playerNameToLong(s: String): Long = UtilityNames.playerNameToLong(s)

    @JvmStatic
    fun textUnpack(
        packedData: ByteArray,
        size: Int,
    ): String = UtilityText.textUnpack(packedData, size)

    @JvmStatic
    fun direction(
        srcX: Int,
        srcY: Int,
        destX: Int,
        destY: Int,
    ): Int = UtilityDirection.direction(srcX, srcY, destX, destY)

    @JvmStatic
    fun format(num: Int): String = UtilityFormatting.format(num)

    @JvmStatic
    fun getDistance(
        coordX1: Int,
        coordY1: Int,
        coordX2: Int,
        coordY2: Int,
    ): Int = UtilityGeometry.getDistance(coordX1, coordY1, coordX2, coordY2)

    @JvmStatic
    fun println_debug(message: String) = UtilityFormatting.printlnDebug(message)

    @JvmStatic
    fun capitalize(str: String?): String? = UtilityFormatting.capitalize(str)
}
