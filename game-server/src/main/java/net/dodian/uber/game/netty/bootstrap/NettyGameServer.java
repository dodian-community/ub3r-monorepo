package net.dodian.uber.game.netty.bootstrap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.dodian.uber.game.engine.config.DotEnvKt.getNettyLeakDetection;

public class NettyGameServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyGameServer.class);

    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyGameServer(int port) {
        this.port = port;
    }

    public void start() {
        String configuredLevel = System.getProperty("netty.leakDetection", getNettyLeakDetection());
        ResourceLeakDetector.Level leakDetectionLevel = resolveLeakDetectionLevel(configuredLevel);
        ResourceLeakDetector.setLevel(leakDetectionLevel);
        logger.info("[Netty] Resource leak detection {} ({})", leakDetectionLevel, describeLeakDetectionSource(configuredLevel));

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childHandler(new GameChannelInitializer())
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

    static ResourceLeakDetector.Level resolveLeakDetectionLevel(String configuredLevel) {
        if (configuredLevel == null || configuredLevel.isBlank()) {
            return ResourceLeakDetector.Level.DISABLED;
        }

        switch (configuredLevel.trim().toLowerCase()) {
            case "disabled":
            case "off":
            case "false":
                return ResourceLeakDetector.Level.DISABLED;
            case "simple":
                return ResourceLeakDetector.Level.SIMPLE;
            case "advanced":
                return ResourceLeakDetector.Level.ADVANCED;
            case "paranoid":
            case "true":
                return ResourceLeakDetector.Level.PARANOID;
            default:
                throw new IllegalArgumentException("Unsupported NETTY_LEAK_DETECTION value: " + configuredLevel);
        }
    }

    private static String describeLeakDetectionSource(String configuredLevel) {
        if (configuredLevel == null || configuredLevel.isBlank() || "disabled".equalsIgnoreCase(configuredLevel)) {
            return "default disabled";
        }
        return "explicit override";
    }
}
