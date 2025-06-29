package net.dodian.uber.game.networking;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Final handler in every Netty pipeline.  Responsible for:
 *   • Logging and closing on any uncaught exception.
 *   • Simple connection‐lifecycle logging.
 *
 * Heavy-weight cleanup (removing the {@code Client} instance from
 * {@link net.dodian.uber.game.model.entity.player.PlayerHandler}) is already
 * performed in {@code GamePacketHandler.channelInactive()}, so this class just
 * adds an extra safety net.
 */
@Sharable
public class UpstreamHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Uncaught exception for " + ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("Channel inactive {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }
}
