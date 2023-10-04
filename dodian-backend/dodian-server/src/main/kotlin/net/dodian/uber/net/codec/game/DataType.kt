package net.dodian.uber.net.codec.game

enum class DataType(val bytes: Int) {
    BYTE(1),
    SHORT(2),
    TRI_BYTE(3),
    INT(4),
    LONG(8)
}