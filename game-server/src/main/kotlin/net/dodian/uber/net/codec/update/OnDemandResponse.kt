package net.dodian.uber.net.codec.update

import io.netty.buffer.ByteBuf
import net.dodian.uber.cache.FileDescriptor

data class OnDemandResponse(
    val chunkData: ByteBuf,
    val chunkId: Int,
    val descriptor: FileDescriptor,
    val fileSize: Int
)