package net.dodian.uber.game.netty.util;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Basic upstream handler that logs exceptions and channelInactive.
 * Marked as @Sharable since it contains no instance state.
 */
@Sharable
public class UpstreamHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamHandler.class);

    /**
     * Set of commonly ignored connection exceptions to reduce log noise.
     */
    private static final Set<String> IGNORED_EXCEPTIONS = Set.of(
        "An existing connection was forcibly closed by the remote host",
        "An established connection was aborted by the software in your host machine",
        "Connection reset by peer",
        "Broken pipe"
    );

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        boolean isReadTimeout = cause instanceof ReadTimeoutException;
        boolean isIgnoredMessage = IGNORED_EXCEPTIONS.contains(cause.getMessage());

        if (!isReadTimeout && !isIgnoredMessage) {
            logger.warn("[Netty] Exception from {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        } else if (isReadTimeout) {
            logger.debug("[Netty] Read timeout for {}", ctx.channel().remoteAddress());
        }
        
        ctx.close();
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("[Netty] Channel inactive {}", ctx.channel().remoteAddress());

        // If this channel had an active Client linked, remove it from the server.
        Object attr = ctx.channel().attr(AttributeKey.valueOf("activeClient")).getAndSet(null);
        if (attr instanceof net.dodian.uber.game.model.entity.player.Client) {
            net.dodian.uber.game.model.entity.player.Client client = (net.dodian.uber.game.model.entity.player.Client) attr;
            net.dodian.uber.game.Server.playerHandler.removePlayer(client);
        }
        super.channelInactive(ctx);
    }
}


