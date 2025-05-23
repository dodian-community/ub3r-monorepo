package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.comm.Memory;
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
    static final Object SLOT_LOCK = new Object();

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
        int slot;
        synchronized (SLOT_LOCK) {
            slot = findFreeSlot();
            if (slot == -1 || slot > Constants.maxPlayers) {
                logger.warning("No free slots available for a new player connection.");
                closeSocketChannel(socketChannel);
                return;
            }
        }

        logger.info("Attempting to create new client in slot: " + slot);

        Client newClient = null;

        try {
            socketChannel.configureBlocking(false);
            newClient = new Client(socketChannel, slot);
            newClient.handler = this;
            players[slot] = newClient;
            players[slot].connectedFrom = connectedFrom;
            players[slot].ip = ((InetSocketAddress) socketChannel.getRemoteAddress()).getAddress().hashCode();
            newClient.run(); //TODO thread pool would be better
           // logger.info("New client connected from " + connectedFrom + " at slot " + slot);

            // Mark the slot as used only after successful login
            if (newClient.isActive) {
                Player.localId = slot;
                playersOnline.put(Utils.playerNameToLong(newClient.getPlayerName()), newClient);
                logger.info("Player " + newClient.getPlayerName() + " successfully added to slot " + slot);
            } else {
                logger.info("Client created but not active for slot " + slot);
                synchronized (SLOT_LOCK) {
                    usedSlots.clear(slot);  // Free the slot if login wasn't successful
                }
            }

            Memory.getSingleton().process(); // Print memory usage after adding player
        } catch (IOException e) {
            logger.severe("Error configuring new client connection: " + e.getMessage());
            closeSocketChannel(socketChannel);
            synchronized (SLOT_LOCK) {
                usedSlots.clear(slot);  // Free the slot if an exception occurred
            }
        }
    }

    private int findFreeSlot() {
        synchronized (SLOT_LOCK) {
            for (int i = 1; i <= Constants.maxPlayers; i++) {
                if (!usedSlots.get(i)) {
                    usedSlots.set(i);  // Mark the slot as used immediately
                    return i;
                }
            }
        }
        return -1;
    }

    private void closeSocketChannel(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (IOException closeError) {
            logger.warning("Error closing socket channel: " + closeError.getMessage());
        }
    }

    public static int getPlayerCount() {
        return usedSlots.cardinality();
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
                synchronized (SLOT_LOCK) {
                    usedSlots.clear(slot); // Mark the slot as available
                }
            }
            playersOnline.remove(Utils.playerNameToLong(temp.getPlayerName()));
        } else {
            logger.warning("Tried to remove a null player!");
        }

        Memory.getSingleton().process(); // Print memory usage after removing player
    }

    public static Player getPlayer(String name) {
        long playerId = Utils.playerNameToLong(name);
        return playersOnline.get(playerId);
    }




}
