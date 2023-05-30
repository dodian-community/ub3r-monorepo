package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.prayer.Prayer;
import net.dodian.utilities.Utils;

public class ItemOnObject implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        client.getInputStream().readSignedWordBigEndianA();
        int UsedOnObjectID = client.getInputStream().readUnsignedWordBigEndian();
        int UsedOnY = client.getInputStream().readSignedWordBigEndianA();
        int ItemSlot = client.getInputStream().readSignedWordBigEndianA() - 128;
        int UsedOnX = client.getInputStream().readUnsignedWordBigEndianA();
        int ItemID = client.getInputStream().readUnsignedWord();
        if (!client.playerHasItem(ItemID)) {
            return;
        }
        if (client.distanceToPoint(UsedOnX, UsedOnY) > 2) {
            return;
        }
        if (ItemID == 5733) {
            client.send(new SendMessage("Objectid: " + UsedOnObjectID + ", Slot: " + ItemSlot));
            return;
        }
        if (ItemID == 229 && UsedOnObjectID == 879) {
            client.filling = true;
        }
        if (ItemID == 1925 && UsedOnObjectID == 8689) {
            client.setFocus(UsedOnX, UsedOnY);
            client.deleteItem(ItemID, 1);
            client.addItem(ItemID + 2, 1);
        }
        if (UsedOnObjectID == 3994 || UsedOnObjectID == 11666 || UsedOnObjectID == 16469) {
            if (ItemID == 2357) { // 2357 = gold
                client.showItemsGold();
                client.showInterface(4161);
            } else {
                for (int fi = 0; fi < Utils.smelt_frame.length; fi++) {
                    client.sendFrame246(Utils.smelt_frame[fi], 150, Utils.smelt_bars[fi][0]);
                }
                client.sendFrame164(2400);
            }
        }
        if (UsedOnObjectID == 409 && Prayer.altarBones(client, ItemID)) {
            client.lastAction = System.currentTimeMillis();
            client.skillX = UsedOnX;
            client.setSkillY(UsedOnY);
            client.stillgfx(624, new Position(client.skillY, client.skillX, client.getPosition().getZ()), 0);
            client.boneItem = ItemID;
        }
        if(UsedOnObjectID == 2097 && (ItemID == 1540 || ItemID == 11286)) {
            client.setFocus(UsedOnX, UsedOnY);
            if(!client.playerHasItem(2347)) client.send(new SendMessage("You need a hammer!"));
            else if (ItemID == 1540 && !client.playerHasItem(11286)) client.send(new SendMessage("You need a draconic visage!"));
            else if (ItemID == 11286 && !client.playerHasItem(1540)) client.send(new SendMessage("You need a anti-dragon shield!"));
            else if(Skills.getLevelForExperience(client.getExperience(Skill.SMITHING)) < 90) client.send(new SendMessage("You need level 90 smithing to do this!"));
            else { //Preforming action!
                client.deleteItem(ItemID, ItemSlot, 1);
                client.deleteItem(ItemID == 1540 ? 11286 : 1540, 1);
                client.addItemSlot(11284, 1, ItemSlot);
                client.giveExperience(15000, Skill.SMITHING);
                client.send(new SendMessage("Your smithing craft made a Dragonfire shield out of the visage."));
            }
        }
        if (UsedOnObjectID == 2781 || UsedOnObjectID == 2728 || UsedOnObjectID == 26181) { // Cooking range!
            client.skillX = UsedOnX;
            client.setSkillY(UsedOnY);
            client.startCooking(ItemID);
            client.setFocus(UsedOnX, UsedOnY);
        } else if (UsedOnObjectID == 2783) { // anvil
            int Type = client.CheckSmithing(ItemID);

            if (Type != -1) {
                client.skillX = UsedOnX;
                client.setSkillY(UsedOnY);
                client.OpenSmithingFrame(Type);
            }
        } else {
            client.println_debug(
                    "Item: " + ItemID + " - Used On Object: " + UsedOnObjectID + " -  X: " + UsedOnX + " - Y: " + UsedOnY);
        }
    }

}
