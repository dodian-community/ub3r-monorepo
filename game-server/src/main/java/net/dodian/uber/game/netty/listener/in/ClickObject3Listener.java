package net.dodian.uber.game.netty.listener.in;


import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.utilities.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static net.dodian.utilities.DotEnvKt.getGameWorldId;

/**
 * Handles third-clicks on objects (opcode 70).
 */
@PacketHandler(opcode = 70)
public class ClickObject3Listener implements PacketListener {

    static {
        PacketListenerManager.register(70, new ClickObject3Listener());
    }

    private static final Logger logger = LoggerFactory.getLogger(ClickObject3Listener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteMessage msg = ByteMessage.wrap(packet.getPayload());
        // order: objectX (signed, big), objectY (unsigned, big), objectID (unsigned, big, ADD)
        int objectX = msg.getShort(false, ByteOrder.LITTLE, ValueType.NORMAL);
        int objectY = msg.getShort(false, ByteOrder.BIG, ValueType.NORMAL);
        int objectID = msg.getShort(false, ByteOrder.LITTLE, ValueType.ADD);

        logger.info("ClickObject3: id={} x={} y={} player={} ", objectID, objectX, objectY, client.getPlayerName());

        final WalkToTask task = new WalkToTask(WalkToTask.Action.OBJECT_THIRD_CLICK, objectID,
                new Position(objectX, objectY));

        GameObjectDef def = Misc.getObject(objectID, objectX, objectY, client.getPosition().getZ());
        GameObjectData object = GameObjectData.forId(task.getWalkToId());

        client.setWalkToTask(task);

        if (getGameWorldId() > 1 && object != null) {
            client.send(new SendMessage("Obj click3: " + object.getId() + ", " + object.getName() + ", Coord: " + objectX + ", " + objectY + ", " + (def == null ? "Def is null!" : def.getFace())));
        }

        if (client.randomed || client.UsingAgility) {
            return;
        }

        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected) {
                    this.stop();
                    return;
                }
                if (client.getWalkToTask() != task) {
                    this.stop();
                    return;
                }
                Position objectPosition = null;
                if (def != null) {
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), Objects.requireNonNull(object).getSizeX(def.getFace()), object.getSizeY(def.getFace()), client.getPosition().getZ());
                } else {
                    Object o = new Object(objectID, task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), task.getWalkToPosition().getZ(), 10);
                    if (GlobalObject.hasGlobalObject(o)) {
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), Objects.requireNonNull(object).getSizeX(o.face), object.getSizeY(o.type), client.getPosition().getZ());
                    } else if (object != null) {
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(), object.getSizeY(), client.getPosition().getZ());
                    }
                }
                if (objectPosition == null) {
                    return;
                }

                clickObject3(client, task.getWalkToId(), task.getWalkToPosition(), object);
                client.setWalkToTask(null);
                this.stop();
            }
        });
    }

    private void clickObject3(Client client, int objectID, Position position, GameObjectData obj) {
        client.setFocus(position.getX(), position.getY());
        String objectName = obj == null ? "" : obj.getName().toLowerCase();

        if (objectID == 1739) {
            client.moveTo(client.getPosition().getX(), client.getPosition().getY(), client.getPosition().getZ() - 1);
        }
        if ((objectID == 2213) || (objectID == 2214) || (objectID == 3045) || (objectID == 5276)
                || (objectID == 6084) || objectName.contains("bank booth")) {
            client.setRefundList();
            client.refundSlot = 0;
            client.setRefundOptions();
        }
    }
}
