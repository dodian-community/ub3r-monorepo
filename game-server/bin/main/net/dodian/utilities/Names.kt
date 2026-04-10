package net.dodian.utilities

object Names {
    @JvmField
    val playerNameXlateTable: CharArray =
        charArrayOf(
            '_',
            'a',
            'b',
            'c',
            'd',
            'e',
            'f',
            'g',
            'h',
            'i',
            'j',
            'k',
            'l',
            'm',
            'n',
            'o',
            'p',
            'q',
            'r',
            's',
            't',
            'u',
            'v',
            'w',
            'x',
            'y',
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
        )

    @JvmStatic
    fun longToPlayerName(value: Long): String {
        var l = value
        var i = 0
        val ac = CharArray(12)
        while (l != 0L) {
            val l1 = l
            l /= 37L
            ac[11 - i++] = playerNameXlateTable[(l1 - l * 37L).toInt()]
        }
        return String(ac, 12 - i, i)
    }

    @JvmStatic
    fun playerNameToInt64(s: String): Long {
        var l = 0L
        var i = 0
        while (i < s.length && i < 12) {
            val c = s[i]
            l *= 37L
            if (c in 'A'..'Z') {
                l += (1 + c.code) - 65
            } else if (c in 'a'..'z') {
                l += (1 + c.code) - 97
            } else if (c in '0'..'9') {
                l += (27 + c.code) - 48
            }
            i++
        }
        while (l % 37L == 0L && l != 0L) {
            l /= 37L
        }
        return l
    }

    @JvmStatic
    fun playerNameToLong(s: String): Long = playerNameToInt64(s)
}
