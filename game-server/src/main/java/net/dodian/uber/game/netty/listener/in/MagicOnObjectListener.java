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
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.utilities.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;



/**
 * Netty implementation of legacy {@code MagicOnObject} (opcode 35).
 */
public class MagicOnObjectListener implements PacketListener {

    static { PacketListenerManager.register(35, new MagicOnObjectListener()); }

    private static final Logger logger = LoggerFactory.getLogger(MagicOnObjectListener.class);

    // ---- helpers ----
    private static int readSignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        int val = (high << 8) | low;
        if (val > 32767) val -= 0x10000;
        return val;
    }
    // signed word, big-endian A (subtract 128 from low byte, high byte second)
    // signed word, big-endian A (low byte is (value-128))
    private static int readSignedWordBigEndianA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int val = (high << 8) | low;
        if (val > 32767) val -= 65536;
        return val;
    }
    // unsigned word big-endian ADD (subtract 128 from low byte)
    // little-endian unsigned short with ADD transform on low byte (low - 128)
    private static int readUnsignedWordLEA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        return ((high << 8) | low) & 0xFFFF;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        if (buf.readableBytes() < 8) {
            logger.warn("MagicOnObject packet too short from {}", client.getPlayerName());
            return;
        }

        int objectX = readSignedWordBigEndian(buf);
        int magicID = readSignedWordBigEndianA(buf);
        int objectY = readSignedWordBigEndianA(buf);
        int objectID = readUnsignedWordLEA(buf) - 128;

        logger.debug("MagicOnObjectListener: magic {} on obj {} at ({},{})", magicID, objectID, objectX, objectY);

        final WalkToTask task = new WalkToTask(WalkToTask.Action.OBJECT_FIRST_CLICK, objectID, new Position(objectX, objectY));
        GameObjectDef def = Misc.getObject(objectID, objectX, objectY, client.getPosition().getZ());
        GameObjectData object = GameObjectData.forId(task.getWalkToId());
        client.setWalkToTask(task);

        if (object != null) { // world debug
            client.send(new SendMessage("Magic: id:" + magicID + ", obj id:" + object.getId() + ", " + object.getName() + ", Coord: " + objectX + ", " + objectY + ", " + (def == null ? "Def is null!" : def.getFace())));
        }
        if (client.randomed || client.UsingAgility) return;

        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) {
                    stop();
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
                if (objectID == 23131)
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), 3552, client.getPosition().getX(), client.getPosition().getY(), Objects.requireNonNull(object).getSizeX(), object.getSizeY(), client.getPosition().getZ());
                if (objectID == 16466)
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), 2972, client.getPosition().getX(), client.getPosition().getY(), 1, 1, client.getPosition().getZ());
                if (objectID == 11643) {
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), 2, client.getPosition().getZ());
                }
                if (objectPosition == null) {
                    stop();
                    return;
                }

                atObject(client, magicID, task.getWalkToId(), task.getWalkToPosition(), object);
                client.setWalkToTask(null);
                stop();
            }
        });
    }

    private void atObject(Client client, int magicID, int objectID, Position objectPosition, GameObjectData obj) {
        // replicate legacy behavior
        client.setFocus(objectPosition.getX(), objectPosition.getY());

        if (objectID == 2151 && magicID == 1179) { // Water Obelisk
            if (client.getSkillLevel(Skill.MAGIC) >= 55) {
                if (!client.playerHasItem(567) || !client.playerHasItem(564, 3)) {
                    client.send(new SendMessage("You need one unpowered orb and 3 cosmic runes to cast on this obelisk."));
                    return;
                }
                client.setSkillAction(Skill.MAGIC.getId(), 571, 1, 567, 564, 725, 726, 5);
                client.skillMessage = "You charge the orb with the power of water.";
            } else client.send(new SendMessage("You need level 55 magic in order to cast this spell!"));
        } else if (objectID == 2150 && magicID == 1182) { // Earth Obelisk
            if (client.getSkillLevel(Skill.MAGIC) >= 60) {
                if (!client.playerHasItem(567) || !client.playerHasItem(564, 3)) {
                    client.send(new SendMessage("You need one unpowered orb and 3 cosmic runes to cast on this obelisk."));
                    return;
                }
                client.setSkillAction(Skill.MAGIC.getId(), 575, 1, 567, 564, 800, 726, 5);
                client.skillMessage = "You charge the orb with the power of earth.";
            } else client.send(new SendMessage("You need level 60 magic in order to cast this spell!"));
        } else if (objectID == 2153 && magicID == 1184) { // Fire Obelisk
            if (client.getSkillLevel(Skill.MAGIC) >= 65) {
                if (!client.playerHasItem(567) || !client.playerHasItem(564, 3)) {
                    client.send(new SendMessage("You need one unpowered orb and 3 cosmic runes to cast on this obelisk."));
                    return;
                }
                client.setSkillAction(Skill.MAGIC.getId(), 569, 1, 567, 564, 875, 726, 5);
                client.skillMessage = "You charge the orb with the power of fire.";
            } else client.send(new SendMessage("You need level 65 magic in order to cast this spell!"));
        } else if (objectID == 2152 && magicID == 1186) { // Air Obelisk
            if (client.getSkillLevel(Skill.MAGIC) >= 70) {
                if (!client.playerHasItem(567) || !client.playerHasItem(564, 3)) {
                    client.send(new SendMessage("You need one unpowered orb and 3 cosmic runes to cast on this obelisk."));
                    return;
                }
                client.setSkillAction(Skill.MAGIC.getId(), 573, 1, 567, 564, 950, 726, 5);
                client.skillMessage = "You charge the orb with the power of air.";
            } else client.send(new SendMessage("You need level 70 magic in order to cast this spell!"));
        }
    }
}
