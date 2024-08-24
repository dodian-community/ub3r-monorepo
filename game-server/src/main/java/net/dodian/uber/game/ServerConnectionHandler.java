package net.dodian.uber.game;

import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.utilities.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ServerConnectionHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(ServerConnectionHandler.class.getName());
    private static final int DELAY = 50;
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private boolean shutdownHandler = false;
    private final PlayerHandler playerHandler;
    private final ExecutorService connectionExecutor;

    public ServerConnectionHandler(int port, PlayerHandler playerHandler) throws IOException {
        this.playerHandler = playerHandler;
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(new InetSocketAddress(port));
        this.serverSocketChannel.configureBlocking(false);
        this.selector = Selector.open();
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.connectionExecutor = Executors.newCachedThreadPool();
        logger.info("ServerConnectionHandler initialized on port " + port);
    }

    @Override
    public void run() {
        try {
            while (!shutdownHandler) {
                selector.select(DELAY);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        acceptConnection();
                    }
                }
            }
        } catch (IOException e) {
            if (!shutdownHandler) {
                logger.severe("Server error: " + e.getMessage());
            } else {
                logger.info("ServerConnectionHandler was shut down.");
            }
        } finally {
            shutdown();
        }
    }

    private void acceptConnection() {
        try {
            final SocketChannel socketChannel = serverSocketChannel.accept();
            if (socketChannel != null) {
                socketChannel.configureBlocking(false);
                final String connectingHost = extractConnectingHost(socketChannel);

                if (Server.antiddos && !Server.tempConns.containsKey(connectingHost)) {
                    socketChannel.close();
                } else {
                    Server.tempConns.remove(connectingHost);
                    Server.connections.add(connectingHost);
                    if (checkHost(connectingHost)) {
                        connectionExecutor.submit(() -> {
                            try {
                                playerHandler.newPlayerClient(socketChannel, connectingHost);
                            } catch (Exception e) {
                                logger.severe("Error processing new client connection: " + e.getMessage());
                                closeSocketChannel(socketChannel);
                            }
                        });
                    } else {
                        socketChannel.close();
                    }
                }
            }
        } catch (IOException e) {
            logger.warning("Error accepting connection: " + e.getMessage());
        }
    }

    private String extractConnectingHost(SocketChannel socketChannel) throws IOException {
        String fullAddress = socketChannel.getRemoteAddress().toString();
        return fullAddress.substring(1, fullAddress.indexOf(":"));
    }

    private void closeSocketChannel(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (IOException e) {
            logger.warning("Error closing socket channel: " + e.getMessage());
        }
    }

    private boolean checkHost(String host) {
        //TODO complete checkHost
        return true;
    }

    public void shutdown() {
        shutdownHandler = true;
        connectionExecutor.shutdown();
        try {
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
            if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
                serverSocketChannel.close();
            }
        } catch (IOException e) {
            logger.severe("Error during server shutdown: " + e.getMessage());
        }
    }
}