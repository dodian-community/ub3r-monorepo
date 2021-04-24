package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.Constants;
import net.dodian.utilities.Utils;

import java.util.concurrent.ConcurrentHashMap;

public class PlayerHandler {

  public static ConcurrentHashMap<Long, Client> playersOnline = new ConcurrentHashMap<>();
  public static int cycle = 0;
  public static Player players[] = new Player[Constants.maxPlayers];
  public static int playerSlotSearchStart = 1; // where we start searching at when
                                        // adding a new player
  public static String kickNick = "";
  public static int playerCount = 0;
  public static String playersCurrentlyOn[] = new String[Constants.maxPlayers];

  // public static ArrayList<PkMatch> matches = new ArrayList<PkMatch>();
  public boolean validClient(int index) {
    Client p = (Client) players[index];
    return p != null && !p.disconnected && p.dbId > 0;
  }

  public Client getClient(int index) {
    return ((Client) players[index]);
  }

  public PlayerHandler() {
    for (int i = 0; i < Constants.maxPlayers; i++) {
      players[i] = null;
    }
  }
  
//	public static boolean isPlayerOn(String playerName) {
//		for (int i = 0; i < Constants.maxPlayers; i++) {
//			if (players[i] != null) {
//				if (players[i].getPlayerName().equalsIgnoreCase(playerName)) {
//					return true;
//				}
//			}
//		}
//		return false;
//	}

  public void newPlayerClient(java.net.Socket s, String connectedFrom) {
    int slot = -1;
    for (int i = 1; i < players.length; i++) {
      if (players[i] == null || players[i].disconnected) {
        slot = i;
        break;
      }
    }

    Client newClient = new Client(s, slot);
    newClient.handler = this;
    (new Thread(newClient)).start();
    if (slot == -1)
      return;
    players[slot] = newClient;
    players[slot].connectedFrom = connectedFrom;
    Player.localId = slot;
    players[slot].lastPacket = System.currentTimeMillis();
    playerSlotSearchStart = slot + 1;
    if (playerSlotSearchStart > Constants.maxPlayers)
      playerSlotSearchStart = 1;
  }

  public static int getPlayerCount() {
    int count = 0;
    for (int i = 0; i < players.length; i++) {
      if (players[i] != null && !players[i].disconnected && players[i].dbId > 0) {
        count++;
      }
    }
    return count;
  }

  public void updatePlayerNames() {
    playerCount = 0;
    for (int i = 0; i < Constants.maxPlayers; i++) {
      if (players[i] != null) {
        playersCurrentlyOn[i] = players[i].getPlayerName();
        playerCount++;
      } else
        playersCurrentlyOn[i] = "";
    }
  }

  public static boolean isPlayerOn(String playerName) {
    return playersOnline.containsKey(Utils.playerNameToInt64(playerName));
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
    if (plr == null)
      return;
    Client temp = (Client) plr;
    if (temp != null && temp.dbId > 0 && temp.saveNeeded) {
      temp.saveStats(true);
      Utils.println("Disconnecting lagged out valid player " + plr.getPlayerName());
    }
    plr.destruct();
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