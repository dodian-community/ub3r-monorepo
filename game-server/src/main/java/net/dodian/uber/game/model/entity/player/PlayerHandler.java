package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.comm.Memory;
import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.engine.loop.GameThreadTaskQueue;
import net.dodian.utilities.Utils;

import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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
        return playersOnline.size();
    }

    public static void forEachActivePlayer(Consumer<Client> consumer) {
        if (consumer == null) {
            return;
        }
        for (Client client : playersOnline.values()) {
            if (isActiveClient(client)) {
                consumer.accept(client);
            }
        }
    }

    public static List<Client> snapshotActivePlayers() {
        List<Client> activePlayers = new ArrayList<>(Math.max(1, playersOnline.size()));
        forEachActivePlayer(activePlayers::add);
        return activePlayers;
    }

    private static boolean isActiveClient(Client client) {
        return client != null
                && client.isActive
                && !client.disconnected
                && client.getChannel() != null
                && client.getChannel().isActive();
    }

    public static boolean isPlayerOn(String playerName) {
        long playerId = Utils.playerNameToLong(playerName);
        Client existing = playersOnline.get(playerId);
        if (existing == null) {
            return false;
        }

        // Treat stale/disconnected sessions as offline. This avoids relog loops where the Netty
        // disconnect cleanup hasn't been drained by the game thread yet.
        boolean stale =
                existing.disconnected ||
                !existing.isActive ||
                existing.getChannel() == null ||
                !existing.getChannel().isActive();
        if (stale) {
            playersOnline.remove(playerId, existing);
            GameThreadTaskQueue.submit(() -> Server.playerHandler.removePlayer(existing));
            return false;
        }

        logger.info("Player is already logged in as: " + playerName);
        return true;
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
            playersOnline.remove(temp.longName, temp);
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
