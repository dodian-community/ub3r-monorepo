package net.dodian.uber.net.codec.login

data class LoginResponse(
    val flagged: Boolean,
    val rights: Int,
    val status: Int
) {
    val isFlagged get() = flagged
}