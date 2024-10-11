package net.dodian.database

import net.dodian.utilities.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

val dbClient = DatabaseClient()

class DatabaseClient {
    private val jdbcUrl = "jdbc:mysql://$databaseHost:$databasePort/$databaseName?serverTimezone=UTC"

    private var con: Connection = DriverManager.getConnection(jdbcUrl, databaseUsername, databasePassword)

    init {
        connect()
    }

    private fun connect() {
        if (!con.isValid(1))
            con = DriverManager.getConnection(jdbcUrl, databaseUsername, databasePassword)
    }

    val connection: Connection get() = con

    fun <T> executeQuery(query: String, withResult: ((ResultSet) -> T)? = null): T? {
        if (!con.isValid(1))
            connect()

        val statement = con.createStatement()
        val result = statement.executeQuery(query)

        val finalValue = if (result.next())
            withResult?.invoke(result)
        else null

        if (!statement.isClosed)
            statement.close()

        if (!result.isClosed)
            result.close()

        return finalValue
    }
}