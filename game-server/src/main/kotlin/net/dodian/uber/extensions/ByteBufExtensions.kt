package net.dodian.uber.extensions

import io.netty.buffer.ByteBuf
import java.lang.StringBuilder

const val STRING_TERMINATOR = 10

fun ByteBuf.readString(): String {
    val builder = StringBuilder()

    while (isReadable) {
        val character = readUnsignedByte().toInt()
        if (character == STRING_TERMINATOR)
            continue

        builder.append(character.toChar())
    }

    return builder.toString()
}