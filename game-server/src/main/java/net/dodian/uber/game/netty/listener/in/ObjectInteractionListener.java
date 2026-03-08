package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.runtime.interaction.ItemOnObjectIntent;
import net.dodian.uber.game.runtime.interaction.MagicOnObjectIntent;
import net.dodian.uber.game.runtime.interaction.ObjectClickIntent;
import net.dodian.uber.game.runtime.interaction.scheduler.InteractionTaskScheduler;
import net.dodian.uber.game.runtime.interaction.scheduler.ObjectInteractionTask;
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
        safeRegister(132, listener); // click1
        safeRegister(252, listener); // click2
        safeRegister(70, listener);  // click3
        safeRegister(234, listener); // click4
        safeRegister(228, listener); // click5
        safeRegister(192, listener); // item on object
        safeRegister(35, listener);  // magic on object
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        switch (packet.opcode()) {
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
                    packet.opcode(),
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

        final Position targetPosition = new Position(objectX, objectY, client.getPosition().getZ());
        final GameObjectDef def = Misc.getObject(objectId, objectX, objectY, client.getPosition().getZ());
        final GameObjectData object = GameObjectData.forId(objectId);

        if (client.randomed || client.UsingAgility) {
            return;
        }

        // OpenRune-style: tick-owned routing. The game thread will execute when in range.
        ObjectClickIntent intent =
                new ObjectClickIntent(
                        packet.opcode(),
                        net.dodian.uber.game.model.entity.player.PlayerHandler.cycle,
                        option,
                        objectId,
                        targetPosition,
                        object,
                        def
                );
        InteractionTaskScheduler.schedule(client, intent, new ObjectInteractionTask(client, intent));
    }

    private void handleItemOnObject(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
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

        final Position targetPosition = new Position(objectX, objectY, client.getPosition().getZ());
        GameObjectData objectData = GameObjectData.forId(objectId);
        GameObjectDef def = Misc.getObject(objectId, targetPosition.getX(), targetPosition.getY(), client.getPosition().getZ());
        ItemOnObjectIntent intent =
                new ItemOnObjectIntent(
                        packet.opcode(),
                        net.dodian.uber.game.model.entity.player.PlayerHandler.cycle,
                        interfaceId,
                        itemSlot,
                        itemId,
                        objectId,
                        targetPosition,
                        objectData,
                        def
                );
        InteractionTaskScheduler.schedule(client, intent, new ObjectInteractionTask(client, intent));
    }

    private void handleMagicOnObject(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < 8) {
            logger.warn("MagicOnObject packet too short for player={}", client.getPlayerName());
            return;
        }

        final int objectX = readSignedWordBigEndian(buf);
        final int spellId = readSignedWordBigEndianA(buf);
        final int objectY = readSignedWordBigEndianA(buf);
        final int objectId = readUnsignedWordLEA(buf) - 128;

        final Position targetPosition = new Position(objectX, objectY, client.getPosition().getZ());
        final GameObjectDef def = Misc.getObject(objectId, objectX, objectY, client.getPosition().getZ());
        final GameObjectData object = GameObjectData.forId(objectId);

        if (client.randomed || client.UsingAgility) {
            return;
        }
        MagicOnObjectIntent intent =
                new MagicOnObjectIntent(
                        packet.opcode(),
                        net.dodian.uber.game.model.entity.player.PlayerHandler.cycle,
                        spellId,
                        objectId,
                        targetPosition,
                        object,
                        def
                );
        InteractionTaskScheduler.schedule(client, intent, new ObjectInteractionTask(client, intent));
    }

    private DecodedObjectClick decodeClickPacket(GamePacket packet, int option) {
        switch (option) {
            case 1:
                return decodeFirstClick(packet.payload());
            case 2:
                return decodeSecondClick(packet.payload());
            case 3:
                return decodeThirdClick(packet.payload());
            case 4:
                return decodeFourthClick(packet.payload());
            case 5:
                return decodeFifthClick(packet.payload());
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

    private static void safeRegister(int opcode, PacketListener listener) {
        try {
            PacketListenerManager.register(opcode, listener);
        } catch (RuntimeException ex) {
            logger.debug("Skipping object interaction listener registration for opcode {}: {}", opcode, ex.getMessage());
        }
    }

    private record DecodedObjectClick(int objectId, int objectX, int objectY) {
    }
}
