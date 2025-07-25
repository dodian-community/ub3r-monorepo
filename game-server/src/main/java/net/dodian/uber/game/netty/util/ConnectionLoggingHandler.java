package net.dodian.uber.game.netty.util;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs basic connection lifecycle events and outbound writes for debugging.
 */
public class ConnectionLoggingHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionLoggingHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("[Netty] CONNECT {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("[Netty] DISCONNECT {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("[Netty] Exception from {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        int size = (msg instanceof io.netty.buffer.ByteBuf) ? ((io.netty.buffer.ByteBuf) msg).readableBytes() : -1;
        logger.trace("[Netty] WRITE {} bytes to {}", size, ctx.channel().remoteAddress());
        super.write(ctx, msg, promise);
    }
}

