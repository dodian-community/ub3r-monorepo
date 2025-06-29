package net.dodian.uber.game.networking;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal Netty bootstrap that listens for incoming connections and wires a
 * {@link GameChannelInitializer}. For this first incremental commit it simply
 * accepts TCP connections and attaches a {@link } that
 * discards all inbound bytes.
 */
public class NettyGameServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(NettyGameServer.class);

    private final int port;
    private final PlayerHandler playerHandler;
    private ChannelFuture bindFuture;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyGameServer(int port, PlayerHandler playerHandler) {
        this.port = port;
        this.playerHandler = playerHandler;
    }

    @Override
    public void run() {
        bossGroup = new NioEventLoopGroup(1); // single acceptor thread
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new GameChannelInitializer(playerHandler))
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            logger.info("[Netty] Binding game server on port {}", port);
            bindFuture = bootstrap.bind(port).sync();
            logger.info("[Netty] Game server is now listening on {}", port);

            // Wait until the server socket is closed.
            bindFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.warn("[Netty] Game server interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        logger.info("[Netty] Shutting down game server");
        try {
            if (bindFuture != null) {
                bindFuture.channel().close().sync();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }
}
