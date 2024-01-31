package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.Constants;
import net.dodian.utilities.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerHandler {

    public static ConcurrentHashMap<Long, Client> playersOnline = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Long, Integer> allOnline = new ConcurrentHashMap<Long, Integer>();
    public static int cycle = 1;
    //Players online!
    public static Player[] players = new Player[Constants.maxPlayers];
    public static String[] playersCurrentlyOn = new String[Constants.maxPlayers];
    public static int playerCount = 0;

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

    public void newPlayerClient(java.net.Socket s, String connectedFrom) {
        int slot = -1;
        for (int i = 1; i < players.length; i++) {
            if (players[i] == null || players[i].disconnected) {
                slot = i;
                break;
            }
        }
        if (slot == -1)
            return; // no free slot found - world is full
        Client newClient = new Client(s, slot);
        newClient.handler = this;
        (new Thread(newClient)).start();
        players[slot] = newClient;
        players[slot].connectedFrom = connectedFrom;
        players[slot].ip = s.getInetAddress().hashCode();
        Player.localId = slot;
        players[slot].lastPacket = System.currentTimeMillis();
    }
    public void destruct() {
        for (int i = 0; i < Constants.maxPlayers; i++) {
            if (players[i] == null)
                continue;
            players[i].destruct();
            players[i] = null;
        }
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

    public void updatePlayerNames() {
        playerCount = 0;
        for (int i = 0; i < Constants.maxPlayers; i++) {
            if (players[i] != null && !players[i].disconnected) {
                playersCurrentlyOn[i] = players[i].getPlayerName();
                playerCount++;
            } else
                playersCurrentlyOn[i] = "";
        }
    }

    public static boolean isPlayerOn(String playerName) { //Already logged in!
        if (PlayerHandler.allOnline.containsKey(Utils.playerNameToLong(playerName))) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            System.out.println("[" + timestamp + "] is already logged in as: " + Utils.playerNameToLong(playerName));
            return true;
        }
        return false;
    }

    public static int getPlayerID(String playerName) {
        for (int i = 0; i < Constants.maxPlayers; i++) {
            if (playersCurrentlyOn[i] != null) {
                if (playersCurrentlyOn[i].equalsIgnoreCase(playerName))
                    return i;
            }
        }
        return -1;
    }

    public int lastchatid = 1; // PM System

    public void removePlayer(Player plr) {
        Client temp = (Client) plr;
        if (temp != null) {
            if(temp.dbId > 0 && temp.saveNeeded)
                temp.saveStats(true);
            PlayerHandler.playersOnline.remove(temp.longName);
            PlayerHandler.allOnline.remove(temp.longName);
            temp.println_debug("Sending a destruct to remove the player... '"+temp.getPlayerName()+"'");
            temp.destruct();
        }
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