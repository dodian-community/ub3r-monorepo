package net.dodian.utilities

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import org.slf4j.LoggerFactory

object UtilityFormatting {
    private val logger = LoggerFactory.getLogger(UtilityFormatting::class.java)

    @JvmStatic
    fun format(number: Int): String = NumberFormat.getInstance().format(number)

    @JvmStatic
    fun println(str: String) {
        System.out.println(str)
    }

    @JvmStatic
    fun printlnDebug(message: String) {
        if (!clientPacketTraceEnabled && !clientUiTraceEnabled) {
            return
        }
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        if (logger.isDebugEnabled) {
            logger.debug("[{}] {}", timestamp, message)
        }
    }

    @JvmStatic
    fun capitalize(str: String?): String? {
        if (str.isNullOrEmpty()) {
            return str
        }
        return str.substring(0, 1).uppercase() + str.substring(1).lowercase()
    }
}
