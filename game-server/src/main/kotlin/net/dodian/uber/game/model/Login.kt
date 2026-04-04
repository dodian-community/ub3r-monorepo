package net.dodian.uber.game.model

import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import net.dodian.uber.game.engine.config.gameWorldId
import org.slf4j.LoggerFactory

class Login {
    private val logger = LoggerFactory.getLogger(Login::class.java)

    @Synchronized
    fun sendSession(
        dbId: Int,
        clientPid: Int,
        elapsed: Int,
        connectedFrom: String,
        start: Long,
        end: Long,
    ) {
        try {
            DbAsyncRepository.withConnection { conn ->
                val query =
                    "INSERT INTO ${DbTables.GAME_PLAYER_SESSIONS} (dbid, client, duration, hostname, start, end, world) VALUES (?, ?, ?, ?, ?, ?, ?)"
                conn.prepareStatement(query).use { statement ->
                    statement.setInt(1, dbId)
                    statement.setInt(2, clientPid)
                    statement.setInt(3, elapsed)
                    statement.setString(4, connectedFrom)
                    statement.setLong(5, start)
                    statement.setLong(6, end)
                    statement.setInt(7, gameWorldId)
                    statement.executeUpdate()
                }
            }
        } catch (exception: Exception) {
            logger.error("Failed to record player session for dbId={}", dbId, exception)
        }
    }

    companion object {
        private const val UUID_BANS_PATH = "./data/UUIDBans.txt"

        @JvmField
        val bannedUid: MutableSet<String> = LinkedHashSet()

        @JvmStatic
        fun isUidBanned(UUID: Array<String>?): Boolean {
            if (UUID == null) {
                return false
            }
            for (value in UUID) {
                if (isUidBanned(value)) {
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun isUidBanned(UUID: String?): Boolean {
            if (UUID.isNullOrBlank()) {
                return false
            }
            return bannedUid.contains(UUID)
        }

        @JvmStatic
        fun banUid() {
            val file = File(UUID_BANS_PATH)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                try {
                    file.createNewFile()
                } catch (exception: Exception) {
                    logger.warn("Could not initialize UUID ban file.", exception)
                }
                return
            }
            try {
                file.forEachLine { line ->
                    val value = line.trim()
                    if (value.isNotEmpty()) {
                        bannedUid.add(value)
                    }
                }
            } catch (exception: Exception) {
                logger.error("Failed reading UUID bans.", exception)
            }
        }

        @JvmStatic
        fun addUidToFile(UUID: Array<String>?) {
            if (UUID == null || UUID.isEmpty()) {
                return
            }
            try {
                File(UUID_BANS_PATH).parentFile?.mkdirs()
                FileWriter(UUID_BANS_PATH, true).use { writer ->
                    for (value in UUID) {
                        if (value.isBlank() || isUidBanned(value)) {
                            continue
                        }
                        bannedUid.add(value)
                        writer.write(value)
                        writer.write(System.lineSeparator())
                    }
                }
            } catch (_: FileNotFoundException) {
                // This file is often absent in local dev; ignore missing-file noise.
            } catch (exception: Exception) {
                logger.error("Failed appending UUID bans.", exception)
            }
        }

        private val logger = LoggerFactory.getLogger(Login::class.java)
    }
}
