package net.dodian.uber.game.model

import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter
import net.dodian.utilities.DbTables
import net.dodian.utilities.dbConnection
import net.dodian.utilities.gameWorldId

class Login {
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
            dbConnection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(
                        "INSERT INTO ${DbTables.GAME_PLAYER_SESSIONS} SET dbid='$dbId', client='$clientPid', duration='$elapsed', " +
                            "hostname='$connectedFrom',start='$start',end='$end',world='$gameWorldId'",
                    )
                }
            }
        } catch (exception: Exception) {
            println(exception.message)
            exception.printStackTrace()
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
                    println("Could not initialize UUID ban file.")
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
                exception.printStackTrace()
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
                exception.printStackTrace()
            }
        }
    }
}
