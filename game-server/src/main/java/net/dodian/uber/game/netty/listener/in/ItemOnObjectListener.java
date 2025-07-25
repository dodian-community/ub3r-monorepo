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
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.prayer.Prayer;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.dodian.utilities.DotEnvKt.getGameWorldId;

/**
 * Netty implementation for opcode 192 (legacy ItemOnObject).
 * This class contains all logic from the original file, fully ported with the explicitly specified packet decoding format.
 */
@PacketHandler(opcode = 192)
public class ItemOnObjectListener implements PacketListener {

    static { PacketListenerManager.register(192, new ItemOnObjectListener()); }

    private static final Logger logger = LoggerFactory.getLogger(ItemOnObjectListener.class);

    //<editor-fold desc="Packet Decoding Helpers based on user-provided format">
    /**
     * Reads a Little-Endian short (LESHORT).
     */
    private int readLEShort(ByteBuf buf) {
        return buf.readUnsignedByte() | (buf.readUnsignedByte() << 8);
    }

    /**
     * Reads a Little-Endian short with an 'A' transformation (LESHORTA).
     */
    private int readLEShortA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        return (high << 8) | low;
    }
    //</editor-fold>

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        // Decoding based on the explicit format: SHORT, LESHORT, LESHORTA, LESHORT, LESHORTA, SHORT
        // This mapping aligns the new format with the variable names from the legacy file.
        buf.readShort(); // Field 1: SHORT (Unused, likely interface ID)
        int objectId = readLEShort(buf);      // Field 2: LESHORT
        int objectY = readLEShortA(buf);     // Field 3: LESHORTA
        int itemSlot = readLEShort(buf);      // Field 4: LESHORT
        int objectX = readLEShortA(buf);     // Field 5: LESHORTA
        int itemId = buf.readShort();         // Field 6: SHORT

        if (getGameWorldId() > 1) {
            logger.debug("ItemOnObject: objId={}, objX={}, objY={}, itemSlot={}, itemId={}", objectId, objectX, objectY, itemSlot, itemId);
        }

        if (client.randomed) {
            return;
        }

        if (itemSlot < 0 || itemSlot >= client.playerItems.length) {
            return;
        }

        final WalkToTask task = new WalkToTask(WalkToTask.Action.ITEM_ON_OBJECT, objectId, new Position(objectX, objectY, client.getPosition().getZ()));
        client.setWalkToTask(task);

        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) {
                    this.stop();
                    return;
                }

                Position objectPosition = task.getWalkToPosition();
                GameObjectData objectData = GameObjectData.forId(objectId);
                GameObjectDef def = Misc.getObject(objectId, objectPosition.getX(), objectPosition.getY(), client.getPosition().getZ());

                Position goodDistancePosition = null;
                Object o = new Object(objectId, objectPosition.getX(), objectPosition.getY(), objectPosition.getZ(), 10);
                if (def != null && !GlobalObject.hasGlobalObject(o)) {
                    goodDistancePosition = Misc.goodDistanceObject(objectPosition.getX(), objectPosition.getY(), client.getPosition().getX(), client.getPosition().getY(), objectData.getSizeX(def.getFace()), objectData.getSizeY(def.getFace()), client.getPosition().getZ());
                } else {
                    if (GlobalObject.hasGlobalObject(o)) {
                        goodDistancePosition = Misc.goodDistanceObject(objectPosition.getX(), objectPosition.getY(), client.getPosition().getX(), client.getPosition().getY(), objectData.getSizeX(o.face), objectData.getSizeY(o.type), o.z);
                    } else if (objectData != null) {
                        goodDistancePosition = Misc.goodDistanceObject(objectPosition.getX(), objectPosition.getY(), client.getPosition().getX(), client.getPosition().getY(), objectData.getSizeX(), objectData.getSizeY(), client.getPosition().getZ());
                    }
                }

                if (objectId == 23131)
                    goodDistancePosition = Misc.goodDistanceObject(objectPosition.getX(), 3552, client.getPosition().getX(), client.getPosition().getY(), objectData.getSizeX(), objectData.getSizeY(), client.getPosition().getZ());
                if(objectId == 16466)
                    goodDistancePosition = Misc.goodDistanceObject(objectPosition.getX(), 2972, client.getPosition().getX(), client.getPosition().getY(), 1, 3, client.getPosition().getZ());

                if (goodDistancePosition == null) {
                    return; // Not close enough yet.
                }

                if (client.playerHasItem(itemId)) {
                    preformObject(client, objectId, itemId, itemSlot, objectPosition, objectData);
                }

                client.setWalkToTask(null);
                this.stop();
            }
        });
    }

    public void preformObject(Client client, int objectId, int item, int slot, Position objectPosition, GameObjectData obj) {
        int UsedOnX = objectPosition.getX();
        int UsedOnY = objectPosition.getY();
        client.setFocus(UsedOnX, UsedOnY);

        client.farming.interactItemBin(client, objectId, item);
        if (item == 5733) {
            client.send(new SendMessage("ItemOnObject: " + objectId + " at " + objectPosition.copy().toString()));
            return;
        }
        if (objectId == 879 || objectId == 873 || objectId == 874 || objectId == 6232 ||
                objectId == 12279 || objectId == 14868 || objectId == 20358 || objectId == 25929) { //Water source!
            client.fillingObj = objectId;
            client.filling = true;
        }
        if (objectId == 884 || objectId == 878 || objectId == 6249) { //Bucket on well!
            client.fillingObj = objectId;
            client.filling = true;
        }
        if (objectId == 14890) { //Sand pit
            client.fillingObj = objectId;
            client.filling = true;
        }
        if (item == 1925 && objectId == 8689) {
            client.setFocus(UsedOnX, UsedOnY);
            client.deleteItem(item, 1);
            client.addItem(item + 2, 1);
            client.checkItemUpdate();
        }
        if (objectId == 3994 || objectId == 11666 || objectId == 16469 || objectId == 29662) {
            if(item == 1783 || item == 1781) { //Soda ash or bucket of sand
                client.send(new RemoveInterfaces());
                if(!client.playerHasItem(1783) || !client.playerHasItem(1781)) {
                    client.send(new SendMessage("You need one bucket of sand and one soda ash"));
                    return;
                }
                client.setSkillAction(Skill.CRAFTING.getId(), 1775, 1, 1783, 1781, 80, 899, 3);
                client.skillMessage = "You smelt soda ash with the sand and made molten glass.";
            } else if (item == 2357) { // 2357 = gold
                client.showItemsGold();
                client.showInterface(4161);
            } else {
                for (int fi = 0; fi < Utils.smelt_frame.length; fi++) {
                    client.sendFrame246(Utils.smelt_frame[fi], 150, Utils.smelt_bars[fi][0]);
                }
                client.sendFrame164(2400);
            }
        }
        if (objectId == 409 && Prayer.altarBones(client, item)) {
            client.skillX = UsedOnX;
            client.setSkillY(UsedOnY);
            client.boneItem = item;
            Prayer.altarBones(client, client.boneItem);
        }
        if (objectId == 2097 && (item == 1540 || item == 11286)) {
            if (!client.playerHasItem(2347)) client.send(new SendMessage("You need a hammer!"));
            else if (item == 1540 && !client.playerHasItem(11286))
                client.send(new SendMessage("You need a draconic visage!"));
            else if (item == 11286 && !client.playerHasItem(1540))
                client.send(new SendMessage("You need a anti-dragon shield!"));
            else if (Skills.getLevelForExperience(client.getExperience(Skill.SMITHING)) < 90)
                client.send(new SendMessage("You need level 90 smithing to do this!"));
            else { //Preforming action!
                client.deleteItem(item, slot, 1);
                client.deleteItem(item == 1540 ? 11286 : 1540, 1);
                client.addItemSlot(11284, 1, slot);
                client.checkItemUpdate();
                client.giveExperience(15000, Skill.SMITHING);
                client.send(new SendMessage("Your smithing craft made a Dragonfire shield out of the visage."));
            }
        } else if (objectId == 2097) {
            int type = item;
            if (client.CheckSmithing(type) != -1) {
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.OpenSmithingFrame(client.CheckSmithing(type));
            }
        }
        if(objectId == 26181 && item == 401) {
            int amount = client.getInvAmt(401);
            for(int i = 0; i < amount; i++) {
                client.deleteItem(401, 1);
                client.addItem(1781, 1);
            }
            client.checkItemUpdate();
            client.send(new SendMessage("You burn all your seaweed into ashes."));
        } else if (objectId == 2781 || objectId == 2728 || objectId == 26181) { // Cooking range!
            client.skillX = UsedOnX;
            client.setSkillY(UsedOnY);
            client.startCooking(item);
        } else if (objectId == 2783) { // anvil
            int Type = client.CheckSmithing(item);

            if (Type != -1) {
                client.skillX = UsedOnX;
                client.setSkillY(UsedOnY);
                client.OpenSmithingFrame(Type);
            }
        }
    }
}