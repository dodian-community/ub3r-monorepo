package net.dodian.uber.net.codec.jaggrab

import io.netty.buffer.ByteBuf

data class JagGrabResponse(
    val fileData: ByteBuf
)