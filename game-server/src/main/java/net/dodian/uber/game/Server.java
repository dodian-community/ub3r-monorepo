package net.dodian.uber.game;

import net.dodian.cache.Cache;
import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.ObjectDef;
import net.dodian.cache.object.ObjectLoader;
import net.dodian.cache.region.Region;
import net.dodian.jobs.JobScheduler;
import net.dodian.jobs.impl.*;
import net.dodian.uber.comm.LoginManager;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.ChatLine;
import net.dodian.uber.game.model.Login;
import net.dodian.uber.game.model.ShopHandler;
import net.dodian.uber.game.model.entity.npc.NpcManager;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.ItemManager;
import net.dodian.uber.game.model.object.DoorHandler;
import net.dodian.uber.game.model.player.casino.SlotMachine;
import net.dodian.uber.game.model.player.skills.thieving.PyramidPlunder;
import net.dodian.uber.game.model.player.skills.thieving.Thieving;
import net.dodian.uber.game.network.SocketHandler;
import net.dodian.utilities.DotEnvKt;
import net.dodian.utilities.Rangable;
import net.dodian.utilities.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.dodian.utilities.DotEnvKt.*;
import static net.dodian.utilities.DatabaseInitializerKt.initializeDatabase;
import static net.dodian.utilities.DatabaseInitializerKt.isDatabaseInitialized;

public class Server implements Runnable {

    public static boolean trading = true, dueling = true, chatOn = true, pking = true, dropping = true, banking = true, shopping = true;
    private static int delay = 100;
    public static int TICK = 600;
    public static boolean updateRunning;
    public static int updateSeconds;
    public static long updateStartTime, serverStartup;
    public Player player;
    public Client c;
    public static ArrayList<String> connections = new ArrayList<>();
    public static ArrayList<String> banned = new ArrayList<>();
    public static CopyOnWriteArrayList<ChatLine> chat = new CopyOnWriteArrayList<>();
    public static int nullConnections = 0;
    public static Login login = null;
    public static ItemManager itemManager = null;
    public static NpcManager npcManager = null;
    public static LoginManager loginManager = null;
    public static PyramidPlunder entryObject = null;
    public static JobScheduler job = null;
    public static SlotMachine slots = new SlotMachine();
    public static Map<String, Long> tempConns = new HashMap<>();
    private Map<String, Integer> connectionCounts = new ConcurrentHashMap<>();


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
        ObjectLoaderService.loadObjects();
        new DoorHandler();

        new Thread(EventManager.getInstance()).start();
        new Thread(clientHandler).start();
        new Thread(login).start();

        job = new JobScheduler();
        job.ScheduleStaticRepeatForeverJob(TICK, EntityProcessor.class);
        job.ScheduleStaticRepeatForeverJob(TICK, GroundItemProcessor.class);
        job.ScheduleStaticRepeatForeverJob(TICK, ItemProcessor.class);
        job.ScheduleStaticRepeatForeverJob(TICK, ShopProcessor.class);
        job.ScheduleStaticRepeatForeverJob(TICK, ObjectProcess.class);
        job.ScheduleStaticRepeatForeverJob(TICK * 100, WorldProcessor.class);
        entryObject = new PyramidPlunder();

        System.gc();
        serverStartup = System.currentTimeMillis();
        System.out.println("Server is now running on world " + getGameWorldId() + "!");
    }

    public static Server clientHandler = null;
    public static Selector selector = null;
    public static boolean shutdownServer = false;
    public static boolean shutdownClientHandler;
    public static PlayerHandler playerHandler = null;
    public static Thieving thieving = null;
    public static ShopHandler shopHandler = null;
    public static boolean antiddos = false;


    public static ServerSocketChannel serverSocketChannel = null;

    public void run() {
        System.out.println("Starting server listener.");

        try {
            shutdownClientHandler = false;
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new java.net.InetSocketAddress(DotEnvKt.getServerPort()));
            serverSocketChannel.configureBlocking(false);
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server listening on port: " + DotEnvKt.getServerPort());

            while (!shutdownClientHandler) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectedKeys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
               Thread.sleep(delay);
            }
        } catch (Exception e) {
            if (!shutdownClientHandler) {
                System.out.println("Server is already in use.");
            } else {
                System.out.println("ClientHandler was shut down.");
            }
            e.printStackTrace();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverChannel.accept();
        socketChannel.configureBlocking(false);
        SelectionKey clientKey = socketChannel.register(selector, SelectionKey.OP_READ);

        String connectingHost = socketChannel.getRemoteAddress().toString();
        connectingHost = connectingHost.substring(1, connectingHost.indexOf(":"));
        System.out.println("Connection attempt from: " + connectingHost);

        if (antiddos) {
            if (tempConns.containsKey(connectingHost)) {
                tempConns.remove(connectingHost);
            } else {
                System.out.println("Connection from " + connectingHost + " denied due to anti-DDOS measures.");
                closeChannel(socketChannel);
                return;
            }
        }

        synchronized (connections) {
            connections.add(connectingHost);
        }

        if (checkHost(connectingHost)) {
            nullConnections++;
            System.out.println("Host " + connectingHost + " accepted. Total connections: " + connections.size());
            Client newClient = playerHandler.newPlayerClient(socketChannel, connectingHost);
            clientKey.attach(newClient);
        } else {
            nullConnections++;
            System.out.println("Host " + connectingHost + " accepted. Total connections: " + connections.size());
            Client newClient = playerHandler.newPlayerClient(socketChannel, connectingHost);
            clientKey.attach(newClient);
        }
    }


    private void closeChannel(SocketChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            // Handle the exception appropriately (e.g., log the error)
            e.printStackTrace();
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = 0;
        try {
            bytesRead = socketChannel.read(buffer);
        } catch (IOException e) {
            if (e instanceof java.net.SocketException && e.getMessage().equals("Connection reset")) {
                System.out.println("Connection reset by client: " + socketChannel.getRemoteAddress());
                closeChannel(socketChannel);
                return;
            }
            throw e;
        }

        if (bytesRead == -1) {
            handleConnectionClosed(socketChannel);
        } else {
            buffer.flip();
            Client client = (Client) key.attachment();
            if (client != null) {
                client.processData(buffer);
            } else {
                // Handle the case when the client is not attached to the key
                System.out.println("No client attached to the key for socket: " + socketChannel.getRemoteAddress());
                closeChannel(socketChannel);
            }
        }
    }

    private void handleConnectionClosed(SocketChannel socketChannel) {
        try {
            System.out.println("Connection closed by client: " + socketChannel.getRemoteAddress());
            closeChannel(socketChannel);
        } catch (IOException e) {
            // Handle the exception appropriately (e.g., log the error)
            e.printStackTrace();
        }
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
      //  if (banned.contains(host)) {
       //     return false;
      //  }

        int count = connectionCounts.getOrDefault(host, 0);
       // if (count > 5) {
            // banHost(host, count);
         //   return false;
       // }

        connectionCounts.put(host, count + 1);
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
}