package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.engine.systems.interaction.ItemOnObjectIntent;
import net.dodian.uber.game.engine.systems.interaction.ObjectClickIntent;
import net.dodian.uber.game.engine.systems.interaction.scheduler.InteractionTaskScheduler;
import net.dodian.uber.game.engine.systems.interaction.scheduler.ObjectInteractionTask;
import net.dodian.uber.game.engine.systems.net.PacketMagicService;
import net.dodian.uber.game.engine.systems.net.PacketObjectService;
import net.dodian.utilities.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PacketHandler(opcode = 132)
public class ObjectInteractionListener implements PacketListener {
    private static final Logger logger = LoggerFactory.getLogger(ObjectInteractionListener.class);

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

        PacketObjectService.handleObjectClick(client, packet.opcode(), option, objectId, objectX, objectY);
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

        PacketObjectService.handleItemOnObject(client, packet.opcode(), interfaceId, objectId, objectX, objectY, itemSlot, itemId);
    }

    private void handleMagicOnObject(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < 8) {
            logger.warn("MagicOnObject packet too short for player={}", client.getPlayerName());
            return;
        }

        final int objectX = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
        final int spellId = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.ADD);
        final int objectY = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.ADD);
        final int objectId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD) - 128;

        PacketMagicService.handleMagicOnObject(client, packet.opcode(), objectX, objectY, objectId, spellId);
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
        int objectX = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.ADD);
        int objectID = buf.readUnsignedShort();
        int objectY = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        return new DecodedObjectClick(objectID, objectX, objectY);
    }

    private DecodedObjectClick decodeSecondClick(ByteBuf buf) {
        if (buf.readableBytes() < 6) {
            return null;
        }
        int objectID = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
        int objectY = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
        int objectX = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        return new DecodedObjectClick(objectID, objectX, objectY);
    }

    private DecodedObjectClick decodeThirdClick(ByteBuf buf) {
        if (buf.readableBytes() < 6) {
            return null;
        }
        int objectX = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
        int objectY = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
        int objectID = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
        return new DecodedObjectClick(objectID, objectX, objectY);
    }

    private DecodedObjectClick decodeFourthClick(ByteBuf buf) {
        if (buf.readableBytes() < 6) {
            return null;
        }
        int objectX = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
        int objectId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int objectY = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
        return new DecodedObjectClick(objectId, objectX, objectY);
    }

    private DecodedObjectClick decodeFifthClick(ByteBuf buf) {
        if (buf.readableBytes() < 6) {
            return null;
        }
        int objectId = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD);
        int objectY = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD);
        int objectX = buf.readShort();
        return new DecodedObjectClick(objectId, objectX, objectY);
    }

    private static int readLEShort(ByteBuf buf) {
        return buf.readUnsignedByte() | (buf.readUnsignedByte() << 8);
    }

    private static int readLEShortA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        return (high << 8) | low;
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
