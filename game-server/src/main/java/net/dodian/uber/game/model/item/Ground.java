package net.dodian.uber.game.model.item;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.outgoing.CreateGroundItem;
import net.dodian.uber.game.model.player.packets.outgoing.RemoveGroundItem;

import java.util.concurrent.CopyOnWriteArrayList;

public class Ground {
    public static CopyOnWriteArrayList<GroundItem> items = new CopyOnWriteArrayList<GroundItem>();

    public static void deleteItem(GroundItem item) {

    }
}