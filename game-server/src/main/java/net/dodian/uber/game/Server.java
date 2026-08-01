package net.dodian.uber.game;

import net.dodian.cache.objects.GameObjectData;
import net.dodian.uber.game.persistence.account.Login;
import net.dodian.uber.game.shop.ShopManager;
import net.dodian.uber.game.model.chunk.ChunkManager;
import net.dodian.uber.game.engine.systems.world.npc.NpcManager;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry;
import net.dodian.uber.game.item.ItemManager;
import net.dodian.uber.game.model.objects.DoorRegistry;
import net.dodian.uber.game.model.objects.WorldObject;
import net.dodian.uber.game.activity.casino.SlotMachine;
import net.dodian.uber.game.engine.lifecycle.EnginePluginBootstrap;
import net.dodian.uber.game.engine.lifecycle.StartupValidationService;
import net.dodian.uber.game.engine.loop.GameLoopService;
import net.dodian.uber.game.engine.loop.GameThreadIngress;
import net.dodian.uber.game.engine.loop.GameThreadTimers;
import net.dodian.uber.game.engine.systems.world.npc.NpcTimerScheduler;
import net.dodian.uber.game.engine.webapi.WebApi;
import net.dodian.uber.game.persistence.account.AccountPersistenceService;
import net.dodian.uber.game.persistence.DbDispatchers;
import net.dodian.uber.game.persistence.player.PlayerSaveReason;
import net.dodian.uber.game.persistence.player.PlayerSaveService;
import net.dodian.uber.game.persistence.world.WorldDbPollService;
import net.dodian.uber.game.persistence.WorldSavePublisher;
import net.dodian.uber.game.persistence.audit.ChatLog;
import net.dodian.uber.game.persistence.world.ObjectDefinitionRepository;
import net.dodian.uber.game.engine.config.DotEnvKt;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.dodian.uber.game.engine.systems.cache.CacheBootstrapService;
import net.dodian.uber.game.engine.systems.interaction.ObjectClipService;


