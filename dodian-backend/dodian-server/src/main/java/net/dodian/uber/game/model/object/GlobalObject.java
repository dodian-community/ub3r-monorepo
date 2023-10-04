package net.dodian.uber.game.model.object;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;

import java.util.concurrent.CopyOnWriteArrayList;

public class GlobalObject {

    private static CopyOnWriteArrayList<Object> globalObject = new CopyOnWriteArrayList<Object>();

    public static CopyOnWriteArrayList<Object> getGlobalObject() {
        return globalObject;
    }

    public static void updateNewObject(Object o) {
        for (Player p : PlayerHandler.players) {
            if (p == null || !p.isActive) {
                continue;
            }
            Client c = (Client) p;
            if (c.withinDistance(o))
                c.ReplaceObject2(new Position(o.x, o.y, o.z), o.id, o.face, o.type);
        }
    }

    public static void updateOldObject(Object o) {
        for (Player p : PlayerHandler.players) {
            if (p == null || !p.isActive) {
                continue;
            }
            Client c = (Client) p;
            if (c.withinDistance(o))
                c.ReplaceObject(o.x, o.y, o.oldId, o.face, o.type);
        }
    }

    public static boolean addGlobalObject(final Object o, int time) {
        if ((o.getAttachment() != null && (long) o.getAttachment() - System.currentTimeMillis() > 0) || hasGlobalObject(o))
            return false;
        o.setAttachment(System.currentTimeMillis() + time);
        globalObject.add(o);
        updateNewObject(o);
        return true;
    }

    public static void updateObject(Client c) {
        for (Object o : globalObject) {
            if (c.withinDistance(o)) {
                if ((long) o.getAttachment() - System.currentTimeMillis() > 0) {
                    updateNewObject(o);
                } else {
                    updateOldObject(o);
                    globalObject.remove(o);
                }
            }
        }
    }

    public static boolean hasGlobalObject(Object o) {
        for (Object ob : globalObject) {
            if (ob.x == o.x && ob.y == o.y)
                return true;
        }
        return false;
    }

    public static Object getGlobalObject(int objectX, int objectY) {
        for (Object ob : globalObject) {
            if (ob.x == objectX && ob.y == objectY)
                return ob;
        }
        return null;
    }

}
