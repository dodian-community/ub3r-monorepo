package net.dodian.uber.game.persistence.repository

import java.sql.PreparedStatement
import java.sql.ResultSet

fun PreparedStatement.bindInt(index: Int, value: Int): PreparedStatement = apply { setInt(index, value) }

fun PreparedStatement.bindLong(index: Int, value: Long): PreparedStatement = apply { setLong(index, value) }

fun PreparedStatement.bindString(index: Int, value: String): PreparedStatement = apply { setString(index, value) }

inline fun <T> ResultSet.mapFirstOrNull(mapper: (ResultSet) -> T): T? {
    if (!next()) {
        return null
    }
    return mapper(this)
}

