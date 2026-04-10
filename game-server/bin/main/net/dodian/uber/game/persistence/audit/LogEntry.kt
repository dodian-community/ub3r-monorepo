package net.dodian.uber.game.persistence.audit

import java.text.SimpleDateFormat
import java.util.Date

object LogEntry {
    private const val TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss"

    @JvmStatic
    fun getTimeStamp(): String = SimpleDateFormat(TIMESTAMP_PATTERN).format(Date())
}
