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

    public static boolean isTracked(GroundItem item) {
        if (item == null) {
            return false;
        }
        switch (item.type) {
            case 0:
                return ground_items.contains(item);
            case 1:
                return untradeable_items.contains(item);
            default:
                return tradeable_items.contains(item);
        }
    }

    public static boolean canPickup(Client client, GroundItem item) {
        if (client == null || item == null || item.isTaken() || client.getPosition().getZ() != item.z) {
            return false;
        }
        switch (item.type) {
            case 0:
                return item.isVisible();
            case 1:
                return client.dbId == item.playerId;
            default:
                return item.isVisible() || client.dbId == item.playerId;
        }
    }

    private static GroundItem findGroundItem(CopyOnWriteArrayList<GroundItem> list, Client client, int id, int x, int y, int z) {
        if (list.isEmpty()) {
            return null;
        }
        for (GroundItem item : list) {
            if (item.id != id || item.x != x || item.y != y || item.z != z || item.isTaken()) {
                continue;
            }
            if (client != null && !canPickup(client, item)) {
                continue;
            }
            return item;
        }
        return null;
    }

    public static GroundItem findGroundItem(Client client, int id, int x, int y, int z) {
        boolean tradeable = Server.itemManager.isTradable(id);
        if (!tradeable) {
            GroundItem staticItem = findGroundItem(ground_items, client, id, x, y, z);
            if (staticItem != null) {
                return staticItem;
            }
            return findGroundItem(untradeable_items, client, id, x, y, z);
        }

        GroundItem staticItem = findGroundItem(ground_items, client, id, x, y, z);
        if (staticItem != null) {
            return staticItem;
        }
        return findGroundItem(tradeable_items, client, id, x, y, z);
    }

    public static GroundItem findGroundItem(int id, int x, int y, int z) {
        return findGroundItem(null, id, x, y, z);
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
