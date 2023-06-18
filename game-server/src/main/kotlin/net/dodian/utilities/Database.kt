package net.dodian.utilities

import net.dodian.config.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

val jdbcUrl = "jdbc:mysql://$dbHost:$dbPort/$dbName?serverTimezone=UTC"

val dbConnection: Connection = connect()

val dbStatement: Statement = dbConnection.createStatement()

private fun connect(): Connection {
    val con = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)
    if (!con.isValid(0))
        error("Failed to connect to database")

    return con;
}

enum class DbTables(val table: String) {
    WEB_USERS_TABLE("user"),
    WEB_THREAD("thread"),

    GAME_PLAYER_SESSIONS("uber3_sessions"),

    GAME_LOGS("uber3_log"),

    GAME_LOGS_PLAYER("uber3_logs"),
    GAME_LOGS_PLAYER_TRADES("uber3_trades"),
    GAME_LOGS_PLAYER_PRIVATE_CHAT("pm_log"),
    GAME_LOGS_PLAYER_PUBLIC_CHAT("chat_log"),
    GAME_LOGS_PLAYER_ITEM_PICKUPS("pickup_log"),
    GAME_LOGS_PLAYER_DUELS("duel_log"),

    GAME_LOGS_ITEM_DROPS("drop_log"),
    GAME_LOGS_STAFF_COMMANDS("uber3_command_log"),

    GAME_CHARACTERS("characters"),
    GAME_CHARACTERS_STATS("character_stats"),
    GAME_CHARACTERS_STATS_PROGRESS("character_stats_progress"),

    GAME_ACTIONS("uber3_actions"),

    GAME_DOOR_DEFINITIONS("uber3_doors"),
    GAME_ITEM_DEFINITIONS("uber3_items"),
    GAME_NPC_DEFINITIONS("uber3_npcs"),
    GAME_OBJECT_DEFINITIONS("uber3_objects"),

    GAME_NPC_SPAWNS("uber3_spawn"),
    GAME_NPC_DROPS("uber3_drops"),
    GAME_MISC("uber3_misc"),
    GAME_REFUNDS("uber3_refunds"),

    GAME_PETE_CO("pete_co"),
    GAME_WORLDS("worlds"),

    ;

    override fun toString() = dbTablePrefix + this.table
}