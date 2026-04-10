package net.dodian.utilities

import org.slf4j.LoggerFactory
import java.text.NumberFormat

object Formatting {
    private val logger = LoggerFactory.getLogger(Formatting::class.java)

    @JvmStatic
    fun format(number: Int): String = NumberFormat.getInstance().format(number)

    @JvmStatic
    fun println(str: String) {
        logger.info(str)
    }

    @JvmStatic
    fun printlnDebug(message: String) {
        logger.debug(message)
    }

    @JvmStatic
    fun capitalize(str: String?): String? {
        if (str.isNullOrEmpty()) {
            return str
        }
        return str.substring(0, 1).uppercase() + str.substring(1).lowercase()
    }
}
