/**
 *
 */
package net.dodian.uber.comm;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.utilities.Utils;

import java.util.HashMap;

/**
 * @author Owner
 *
 */
public class Receiver {
    public int addPlayer(long name, int world) {
        System.out.println("Adding " + name + " (world-" + world + ")");
        //PlayerHandler.playersOnline.put(name, world);
        updatePlayersWithFriend(name);
        return 1;
    }

    public int removePlayer(long name) {
        System.out.println("Removing " + name);
        PlayerHandler.playersOnline.remove(name);
        updatePlayersWithFriend(name);
        return 1;
    }

    public void updatePlayersWithFriend(long name) {
        for (Client c : PlayerHandler.playersOnline.values()) {
            if (c.hasFriend(name)) {
                c.refreshFriends();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public int deliverPm(HashMap data) {
        long sender = (Long) data.get("from");
        int rights = (Integer) data.get("rights");
        long to = (Long) data.get("to");
        String msg = (String) data.get("msg");
        byte[] bytes = Utils.encodePm(msg);
        int len = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                len = i + 1;
                break;
            }
        }
        if (PlayerHandler.playersOnline.containsKey(to)) {
            Client target = PlayerHandler.playersOnline.get(to);
            target.sendpm(sender, rights, bytes, len);
            return 1;
        } else {
            System.out.println("Undelivered pm");
            return -1;
        }
    }

    public int changeSetting(int setting, int value) {
        switch (setting) {
            case 1: // trading
                Server.trading = (value == 1);
                break;
            case 2: // dueling
                Server.dueling = (value == 1);
                break;
            case 3: // pking
                Server.pking = (value == 1);
                break;
            case 4: // dropping
                Server.dropping = (value == 1);
                break;
            case 7:
                Server.updateSeconds = value;
                Server.updateRunning = true;
                Server.updateStartTime = System.currentTimeMillis();
                Server.trading = false;
                Server.dueling = false;
                Server.dropping = false;
                yell("The server is being reset, please logout to save your progress");
                yell("It will be back up again within 2 minutes");
                break;
            default:
                return 0; // unknown var
        }
        System.out.println("Setting " + setting + " changed (" + value + ")");
        return 1; // ok
    }

    private void yell(String message) {
        for (Player p : PlayerHandler.players) {
            if (p == null || !p.isActive) {
                continue;
            }
            Client temp = (Client) p;
            if (temp.getPosition().getX() > 0 && temp.getPosition().getY() > 0) {
                if (temp != null && !temp.disconnected && p.isActive) {
                    temp.send(new SendMessage(message));
                }
            }
        }
    }

    public int kick(long name) {
        Client target = PlayerHandler.playersOnline.get(name);
        if (target != null) {
            target.logout();
        } else
            return -1;
        return 1;
    }
}
