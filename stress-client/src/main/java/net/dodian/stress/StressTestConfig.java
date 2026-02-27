package net.dodian.stress;

public final class StressTestConfig {

    private final String host;
    private final int port;
    private final String usernamePrefix;
    private final int startIndex;
    private final int botCount;
    private final double connectRatePerSecond;
    private final String password;
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
                            double connectRatePerSecond,
                            String password,
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
        this.connectRatePerSecond = connectRatePerSecond;
        this.password = password;
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

    public double getConnectRatePerSecond() {
        return connectRatePerSecond;
    }

    public String getPassword() {
        return password;
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
