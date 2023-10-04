package net.dodian.uber.net.message

import net.dodian.uber.net.protocol.decoders.ButtonMessageDecoder
import net.dodian.uber.net.protocol.decoders.KeepAliveMessageDecoder
import net.dodian.uber.net.protocol.decoders.MouseClickedMessageDecoder
import net.dodian.uber.net.protocol.decoders.WalkMessageDecoder

class MessageDecoderList(
    private val decoders: MutableMap<Int, MessageDecoder<*>> = mutableMapOf(
        0 to KeepAliveMessageDecoder(),

        98 to WalkMessageDecoder(),
        164 to WalkMessageDecoder(),
        248 to WalkMessageDecoder(),

        241 to MouseClickedMessageDecoder(),

        185 to ButtonMessageDecoder()
    )
) : MutableMap<Int, MessageDecoder<*>> by decoders