package net.dodian.uber.net.codec.login

import net.dodian.utilities.security.IsaacRandomPair
import net.dodian.utilities.security.PlayerCredentials

data class LoginRequest(
    val credentials: PlayerCredentials,
    val randomPair: IsaacRandomPair,
    val lowMemory: Boolean,
    val isReconnecting: Boolean,
    val releaseNumber: Int,
    val archiveCrcs: List<Int>,
    val clientVersion: Int
)