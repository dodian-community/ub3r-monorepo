package net.dodian.uber.net.message

import net.dodian.uber.net.protocol.decoders.KeepAliveDecoder

class MessageDecoderList(
    private val decoders: MutableMap<Int, MessageDecoder<*>> = mutableMapOf(
        0 to KeepAliveDecoder()
    )
) : MutableMap<Int, MessageDecoder<*>> by decoders