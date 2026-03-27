package net.dodian.utilities

object UtilityText {
    private val decodeBuf = CharArray(4096)

    @JvmField
    val xlateTable: CharArray =
        charArrayOf(
            ' ',
            'e',
            't',
            'a',
            'o',
            'i',
            'h',
            'n',
            's',
            'r',
            'd',
            'l',
            'u',
            'm',
            'w',
            'c',
            'y',
            'f',
            'g',
            'p',
            'b',
            'v',
            'k',
            'x',
            'j',
            'q',
            'z',
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9',
            ' ',
            '!',
            '?',
            '.',
            ',',
            ':',
            ';',
            '(',
            ')',
            '-',
            '&',
            '*',
            '\\',
            '\'',
            '@',
            '#',
            '+',
            '=',
            '\u00a3',
            '$',
            '%',
            '"',
            '[',
            ']',
        )

    @JvmStatic
    fun hexToInt(
        data: ByteArray,
        offset: Int,
        len: Int,
    ): Int {
        var temp = 0
        var i = 1000
        for (cntr in 0 until len) {
            val num = (data[offset + cntr].toInt() and 0xFF) * i
            temp += num
            if (i > 1) {
                i /= 1000
            }
        }
        return temp
    }

    @JvmStatic
    fun textUnpack(
        packedData: ByteArray,
        size: Int,
    ): String {
        var idx = 0
        var highNibble = -1
        for (i in 0 until size * 2) {
            val value = packedData[i / 2].toInt() shr (4 - 4 * (i % 2)) and 0xF
            if (highNibble == -1) {
                if (value < 13) {
                    decodeBuf[idx++] = xlateTable[value]
                } else {
                    highNibble = value
                }
            } else {
                decodeBuf[idx++] = xlateTable[(highNibble shl 4) + value - 195]
                highNibble = -1
            }
        }
        return String(decodeBuf, 0, idx)
    }
}
