package net.dodian.uber.net.codec.update

import io.netty.buffer.ByteBuf
import org.apollo.cache.FileDescriptor

data class OnDemandResponse(
    val descriptor: FileDescriptor,
    val fileSize: Int,
    val chunkId: Int,
    val chunkData: ByteBuf,
)