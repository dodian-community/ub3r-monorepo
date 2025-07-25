package net.dodian.uber.game.netty.bootstrap;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.netty.login.LoginHandshakeHandler;
import net.dodian.uber.game.netty.util.UpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * New Netty pipeline bootstrap that lives under the clean `netty` package.
 * This class mirrors the behaviour of the legacy GameChannelInitializer but
 * references the handlers in the new package.  The last two legacy utility
 * classes (ConnectionLoggingHandler, ConnectionThrottleFilter, UpstreamHandler)
 * are still referenced for now – they are stateless and can be migrated later.
 */
public class GameChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger(GameChannelInitializer.class);

    private final PlayerHandler playerHandler;
    
    /**
     * Shared upstream handler instance for performance optimization.
     */
    private final UpstreamHandler upstreamHandler = new UpstreamHandler();

    public GameChannelInitializer(PlayerHandler playerHandler) {
        this.playerHandler = playerHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        logger.debug("[Netty] Accepted connection from {}", ch.remoteAddress());

        // 1. Read timeout handler - disconnect idle connections after 30 seconds
        ch.pipeline().addLast("read-timeout", new ReadTimeoutHandler(30));

        // 2. Login Handshake
        ch.pipeline().addLast("login-handshake", new LoginHandshakeHandler(playerHandler));

        // 3. Login Payload Decoder & Processor
        ch.pipeline().addLast("login-decoder", new net.dodian.uber.game.netty.login.LoginPayloadDecoder());
        ch.pipeline().addLast("login-processor", new net.dodian.uber.game.netty.login.LoginProcessorHandler(playerHandler));

        // 4. Final handler for exceptions and logging
        ch.pipeline().addLast("upstream-handler", upstreamHandler);
    }
}
