package net.dodian.utilities

object Utils {
    @JvmField val playerNameXlateTable: CharArray = Names.playerNameXlateTable
    @JvmField val directionDeltaX: ByteArray = Direction.directionDeltaX
    @JvmField val directionDeltaY: ByteArray = Direction.directionDeltaY
    @JvmField val xlateDirectionToClient: ByteArray = Direction.xlateDirectionToClient
    @JvmField val xlateTable: CharArray = TextCodec.xlateTable

    @JvmStatic
    fun longToPlayerName(l: Long): String = Names.longToPlayerName(l)

    @JvmStatic
    fun println(str: String) = Formatting.println(str)

    @JvmStatic
    fun HexToInt(
        data: ByteArray,
        offset: Int,
        len: Int,
    ): Int = TextCodec.hexToInt(data, offset, len)

    @JvmStatic
    fun random(range: Int): Int = Randoms.random(range)

    @JvmStatic
    fun dRandom2(range: Double): Double = Randoms.dRandom2(range)

    @JvmStatic
    fun playerNameToInt64(s: String): Long = Names.playerNameToInt64(s)

    @JvmStatic
    fun playerNameToLong(s: String): Long = Names.playerNameToLong(s)

    @JvmStatic
    fun textUnpack(
        packedData: ByteArray,
        size: Int,
    ): String = TextCodec.textUnpack(packedData, size)

    @JvmStatic
    fun direction(
        srcX: Int,
        srcY: Int,
        destX: Int,
        destY: Int,
    ): Int = Direction.direction(srcX, srcY, destX, destY)

    @JvmStatic
    fun format(num: Int): String = Formatting.format(num)

    @JvmStatic
    fun getDistance(
        coordX1: Int,
        coordY1: Int,
        coordX2: Int,
        coordY2: Int,
    ): Int = Geometry.getDistance(coordX1, coordY1, coordX2, coordY2)

    @JvmStatic
    fun println_debug(message: String) = Formatting.printlnDebug(message)

    @JvmStatic
    fun capitalize(str: String?): String? = Formatting.capitalize(str)
}
