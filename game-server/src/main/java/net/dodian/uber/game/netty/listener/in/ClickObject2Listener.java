package net.dodian.uber.game.netty.listener.in;

import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.model.object.RS2Object;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.model.player.skills.thieving.Thieving;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.utilities.Misc;
import net.dodian.uber.game.content.objects.ObjectClickDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Random;

import static net.dodian.utilities.DotEnvKt.getGameWorldId;

/**
 * Handles the second click on an object (Opcode 252), refactored to use the ByteMessage API.
 * This listener is a full migration of the legacy ClickObject2 class.
 */
@PacketHandler(opcode = 252)
public class ClickObject2Listener implements PacketListener {

    static {
        PacketListenerManager.register(252, new ClickObject2Listener());
    }

    private static final Logger logger = LoggerFactory.getLogger(ClickObject2Listener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteMessage msg = ByteMessage.wrap(packet.getPayload());
        if (msg.getBuffer().readableBytes() < 6) {
            logger.warn("ClickObject2 packet (opcode 252) is too short for client: {}", client.getPlayerName());
            return;
        }

        // Decode the packet using the new ByteMessage API, matching the client's stream.
        // Opcode 252: [uword, big, A], [sword, big], [uword, little, A]
        int objectID = msg.getShort(false, ByteOrder.LITTLE, ValueType.ADD);
        int objectY = msg.getShort(true, ByteOrder.LITTLE, ValueType.NORMAL);
        int objectX = msg.getShort(false, ByteOrder.BIG, ValueType.ADD);

        logger.info("ClickObject2: id={} x={} y={} player={}", objectID, objectX, objectY, client.getPlayerName());

        final WalkToTask task = new WalkToTask(WalkToTask.Action.OBJECT_SECOND_CLICK, objectID, new Position(objectX, objectY));
        GameObjectDef def = Misc.getObject(objectID, objectX, objectY, client.getPosition().getZ());
        GameObjectData object = GameObjectData.forId(task.getWalkToId());
        client.setWalkToTask(task);

        if (getGameWorldId() > 1 && object != null) {
            client.send(new SendMessage("Obj click2: " + object.getId() + ", " + object.getName() + ", Coord: " + objectX + ", " + objectY + ", " + (def == null ? "Def is null!" : def.getFace())));
        }

        // Handle immediate actions that don't require walking.
        if (objectID == 14896 || objectID == 14909) {
            client.addItem(1779, 1);
            client.checkItemUpdate();
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
                Object o = new Object(objectID, task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), task.getWalkToPosition().getZ(), 10);
                if (def != null && !GlobalObject.hasGlobalObject(o)) {
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), Objects.requireNonNull(object).getSizeX(def.getFace()), object.getSizeY(def.getFace()), client.getPosition().getZ());
                } else {
                    if (GlobalObject.hasGlobalObject(o)) {
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), Objects.requireNonNull(object).getSizeX(o.face), object.getSizeY(o.type), o.z);
                    } else if (object != null) {
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(), object.getSizeY(), client.getPosition().getZ());
                    }
                }

                if (objectPosition == null) {
                    return;
                }

                try {
                    clickObject2(client, task.getWalkToId(), task.getWalkToPosition(), object);
                } catch (Exception e) {
                    logger.error("Error executing clickObject2 for client: {}", client.getPlayerName(), e);
                }

                client.setWalkToTask(null);
                this.stop();
            }
        });
    }

    /**
     * Contains all the logic for handling a second-click object interaction
     * once the player has successfully walked to it.
     */
    public void clickObject2(Client client, int objectID, Position position, GameObjectData obj) {
        if (client.adding) {
            client.objects.add(new RS2Object(objectID, position.getX(), position.getY(), 2));
        }
        if (System.currentTimeMillis() < client.walkBlock) {
            return;
        }
        client.setFocus(position.getX(), position.getY());
        String objectName = obj == null ? "" : obj.getName().toLowerCase();

        if (ObjectClickDispatcher.tryHandle(client, 2, objectID, position, obj)) {
            return;
        }

        switch (objectID) {
            case 20873: // Cage
            case 11729:
            case 11730:
            case 11731:
            case 11732:
            case 11733:
            case 11734:
                Thieving.attemptSteal(client, objectID, position);
                break;
            case 378: // Empty chest
                client.send(new SendMessage("This chest is empty!"));
                break;
            case 7962:
                client.send(new SendMessage("You inspect the monolith, but can't make sense of the inscription."));
                break;
            case 20931: // Quick exit pyramid plunder
                client.getPlunder.resetPlunder();
                break;
        }

        // Handle thieving stalls by setting the WanneThieve variable
        if (objectID == 4877) {
            client.skillX = position.getX();
            client.setSkillY(position.getY());
            client.WanneThieve = 4877;
        }

        // Handle gold crafting at a furnace
        if (objectID == 3994 || objectID == 11666 || objectID == 16469 || objectID == 29662) {
            client.showItemsGold();
            client.showInterface(4161);
        }

        // Handle spinning wheel animation
        if (objectID == 25824 || objectID == 14889) {
            client.spinning = true;
            client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
        }

        // Handle wilderness obelisk teleport
        if (objectID == 823) {
            Random r = new Random();
            client.moveTo(2602 + r.nextInt(5), 3162 + r.nextInt(5), client.getPosition().getZ());
        }

        // Handle banking
        if ((objectID == 2213) || (objectID == 2214) || (objectID == 3045) || (objectID == 5276)
                || (objectID == 6084) || objectName.contains("bank booth")) {
            client.skillX = position.getX();
            client.setSkillY(position.getY());
            client.WanneBank = 1;
            client.WanneShop = -1;
        }

        // Handle farming patch inspection
        client.farming.inspectPatch(client, objectID);
    }
}
