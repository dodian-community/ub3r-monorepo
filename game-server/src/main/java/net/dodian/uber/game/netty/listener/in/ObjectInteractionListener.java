package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.content.objects.ObjectContentDispatcher;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.model.object.RS2Object;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.utilities.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PacketHandler(opcode = 132)
public class ObjectInteractionListener implements PacketListener {
    private static final Logger logger = LoggerFactory.getLogger(ObjectInteractionListener.class);

    private enum DistanceMode {
        CLICK,
        ITEM_ON_OBJECT,
        MAGIC,
    }

    static {
        ObjectInteractionListener listener = new ObjectInteractionListener();
        PacketListenerManager.register(132, listener); // click1
        PacketListenerManager.register(252, listener); // click2
        PacketListenerManager.register(70, listener);  // click3
        PacketListenerManager.register(234, listener); // click4
        PacketListenerManager.register(228, listener); // click5
        PacketListenerManager.register(192, listener); // item on object
        PacketListenerManager.register(35, listener);  // magic on object
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        switch (packet.getOpcode()) {
            case 132:
                handleClick(client, packet, 1);
                return;
            case 252:
                handleClick(client, packet, 2);
                return;
            case 70:
                handleClick(client, packet, 3);
                return;
            case 234:
                handleClick(client, packet, 4);
                return;
            case 228:
                handleClick(client, packet, 5);
                return;
            case 192:
                handleItemOnObject(client, packet);
                return;
            case 35:
                handleMagicOnObject(client, packet);
                return;
            default:
                logger.warn(
                    "ObjectInteractionListener got unexpected opcode={} for player={}",
                    packet.getOpcode(),
                    client.getPlayerName()
                );
        }
    }

