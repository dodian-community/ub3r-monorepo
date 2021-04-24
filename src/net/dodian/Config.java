package net.dodian;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Fabrice L
 *
 */
public class Config {

  @SuppressWarnings("unused")
  private static String SERVER_NAME = "";
  private static int SERVER_PORT    = 43594;
  private static String CLIENT_CHECK    = "-1";

  private static String MYSQL_HOST = "51.75.64.180";
  private static String MYSQL_USER = "old_exorth";
  private static String MYSQL_PASS = "Logan11122!!";
  private static String MYSQL_NAME = "dodiannet";
  private static int MYSQL_PORT    = 3306;

  public static List<String> MULTILOG_EXCEPTION = new ArrayList<>();

  public static int worldId = 1;
  public static String customClientVersion;

  private static int experienceMultiplier = 1;

  private static String DIRECTORY_DATA = System.getProperty("user.dir") + "/data/";

  public static int getPort() {
    return SERVER_PORT;
  }

  public static String getClient() {
    return CLIENT_CHECK;
  }

  public static int getWorldId() {
    return worldId;
  }

  public static String getMysqlDatabase() {
    return MYSQL_NAME;
  }

  public static String getMysqlUser() {
    return MYSQL_USER;
  }

  public static String getMysqlPass() {
    return MYSQL_PASS;
  }

  public static String getMysqlUrl() {
    return "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/";
  }

  public static int getExperienceMultiplier() {
    return experienceMultiplier;
  }

  public static void loadConfig() {
    Gson gson = new Gson();
    try {
      BufferedReader reader = new BufferedReader(new FileReader(DIRECTORY_DATA + "config.json"));
      JsonObject configData = gson.fromJson(reader, JsonObject.class);

      if(configData.has("server")) {
        JsonObject server = configData.get("server").getAsJsonObject();
        if(server.has("name")) {
          SERVER_NAME = server.get("name").getAsString();
        }

        if(server.has("port")) {
          SERVER_PORT = server.get("port").getAsInt();
        }

        if(server.has("client")) {
          CLIENT_CHECK = server.get("client").getAsString();
        }

        if(server.has("debug")) {
          //SERVER_DEBUG = server.get("debug").getAsBoolean();
        }

        if(server.has("connection-limit")) {
          //CONNECTION_LIMIT = server.get("connection-limit").getAsInt();
        }

        if(server.has("connection-whitelist")) {

        }
      }
      if(configData.has("mysql")) {
        JsonObject sqlData = configData.get("mysql").getAsJsonObject();
        if(sqlData.has("port")) {
          MYSQL_PORT = sqlData.get("port").getAsInt();
        }

        if(sqlData.has("host")) {
          MYSQL_HOST = sqlData.get("host").getAsString();
        }

        if(sqlData.has("database")) {
          MYSQL_NAME = sqlData.get("database").getAsString();
        }

        if(sqlData.has("user")) {
          MYSQL_USER = sqlData.get("user").getAsString();
        }

        if(sqlData.has("pass")) {
          MYSQL_PASS = sqlData.get("pass").getAsString();
        }

        if(sqlData.has("prefix")) {
          //SQL_PREFIX = sqlData.get("prefix").getAsString();
        }
      }
      if(configData.has("world")) {
        JsonObject worldData = configData.get("world").getAsJsonObject();
        if(worldData.has("worldId")) {
            worldId = worldData.get("worldId").getAsInt();
        }
      }
    } catch (FileNotFoundException e) {
      System.out.println("Didn't find a config.json, using fallback configs instead.");
    }
  }

}
