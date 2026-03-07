package net.dodian.utilities

internal fun buildDatabaseJdbcUrl(
    host: String,
    port: Int,
    databaseName: String,
): String = "jdbc:mysql://$host:$port/$databaseName?serverTimezone=UTC&autoReconnect=true"

internal fun validateDatabaseConfig(
    host: String,
    databaseName: String,
    username: String,
) {
    require(host.isNotBlank()) { "DATABASE_HOST must not be blank" }
    require(databaseName.isNotBlank()) { "DATABASE_NAME must not be blank" }
    require(username.isNotBlank()) { "DATABASE_USERNAME must not be blank" }
}

internal fun sanitizedJdbcTarget(
    host: String,
    port: Int,
    databaseName: String,
): String = "mysql://$host:$port/$databaseName"
