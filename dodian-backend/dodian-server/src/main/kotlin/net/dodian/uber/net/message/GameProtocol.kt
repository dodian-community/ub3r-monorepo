package net.dodian.uber.net.message

import net.dodian.uber.net.message.meta.PacketMetadataGroup

data class GameProtocol(
    val decoders: MessageDecoderList = MessageDecoderList(),
    val encoders: MessageEncoderList = MessageEncoderList(),
    val metadata: PacketMetadataGroup = PacketMetadataGroup()
)