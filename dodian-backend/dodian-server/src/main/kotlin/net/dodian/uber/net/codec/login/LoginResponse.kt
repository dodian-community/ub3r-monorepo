package net.dodian.uber.net.codec.login

data class LoginResponse(
    val status: Int,
    val rights: Int,
    val flagged: Boolean
) {
    val isFlagged get() = flagged
}