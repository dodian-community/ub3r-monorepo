package net.dodian.stress;

public final class StressTestConfig {

    private final String host;
    private final int port;
    private final String usernamePrefix;
    private final int startIndex;
    private final int botCount;
    private final int connectIntervalMs;
    private final int keepAliveSeconds;
    private final int connectTimeoutMs;
    private final int clientVersion;
    private final boolean lowMemory;
    private final boolean reconnecting;

    public StressTestConfig(String host,
                            int port,
                            String usernamePrefix,
                            int startIndex,
                            int botCount,
                            int connectIntervalMs,
                            int keepAliveSeconds,
                            int connectTimeoutMs,
                            int clientVersion,
                            boolean lowMemory,
                            boolean reconnecting) {
        this.host = host;
        this.port = port;
        this.usernamePrefix = usernamePrefix;
        this.startIndex = startIndex;
        this.botCount = botCount;
        this.connectIntervalMs = connectIntervalMs;
        this.keepAliveSeconds = keepAliveSeconds;
        this.connectTimeoutMs = connectTimeoutMs;
        this.clientVersion = clientVersion;
        this.lowMemory = lowMemory;
        this.reconnecting = reconnecting;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsernamePrefix() {
        return usernamePrefix;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getBotCount() {
        return botCount;
    }

    public int getConnectIntervalMs() {
        return connectIntervalMs;
    }

    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getClientVersion() {
        return clientVersion;
    }

    public boolean isLowMemory() {
        return lowMemory;
    }

    public boolean isReconnecting() {
        return reconnecting;
    }
}
