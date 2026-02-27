package net.dodian.stress;

import net.dodian.stress.protocol.Rs317LoginProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

final class StressBotSession implements Runnable {

    interface Listener {
        void onLoginSuccess(String username, long connectMs, int rights);

        void onSessionEnded(String username, boolean loggedIn, String reason);
    }

    private final StressTestConfig config;
    private final String username;
    private final Listener listener;
    private final BooleanSupplier shouldRun;

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private volatile Socket socket;

    StressBotSession(StressTestConfig config,
                     String username,
                     Listener listener,
                     BooleanSupplier shouldRun) {
        this.config = config;
        this.username = username;
        this.listener = listener;
        this.shouldRun = shouldRun;
    }

    @Override
    public void run() {
        boolean loggedIn = false;
        String reason = "closed";
        long connectStart = System.currentTimeMillis();

        try (Socket s = new Socket()) {
            socket = s;
            s.connect(new InetSocketAddress(config.getHost(), config.getPort()), config.getConnectTimeoutMs());
            s.setTcpNoDelay(true);
            s.setSoTimeout(1000);

            InputStream in = new BufferedInputStream(s.getInputStream());
            OutputStream out = new BufferedOutputStream(s.getOutputStream());

            Rs317LoginProtocol.LoginSession loginSession = Rs317LoginProtocol.login(
                    in,
                    out,
                    username,
                    config.getPassword(),
                    config.isReconnecting(),
                    config.getClientVersion(),
                    config.isLowMemory()
            );

            loggedIn = true;
            long connectMs = Math.max(1L, System.currentTimeMillis() - connectStart);
            listener.onLoginSuccess(username, connectMs, loginSession.getRights());

            long keepAliveEveryMs = Math.max(5_000L, config.getKeepAliveSeconds() * 1_000L);
            long nextKeepAliveAt = System.currentTimeMillis() + keepAliveEveryMs;
            byte[] sink = new byte[4096];

            while (!stopRequested.get() && shouldRun.getAsBoolean()) {
                try {
                    int read = in.read(sink);
                    if (read < 0) {
                        reason = "server closed";
                        break;
                    }
                } catch (SocketTimeoutException ignored) {
                    // Keep waiting for activity and keep-alive timer.
                }

                long now = System.currentTimeMillis();
                if (now >= nextKeepAliveAt) {
                    Rs317LoginProtocol.writeKeepAlive(out, loginSession.getOutCipher());
                    nextKeepAliveAt = now + keepAliveEveryMs;
                }
            }

            if (stopRequested.get() || !shouldRun.getAsBoolean()) {
                reason = "stopped";
            }
        } catch (Exception ex) {
            reason = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        } finally {
            closeSocket();
            listener.onSessionEnded(username, loggedIn, reason);
        }
    }

    void requestStop() {
        stopRequested.set(true);
        closeSocket();
    }

    private void closeSocket() {
        Socket current = socket;
        if (current == null) {
            return;
        }
        try {
            current.close();
        } catch (IOException ignored) {
        }
    }
}
