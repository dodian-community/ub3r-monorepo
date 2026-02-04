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

import static net.dodian.utilities.DotEnvKt.getGameWorldId;

/**
 * Netty handler for opcode 228 (fifth object click).
 * This is a complete port of the legacy ClickObject5 packet.
 */
@PacketHandler(opcode = 228)
public class ClickObject5Listener implements PacketListener {

    static { PacketListenerManager.register(228, new ClickObject5Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(ClickObject5Listener.class);

    //<editor-fold desc="Packet Decoding Helpers">
    /**
     * Reads a short with an 'A' transformation (ADD).
     */
    private int readShortA(ByteBuf buf) {
        int high = buf.readByte() << 8;
        int low = (buf.readByte() - 128) & 0xFF;
        return high | low;
    }
    //</editor-fold>

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        // Decoding based on the explicit format: SHORTA, SHORTA, SHORT
        int objectId = readShortA(buf);
        int objectY = readShortA(buf);
        int objectX = buf.readShort();

        // CORRECTED: Use the enum member that exists in the legacy code.
        final WalkToTask task = new WalkToTask(WalkToTask.Action.OBJECT_THIRD_CLICK, objectId,
                new Position(objectX, objectY, client.getPosition().getZ()));

        GameObjectDef def = Misc.getObject(objectId, objectX, objectY, client.getPosition().getZ());
        GameObjectData object = GameObjectData.forId(task.getWalkToId());
        client.setWalkToTask(task);

        if (getGameWorldId() > 1 && object != null) {
            client.send(new SendMessage("Obj click5: " + object.getId() + ", " + object.getName() + ", Coord: " + objectX + ", " + objectY + ", " + (def == null ? "Def is null!" : def.getFace())));
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
     * Handles the gameplay logic for the fifth object click.
     * This is a direct port of the legacy `clickObject3` method from ClickObject5.java.
     * @param client The player clicking the object.
     * @param objectID The ID of the object.
     * @param position The position of the object.
     * @param obj The GameObjectData for the object.
     */
    public void handleObjectClick(Client client, int objectID, Position position, GameObjectData obj) {
        client.setFocus(position.getX(), position.getY());
        if (ObjectDispatcher.tryHandle(client, 5, objectID, position, obj)) {
            return;
        }
        client.farming.interactBin(client, objectID, 5);
    }
}
