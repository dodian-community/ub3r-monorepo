package net.dodian;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.dodian.models.config.ServerConfig;
import net.dodian.uber.game.Server;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Fabrice L
 */
public class Config {
    private static final String DIRECTORY_DATA = "./data/";

    private static ServerConfig serverConfig = new ServerConfig();

    public static List<String> MULTILOG_EXCEPTION = new ArrayList<>();

    public static void loadConfig() throws Exception {
        serverConfig = ConfigFromEnvKt.getConfigFromEnv();

        ObjectMapper mapper = new ObjectMapper()
                .findAndRegisterModules()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        try {
            File file = Paths.get(DIRECTORY_DATA + "config.json").toFile();

            if (!file.exists()) {
                mapper.writeValue(file, serverConfig);
                throw new Exception("Created default config, please verify that the config values are appropriate before launching.");
            }

            serverConfig = mapper.readValue(Paths.get(DIRECTORY_DATA + "config.json").toFile(), ServerConfig.class);
        } catch (Exception e) {
            if (serverConfig.isDebugMode()) {
                e.printStackTrace();
            }

            Server.logError("Config file does not exist for the server, unless values are set by environment variables, the values uses fallback defaults.");
            Server.logError("Config file expected location: " + Paths.get(DIRECTORY_DATA + "config.json").toAbsolutePath());
        }
    }

    public static int getPort() {
        return serverConfig.getPort();
    }

    public static String getClient() {
        return serverConfig.getClientSecret();
    }

    public static int getWorldId() {
        return serverConfig.getWorldId();
    }

    public static String getMysqlDatabase() {
        return serverConfig.getDatabaseConfig().getDatabase();
    }

    public static String getMysqlUser() {
        return serverConfig.getDatabaseConfig().getUsername();
    }

    public static String getMysqlPass() {
        return serverConfig.getDatabaseConfig().getPassword();
    }

    public static String getMysqlUrl() {
        String host = serverConfig.getDatabaseConfig().getHost();
        int port = serverConfig.getDatabaseConfig().getPort();
        return "jdbc:mysql://" + host + ":" + port + "/";
    }

    public static int getExperienceMultiplier() {
        return serverConfig.getExperienceMultiplier();
    }

    public static String getCustomClientVersion() {
        return serverConfig.getCustomClientVersion();
    }

    public static void setCustomClientVersion(String clientVersion) {
        serverConfig.setCustomClientVersion(clientVersion);
    }
}
