package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.Constants;
import net.dodian.utilities.Utils;

import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class PlayerHandler {

    private static final Logger logger = Logger.getLogger(PlayerHandler.class.getName());

    private static final ExecutorService clientThreadPool = Executors.newCachedThreadPool();
    private static final AtomicInteger nextSlot = new AtomicInteger(1); // Starts from 1, assuming 0 is reserved

    public static ConcurrentHashMap<Long, Client> playersOnline = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Long, Integer> allOnline = new ConcurrentHashMap<>();
    public static int cycle = 1;

    public static Player[] players = new Player[Constants.maxPlayers];

    public boolean validClient(int index) {
        Client p = (Client) players[index];
        return p != null && !p.disconnected && p.dbId >= 0;
    }

    public Client getClient(int index) {
        return (Client) players[index];
    }

    public PlayerHandler() {
        for (int i = 0; i < Constants.maxPlayers; i++) {
            players[i] = null;
        }
    }

    public void newPlayerClient(SocketChannel socketChannel, String connectedFrom) {
        int slot = findFreeSlot();
        if (slot == -1) {
            logger.warning("No free slots available for a new player connection.");
            return; // no free slot found - world is full
        }

        try {
            socketChannel.configureBlocking(false); // Set non-blocking as early as possible
            Client newClient = new Client(socketChannel, slot);
            newClient.handler = this;
            players[slot] = newClient;
            players[slot].connectedFrom = connectedFrom;
            players[slot].ip = ((InetSocketAddress) socketChannel.getRemoteAddress()).getAddress().hashCode();
            Player.localId = slot;

            clientThreadPool.submit(newClient); // Use thread pool to manage client threads
            logger.info("New client connected from " + connectedFrom + " at slot " + slot);
        } catch (IOException e) {
            logger.severe("Error configuring new client connection: " + e.getMessage());
            closeSocketChannel(socketChannel);
        }
    }

    private int findFreeSlot() {
        int startSlot = nextSlot.get();
        int slot = startSlot;
        do {
            if (players[slot] == null || players[slot].disconnected) {
                nextSlot.set((slot + 1) % Constants.maxPlayers); // Cycle through slots
                return slot;
            }
            slot = (slot + 1) % Constants.maxPlayers;
        } while (slot != startSlot);

        return -1; // No free slots
    }

    private void closeSocketChannel(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (IOException closeError) {
            logger.warning("Error closing socket channel: " + closeError.getMessage());
        }
    }

    public static int getPlayerCount() {
        return (int) playersOnline.values().stream().filter(player -> !player.disconnected).count();
    }

    public static boolean isPlayerOn(String playerName) {
        long playerId = Utils.playerNameToLong(playerName);
        if (playersOnline.containsKey(playerId)) {
            logger.info("Player is already logged in as: " + playerName);
            return true;
        }
        return false;
    }

    public static int getPlayerID(String playerName) {
        long playerId = Utils.playerNameToLong(playerName);
        Client client = playersOnline.get(playerId);
        return client != null ? client.getSlot() : -1;
    }

    public int lastchatid = 1; // PM System

    public void removePlayer(Player plr) {
        Client temp = (Client) plr;
        if (temp != null) {
            temp.destruct(); // Destruct after player has saved!
            logger.info("Finished removing player: '" + temp.getPlayerName() + "'");
            players[temp.getSlot()] = null; // Clear the player from the slot
        } else {
            logger.warning("Tried to remove a null player!");
        }
    }

    public static Player getPlayer(String name) {
        long playerId = Utils.playerNameToLong(name);
        return playersOnline.get(playerId);
    }
}
