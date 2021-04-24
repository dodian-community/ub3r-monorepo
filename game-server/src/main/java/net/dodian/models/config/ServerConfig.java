package net.dodian.models.config;

public class ServerConfig {
    private String name = "Dodian";
    private int port = 43594;
    private String clientSecret = "NOT_SET";
    private boolean debugMode = false;
    private int connectionsPerIp = 2;
    private int worldId = 1;
    private int experienceMultiplier = 1;
    private String customClientVersion = "";
    private DatabaseConfig databaseConfig = new DatabaseConfig();

    public ServerConfig() {

    }

    public int getExperienceMultiplier() {
        return experienceMultiplier;
    }

    public void setExperienceMultiplier(int experienceMultiplier) {
        this.experienceMultiplier = experienceMultiplier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public int getConnectionsPerIp() {
        return connectionsPerIp;
    }

    public void setConnectionsPerIp(int connectionsPerIp) {
        this.connectionsPerIp = connectionsPerIp;
    }

    public int getWorldId() {
        return worldId;
    }

    public void setWorldId(int worldId) {
        this.worldId = worldId;
    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    public void setDatabaseConfig(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public String getCustomClientVersion() {
        return customClientVersion;
    }

    public void setCustomClientVersion(String customClientVersion) {
        this.customClientVersion = customClientVersion;
    }
}
