package net.dodian.uber.game.model.item;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.player.packets.outgoing.CreateGroundItem;
import net.dodian.uber.game.model.player.packets.outgoing.RemoveGroundItem;

import java.util.concurrent.CopyOnWriteArrayList;

public class Ground {
    public static CopyOnWriteArrayList<GroundItem> items = new CopyOnWriteArrayList<GroundItem>();

    public static void deleteItem(GroundItem item) {
        item.setTaken(true);
        for (int i = 0; i < PlayerHandler.players.length; i++) {
            Client p = Server.playerHandler.getClient(i);
            if (p == null || !p.isActive) {
            } else if (!item.canDespawn) {
                item.dropped = System.currentTimeMillis();
                p.send(new RemoveGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
            } else if (Server.itemManager.isTradable(item.id) || (p.dbId == item.playerId && !Server.itemManager.isTradable(item.id))) {
                p.send(new RemoveGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
            }
        }
        if (item.canDespawn)
            items.remove(item);
    }
}