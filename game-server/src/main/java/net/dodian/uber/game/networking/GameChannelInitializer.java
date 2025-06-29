package net.dodian.uber.game.networking;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import org.slf4j.Logger;
import net.dodian.uber.game.networking.login.LoginPayloadDecoder;
import net.dodian.uber.game.networking.login.LoginProcessorHandler;
import net.dodian.uber.game.networking.login.LoginHandshakeHandler;

import org.slf4j.LoggerFactory;


public class GameChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger(GameChannelInitializer.class);

    private static final ConnectionThrottleFilter IP_FILTER = new ConnectionThrottleFilter(3);
    private static final UpstreamHandler UPSTREAM_HANDLER = new UpstreamHandler();

    private final PlayerHandler playerHandler;

    public GameChannelInitializer(PlayerHandler playerHandler) {
        this.playerHandler = playerHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        logger.debug("[Netty] Accepted connection from {}", ch.remoteAddress());

        // Timeout & IP throttle
        ch.pipeline().addLast("read-timeout", new ReadTimeoutHandler(15));
        ch.pipeline().addLast("ip-filter", IP_FILTER);

        // Login handshake stage
        ch.pipeline().addLast(new LoginHandshakeHandler(playerHandler));
        // After handshake, decode login payload and process login
        ch.pipeline().addLast(new LoginPayloadDecoder());
        ch.pipeline().addLast("login-processor", new LoginProcessorHandler(playerHandler));
        // Post-login connection logging / keepalive
        ch.pipeline().addLast(new ConnectionLoggingHandler());
        ch.pipeline().addLast(UPSTREAM_HANDLER);
    }
}
