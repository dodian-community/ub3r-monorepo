package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.comm.Memory;
import net.dodian.uber.game.Constants;
import net.dodian.utilities.Utils;

import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.io.IOException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.BitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerHandler {

    private static final Logger logger = LoggerFactory.getLogger(PlayerHandler.class);
    public static final Object SLOT_LOCK = new Object();

    public static final BitSet usedSlots = new BitSet(Constants.maxPlayers + 1);

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

//    public void newPlayerClient(SocketChannel socketChannel, String connectedFrom) {
//        int slot;
//        synchronized (SLOT_LOCK) {
//            slot = findFreeSlot();
//            if (slot == -1 || slot > Constants.maxPlayers) {
//                logger.warn("No free slots available for a new player connection.");
//                closeSocketChannel(socketChannel);
//                return;
//            }
//        }
//
//        logger.info("Attempting to create new client in slot: " + slot);
//
//        Client newClient = null;
//
//        try {
//            socketChannel.configureBlocking(false);
//
//            newClient = new Client(socketChannel, slot);
//            newClient.handler = this;
//            newClient.connectedFrom = connectedFrom;
//            newClient.ip = ((InetSocketAddress) socketChannel.getRemoteAddress()).getAddress().hashCode();
//
//            try {
//                newClient.run(); //TODO thread pool would be better
//                // Only add client players array if login was successful
//                if (newClient.isActive) {
//                    players[slot] = newClient;
//                    Player.localId = slot;
//                    playersOnline.put(Utils.playerNameToLong(newClient.getPlayerName()), newClient);
//                } else {
//                    logger.warn("Login failed - Client not active for slot {}", slot);
//                    synchronized (SLOT_LOCK) {
//                        usedSlots.clear(slot);
//                    }
//                }
//            } catch (Exception e) {
//                logger.error("Error during client initialization: {}", e.getMessage(), e);
//                logger.error("Player array state during error: {}", getPlayerArrayState());
//                throw e; // Re-throw to be handled by the outer try-catch
//            }
//
//            Memory.getSingleton().process(); // Print memory usage after adding player
//        } catch (Exception e) {
//            logger.error("Error processing new client connection: {}", e.getMessage(), e);
//            e.printStackTrace();  // This will give us more detailed error information
//            closeSocketChannel(socketChannel);
//            synchronized (SLOT_LOCK) {
//                usedSlots.clear(slot);  // Free the slot if an exception occurred
//            }
//        }
//    }

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

//    private void closeSocketChannel(SocketChannel socketChannel) {
//        try {
//            socketChannel.close();
//        } catch (IOException closeError) {
//            logger.warn("Error closing socket channel: {}", closeError.getMessage(), closeError);
//        }
//    }
    

//    private String getPlayerArrayState() {
//        StringBuilder sb = new StringBuilder("[");
//        for (int i = 0; i < players.length; i++) {
//            if (players[i] != null) {
//                if (sb.length() > 1) sb.append(", ");
//                sb.append(i).append(":").append(players[i].getPlayerName());
//                if (!players[i].isActive) sb.append(" (inactive)");
//            }
//        }
//        return sb.append("]").toString();
//    }

    // Returns nearby players within the 3x3 region neighbourhood (64-tile regions).
    public static java.util.List<Player> getLocalPlayers(Player p) {
        int mx = p.getPosition().getX() >> 6;
        int my = p.getPosition().getY() >> 6;
        java.util.List<Player> list = new java.util.ArrayList<>(256);
        for (Player other : players) {
            if (other == null || !other.isActive || other == p) continue;
            int ox = other.getPosition().getX() >> 6;
            int oy = other.getPosition().getY() >> 6;
            if (Math.abs(mx - ox) <= 1 && Math.abs(my - oy) <= 1) {
                list.add(other);
            }
        }
        return list;
    }

    public static int getPlayerCount() {
        return (int) playersOnline.size();
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
            logger.info("Finished removing player: '{}' slot={} active={} disconnected={}",
                    temp.getPlayerName(), temp.getSlot(), temp.isActive, temp.disconnected);
            int slot = temp.getSlot();
            if (slot >= 1 && slot <= Constants.maxPlayers) {
                synchronized (SLOT_LOCK) {
                    usedSlots.clear(slot); // Mark the slot as available for all disconnect paths
                }
                players[slot] = null;
            }
            playersOnline.remove(Utils.playerNameToLong(temp.getPlayerName()));
            temp.isActive = false;
            temp.disconnected = true;
        } else {
            logger.warn("Tried to remove a null player!");
        }

        Memory.getSingleton().process(); // Print memory usage after removing player
    }

    public static Player getPlayer(String name) {
        long playerId = Utils.playerNameToLong(name);
        return playersOnline.get(playerId);
    }
}
