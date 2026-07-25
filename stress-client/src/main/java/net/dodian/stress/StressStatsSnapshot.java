package net.dodian.stress;

public final class StressStatsSnapshot {

    private final boolean running;
    private final int targetBots;
    private final int attempted;
    private final int succeeded;
    private final int failed;
    private final int active;
    private final int disconnectedAfterLogin;
    private final long avgConnectMs;
    private final long p95ConnectMs;
    private final long maxConnectMs;
    private final long uptimeMs;

    public StressStatsSnapshot(boolean running,
                               int targetBots,
                               int attempted,
                               int succeeded,
                               int failed,
                               int active,
                               int disconnectedAfterLogin,
                               long avgConnectMs,
                               long p95ConnectMs,
                               long maxConnectMs,
                               long uptimeMs) {
        this.running = running;
        this.targetBots = targetBots;
        this.attempted = attempted;
        this.succeeded = succeeded;
        this.failed = failed;
        this.active = active;
        this.disconnectedAfterLogin = disconnectedAfterLogin;
        this.avgConnectMs = avgConnectMs;
        this.p95ConnectMs = p95ConnectMs;
        this.maxConnectMs = maxConnectMs;
        this.uptimeMs = uptimeMs;
    }

    public boolean isRunning() {
        return running;
    }

    public int getTargetBots() {
        return targetBots;
    }

    public int getAttempted() {
        return attempted;
    }

    public int getSucceeded() {
        return succeeded;
    }

    public int getFailed() {
        return failed;
    }

    public int getActive() {
        return active;
    }

    public int getDisconnectedAfterLogin() {
        return disconnectedAfterLogin;
    }

    public long getAvgConnectMs() {
        return avgConnectMs;
    }

    public long getP95ConnectMs() {
        return p95ConnectMs;
    }

    public long getMaxConnectMs() {
        return maxConnectMs;
    }

    public long getUptimeMs() {
        return uptimeMs;
    }
}
