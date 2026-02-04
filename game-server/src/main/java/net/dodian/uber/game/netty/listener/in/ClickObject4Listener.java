package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.content.objects.ObjectDispatcher;
import net.dodian.utilities.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static net.dodian.uber.game.model.player.skills.Skill.FARMING;
import static net.dodian.utilities.DotEnvKt.getGameWorldId;

/**
 * Netty handler for opcode 234 (fourth object click).
 * This is a complete port of the legacy ClickObject4 packet.
 */
@PacketHandler(opcode = 234)
public class ClickObject4Listener implements PacketListener {

    static { PacketListenerManager.register(234, new ClickObject4Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(ClickObject4Listener.class);

    //<editor-fold desc="Packet Decoding Helpers">
    private int readUnsignedWordBigEndianA(ByteBuf buf) {
        int low  = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        return (high << 8) | low;
    }

    private int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low  = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }
    //</editor-fold>

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        // Faithful replication of legacy decoding order.
        int objectX = readUnsignedWordBigEndianA(buf);
        int objectId = readUnsignedWordA(buf);
        int objectY = readUnsignedWordBigEndianA(buf);

        final WalkToTask task = new WalkToTask(WalkToTask.Action.OBJECT_FOURTH_CLICK, objectId,
                new Position(objectX, objectY, client.getPosition().getZ()));

        GameObjectDef def = Misc.getObject(objectId, objectX, objectY, client.getPosition().getZ());
        GameObjectData object = GameObjectData.forId(task.getWalkToId());
        client.setWalkToTask(task);

        if (getGameWorldId() > 1 && object != null) {
            client.send(new SendMessage("Obj click4: " + object.getId() + ", " + object.getName() + ", Coord: " + objectX + ", " + objectY + ", " + (def == null ? "Def is null!" : def.getFace())));
        }

        if (client.randomed || client.UsingAgility) {
            return;
        }

        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) {
                    this.stop();
                    return;
                }

                Position objectPosition = null;
                if (def != null) {
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), Objects.requireNonNull(object).getSizeX(def.getFace()), object.getSizeY(def.getFace()), client.getPosition().getZ());
                } else {
                    Object o = new Object(objectId, task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), task.getWalkToPosition().getZ(), 10);
                    if (GlobalObject.hasGlobalObject(o)) {
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), Objects.requireNonNull(object).getSizeX(o.face), object.getSizeY(o.type), client.getPosition().getZ());
                    } else if (object != null) {
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(), object.getSizeY(), client.getPosition().getZ());
                    }
                }

                if (objectPosition == null) {
                    return; // Not close enough yet.
                }

                handleObjectClick(client, task.getWalkToId(), task.getWalkToPosition(), object);
                client.setWalkToTask(null);
                this.stop();
            }
        });
    }

    /**
     * Handles the gameplay logic for the fourth object click.
     * This is a direct port of the legacy `clickObject3` method.
     * @param client The player clicking the object.
     * @param objectID The ID of the object.
     * @param position The position of the object.
     * @param obj The GameObjectData for the object.
     */
    public void handleObjectClick(Client client, int objectID, Position position, GameObjectData obj) {
        client.setFocus(position.getX(), position.getY());
        String objectName = obj == null ? "" : obj.getName().toLowerCase();

        if (ObjectDispatcher.tryHandle(client, 4, objectID, position, obj)) {
            return;
        }

        if(objectID >= 8550 && objectID <= 8557 || (objectID == 27114 || objectID == 27113)) { //Allotment guide
            client.showSkillMenu(FARMING.getId(), 0);
        }
        if(objectID >= 7847 && objectID <= 7850 || objectID == 27111) { //Flowers guide
            client.showSkillMenu(FARMING.getId(), 1);
        }
        if(objectID >= 7577 && objectID <= 7580) { //Bushes guide
            client.showSkillMenu(FARMING.getId(), 2);
        }
        if(objectID >= 8150 && objectID <= 8153 || objectID == 27115) { //Herbs guide
            client.showSkillMenu(FARMING.getId(), 3);
        }
        if(objectID >= 8389 && objectID <= 8391 || objectID == 19147) { //Tree guide
            client.showSkillMenu(FARMING.getId(), 4);
        }
        if(objectID >= 7962 && objectID <= 7965 || objectID == 26579) { //Fruit tree guide
            client.showSkillMenu(FARMING.getId(), 5);
        }
        if ((objectID == 2213) || (objectID == 2214) || (objectID == 3045) || (objectID == 5276)
                || (objectID == 6084) || objectName.contains("bank booth")) {
            client.send(new SendMessage("This bank options are not working currently!"));
        }
    }
}
