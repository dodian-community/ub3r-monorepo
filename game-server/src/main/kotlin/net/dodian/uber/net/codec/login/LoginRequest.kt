package net.dodian.uber.net.codec.login

import net.dodian.uber.game.session.PlayerCredentials
import net.dodian.utilities.security.IsaacRandomPair

data class LoginRequest(
    val credentials: PlayerCredentials,
    val randomPair: IsaacRandomPair,
    val lowMemory: Boolean,
    val reconnecting: Boolean,
    val releaseNumber: Int,
    val archiveCrcs: List<Int>,
    val clientVersion: Int
)