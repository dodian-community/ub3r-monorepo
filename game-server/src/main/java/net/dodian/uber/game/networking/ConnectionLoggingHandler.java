package net.dodian.uber.game.networking;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Sharable
public class ConnectionLoggingHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionLoggingHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("[Netty] Channel active: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("[Netty] Channel inactive: {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        // Discard any incoming data for now.
        msg.skipBytes(msg.readableBytes());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.debug("[Netty] Exception on channel {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }
}
