package net.dodian.uber.game;

import net.dodian.cache.Cache;
import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.ObjectDef;
import net.dodian.cache.object.ObjectLoader;
import net.dodian.cache.region.Region;
import net.dodian.jobs.JobScheduler;
import net.dodian.jobs.impl.*;
import net.dodian.uber.comm.LoginManager;
import net.dodian.uber.comm.SocketHandler;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Login;
import net.dodian.uber.game.model.ShopHandler;
import net.dodian.uber.game.model.entity.npc.NpcManager;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.ItemManager;
import net.dodian.uber.game.model.object.DoorHandler;
import net.dodian.uber.game.model.object.RS2Object;
import net.dodian.uber.game.model.player.casino.SlotMachine;
import net.dodian.uber.game.model.player.skills.thieving.PyramidPlunder;
import net.dodian.uber.game.model.player.skills.thieving.Thieving;
import net.dodian.utilities.DbTables;
import net.dodian.utilities.DotEnvKt;
import net.dodian.utilities.Rangable;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.dodian.utilities.DotEnvKt.*;
import static net.dodian.utilities.DatabaseInitializerKt.initializeDatabase;
import static net.dodian.utilities.DatabaseInitializerKt.isDatabaseInitialized;
import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static boolean trading = true, dueling = true, chatOn = true, pking = true, dropping = true, banking = true, shopping = true;
    public static int TICK = 600;
    public static boolean updateRunning;
    public static int updateSeconds;
    public static long updateStartTime, serverStartup;
    public Player player;
    public Client c;
    public static ArrayList connections = new ArrayList<>();
    public static ArrayList banned = new ArrayList<>();

    public static CopyOnWriteArrayList chat = new CopyOnWriteArrayList<>();
    public static ArrayList<RS2Object> objects = new ArrayList<>();
    public static int nullConnections = 0;
    public static Login login = null;
    public static ItemManager itemManager = null;
    public static NpcManager npcManager = null;
    public static LoginManager loginManager = null;
    public static PyramidPlunder entryObject = null;
    public static SlotMachine slots = new SlotMachine();
    public static Map tempConns = new HashMap<>();
    public static Server clientHandler = null;
    public static boolean shutdownServer = false;
    public static PlayerHandler playerHandler = null;
    public static Thieving thieving = null;
    public static ShopHandler shopHandler = null;
    public static boolean antiddos = false;

    private static ServerConnectionHandler connectionHandler;

    public static void main(String[] args) throws Exception {
        logger.info("Info log!");
        logger.error("Error log!");
        logger.warn("Warning log!");
        logger.debug("Debug log!");

        serverStartup = System.currentTimeMillis();
        System.out.println();
        System.out.println("    ____ ");
        System.out.println("   / __ \\____ ____/ (_)___ _____ ");
        System.out.println("  / / / / __ \\/ __ / / __ `/ __ \\ ");
        System.out.println(" / /_/ / /_/ / /_/ / / /_/ / / / / ");
        System.out.println("/_____/\\____/\\____/_/\\____/_/ /_/ ");
        System.out.println();

        if (getDatabaseInitialize() && !isDatabaseInitialized()) {
            initializeDatabase();
        }

        npcManager = new NpcManager();
        npcManager.loadSpawns();
        System.out.println("[NpcManager] DONE LOADING NPC CONFIGURATION");
        itemManager = new ItemManager();
        playerHandler = new PlayerHandler();
        loginManager = new LoginManager();
        shopHandler = new ShopHandler();
        thieving = new Thieving();
        clientHandler = new Server();
        login = new Login();
        Cache.load();
        ObjectDef.loadConfig();
        Region.load();
        Rangable.load();
        ObjectLoader objectLoader = new ObjectLoader();
        objectLoader.load();
        GameObjectData.init();
        loadObjects();
        new DoorHandler();
        new Thread(EventManager.getInstance()).start();

        try {
            connectionHandler = new ServerConnectionHandler(DotEnvKt.getServerPort(), playerHandler);
            new Thread(connectionHandler).start();
            System.out.println("Server connection handler started on port " + DotEnvKt.getServerPort());
        } catch (IOException e) {
            System.out.println("Failed to start server connection handler: " + e.getMessage());
            System.exit(1);
        }

        new Thread(login).start();
        /* Processor for various stuff */
        JobScheduler.ScheduleRepeatForeverJob(TICK, EntityProcessor.class);
        JobScheduler.ScheduleRepeatForeverJob(TICK * 100, WorldProcessor.class);
        JobScheduler.ScheduleRepeatForeverJob((TICK * 100) / 20, FarmingProcess.class);
        JobScheduler.ScheduleRepeatForeverJob(TICK, ItemProcessor.class);
        JobScheduler.ScheduleRepeatForeverJob(TICK, ShopProcessor.class);
        JobScheduler.ScheduleRepeatForeverJob(TICK, ObjectProcess.class);
        entryObject = new PyramidPlunder();
        System.gc();
        Login.banUid();
        System.out.println("Server is now running on world " + getGameWorldId() + "!");
    }

    public static Thread createNewConnection(SocketHandler socketHandler) {
        return new Thread(socketHandler);
    }

    public static void logError(String message) {
        Utils.println(message);
    }

    public static int totalHostConnection(String host) {
        int num = 0;
        for (int slot = 0; slot < PlayerHandler.players.length; slot++) {
            Player p = PlayerHandler.players[slot];
            if (p != null) {
                if (host.equals(p.connectedFrom))
                    num++;
            }
        }
        return num;
    }




    public static void loadObjects() {
        try {
            Statement statement = getDbConnection().createStatement();
            ResultSet results = statement.executeQuery("SELECT * from " + DbTables.GAME_OBJECT_DEFINITIONS);
            while (results.next()) {
                objects.add(new RS2Object(results.getInt("id"), results.getInt("x"), results.getInt("y"), results.getInt("type")));
            }
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        if (connectionHandler != null) {
            connectionHandler.shutdown();
        }
        // Add any other shutdown logic here
    }
}