package net.dodian.uber.game.model;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.network.packets.outgoing.SendMessage;

public class YellSystem {

    public static void yell(String message) {
        for (Player p : PlayerHandler.players) {
            if (message.indexOf("tradereq") > 0 || message.indexOf("duelreq") > 0)
                return;
            if (p == null || !p.isActive)
                continue;
            Client temp = (Client) p;
            if (temp.getPosition().getX() > 0 && temp.getPosition().getY() > 0)
                if (temp != null && !temp.disconnected && p.isActive)
                    temp.send(new SendMessage(message));
        }
    }

    /**
     * Sends a message to all the online staff.
     *
     * @param message The message to send.
     */
    public static void alertStaff(String message) {
        for (Player p : PlayerHandler.players) {
            if (message.indexOf("tradereq") > 0 || message.indexOf("duelreq") > 0)
                return;
            if (p == null || !p.isActive)
                continue;
            Client temp = (Client) p;
            if (temp.getPosition().getX() > 0 && temp.getPosition().getY() > 0)
                if (temp != null && !temp.disconnected && p.isActive && p.playerRights >= 1) {
                    temp.send(new SendMessage(message));
                }
        }
    }

}