    private void handleClick(Client client, GamePacket packet, int option) {
        DecodedObjectClick decoded = decodeClickPacket(packet, option);
        if (decoded == null) {
            logger.warn("Object click packet decode failed option={} player={}", option, client.getPlayerName());
            return;
        }

        final int objectId = decoded.objectId;
        final int objectX = decoded.objectX;
        final int objectY = decoded.objectY;

        final WalkToTask task = new WalkToTask(actionForOption(option), objectId, new Position(objectX, objectY));
        final GameObjectDef def = Misc.getObject(objectId, objectX, objectY, client.getPosition().getZ());
        final GameObjectData object = GameObjectData.forId(task.getWalkToId());
        client.setWalkToTask(task);

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

                Position objectPosition = resolveDistancePosition(client, task, objectId, object, def, DistanceMode.CLICK);
                if (objectPosition == null) {
                    return;
                }

                if (option == 1) {
                    if (!client.validClient || client.randomed) {
                        client.setWalkToTask(null);
                        this.stop();
                        return;
                    }
                    if (client.adding) {
                        client.objects.add(new RS2Object(objectId, task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), 1));
                    }
                    if (System.currentTimeMillis() < client.walkBlock || client.genie || client.antique) {
                        client.setWalkToTask(null);
                        this.stop();
                        return;
                    }
                    Position playerPos = client.getPosition().copy();
                    int xDiff = Math.abs(playerPos.getX() - task.getWalkToPosition().getX());
                    int yDiff = Math.abs(playerPos.getY() - task.getWalkToPosition().getY());
                    client.resetAction(false);
                    client.setFocus(task.getWalkToPosition().getX(), task.getWalkToPosition().getY());
                    if (xDiff > 5 || yDiff > 5) {
                        client.setWalkToTask(null);
                        this.stop();
                        return;
                    }
                } else if (option == 2) {
                    if (client.adding) {
                        client.objects.add(new RS2Object(objectId, task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), 2));
                    }
                    if (System.currentTimeMillis() < client.walkBlock) {
                        client.setWalkToTask(null);
                        this.stop();
                        return;
                    }
                    client.setFocus(task.getWalkToPosition().getX(), task.getWalkToPosition().getY());
                } else if (option == 3) {
                    client.setFocus(task.getWalkToPosition().getX(), task.getWalkToPosition().getY());
                }

                ObjectContentDispatcher.tryHandleClick(client, option, task.getWalkToId(), task.getWalkToPosition(), object);
                client.setWalkToTask(null);
                this.stop();
            }
        });
    }

    private void handleItemOnObject(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        if (buf.readableBytes() < 12) {
            logger.warn("ItemOnObject packet too short for player={}", client.getPlayerName());
            return;
        }

        final int interfaceId = buf.readShort();
        final int objectId = buf.readShort();
        final int objectY = readLEShortA(buf);
        final int itemSlot = readLEShort(buf);
        final int objectX = readLEShortA(buf);
        final int itemId = buf.readShort();

        if (client.randomed) {
            return;
        }
        if (itemSlot < 0 || itemSlot >= client.playerItems.length || interfaceId != 3214) {
            return;
        }

        final WalkToTask task = new WalkToTask(
            WalkToTask.Action.ITEM_ON_OBJECT,
            objectId,
            new Position(objectX, objectY, client.getPosition().getZ())
        );
        client.setWalkToTask(task);

        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) {
                    this.stop();
                    return;
                }

                GameObjectData objectData = GameObjectData.forId(objectId);
                GameObjectDef def = Misc.getObject(objectId, task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getZ());
                Position objectPosition = resolveDistancePosition(client, task, objectId, objectData, def, DistanceMode.ITEM_ON_OBJECT);
                if (objectPosition == null) {
                    return;
                }

                if (client.playerHasItem(itemId)) {
                    client.setFocus(task.getWalkToPosition().getX(), task.getWalkToPosition().getY());
                    ObjectContentDispatcher.tryHandleUseItem(
                        client,
                        objectId,
                        task.getWalkToPosition(),
                        objectData,
                        itemId,
                        itemSlot,
                        interfaceId
                    );
                }

                client.setWalkToTask(null);
                this.stop();
            }
        });
    }

    private void handleMagicOnObject(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        if (buf.readableBytes() < 8) {
            logger.warn("MagicOnObject packet too short for player={}", client.getPlayerName());
            return;
        }

        final int objectX = readSignedWordBigEndian(buf);
        final int spellId = readSignedWordBigEndianA(buf);
        final int objectY = readSignedWordBigEndianA(buf);
        final int objectId = readUnsignedWordLEA(buf) - 128;

        final WalkToTask task = new WalkToTask(
            WalkToTask.Action.OBJECT_FIRST_CLICK,
            objectId,
            new Position(objectX, objectY)
        );
        final GameObjectDef def = Misc.getObject(objectId, objectX, objectY, client.getPosition().getZ());
        final GameObjectData object = GameObjectData.forId(task.getWalkToId());
        client.setWalkToTask(task);

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

                Position objectPosition = resolveDistancePosition(client, task, objectId, object, def, DistanceMode.MAGIC);
                if (objectPosition == null) {
                    return;
                }

                client.setFocus(task.getWalkToPosition().getX(), task.getWalkToPosition().getY());
                ObjectContentDispatcher.tryHandleMagic(client, task.getWalkToId(), task.getWalkToPosition(), object, spellId);
                client.setWalkToTask(null);
                this.stop();
            }
        });
    }

    private Position resolveDistancePosition(
        Client client,
        WalkToTask task,
        int objectId,
        GameObjectData objectData,
        GameObjectDef def,
        DistanceMode mode
    ) {
        Position walkTo = task.getWalkToPosition();
        Position objectPosition = null;

        Object objectAtTile = new Object(objectId, walkTo.getX(), walkTo.getY(), walkTo.getZ(), 10);
        if (def != null && !GlobalObject.hasGlobalObject(objectAtTile)) {
            if (objectData != null) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.getX(),
                    walkTo.getY(),
                    client.getPosition().getX(),
                    client.getPosition().getY(),
                    objectData.getSizeX(def.getFace()),
                    objectData.getSizeY(def.getFace()),
                    client.getPosition().getZ()
                );
            }
        } else {
            if (GlobalObject.hasGlobalObject(objectAtTile)) {
                if (objectData != null) {
                    objectPosition = Misc.goodDistanceObject(
                        walkTo.getX(),
                        walkTo.getY(),
                        client.getPosition().getX(),
                        client.getPosition().getY(),
                        objectData.getSizeX(objectAtTile.face),
                        objectData.getSizeY(objectAtTile.type),
                        objectAtTile.z
                    );
                }
            } else if (objectData != null) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.getX(),
                    walkTo.getY(),
                    client.getPosition().getX(),
                    client.getPosition().getY(),
                    objectData.getSizeX(),
                    objectData.getSizeY(),
                    client.getPosition().getZ()
                );
            }
        }

        if (mode == DistanceMode.CLICK || mode == DistanceMode.MAGIC) {
            if (objectId == 23131 && objectData != null) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.getX(),
                    3552,
                    client.getPosition().getX(),
                    client.getPosition().getY(),
                    objectData.getSizeX(),
                    objectData.getSizeY(),
                    client.getPosition().getZ()
                );
            }
            if (objectId == 16466) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.getX(),
                    2972,
                    client.getPosition().getX(),
                    client.getPosition().getY(),
                    1,
                    1,
                    client.getPosition().getZ()
                );
            }
            if (objectId == 11643) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.getX(),
                    walkTo.getY(),
                    client.getPosition().getX(),
                    client.getPosition().getY(),
                    2,
                    client.getPosition().getZ()
                );
            }
        }

        if (mode == DistanceMode.ITEM_ON_OBJECT) {
            if (objectId == 23131 && objectData != null) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.getX(),
                    3552,
                    client.getPosition().getX(),
                    client.getPosition().getY(),
                    objectData.getSizeX(),
                    objectData.getSizeY(),
                    client.getPosition().getZ()
                );
            }
            if (objectId == 16466) {
                objectPosition = Misc.goodDistanceObject(
                    walkTo.getX(),
                    2972,
                    client.getPosition().getX(),
                    client.getPosition().getY(),
                    1,
                    3,
                    client.getPosition().getZ()
                );
            }
        }

        return objectPosition;
    }

    private WalkToTask.Action actionForOption(int option) {
        switch (option) {
            case 1:
                return WalkToTask.Action.OBJECT_FIRST_CLICK;
            case 2:
                return WalkToTask.Action.OBJECT_SECOND_CLICK;
            case 3:
                return WalkToTask.Action.OBJECT_THIRD_CLICK;
            case 4:
                return WalkToTask.Action.OBJECT_FOURTH_CLICK;
            case 5:
                // Preserved legacy mapping: click5 used OBJECT_THIRD_CLICK.
                return WalkToTask.Action.OBJECT_THIRD_CLICK;
            default:
                return WalkToTask.Action.OBJECT_FIRST_CLICK;
        }
    }

    private DecodedObjectClick decodeClickPacket(GamePacket packet, int option) {
        switch (option) {
            case 1:
                return decodeFirstClick(packet.getPayload());
            case 2:
                return decodeSecondClick(packet.getPayload());
            case 3:
                return decodeThirdClick(packet.getPayload());
            case 4:
                return decodeFourthClick(packet.getPayload());
            case 5:
                return decodeFifthClick(packet.getPayload());
            default:
                return null;
        }
    }

    private DecodedObjectClick decodeFirstClick(ByteBuf buf) {
        if (buf.readableBytes() < 6) {
            return null;
        }
        int objectX = readSignedWordBigEndianA(buf);
        int objectID = buf.readUnsignedShort();
        int objectY = readUnsignedWordA(buf);
        return new DecodedObjectClick(objectID, objectX, objectY);
    }

    private DecodedObjectClick decodeSecondClick(ByteBuf payload) {
        ByteMessage msg = ByteMessage.wrap(payload);
        if (msg.getBuffer().readableBytes() < 6) {
            return null;
        }
        int objectID = msg.getShort(false, ByteOrder.LITTLE, ValueType.ADD);
        int objectY = msg.getShort(true, ByteOrder.LITTLE, ValueType.NORMAL);
        int objectX = msg.getShort(false, ByteOrder.BIG, ValueType.ADD);
        return new DecodedObjectClick(objectID, objectX, objectY);
    }

    private DecodedObjectClick decodeThirdClick(ByteBuf payload) {
        ByteMessage msg = ByteMessage.wrap(payload);
        if (msg.getBuffer().readableBytes() < 6) {
            return null;
        }
        int objectX = msg.getShort(false, ByteOrder.LITTLE, ValueType.NORMAL);
        int objectY = msg.getShort(false, ByteOrder.BIG, ValueType.NORMAL);
        int objectID = msg.getShort(false, ByteOrder.LITTLE, ValueType.ADD);
        return new DecodedObjectClick(objectID, objectX, objectY);
    }

    private DecodedObjectClick decodeFourthClick(ByteBuf buf) {
        if (buf.readableBytes() < 6) {
            return null;
        }
        int objectX = readUnsignedWordBigEndianA(buf);
        int objectId = readUnsignedWordA(buf);
        int objectY = readUnsignedWordBigEndianA(buf);
        return new DecodedObjectClick(objectId, objectX, objectY);
    }

    private DecodedObjectClick decodeFifthClick(ByteBuf buf) {
        if (buf.readableBytes() < 6) {
            return null;
        }
        int objectId = readShortA(buf);
        int objectY = readShortA(buf);
        int objectX = buf.readShort();
        return new DecodedObjectClick(objectId, objectX, objectY);
    }

    private static int readSignedWordBigEndianA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readByte() & 0xFF;
        int val = (high << 8) | low;
        return val > 32767 ? val - 65536 : val;
    }

    private static int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }

    private static int readUnsignedWordBigEndianA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        return (high << 8) | low;
    }

    private static int readShortA(ByteBuf buf) {
        int high = buf.readByte() << 8;
        int low = (buf.readByte() - 128) & 0xFF;
        return high | low;
    }

    private static int readLEShort(ByteBuf buf) {
        return buf.readUnsignedByte() | (buf.readUnsignedByte() << 8);
    }

    private static int readLEShortA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        return (high << 8) | low;
    }

    private static int readSignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        int val = (high << 8) | low;
        return val > 32767 ? val - 0x10000 : val;
    }

    private static int readUnsignedWordLEA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        return ((high << 8) | low) & 0xFFFF;
    }

    private static final class DecodedObjectClick {
        private final int objectId;
        private final int objectX;
        private final int objectY;

        private DecodedObjectClick(int objectId, int objectX, int objectY) {
            this.objectId = objectId;
            this.objectX = objectX;
            this.objectY = objectY;
        }
    }
}
