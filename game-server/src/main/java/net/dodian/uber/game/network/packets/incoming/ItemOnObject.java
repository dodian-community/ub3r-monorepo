package net.dodian.uber.game.network.packets.incoming;

import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.network.packets.Packet;
import net.dodian.uber.game.network.packets.outgoing.RemoveInterfaces;
import net.dodian.uber.game.network.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.prayer.Prayer;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

import static net.dodian.utilities.DotEnvKt.getGameWorldId;

public class ItemOnObject implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        client.getInputStream().readSignedWordBigEndianA();
        int objectID = client.getInputStream().readUnsignedWordBigEndian();
        int objectY = client.getInputStream().readSignedWordBigEndianA();
        int ItemSlot = client.getInputStream().readSignedWordBigEndianA() - 128;
        int objectX = client.getInputStream().readUnsignedWordBigEndianA();
        int ItemID = client.getInputStream().readUnsignedWord();
        final WalkToTask task = new WalkToTask(WalkToTask.Action.ITEM_ON_OBJECT, objectID,
                new Position(objectX, objectY));
        GameObjectDef def = Misc.getObject(objectID, objectX, objectY, client.getPosition().getZ());
        GameObjectData object = GameObjectData.forId(task.getWalkToId());
        client.setWalkToTask(task);
        if (getGameWorldId() > 1 && object != null && ItemID != 5733)
            client.send(new SendMessage("ItemOnObject: " + object.getId() + ", " + object.getName() + ", Coord: " + objectX + ", " + objectY + ", " + (def == null ? "Def is null!" : def.getFace())));
        if (client.randomed) {
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
                Object o = new Object(objectID, task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), task.getWalkToPosition().getZ(), 10);
                if (def != null && !GlobalObject.hasGlobalObject(o)) {
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(def.getFace()), object.getSizeY(def.getFace()), client.getPosition().getZ());
                } else {
                    if (GlobalObject.hasGlobalObject(o)) {
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(o.face), object.getSizeY(o.type), o.z);
                    } else if (object != null) {
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(), object.getSizeY(), client.getPosition().getZ());
                    }
                }
                if (objectID == 23131)
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), 3552, client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(), object.getSizeY(), client.getPosition().getZ());
                if(objectID == 16466)
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), 2972, client.getPosition().getX(), client.getPosition().getY(), 1, 3, client.getPosition().getZ());
                if (objectPosition == null)
                    return;
                if (client.playerHasItem(ItemID))
                    preformObject(client, task.getWalkToId(), ItemID, ItemSlot, task.getWalkToPosition(), object);
                client.setWalkToTask(null);
                this.stop();
            }
        });
    }

public void preformObject(Client client, int objectId, int item, int slot, Position objectPosition, GameObjectData obj) {
    /*if (client.distanceToPoint(UsedOnX, UsedOnY) > 2) {
        return;
    }*/
    int UsedOnX = objectPosition.getX();
    int UsedOnY = objectPosition.getY();
    client.setFocus(UsedOnX, UsedOnY);

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
