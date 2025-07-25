package net.dodian.uber.game.netty.bootstrap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clean Netty bootstrap that binds the socket and wires the new {@link GameChannelInitializer}.
 * Behaviour is copied from the legacy NettyGameServer but lives under the new package to avoid
 * importing anything from `networking2`.
 */
public class NettyGameServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyGameServer.class);

    private final int port;
    private final PlayerHandler playerHandler;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyGameServer(int port, PlayerHandler playerHandler) {
        this.port = port;
        this.playerHandler = playerHandler;
    }

    public void start() {
        // Configure resource leak detection like Luna - disabled in production
        // Check if we're in development mode (you can customize this logic)
        boolean isDevelopment = logger.isDebugEnabled() || Boolean.getBoolean("development.mode");
        
        if (isDevelopment) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
            logger.debug("[Netty] Resource leak detection set to PARANOID level (development mode)");
        } else {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
            logger.info("[Netty] Resource leak detection DISABLED (production mode)");
        }

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childHandler(new GameChannelInitializer(playerHandler))
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        logger.info("[Netty] Binding game server on port {}", port);
        bootstrap.bind(port).syncUninterruptibly();
        logger.info("[Netty] Game server listening on {}", port);

    }

    public void shutdown() {
        logger.info("[Netty] Shutting down game server");
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }
}