import net.dodian.uber.game.netty.bootstrap.NettyGameServer;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.dodian.uber.game.engine.config.DotEnvKt.*;
import static net.dodian.uber.game.persistence.db.DatabaseKt.closeConnectionPool;
import static net.dodian.uber.game.persistence.db.DatabaseInitializerKt.initializeDatabase;
import static net.dodian.uber.game.persistence.db.DatabaseInitializerKt.isDatabaseInitialized;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static int TICK = 600;
    public static boolean updateRunning;
    public static int updateSeconds;
    public static long updateStartTime, serverStartup;
    public Player player;
    public Client c;
    public static ArrayList connections = new ArrayList<>();
    public static ArrayList banned = new ArrayList<>();

    public static ArrayList<WorldObject> objects = new ArrayList<>();
    public static int nullConnections = 0;
    public static Login login = null;
    public static ItemManager itemManager = null;
    public static NpcManager npcManager = null;
    public static SlotMachine slots = new SlotMachine();
    public static Server clientHandler = null;
    public static ShopManager shopManager = null;
    public static ChunkManager chunkManager = null;


    private static NettyGameServer nettyServer;

    private static final GameLoopService gameLoopService = new GameLoopService();
    private static final AtomicBoolean SHUTDOWN_STARTED = new AtomicBoolean(false);
    private static final String STARTUP_BANNER = String.join("\n",
            "    ____",
            "   / __ \\____ ____/ (_)___ _____",
            "  / / / / __ \\/ __ / / __ `/ __ \\",
            " / /_/ / /_/ / /_/ / / /_/ / / / /",
            "/_____/\\____/\\____/_/\\____/_/ /_/",
            "",
            "Maintained by Dodian.net");

    static String startupBanner() {
        return STARTUP_BANNER;
    }

    public static void main(String[] args) throws Exception {
        logger.info("\n{}", STARTUP_BANNER);
        List<String> jvmInputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        logger.info("JVM Runtime Arguments: {}", jvmInputArgs);

        serverStartup = System.currentTimeMillis();
        try {
            net.dodian.uber.game.engine.config.SettingsLoader.INSTANCE.load();
            net.dodian.uber.game.engine.config.FeatureStateService.INSTANCE.initialize(
                    net.dodian.uber.game.engine.config.SettingsLoader.INSTANCE.getSettings()
            );
            StartupValidationService.validateOrThrow();
        } catch (IllegalStateException e) {
            logger.error("Startup configuration error: {}", e.getMessage());
            System.exit(1);
            return;
        }
        net.dodian.uber.game.netty.listener.PacketListenerManager.initialize();
        if (getDatabaseInitialize() && !isDatabaseInitialized()) {
            initializeDatabase();
        }

        npcManager = new NpcManager();

        try (var startupExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Void> npcTask = startupExecutor.submit(() -> {
                npcManager.loadSpawns();
                return null;
            });

            Future<ItemManager> itemTask = startupExecutor.submit(() -> new ItemManager());

            Future<Void> cacheTask = startupExecutor.submit(() -> {
                GameObjectData.init();
                net.dodian.uber.game.engine.systems.objectexamines.ObjectExamines.load();
                new CacheBootstrapService().bootstrap();
                return null;
            });

            npcTask.get();
            itemManager = itemTask.get();
            cacheTask.get();
        }

        NpcTimerScheduler.initialize(npcManager.getNpcs());
        chunkManager = new ChunkManager();

        for (net.dodian.uber.game.model.entity.npc.Npc npc : npcManager.getNpcs()) {
            if (npc != null) {
                npc.syncChunkMembership();
            }
        }
        shopManager = new ShopManager();
        clientHandler = new Server();
        login = new Login();
        loadObjects();
        new DoorRegistry();

        // Install shutdown hook before starting I/O so partial startup is cleaned up.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered. Shutting down server...");
            shutdown();
            logger.info("Server shut down.");
        }));

        ObjectClipService.bootstrapStartupOverlays(objects);
        EnginePluginBootstrap.bootstrap();

        System.gc();

        nettyServer = new NettyGameServer(DotEnvKt.getServerPort());
        nettyServer.start();

        /* Processor for various stuff */
        net.dodian.uber.game.persistence.audit.CustomInterfaceRegistry.startBackgroundLoad();
        gameLoopService.start();
        net.dodian.uber.game.npc.LegendsGuardRankingCache.start();
        Login.banUid();
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("/tmp/ub3r-ready"), "ready\n");
        } catch (Exception e) {
            logger.warn("Unable to write readiness marker", e);
        }
        long startupMillis = System.currentTimeMillis() - serverStartup;
        // packet_listeners and items are intentionally omitted here - PacketListenerManager and
        // ItemManager already log those counts themselves (packet_listeners_ready,
        // item_definitions_ready). npcs/objects below are live world-state counts nothing else reports.
        logger.info(
                "server_ready world={} transport={} startup_ms={} npcs={} objects={}",
                getGameWorldId(),
                nettyServer.getTransportName(),
                startupMillis,
                npcManager.getNpcs().size(),
                objects.size()
        );

        logPortsSummary();
    }

    /** Always the last thing logged at startup, so the ports an operator needs are easy to find at the bottom of the boot log. */
    private static void logPortsSummary() {
        boolean webEnabled = DotEnvKt.getWebApiEnabled();
        boolean discordEnabled = DotEnvKt.getDiscordApiEnabled();
        boolean metricsEnabled = DotEnvKt.getMetricsEnabled();
        logger.info("Server & Ports overview [env={}, world={}, xp={}x, max_conn/ip={}]:",
                DotEnvKt.getServerEnv(), DotEnvKt.getGameWorldId(), DotEnvKt.getGameMultiplierGlobalXp(), DotEnvKt.getGameConnectionsPerIp());
        logger.info("  game       {}  - RS317 client connections", DotEnvKt.getServerPort());
        logger.info("  web api    {}  - REST health/status/hiscores API ({})", DotEnvKt.getWebApiPort(), webEnabled ? "enabled" : "disabled");
        logger.info("  discord    {}  - Discord OAuth authentication ({})", DotEnvKt.getDiscordRedirectUrl(), discordEnabled ? "enabled" : "disabled");
        if (webEnabled && metricsEnabled) {
            logger.info("  metrics    http://localhost:{}/prometheus  - Prometheus Text Endpoint ({})", DotEnvKt.getWebApiPort(), DotEnvKt.getMetricsPrometheusEndpoint() ? "enabled" : "disabled");
            logger.info("  dashboard  http://localhost:{}/dashboard   - Real-time Visual Charts ({})", DotEnvKt.getWebApiPort(), DotEnvKt.getMetricsDashboardEnabled() ? "enabled" : "disabled");
        }
        logger.info("SERVER_ENV=prod disables account auto-create & password-bypass entirely; dev allows them only from "
                + "localhost/debug-bypass ranks and only with debug=true. debug=true also crashes on thread-ownership "
                + "violations instead of just logging them, in any env.");
        logger.info(worldRulesetSummary());
    }

    private static String worldRulesetSummary() {
        int world = DotEnvKt.getGameWorldId();
        if (world <= 1) {
            return "World " + world + " runs the live ruleset: bank shortcut command + noclip anti-cheat kick active, "
                    + "beta/admin test commands and weapon specials locked.";
        }
        String betaCommands = "admin/test commands unlocked (forcetask, bosspawn, bosstele, tnpc, slay_sim, rehp, boost, tool, pnpc, plunder)";
        String specials = world == 2
                ? "weapon specials enabled; verbose W2-* combat/dispatch debug logging active"
                : "weapon specials still locked (only world 2 gets those)";
        return "World " + world + " runs a beta ruleset: " + betaCommands + "; " + specials + ".";
    }


    public static void logError(String message) {
        Utils.println(message);
    }



    public static void loadObjects() {
        try {
            objects.clear();
            objects.addAll(ObjectDefinitionRepository.loadObjects());
        } catch (Exception e) {
            logger.warn("Failed to load object definitions", e);
        }

    }

    public static void shutdown() {
        if (!SHUTDOWN_STARTED.compareAndSet(false, true)) {
            return;
        }

        gameLoopService.stop(Duration.ofSeconds(10));

        try {
            net.dodian.uber.game.api.plugin.ContentPluginLifecycle.INSTANCE.stop();
        } catch (Exception exception) {
            logger.warn("Failed to stop content plugins", exception);
        }

        try {
            for (Client player : PlayerRegistry.playersOnline.values()) {
                PlayerSaveService.requestSave(player, PlayerSaveReason.SHUTDOWN, true, true);
            }
        } catch (Exception exception) {
            logger.warn("Failed to request final player saves during shutdown", exception);
        }

        try {
            net.dodian.uber.game.netty.login.LoginPreparationService.shutdown(Duration.ofSeconds(10));
        } catch (Exception exception) {
            logger.warn("Failed to shutdown login preparation workers", exception);
        }

        try {
            AccountPersistenceService.shutdownAndDrain(Duration.ofSeconds(30));
        } catch (Exception exception) {
            logger.warn("Failed to drain account persistence service during shutdown", exception);
        }

        try {
            DbDispatchers.shutdown(DbDispatchers.commandExecutor, Duration.ofSeconds(10));
        } catch (Exception exception) {
            logger.warn("Failed to shutdown command DB dispatcher", exception);
        }

        try {
            WorldSavePublisher.shutdown();
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


        try {
            WebApi.stop();
        } catch (Exception exception) {
            logger.warn("Failed to shutdown web API", exception);
        }

        GameThreadTimers.clearAll();
        GameThreadIngress.clearAll();
    }
}
