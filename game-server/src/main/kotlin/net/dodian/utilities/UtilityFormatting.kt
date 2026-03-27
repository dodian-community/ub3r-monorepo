package net.dodian.utilities

import java.text.NumberFormat

object UtilityFormatting {
    @JvmStatic
    fun format(number: Int): String = NumberFormat.getInstance().format(number)

    @JvmStatic
    fun println(str: String) {
        System.out.println(str)
    }

    @JvmStatic
    fun printlnDebug(message: String) {
        return
    }

    @JvmStatic
    fun capitalize(str: String?): String? {
        if (str.isNullOrEmpty()) {
            return str
        }
        return str.substring(0, 1).uppercase() + str.substring(1).lowercase()
    }
}
