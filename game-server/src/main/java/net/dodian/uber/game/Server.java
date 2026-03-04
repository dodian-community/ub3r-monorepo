package net.dodian.uber.game;

import net.dodian.cache.Cache;
import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.ObjectDef;
import net.dodian.cache.object.ObjectLoader;
import net.dodian.cache.region.Region;
import net.dodian.jobs.GameTickScheduler;
import net.dodian.jobs.impl.*;
import net.dodian.uber.comm.LoginManager;
import net.dodian.uber.game.model.Login;
import net.dodian.uber.game.model.ShopHandler;
import net.dodian.uber.game.model.chunk.ChunkManager;
import net.dodian.uber.game.content.buttons.ButtonContentRegistry;
import net.dodian.uber.game.content.objects.ObjectContentRegistry;
import net.dodian.uber.game.model.entity.npc.NpcManager;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.ItemManager;
import net.dodian.uber.game.model.object.DoorHandler;
import net.dodian.uber.game.model.object.RS2Object;
import net.dodian.uber.game.model.player.casino.SlotMachine;
import net.dodian.uber.game.model.player.skills.thieving.PyramidPlunder;
import net.dodian.uber.game.runtime.loop.GameLoopService;
import net.dodian.uber.game.model.player.skills.thieving.Thieving;
import net.dodian.uber.game.runtime.eventbus.GameEventBus;
import net.dodian.uber.game.runtime.world.npc.NpcTimerScheduler;
import net.dodian.uber.game.persistence.account.AccountPersistenceService;
import net.dodian.uber.game.persistence.WorldDbPollService;
import net.dodian.uber.game.persistence.WorldPollPublisher;
import net.dodian.uber.game.security.AsyncSqlService;
import net.dodian.uber.game.security.ChatLog;
import net.dodian.utilities.DbTables;
import net.dodian.utilities.DotEnvKt;
import net.dodian.utilities.Rangable;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import net.dodian.uber.game.netty.bootstrap.NettyGameServer;

import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.dodian.uber.api.WebApiKt.launchWebApi;
import static net.dodian.utilities.DotEnvKt.*;
import static net.dodian.utilities.DatabaseKt.closeConnectionPool;
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
    public static ChunkManager chunkManager = null;


    private static NettyGameServer nettyServer;
    private static final GameTickScheduler gameTickScheduler = new GameTickScheduler(TICK);
    private static final GameLoopService gameLoopService = new GameLoopService();
    private static final AtomicBoolean SHUTDOWN_STARTED = new AtomicBoolean(false);

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
        NpcTimerScheduler.initialize(npcManager.getNpcs());
        logger.info("DONE LOADING NPC CONFIGURATION");
        itemManager = new ItemManager();
        playerHandler = new PlayerHandler();
        chunkManager = new ChunkManager();
        // NPC spawns are loaded before ChunkManager exists. Now that chunk repos are available,
        // bootstrap chunk membership once so viewport snapshots and active-chunk processing can see NPCs.
        for (net.dodian.uber.game.model.entity.npc.Npc npc : npcManager.getNpcs()) {
            if (npc != null) {
                npc.syncChunkMembership();
            }
        }
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
        ButtonContentRegistry.bootstrap();
        ObjectContentRegistry.bootstrap();
        net.dodian.uber.game.content.npcs.spawns.NpcContentRegistry.bootstrap();
        GameEventBus.bootstrap();
        ObjectContentRegistry.prewarmObjectDefinitions();

        nettyServer = new NettyGameServer(DotEnvKt.getServerPort(), playerHandler);
        logger.info("Starting Netty game server...");
        nettyServer.start();

        // Add a shutdown hook to gracefully close server resources.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered. Shutting down server...");
            shutdown();
            logger.info("Server shut down.");
        }));


        new Thread(login).start();
        /* Processor for various stuff */
        entryObject = new PyramidPlunder();
        if (getGameLoopEnabled()) {
            gameLoopService.start();
        } else {
            gameTickScheduler.registerTask("EntityProcessor", TICK, new EntityProcessor());
            gameTickScheduler.registerTask("ActionProcessor", TICK, new ActionProcessor());
            gameTickScheduler.registerTask("OutboundPacketProcessor", TICK, new OutboundPacketProcessor());
            gameTickScheduler.registerTask("ItemProcessor", TICK, new ItemProcessor());
            gameTickScheduler.registerTask("ShopProcessor", TICK, new ShopProcessor());
            gameTickScheduler.registerTask("ObjectProcess", TICK, new ObjectProcess());
            gameTickScheduler.registerTask("WorldProcessor", TICK * 100L, new WorldProcessor());
            gameTickScheduler.registerTask("FarmingProcess", TICK * 100L, new FarmingProcess());
            gameTickScheduler.registerTask("PlunderDoor", 900_000L, new PlunderDoor());
            gameTickScheduler.start();
        }
        System.gc();
        Login.banUid();
        logger.info("Server is now running on world " + getGameWorldId() + "!");

        launchWebApi();
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
        String sql = "SELECT id, x, y, type FROM " + DbTables.GAME_OBJECT_DEFINITIONS;

        try (java.sql.Connection conn = getDbConnection();
             Statement statement = conn.createStatement();
             ResultSet results = statement.executeQuery(sql)) {

            while (results.next()) {
                objects.add(new RS2Object(results.getInt("id"), results.getInt("x"), results.getInt("y"), results.getInt("type")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void shutdown() {
        if (!SHUTDOWN_STARTED.compareAndSet(false, true)) {
            return;
        }

        if (getGameLoopEnabled()) {
            gameLoopService.stop(Duration.ofSeconds(10));
        } else {
            gameTickScheduler.stop();
        }

        try {
            AccountPersistenceService.shutdownAndDrain(Duration.ofSeconds(30));
        } catch (Exception exception) {
            logger.warn("Failed to drain account persistence service during shutdown", exception);
        }

        try {
            WorldPollPublisher.shutdown();
        } catch (Exception exception) {
            logger.warn("Failed to shutdown world poll publisher", exception);
        }

        try {
            WorldDbPollService.shutdown(Duration.ofSeconds(10));
        } catch (Exception exception) {
            logger.warn("Failed to shutdown world DB poll service", exception);
        }

        try {
            ChatLog.shutdown();
        } catch (Exception exception) {
            logger.warn("Failed to shutdown chat log service", exception);
        }

        try {
            AsyncSqlService.shutdown(Duration.ofSeconds(10));
        } catch (Exception exception) {
            logger.warn("Failed to shutdown async SQL service", exception);
        }

        try {
            closeConnectionPool();
        } catch (Exception exception) {
            logger.warn("Failed to close shared connection pool", exception);
        }

        try {
            if (nettyServer != null) {
                nettyServer.shutdown();
            }
        } catch (Exception exception) {
            logger.warn("Failed to shutdown netty server", exception);
        }
    }
}
