package net.dodian.uber.game.netty.util;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.AttributeKey;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.systems.world.player.PlayerRegistry;
import net.dodian.uber.game.engine.loop.GameThreadTaskQueue;
import net.dodian.utilities.Utils;
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
        Object activeClient = ctx.channel().attr(AttributeKey.valueOf("activeClient")).get();
        Client client = activeClient instanceof Client ? (Client) activeClient : null;

        if (!isReadTimeout && !isIgnoredMessage) {
            logger.warn("[Netty] Exception from {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        } else if (isReadTimeout) {
            if (client != null) {
                client.noteDisconnectReason("read-timeout");
                logger.debug(
                        "[Netty] Read timeout for {} player={} {}",
                        ctx.channel().remoteAddress(),
                        client.getPlayerName(),
                        client.connectionHealthSummary()
                );
            } else {
                logger.debug("[Netty] Read timeout for {}", ctx.channel().remoteAddress());
            }
        }
        
        ctx.close();
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // If this channel had an active Client linked, remove it from the server.
        Object attr = ctx.channel().attr(AttributeKey.valueOf("activeClient")).getAndSet(null);
        if (attr instanceof Client client) {
            if ("unknown".equals(client.getLastDisconnectReason())) {
                client.noteDisconnectReason("channel-inactive");
            }
            logger.debug(
                    "[Netty] Channel inactive {} player={} {}",
                    ctx.channel().remoteAddress(),
                    client.getPlayerName(),
                    client.connectionHealthSummary()
            );
            // Mark disconnected immediately (Netty thread) so the game thread stops processing this client ASAP.
            client.disconnected = true;
            // Remove the stale online entry promptly to avoid "already logged in" false-positives during relog.
            try {
                long key = Utils.playerNameToLong(client.getPlayerName());
                PlayerRegistry.playersOnline.remove(key, client);
            } catch (Exception ignored) {
                // If the name isn't available yet, removal will happen during game-thread cleanup.
            }
            // Enqueue removal onto the game thread to avoid mutating PlayerRegistry state off-thread.
            GameThreadTaskQueue.submit(() -> {
                PlayerRegistry.removePlayer(client);
            });
        } else {
            logger.debug("[Netty] Channel inactive {}", ctx.channel().remoteAddress());
        }
        super.channelInactive(ctx);
    }
}
