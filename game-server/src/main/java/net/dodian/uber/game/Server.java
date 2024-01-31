package net.dodian.uber.game;

import io.netty.bootstrap.ServerBootstrap;
import net.dodian.cache.Cache;
import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.ObjectDef;
import net.dodian.cache.object.ObjectLoader;
import net.dodian.cache.region.Region;
import net.dodian.jobs.JobScheduler;
import net.dodian.jobs.impl.*;
import net.dodian.uber.comm.SocketHandler;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.ChatLine;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.ShopHandler;
import net.dodian.uber.game.model.entity.npc.NpcManager;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.item.GroundItem;
import net.dodian.uber.game.model.item.ItemManager;
import net.dodian.uber.game.model.object.DoorHandler;
import net.dodian.uber.game.model.object.RS2Object;
import net.dodian.uber.game.model.player.PlayerHandler;
import net.dodian.uber.game.model.player.casino.SlotMachine;
import net.dodian.uber.game.model.player.skills.Thieving;
import net.dodian.utilities.DbTables;
import net.dodian.utilities.Rangable;
import net.dodian.utilities.Utils;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.dodian.uber.game.GameThreadKt.runGameThread;
import static net.dodian.uber.game.GameThreadKt.startClientListener;
import static net.dodian.utilities.DotEnvKt.*;
import static net.dodian.utilities.DatabaseInitializerKt.initializeDatabase;
import static net.dodian.utilities.DatabaseInitializerKt.isDatabaseInitialized;
import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class Server implements Runnable {

    public static boolean trading = true, dueling = true, chatOn = true, pking = true, dropping = true, banking = true, shopping = true;
    private static int delay = 30;
    public static int TICK = 600;
    public static boolean updateRunning;
    public static int updateSeconds;
    public static double updateElapsed = 0.0;
    public static long updateStartTime;
    public static ArrayList<String> connections = new ArrayList<>();
    public static ArrayList<String> banned = new ArrayList<>();
    public static ArrayList<RS2Object> objects = new ArrayList<>();
    public static CopyOnWriteArrayList<ChatLine> chat = new CopyOnWriteArrayList<>();
    public static int nullConnections = 0;
    public static ItemManager itemManager = null;
    public static NpcManager npcManager = null;
    public static JobScheduler job = null;
    public static SlotMachine slots = new SlotMachine();


    public static void main(String[] args) throws Exception {
        System.out.println();
        System.out.println("    ____            ___               ");
        System.out.println("   / __ \\____  ____/ (_)___ _____    ");
        System.out.println("  / / / / __ \\/ __  / / __ `/ __ \\  ");
        System.out.println(" / /_/ / /_/ / /_/ / / /_/ / / / /    ");
        System.out.println("/_____/\\____/\\____/_/\\____/_/ /_/  ");
        System.out.println();

        if (getDatabaseInitialize() && !isDatabaseInitialized()) {
            initializeDatabase();
        }
        //ConnectionList.getInstance(); //Let us not utilize this for now!
        /* NPC Data*/
        npcManager = new NpcManager();
        npcManager.loadSpawns();
        System.out.println("[NpcManager] DONE LOADING NPC CONFIGURATION");
        /* Player Stuff */
        itemManager = new ItemManager();
        playerHandler = new PlayerHandler();
        shopHandler = new ShopHandler();
        thieving = new Thieving();
        setGlobalItems();
        /* Load cache */
        Cache.load();
        ObjectDef.loadConfig();
        Region.load();
        Rangable.load();
        // Load objects
        ObjectLoader objectLoader = new ObjectLoader();
        objectLoader.load();
        GameObjectData.init();
        loadObjects(); //sql disabled
        new DoorHandler(); //sql disabled
        /* Start Threads */
        new Thread(EventManager.getInstance()).start();

        //new Thread(new VotingIncentiveManager()).start();
        /* Processes */
        job = new JobScheduler();
        JobScheduler.ScheduleStaticRepeatForeverJob(TICK, EntityProcessor.class);
        //job.ScheduleStaticRepeatForeverJob(TICK, GroundItemProcessor.class); //Ground item revolved inside entity process!
        JobScheduler.ScheduleStaticRepeatForeverJob(TICK, ItemProcessor.class);
        JobScheduler.ScheduleStaticRepeatForeverJob(TICK, ShopProcessor.class);
        JobScheduler.ScheduleStaticRepeatForeverJob(TICK, ObjectProcess.class);
        JobScheduler.ScheduleStaticRepeatForeverJob(TICK, WorldProcessor.class);
        /* Done loading */

        startClientListener();

        System.gc();
        System.out.println("Server is now running on world " + getGameWorldId() + "!");
    }

    public static boolean shutdownServer = false; // set this to true in order to shut down and kill the server
    public static PlayerHandler playerHandler = null;
    public static Thieving thieving = null;
    public static ShopHandler shopHandler = null;

    public void run() {
        runGameThread();
    }

    public static Thread createNewConnection(SocketHandler socketHandler) {
        return new Thread(socketHandler);
    }

    public static void logError(String message) {
        Utils.println(message);
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

    public static void setGlobalItems() { //I set global item spawn here as I do not have a config file for it yet!
        //Yanille
		/*
		Ground.items.add(new GroundItem(2642, 3123, 401, 1, 30 * 1000));
		Ground.items.add(new GroundItem(2641, 3123, 401, 1, 30 * 1000));
		Ground.items.add(new GroundItem(2641, 3122, 401, 1, 30 * 1000));
		//CAtherby
		Ground.items.add(new GroundItem(2849, 3427, 401, 1, 30 * 1000));
		Ground.items.add(new GroundItem(2850, 3427, 401, 1, 30 * 1000));
		Ground.items.add(new GroundItem(2850, 3426, 401, 1, 30 * 1000));
		Ground.items.add(new GroundItem(2849, 3428, 401, 1, 30 * 1000));
		Ground.items.add(new GroundItem(2849, 3429, 401, 1, 30 * 1000));
		Ground.items.add(new GroundItem(2848, 3429, 401, 1, 30 * 1000));
		Ground.items.add(new GroundItem(2848, 3430, 401, 1, 30 * 1000));
		for(int i = 0; i < 4; i++)
			Ground.items.add(new GroundItem(2839 + i, 3433, 401, 1, 30 * 1000));
		Ground.items.add(new GroundItem(2842, 3432, 401, 1, 30 * 1000));
		*/
        /* Troll items */
        Ground.items.add(new GroundItem(new Position(2611, 3096, 0), 11862, 1, 60 * 1000));
        Ground.items.add(new GroundItem(new Position(2612, 3096, 0), 11863, 1, 60 * 1000));
        Ground.items.add(new GroundItem(new Position(2563, 9511, 0), 1631, 1, 60 * 1000));
        Ground.items.add(new GroundItem(new Position(2564, 9511, 0), 6571, 1, 60 * 1000));
        /* Yanille starter items */
        Ground.items.add(new GroundItem(new Position(2605, 3104, 0), 1277, 1, 20 * 1000));
        Ground.items.add(new GroundItem(new Position(2607, 3104, 0), 1171, 1, 20 * 1000));
        /* Snape grass spawns!*/
        Ground.items.add(new GroundItem(new Position(2810, 3203, 0), 231, 1, 60 * 1000));
        Ground.items.add(new GroundItem(new Position(2807, 3204, 0), 231, 1, 60 * 1000));
        Ground.items.add(new GroundItem(new Position(2804, 3207, 0), 231, 1, 60 * 1000));
        Ground.items.add(new GroundItem(new Position(2801, 3210, 0), 231, 1, 60 * 1000));
        /* Limpwurt spawns!*/
        Ground.items.add(new GroundItem(new Position(2874, 3475, 0), 225, 1, 60 * 1000));
        Ground.items.add(new GroundItem(new Position(2876, 3001, 0), 225, 1, 60 * 1000));
        /* White berries spawns!*/
        Ground.items.add(new GroundItem(new Position(2935, 3489, 0), 239, 1, 60 * 1000));
        Ground.items.add(new GroundItem(new Position(2877, 3000, 0), 239, 1, 60 * 1000));
    }

}
