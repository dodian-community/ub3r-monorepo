package net.dodian.uber.game;

import net.dodian.cache.Cache;
import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.ObjectDef;
import net.dodian.cache.object.ObjectLoader;
import net.dodian.cache.region.Region;
import net.dodian.jobs.JobScheduler;
import net.dodian.jobs.impl.*;
import net.dodian.uber.comm.ConnectionList;
import net.dodian.uber.comm.LoginManager;
import net.dodian.uber.comm.Memory;
import net.dodian.uber.comm.SocketHandler;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.ChatLine;
import net.dodian.uber.game.model.Login;
import net.dodian.uber.game.model.ShopHandler;
import net.dodian.uber.game.model.entity.npc.NpcManager;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.item.GroundItem;
import net.dodian.uber.game.model.item.ItemManager;
import net.dodian.uber.game.model.object.DoorHandler;
import net.dodian.uber.game.model.object.RS2Object;
import net.dodian.uber.game.model.player.casino.SlotMachine;
import net.dodian.uber.game.model.player.skills.Thieving;
import net.dodian.utilities.DbTables;
import net.dodian.utilities.Rangable;
import net.dodian.utilities.Utils;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.dodian.DotEnvKt.*;
import static net.dodian.utilities.DatabaseInitializerKt.initializeDatabase;
import static net.dodian.utilities.DatabaseInitializerKt.isDatabaseInitialized;
import static net.dodian.utilities.DatabaseKt.getDbConnection;

/**
 * Testing..
 *
 * @author Arch337
 */
public class Server implements Runnable {

    public static boolean trading = true, dueling = true, chatOn = true, pking = true, dropping = true, banking = true, shopping = true;
    private static int delay = 0;
    public static long lastRunite = 0;
    public static boolean updateAnnounced;
    public static boolean updateRunning;
    public static int updateSeconds;
    public static long updateStartTime;
    public Player player;
    public Client c;
    public static ArrayList<String> connections = new ArrayList<>();
    public static ArrayList<String> banned = new ArrayList<>();
    public static ArrayList<RS2Object> objects = new ArrayList<>();
    public static CopyOnWriteArrayList<ChatLine> chat = new CopyOnWriteArrayList<>();
    public static int nullConnections = 0;
    public static Login login = null;
    public static ItemManager itemManager = null;
    public static NpcManager npcManager = null;
    public static SlotMachine slots = new SlotMachine();
    public static Map<String, Long> tempConns = new HashMap<>();
    public static LoginManager loginManager = null;


    public static void main(String args[]) throws Exception {
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

        new JobScheduler();
        ConnectionList.getInstance();
        playerHandler = new PlayerHandler();
        loginManager = new LoginManager();
        shopHandler = new ShopHandler();
        thieving = new Thieving();
        npcManager = new NpcManager();
        clientHandler = new Server();
        login = new Login();
        itemManager = new ItemManager();

        Memory.getSingleton().process();

        Cache.load();
        // Load regions
        ObjectDef.loadConfig();
        Region.load();
        Rangable.load();
        // Load objects
        ObjectLoader objectLoader = new ObjectLoader();
        objectLoader.load();
        GameObjectData.init();
        loadObjects();
        new DoorHandler();
        setGlobalItems();

        /* Start Threads */
        new Thread(EventManager.getInstance()).start();
        new Thread(npcManager).start();
        new Thread(clientHandler).start(); // launch server listener
        new Thread(login).start();
        //new Thread(new VotingIncentiveManager()).start();
        /* Processes */
        JobScheduler.ScheduleStaticRepeatForeverJob(60000, WorldProcessor.class);
        JobScheduler.ScheduleStaticRepeatForeverJob(600, PlayerProcessor.class);
        JobScheduler.ScheduleStaticRepeatForeverJob(600, ItemProcessor.class);
        JobScheduler.ScheduleStaticRepeatForeverJob(600, ShopProcessor.class);
        JobScheduler.ScheduleStaticRepeatForeverJob(600, GroundItemProcessor.class);
        JobScheduler.ScheduleStaticRepeatForeverJob(600, ObjectProcess.class);
        /* Done loading */
        System.gc();
        System.out.println("Server is now running on world " + getGameWorldId() + "!");
    }

    public static Server clientHandler = null; // handles all the clients
    public static java.net.ServerSocket clientListener = null;
    public static boolean shutdownServer = false; // set this to true in order to
    // shut down and kill the server
    public static boolean shutdownClientHandler; // signals ClientHandler to shut
    // down
    public static PlayerHandler playerHandler = null;
    public static Thieving thieving = null;
    public static ShopHandler shopHandler = null;
    public static boolean antiddos = false;

    public void run() {
        // setup the listener
        try {
            shutdownClientHandler = false;
            clientListener = new java.net.ServerSocket(getServerPort(), 1, null);
            while (true) {
                try {
                    if (clientListener == null)
                        continue;
                    java.net.Socket s = clientListener.accept();
                    if (s == null)
                        continue;
                    s.setTcpNoDelay(true);
                    String connectingHost = "" + s.getRemoteSocketAddress();
                    connectingHost = connectingHost.substring(1, connectingHost.indexOf(":"));
                    if (antiddos && !tempConns.containsKey(connectingHost)) {
                        s.close();
                    } else {
                        ConnectionList.getInstance().addConnection(s.getInetAddress());
                        tempConns.remove(connectingHost);
                        connections.add(connectingHost);
                        if (checkHost(connectingHost)) {
                            nullConnections++;
                            playerHandler.newPlayerClient(s, connectingHost);
                        } else {
                            s.close();
                        }
                    }
                    Thread.sleep(delay);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (java.io.IOException ioe) {
            if (!shutdownClientHandler) {
                Utils.println("Server is already in use.");
            } else {
                Utils.println("ClientHandler was shut down.");
            }
        }
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

    public boolean checkHost(String host) {
        for (String h : banned) {
            if (h.equals(host))
                return false;
        }
        int num = 0;
        for (String h : connections) {
            if (host.equals(h)) {
                num++;
            }
        }
        if (num > 5) {
            //anHost(host, num);
            return false;
        }
        return true;
    }

    public void banHost(String host, int num) {
        try {
            Utils.println("BANNING HOST " + host + " (flooding)");
            banned.add(host);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error banning host " + host);
        }
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
        Ground.items.add(new GroundItem(2611, 3096, 20997, 1, 30 * 1000));
        Ground.items.add(new GroundItem(2612, 3096, 11863, 1, 30 * 1000));
        Ground.items.add(new GroundItem(2563, 9511, 1631, 1, 30 * 1000));
        Ground.items.add(new GroundItem(2564, 9511, 6571, 1, 30 * 1000));
        /* End of troll */
        Ground.items.add(new GroundItem(2605, 3104, 1277, 1, 30 * 1000));
        Ground.items.add(new GroundItem(2607, 3104, 1171, 1, 30 * 1000));
    }

    public static int EnergyRegian = 60;

}
