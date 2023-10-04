package net.dodian.uber.net.codec.login

enum class LoginDecoderState {
    LOGIN_HANDSHAKE,
    LOGIN_HEADER,
    LOGIN_PAYLOAD
}