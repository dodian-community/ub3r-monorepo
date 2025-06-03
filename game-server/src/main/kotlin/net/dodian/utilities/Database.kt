package net.dodian.utilities

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement

private val jdbcUrl = "jdbc:mysql://$databaseHost:$databasePort/$databaseName?serverTimezone=UTC&autoReconnect=true"


private var _dbConnection: Connection? = null


val dbConnection: Connection
    get() {
        val conn = _dbConnection
        return if (conn != null && conn.isValid(1)) {
            conn
        } else {
            when {
                conn == null -> println("[DB] Creating initial database connection...")
                !conn.isValid(1) -> println("[DB] Connection invalid, creating new connection...")
                else -> println("[DB] Connection state unknown, renewing...")
            }

            try {
                conn?.close()
                if (conn != null) {
                    println("[DB] Old connection closed")
                }
            } catch (e: SQLException) {
                println("[DB] Error closing old connection: ${e.message}")
            }
            
            // Create new connection
            try {
                connect().also { 
                    _dbConnection = it
                    println("[DB] New connection established successfully")
                }
            } catch (e: Exception) {
                println("[DB] Failed to create new connection: ${e.message}")
                throw e
            }
        }
    }


val dbStatement: Statement
    get() = dbConnection.createStatement()

private fun connect(): Connection {
    println("[DB] Attempting to connect to database...")
    return DriverManager.getConnection(jdbcUrl, databaseUsername, databasePassword).apply {
        autoCommit = true
        setNetworkTimeout(Runnable::run, 30000) // 30 second timeout
        println("[DB] Connected to ${metaData.databaseProductName} ${metaData.databaseProductVersion}")
    }
}

enum class DbTables(val table: String) {
    WEB_USERS_TABLE("user"),
    WEB_THREAD("thread"),

    GAME_PLAYER_SESSIONS("uber3_sessions"),

    GAME_LOGS("uber3_log"),

    GAME_LOGS_PLAYER("uber3_logs"),
    GAME_LOGS_PLAYER_TRADES("uber3_trades"),
    GAME_LOGS_PLAYER_DUELS("duel_log"),
    GAME_LOGS_ITEMS("uber3_item_log"),
    GAME_CHAT_LOGS("uber3_chat_log"),
    GAME_LOGS_STAFF_COMMANDS("uber3_command_log"),

    GAME_CHARACTERS("characters"),
    GAME_CHARACTERS_STATS("character_stats"),
    GAME_CHARACTERS_STATS_PROGRESS("character_stats_progress"),

    GAME_DOOR_DEFINITIONS("uber3_doors"),
    GAME_ITEM_DEFINITIONS("uber3_items"),
    GAME_NPC_DEFINITIONS("uber3_npcs"),
    GAME_OBJECT_DEFINITIONS("uber3_objects"),

    GAME_REFUND_ITEMS("uber3_refunds"),

    GAME_NPC_SPAWNS("uber3_spawn"),
    GAME_NPC_DROPS("uber3_drops"),

    GAME_PETE_CO("pete_co"),
    GAME_WORLDS("worlds")

    ;

    override fun toString() = databaseTablePrefix + this.table
}