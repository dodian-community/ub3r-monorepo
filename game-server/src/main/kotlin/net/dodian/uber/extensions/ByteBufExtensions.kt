package net.dodian.uber.extensions

import io.netty.buffer.ByteBuf

const val STRING_TERMINATOR = 10

fun ByteBuf.readString(): String {
    val builder = StringBuilder()

    var character: Int = -1
    while (isReadable && readUnsignedByte().toInt().also { character = it } != STRING_TERMINATOR)
        builder.append(character.toChar())

    return builder.toString()
}