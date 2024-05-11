package net.dodian.uber.game.model.item;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;

import java.util.concurrent.CopyOnWriteArrayList;

public class Ground {
    public static CopyOnWriteArrayList<GroundItem> ground_items = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<GroundItem> untradeable_items = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<GroundItem> tradeable_items = new CopyOnWriteArrayList<>();

    public static void deleteItem(GroundItem item) {
        switch(item.type) {
            case 0:
                item.setTaken(true);
                item.visible = false;
                item.removeItemDisplay();
            break;
            case 1:
                item.setTaken(true);
                item.removeItemDisplay();
                untradeable_items.remove(item);
            break;
            default:
                item.setTaken(true);
                item.visible = false;
                item.removeItemDisplay();
                tradeable_items.remove(item);
        }
    }

    public static void addItem(GroundItem item) {
        switch(item.type) {
            case 0:
                if(!ground_items.contains(item))
                    ground_items.add(item);
                else ground_items.set(ground_items.indexOf(item), item);
                item.itemDisplay();
            break;
            case 1:
                untradeable_items.add(item);
            break;
            default: tradeable_items.add(item);
        }
    }

    public static GroundItem findGroundItem(int id, int x, int y, int z) {
        boolean tradeable = Server.itemManager.isTradable(id);
        if(!tradeable) {
            if (!Ground.ground_items.isEmpty())
                for (GroundItem item : Ground.ground_items) {
                    if(item.x == x && item.y == y && item.z == z && item.id == id)
                        return item;
                }
            if (!Ground.untradeable_items.isEmpty())
                for (GroundItem item : Ground.untradeable_items) {
                    if(item.x == x && item.y == y && item.z == z && item.id == id)
                        return item;
                }
        } else {
            if (!Ground.ground_items.isEmpty())
                for (GroundItem item : Ground.ground_items) {
                    if(item.x == x && item.y == y && item.z == z && item.id == id)
                        return item;
                }
            if (!Ground.tradeable_items.isEmpty())
                for (GroundItem item : Ground.tradeable_items) {
                    if(item.x == x && item.y == y && item.z == z && item.id == id && !item.isTaken())
                        return item;
                }
        }
        return null;
    }
    public static void addGroundItem(Position pos, int id, int amount, int time) {
        addItem(new GroundItem(pos, id, amount, time, true));
    }
    public static void addFloorItem(Client c, int id, int amount) {
        addItem(new GroundItem(c.getPosition(), new int[]{c.getSlot(), id, amount, 500}));
    }
    public static void addFloorItem(Client c, Position pos, int id, int amount, int time) {
        addItem(new GroundItem(pos, new int[]{c.getSlot(), id, amount, time}));
    }
    public static void addNpcDropItem(Client c, Npc n, int id, int amount) {
        addItem(new GroundItem(n.getPosition(), id, amount, c.getSlot(), n.getId()));
    }
}