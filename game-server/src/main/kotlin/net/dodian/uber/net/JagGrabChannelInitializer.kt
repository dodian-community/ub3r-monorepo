package net.dodian.uber.net

import com.google.common.base.Charsets
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.timeout.IdleStateHandler
import net.dodian.uber.net.codec.jaggrab.JagGrabRequestDecoder
import net.dodian.uber.net.codec.jaggrab.JagGrabResponseEncoder

private val DOUBLE_LINE_FEED_DELIMITER = Unpooled.buffer(2)
private const val MAX_REQUEST_LENGTH = 8192
private val JAGGRAB_CHARSET = Charsets.US_ASCII

class JagGrabChannelInitializer(
    private val handler: ChannelInboundHandlerAdapter
) : ChannelInitializer<SocketChannel>() {

	init {
		DOUBLE_LINE_FEED_DELIMITER.writeByte(10).writeByte(10);
	}

    override fun initChannel(ch: SocketChannel): Unit = with(ch.pipeline()) {
        addLast("farmer", DelimiterBasedFrameDecoder(MAX_REQUEST_LENGTH, DOUBLE_LINE_FEED_DELIMITER))
        addLast("string-decoder", StringDecoder(JAGGRAB_CHARSET))
        addLast("jaggrab-decoder", JagGrabRequestDecoder())

        addLast("jaggrab-encoder", JagGrabResponseEncoder())

        addLast("timeout", IdleStateHandler(IDLE_TIME, 0, 0))
        addLast("handler", handler)
    }
}