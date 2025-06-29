package net.dodian.uber.game.networking.login;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class LoginHandshakeDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(LoginHandshakeDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int readable = in.readableBytes();
        if (readable == 0) {
            return; // nothing to forward yet
        }

        // Copy the readable bytes to a new buffer and forward downstream.
        ByteBuf slice = in.readBytes(readable);
        out.add(slice);

        logger.trace("[Netty] Forwarded {} bytes from handshake stage for {}", readable, ctx.channel().remoteAddress());
    }
}
