package net.dodian.uber.game.session

import net.dodian.utilities.NameUtil

data class PlayerCredentials(
    val uid: Int,
    val username: String,
    val usernameHash: Int,
    val hostAddress: String,
    var password: String,
    private val encodedUsername: Long,
) {
    val usernameEncoded get() = encodedUsername
}

fun createPlayerCredentials(
    username: String,
    password: String,
    usernameHash: Int,
    uid: Int,
    hostAddress: String
) = PlayerCredentials(
    uid,
    username,
    usernameHash,
    hostAddress,
    password,
    NameUtil.encodeBase37(username)
)