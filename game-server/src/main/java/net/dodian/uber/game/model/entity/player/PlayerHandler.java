package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.Constants;
import net.dodian.utilities.Utils;

import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.io.IOException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.BitSet;
import java.util.logging.Logger;

public class PlayerHandler {

    private static final Logger logger = Logger.getLogger(PlayerHandler.class.getName());

    static final BitSet usedSlots = new BitSet(Constants.maxPlayers + 1);

    public static ConcurrentHashMap<Long, Client> playersOnline = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Long, Integer> allOnline = new ConcurrentHashMap<>();
    public static int cycle = 1;

    public static Player[] players = new Player[Constants.maxPlayers + 1];

    public boolean validClient(int index) {
        Client p = (Client) players[index];
        return p != null && !p.disconnected && p.dbId >= 0;
    }

    public Client getClient(int index) {
        return (Client) players[index];
    }

    public PlayerHandler() {
        for (int i = 1; i <= Constants.maxPlayers; i++) {
            players[i] = null;
        }
    }

    public void newPlayerClient(SocketChannel socketChannel, String connectedFrom) {
        int slot = findFreeSlot();
        if (slot == -1 || slot > Constants.maxPlayers) {
            logger.warning("No free slots available for a new player connection.");
            closeSocketChannel(socketChannel);
            return;
        }

        Client newClient = null;

        try {
            socketChannel.configureBlocking(false);
            newClient = new Client(socketChannel, slot);
            newClient.handler = this;
            players[slot] = newClient;
            players[slot].connectedFrom = connectedFrom;
            players[slot].ip = ((InetSocketAddress) socketChannel.getRemoteAddress()).getAddress().hashCode();
            newClient.run(); // Directly run the client instead of submitting to a thread pool
            logger.info("New client connected from " + connectedFrom + " at slot " + slot);

            // Mark the slot as used only after successful login
            if (newClient.isActive) {
                Player.localId = slot;
                usedSlots.set(slot);
            }
        } catch (IOException e) {
            logger.severe("Error configuring new client connection: " + e.getMessage());
            closeSocketChannel(socketChannel);
        }
    }

    private int findFreeSlot() {
        return usedSlots.nextClearBit(1);
    }

    private void closeSocketChannel(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (IOException closeError) {
            logger.warning("Error closing socket channel: " + closeError.getMessage());
        }
    }

    public static int getPlayerCount() {
        return usedSlots.get(1, Constants.maxPlayers + 1).cardinality();
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
            temp.destruct();
            logger.info("Finished removing player: '" + temp.getPlayerName() + "'");
            int slot = temp.getSlot();
            players[slot] = null;
            if (temp.isActive && slot >= 1 && slot <= Constants.maxPlayers) {
                usedSlots.clear(slot); // Mark the slot as available
            }
            playersOnline.remove(Utils.playerNameToLong(temp.getPlayerName()));
        } else {
            logger.warning("Tried to remove a null player!");
        }
    }

    public static Player getPlayer(String name) {
        long playerId = Utils.playerNameToLong(name);
        return playersOnline.get(playerId);
    }
}