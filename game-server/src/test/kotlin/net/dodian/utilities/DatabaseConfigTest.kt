package net.dodian.utilities

import com.zaxxer.hikari.HikariConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DatabaseConfigTest {
    @Test
    fun `database jdbc url is built from configured host port and name`() {
        assertEquals(
            "jdbc:mysql://localhost:3306/Server?serverTimezone=UTC&autoReconnect=true",
            buildDatabaseJdbcUrl("localhost", 3306, "Server"),
        )
        assertTrue(buildDatabaseJdbcUrl("localhost", 3306, "Server").startsWith("jdbc:mysql://"))
    }

    @Test
    fun `database config accepts blank password while keeping jdbc url populated`() {
        val config =
            HikariConfig().apply {
                jdbcUrl = buildDatabaseJdbcUrl("localhost", 3306, "Server")
                username = "root"
                password = ""
                driverClassName = "com.mysql.cj.jdbc.Driver"
            }

        assertEquals(buildDatabaseJdbcUrl("localhost", 3306, "Server"), config.jdbcUrl)
        assertEquals("root", config.username)
        assertNotNull(config.password)
    }

    @Test
    fun `sanitized jdbc target omits credentials and remains non blank`() {
        assertEquals("mysql://localhost:3306/Server", sanitizedJdbcTarget("localhost", 3306, "Server"))
        assertTrue(sanitizedJdbcTarget("localhost", 3306, "Server").isNotBlank())
    }

    @Test
    fun `database config validation rejects blank required values`() {
        try {
            validateDatabaseConfig("", "Server", "root")
            throw AssertionError("Expected validation to fail for blank host")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("DATABASE_HOST"))
        }
    }
}
