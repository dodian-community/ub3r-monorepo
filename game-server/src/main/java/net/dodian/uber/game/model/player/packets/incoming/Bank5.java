package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.player.content.Skillcape;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.RemoveInterfaces;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Misc;

import static net.dodian.utilities.DotEnvKt.getGameWorldId;

public class Bank5 implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int interfaceID = client.getInputStream().readSignedWordBigEndianA();
        int removeID = client.getInputStream().readSignedWordBigEndianA();
        int removeSlot = client.getInputStream().readSignedWordBigEndian();
        if (getGameWorldId() > 1)
            client.println_debug("RemoveItem 5: " + removeID + " InterID: " + interfaceID + " slot: " + removeSlot);
        if (interfaceID == 3322 && client.inDuel) { // remove from bag to duel window
            client.stakeItem(removeID, removeSlot, 5);
        } else if (interfaceID == 6669) { // remove from duel window
            client.fromDuel(removeID, removeSlot, 5);
        } else if (interfaceID == 5064) { // remove from bag to bank
            if (client.IsBanking)
                client.bankItem(removeID, removeSlot, 5);
            else if (client.isPartyInterface)
                Balloons.offerItems(client, removeID, 5, removeSlot);
        } else if (interfaceID == 5382) { // remove from bank
            client.fromBank(removeID, removeSlot, 5);
        } else if (interfaceID == 2274) { // remove from party
            Balloons.removeOfferItems(client, removeID, 5, removeSlot);
        } else if (interfaceID == 3322 && client.inTrade) { // remove from bag to trade window
            client.tradeItem(removeID, removeSlot, 5);
        } else if (interfaceID == 3415 && client.inTrade) { // remove from trade window
            client.fromTrade(removeID, removeSlot, 5);
        } else if (interfaceID >= 4233 && interfaceID <= 4257) {
            client.startGoldCrafting(interfaceID, removeSlot, 5);
        } else if (interfaceID == 3823) { // Show value to sell items
            client.sellItem(removeID, removeSlot, 1);
        } else if (interfaceID == 1688) { // Operate on equipped item
            if (removeID == 13203) {
                String[] quotes = {
                        "You are easily the spunkiest warrior alive!",
                        "Not a single soul can challenge your spunk!",
                        "You are clearly the most spunktastic in all the land!",
                        "Your might is spunktacular!",
                        "It's spunkalicious!",
                        "You... are... spunky!",
                        "You are too spunktacular to measure!",
                        "You are the real M.V.P. dude!",
                        "More lazier then Spunky is Cache :D"
                };
                client.send(new SendMessage(quotes[Misc.random(quotes.length - 1)]));
            } else if (removeID == 4566) {
                client.requestAnim(1835, 0);
            } else if (removeID == 11864 || removeID == 11865) {
                SlayerTask.slayerTasks checkTask = SlayerTask.slayerTasks.getTask(client.getSlayerData().get(1));
                if (checkTask != null && client.getSlayerData().get(3) > 0)
                    client.send(new SendMessage("You need to kill " + client.getSlayerData().get(3) + " more " + checkTask.getTextRepresentation()));
                else
                    client.send(new SendMessage("You need to be assigned a task!"));
            } else if (Server.itemManager.getName(removeID).toLowerCase().startsWith("cow")) {
                client.requestForceChat("Moooooo!");
            } else {
                Skillcape skillcape = Skillcape.getSkillCape(removeID);
                if (skillcape != null) {
                    if (client.getExperience(skillcape.getSkill()) < 50000000) {
                        client.send(new SendMessage("Need 50 million in "
                                + skillcape.name().toLowerCase().replace("_", " ").replace(" cape", "") + " to convert this cape!"));
                        return;
                    }
                    if (!Skillcape.isTrimmed(removeID)) {
                        client.getEquipment()[Equipment.Slot.CAPE.getId()] = skillcape.getTrimmedId();
                        client.getEquipmentN()[Equipment.Slot.CAPE.getId()] = 1;
                        client.setEquipment(skillcape.getTrimmedId(), 1, removeSlot);
                        client.send(new SendMessage("You turn your cape into a trimmed version!"));
                    } else {
                        client.getEquipment()[Equipment.Slot.CAPE.getId()] = skillcape.getUntrimmedId();
                        client.getEquipmentN()[Equipment.Slot.CAPE.getId()] = 1;
                        client.setEquipment(skillcape.getUntrimmedId(), 1, removeSlot);
                        client.send(new SendMessage("You turn your cape back into a untrimmed version!"));
                    }
                }
            }
        } else if (interfaceID == 3900) { // Show value to buy items
            client.buyItem(removeID, removeSlot, 1);
        } else if (interfaceID >= 1119 && interfaceID <= 1123) { // Smithing
            if (client.smithing[2] > 0) {
                client.smithing[4] = removeID;
                client.smithing[0] = 1;
                client.smithing[5] = 5;
                client.send(new RemoveInterfaces());
            } else {
                client.send(new SendMessage("Illigal Smithing !"));
                client.println_debug("Illigal Smithing !");
            }
        }
    }

}
