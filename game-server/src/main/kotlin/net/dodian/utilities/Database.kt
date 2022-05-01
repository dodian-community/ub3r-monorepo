package net.dodian.utilities

import net.dodian.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

val jdbcUrl = "jdbc:mysql://${databaseHost}:${databasePort}/${databaseName}?serverTimezone=UTC"

val dbConnection: Connection = connect()

val dbStatement: Statement = dbConnection.createStatement()

private fun connect(): Connection {
    val con = DriverManager.getConnection(jdbcUrl, databaseUsername, databasePassword)
    if (!con.isValid(0))
        error("Failed to connect to database...")

    return con;
}