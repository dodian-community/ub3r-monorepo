package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.Constants;
import net.dodian.utilities.Utils;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerHandler {

    public static ConcurrentHashMap<Long, Client> playersOnline = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Long, Integer> allOnline = new ConcurrentHashMap<>();
    public static int cycle = 1;
    //Players online!
    public static Player[] players = new Player[Constants.maxPlayers];

    // public static ArrayList<PkMatch> matches = new ArrayList<PkMatch>();
    public boolean validClient(int index) {
        Client p = (Client) players[index];
        return p != null && !p.disconnected && p.dbId >= 0;
    }

    public Client getClient(int index) {
        return ((Client) players[index]);
    }

    public PlayerHandler() {
        for (int i = 0; i < Constants.maxPlayers; i++) {
            players[i] = null;
        }
    }

    public Client newPlayerClient(SocketChannel socketChannel, String connectedFrom) {
        int slot = -1;
        synchronized (PlayerHandler.players) {
            for (int i = 1; i < PlayerHandler.players.length; i++) {
                if (PlayerHandler.players[i] == null || PlayerHandler.players[i].disconnected) {
                    slot = i;
                    break;
                }
            }
        }

        if (slot == -1) {
            System.out.println("No free slot found - world is full");
            return null; // no free slot found - world is full
        }

        Client newClient = new Client(socketChannel, slot);
        newClient.handler = this;
        synchronized (PlayerHandler.players) {
            PlayerHandler.players[slot] = newClient;
        }
        newClient.connectedFrom = connectedFrom;
        newClient.ip = socketChannel.socket().getInetAddress().hashCode();
        Player.localId = slot;
        System.out.println("New player client initialized. Slot: " + slot + ", IP: " + newClient.ip);

        // Initialize login process for the new client asynchronously
        CompletableFuture.runAsync(() -> newClient.run());

        return newClient;
    }

    public static int getPlayerCount() {
        int count = 0;
        for (Player player : players) {
            if (player != null && !player.disconnected) {
                count++;
            }
        }
        return count;
    }

    public static boolean isPlayerOn(String playerName) { //Already logged in!
        int otherPIndex = getPlayerID(playerName);
        if (otherPIndex != -1) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            System.out.println("[" + timestamp + "] is already logged in as: " + Utils.playerNameToLong(playerName));
            return true;
        }
        /*if (PlayerHandler.playersOnline.containsKey(Utils.playerNameToLong(playerName))) { //Old code test!
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            System.out.println("[" + timestamp + "] is already logged in as: " + Utils.playerNameToLong(playerName));
            return true;
        }*/ //old code!
        return false;
    }

    public static int getPlayerID(String playerName) {
        if (PlayerHandler.playersOnline.containsKey(Utils.playerNameToLong(playerName))) {
            return PlayerHandler.playersOnline.get(Utils.playerNameToLong(playerName)).getSlot();
        }
        return -1;
    }

    public int lastchatid = 1; // PM System

    public void removePlayer(Player plr) {
        Client temp = (Client) plr;
        if (temp != null) {
            temp.destruct(); //Destruct after player have saved!
            temp.println_debug("Finished sending a destruct to remove the player... '"+temp.getPlayerName()+"'");
        } else System.out.println("tried to remove nulled player!");
    }

    public static Player getPlayer(String name) {
        for (int i = 0; i < Constants.maxPlayers; i++) {
            if (players[i] != null && players[i].getPlayerName().equalsIgnoreCase(name)) {
                return players[i];
            }
        }
        return null;
    }

}