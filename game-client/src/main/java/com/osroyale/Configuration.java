package com.osroyale;

/**
 * Handles all configuration settings for the client.
 *
 * @author Daniel
 * @author Jire
 */
public final class Configuration {

    public static final String LIVE_GAME_ADDRESS = "runith.duckdns.org";
    public static final int LIVE_GAME_PORT = 34594;

    public static final String LIVE_CACHE_ADDRESS = "runith.duckdns.org";
    public static final int LIVE_CACHE_PORT = 43595;

    public static final String DEV_GAME_ADDRESS = "10.0.0.63";
    public static final int DEV_GAME_PORT = 43594;

    public static final String DEV_CACHE_ADDRESS = "10.0.0.63";
    public static final int DEV_CACHE_PORT = 43595;

    /**
     * The IP address client will be connecting to.
     */
    public static final Connection CONNECTION =
            Connection.ECONOMY;

    /**
     * State of client being in debug mode.
     */
    public static boolean DEBUG_MODE = false;

    /**
     * Display client data.
     */
    static boolean CLIENT_DATA = false;

    /**
     * Debug the interfaces.
     */
    static boolean DUMP_INTERFACES = false;

    /**
     * State of client enabling RSA encryption.
     */
    static boolean ENABLE_RSA = true;

    /**
     * The current NPC bits.
     */
    static final int NPC_BITS = 16;

    /**
     * The current version of the client.
     */
    public static final int CLIENT_VERSION = 13;

    /**
     * The name of the client.
     */
    public final static String NAME = "Runeith";

    /**
     * The cache file name.
     */
    public final static String CACHE_NAME = ".runeith";

    public static final String SPRITE_FILE_NAME = "main_file_sprites";

    /**
     * The character folder path.
     */
    static final String CHAR_PATH = Utility.findcachedir() + "Character";

    /**
     * All the announcements which will be displayed on the loginscreen.
     */
    public final static String[] ANNOUNCEMENT = {
            "Welcome to " + NAME,
    };

    /**
     * Whether to use Jire SwiftFUP update server.
     */
    public static final boolean USE_UPDATE_SERVER = false;

    public static final String UPDATE_SERVER_IP = CONNECTION.getCacheAddress();
    public static final int UPDATE_SERVER_PORT = CONNECTION.getCachePort();

    /**
     * The HTTP port for the Ktor Web API & Highscores server.
     */
    public static final int WEB_API_PORT = 8081;

    /**
     * The HTTP URL from which to download the cache zip if not present locally.
     */
    public static final String CACHE_DOWNLOAD_URL = "https://engin3d.nl/Tarnish-dodian.zip";

    public static class DiscordConfiguration {
        public static boolean ENABLE_DISCORD_OAUTH_LOGIN = true;
        // Only the public client id belongs here. The OAuth client secret lives solely in the
        // server's .env (DISCORD_CLIENT_SECRET) - the token exchange is server-side, and a
        // secret shipped inside the client jar is readable by every player.
        public static String CLIENT_ID = "1533943718854524949";
        public static int CALLBACK_PORT = 8080;
        public static String REDIRECT_URL = "http://localhost:8080";
    }
}
