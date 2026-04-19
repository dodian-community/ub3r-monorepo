package net.dodian.uber.game.netty.bootstrap;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.dodian.uber.game.netty.login.LoginHandshakeHandler;
import net.dodian.uber.game.netty.util.UpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger(GameChannelInitializer.class);

    private final UpstreamHandler upstreamHandler = new UpstreamHandler();

    public GameChannelInitializer() {}

    @Override
    protected void initChannel(SocketChannel ch) {
        logger.debug("[Netty] Accepted connection from {}", ch.remoteAddress());

        ch.pipeline().addLast("read-timeout", new ReadTimeoutHandler(30));

        ch.pipeline().addLast("login-handshake", new LoginHandshakeHandler());

        ch.pipeline().addLast("login-decoder", new net.dodian.uber.game.netty.login.LoginPayloadDecoder());
        ch.pipeline().addLast("login-processor", new net.dodian.uber.game.netty.login.LoginProcessorHandler());

        ch.pipeline().addLast("upstream-handler", upstreamHandler);
    }
}
