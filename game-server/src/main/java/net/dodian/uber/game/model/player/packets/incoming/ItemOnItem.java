package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.packets.outgoing.SendString;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.utilities.Utils;

public class ItemOnItem implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int usedWithSlot = client.getInputStream().readUnsignedWord();
        int itemUsedSlot = client.getInputStream().readUnsignedWordA();
        client.getInputStream().readUnsignedWord();
        client.getInputStream().readUnsignedWord();

        int useWith = client.playerItems[usedWithSlot] - 1;
        int itemUsed = client.playerItems[itemUsedSlot] - 1;
        if (!client.playerHasItem(itemUsed) || !client.playerHasItem(useWith)) {
            return;
        }
        if (useWith == 5733 || itemUsed == 5733) { //Potato
            client.playerPotato.clear();
            client.playerPotato.add(0, 4);
            client.playerPotato.add(1, useWith == 5733 ? itemUsedSlot : usedWithSlot);
            client.playerPotato.add(2, useWith == 5733 ? itemUsed : useWith);
            client.playerPotato.add(3, 1);
            return;
        }
        int otherItem = client.playerItems[usedWithSlot] - 1;
        boolean knife = (useWith == 946 || itemUsed == 946) || (useWith == 5605 || itemUsed == 5605) ? true : false;
        if ((itemUsed == 2383 && useWith == 2382) || (itemUsed == 2382 && useWith == 2383)) {
            if (client.getLevel(Skill.CRAFTING) >= 60) {
                client.deleteItem(itemUsed, itemUsedSlot, 1);
                client.deleteItem(otherItem, usedWithSlot, 1);
                client.addItem(989, 1);
                client.send(new SendMessage("You have crafted the crystal key!  I wonder what it's for?"));
            } else {
                client.send(new SendMessage("You need 60 crafting to make the crystal key"));
            }
        }
        for (int h = 0; h < Utils.herbs.length; h++) {
            if ((itemUsed == Utils.herbs[h] && otherItem == 227) || (itemUsed == 227 && otherItem == Utils.herbs[h])) {
                if (!client.premium && h > 2) {
                    client.send(new SendMessage("Need premium to mix these pots!"));
                    return;
                }
                if (client.getLevel(Skill.HERBLORE) < Utils.req[h]) {
                    client.send(new SendMessage("Requires herblore level " + Utils.req[h]));
                    return;
                }
                int xp = 0;
                if (client.premium)
                    xp = Utils.grimy_herbs_xp[h] * 10;
                client.setPots(600, itemUsed, otherItem, Utils.unfinished[h], xp);
                return;
            }
            if ((itemUsed == Utils.secondary[h] && otherItem == Utils.unfinished[h])
                    || (itemUsed == Utils.unfinished[h] && otherItem == Utils.secondary[h])) {
                if (!client.premium && h > 2) {
                    client.send(new SendMessage("Need premium to mix these pots!"));
                    return;
                }
                if (client.getLevel(Skill.HERBLORE) < Utils.req[h]) {
                    client.send(new SendMessage("Requires herblore level " + Utils.req[h]));
                    return;
                }
                client.setPots(1200, itemUsed, otherItem, Utils.finished[h], Utils.potexp[h]);
            }
        }

        for (int i = 0; i < Utils.pot_4_dose.length; i++) {
            if ((itemUsed == Utils.pot_4_dose[i] && useWith == 229) || (itemUsed == 229 && useWith == Utils.pot_4_dose[i])) {
                client.deleteItem(itemUsed, 1);
                client.deleteItem(useWith, 1);
                client.addItem(Utils.pot_2_dose[i], 1);
                client.addItem(Utils.pot_2_dose[i], 1);
            }
        }
        for (int i = 0; i < Utils.pot_3_dose.length; i++) {
            if ((itemUsed == Utils.pot_3_dose[i] && useWith == Utils.pot_3_dose[i]) || (itemUsed == Utils.pot_3_dose[i] && useWith == Utils.pot_3_dose[i])) {
                client.deleteItem(itemUsed, 1);
                client.deleteItem(useWith, 1);
                client.addItem(Utils.pot_4_dose[i], 1);
                client.addItem(Utils.pot_2_dose[i], 1);
            } else if ((itemUsed == Utils.pot_3_dose[i] && useWith == Utils.pot_2_dose[i]) || (itemUsed == Utils.pot_2_dose[i] && useWith == Utils.pot_3_dose[i])) {
                client.deleteItem(itemUsed, 1);
                client.deleteItem(useWith, 1);
                client.addItem(Utils.pot_4_dose[i], 1);
                client.addItem(Utils.pot_1_dose[i], 1);
            }
        }
        for (int i = 0; i < Utils.pot_2_dose.length; i++) {
            if ((itemUsed == Utils.pot_2_dose[i] && useWith == 229) || (itemUsed == 229 && useWith == Utils.pot_2_dose[i])) {
                client.deleteItem(itemUsed, 1);
                client.deleteItem(useWith, 1);
                client.addItem(Utils.pot_1_dose[i], 1);
                client.addItem(Utils.pot_1_dose[i], 1);
            } else if ((itemUsed == Utils.pot_2_dose[i] && useWith == Utils.pot_2_dose[i]) || (itemUsed == Utils.pot_2_dose[i] && useWith == Utils.pot_2_dose[i])) {
                client.deleteItem(itemUsed, 1);
                client.deleteItem(useWith, 1);
                client.addItem(Utils.pot_4_dose[i], 1);
                client.addItem(229, 1);
            }
        }
        for (int i = 0; i < Utils.pot_1_dose.length; i++) {
            if ((itemUsed == Utils.pot_1_dose[i] && useWith == Utils.pot_1_dose[i]) || (itemUsed == Utils.pot_1_dose[i] && useWith == Utils.pot_1_dose[i])) {
                client.deleteItem(itemUsed, 1);
                client.deleteItem(useWith, 1);
                client.addItem(Utils.pot_2_dose[i], 1);
                client.addItem(229, 1);
            } else if ((itemUsed == Utils.pot_1_dose[i] && useWith == Utils.pot_2_dose[i]) || (itemUsed == Utils.pot_2_dose[i] && useWith == Utils.pot_1_dose[i])) {
                client.deleteItem(itemUsed, 1);
                client.deleteItem(useWith, 1);
                client.addItem(Utils.pot_3_dose[i], 1);
                client.addItem(229, 1);
            } else if ((itemUsed == Utils.pot_1_dose[i] && useWith == Utils.pot_3_dose[i]) || (itemUsed == Utils.pot_3_dose[i] && useWith == Utils.pot_1_dose[i])) {
                client.deleteItem(itemUsed, 1);
                client.deleteItem(useWith, 1);
                client.addItem(Utils.pot_4_dose[i], 1);
                client.addItem(229, 1);
            }
        }

        if (knife && (itemUsed == 1511 || otherItem == 1511)) {
            client.resetAction();
            client.shafting = true;
        }
        if ((itemUsed == 1733 || otherItem == 1733) && (itemUsed == 1741 || otherItem == 1741)) {
            client.showInterface(2311);
        }
        if ((itemUsed == 314 || otherItem == 314)
                && (itemUsed == 52 || otherItem == 52)) {
            client.resetAction();
            client.send(new SendString("" + client.GetItemName(53), 2799));
            client.sendFrame246(1746, 175, 53);
            client.sendFrame164(4429);
            client.dialogInterface = 53;
            client.fletchOtherAmt = 15;
            client.fletchOtherId1 = itemUsed;
            client.fletchOtherId2 = otherItem;
            client.fletchOtherId3 = 53;
            client.fletchOtherXp = 125;
            client.fletchOtherTime = 600;
        }
        for (int d = 0; d < Constants.darttip.length; d++) {
            if ((itemUsed == Constants.darttip[d] || otherItem == Constants.darttip[d])
                    && (itemUsed == 314 || otherItem == 314)) {
                client.resetAction();
                if (client.getLevel(Skill.FLETCHING) < Constants.darttip_required[d]) {
                    client.send(new SendMessage("You need level " + Constants.darttip_required[d] + " fletcing to make " + client.GetItemName(Constants.darts[d]).toLowerCase() + ""));
                    return;
                }
                if (!client.playerHasItem(314, 10) || !client.playerHasItem(Constants.darttip[d], 10)) {
                    client.send(new SendMessage("You need ten " + client.GetItemName(314).toLowerCase() + "s and ten " + client.GetItemName(Constants.darttip[d]).toLowerCase() + "s."));
                    return;
                }
                if (!client.playerHasItem(Constants.darts[d], 10)) {
                    client.send(new SendMessage("Your inventory is full!")); //Might send this if all else fails
                    return;
                }
                client.deleteItem(314, 10);
                client.deleteItem(Constants.darttip[d], 10);
                client.addItem(Constants.darts[d], 10);
                client.giveExperience(Constants.darttip_xp[d] * 5, Skill.FLETCHING);
                client.send(new SendMessage("You fletch ten " + client.GetItemName(Constants.darts[d]).toLowerCase() + "s."));
            }
        }
        for (int h = 0; h < Constants.heads.length; h++) {
            if ((itemUsed == Constants.heads[h] || otherItem == Constants.heads[h])
                    && (itemUsed == 53 || otherItem == 53)) {
                client.resetAction();
                if (client.getLevel(Skill.FLETCHING) < Constants.required[h]) {
                    client.send(new SendMessage("Requires level " + Constants.required[h]
                            + " fletching"));
                    return;
                }
                client.send(new SendString("" + client.GetItemName(Constants.arrows[h]), 2799));
                client.sendFrame246(1746, 180, Constants.arrows[h]);
                client.sendFrame164(4429);
                client.dialogInterface = Constants.arrows[h];
                client.fletchOtherAmt = 15;
                client.fletchOtherId1 = itemUsed;
                client.fletchOtherId2 = otherItem;
                client.fletchOtherId3 = Constants.arrows[h];
                client.fletchOtherXp = Constants.xp[h] * 5;
                client.fletchOtherTime = 1200;
                break;
            }
        }
        for (int id = 0; id < Constants.logs.length; id++) {
            if ((itemUsed == Constants.logs[id] || otherItem == Constants.logs[id])
                    && knife) {
                client.resetAction();
                client.dialogInterface = 2459;
                client.fletchLog = id;
                client.send(new SendString("Select a bow", 8879));
                client.sendFrame246(8870, 250, Constants.longbows[id]);// right picture
                client.sendFrame246(8869, 250, Constants.shortbows[id]);// left picture
                client.send(new SendString(client.GetItemName(Constants.shortbows[id]), 8871));
                client.send(new SendString(client.GetItemName(Constants.shortbows[id]), 8874));
                client.send(new SendString(client.GetItemName(Constants.longbows[id]), 8878));
                client.send(new SendString(client.GetItemName(Constants.longbows[id]), 8875));
                client.sendFrame164(8866);
                break;
            }
        }
        for (int id1 = 0; id1 < Constants.shortbow.length; id1++) {
            if ((itemUsed == Constants.shortbows[id1] || otherItem == Constants.shortbows[id1])
                    && (itemUsed == 1777 || otherItem == 1777)) {
                client.resetAction();
                client.send(new SendString("" + client.GetItemName(Constants.shortbow[id1]), 2799));
                client.sendFrame246(1746, 220, Constants.shortbow[id1]);
                client.sendFrame164(4429);
                client.dialogInterface = Constants.shortbow[id1];
                client.fletchOtherAmt = 1;
                client.fletchOtherId1 = itemUsed;
                client.fletchOtherId2 = otherItem;
                client.fletchOtherId3 = Constants.shortbow[id1];
                client.fletchOtherXp = Constants.shortexp[id1];
                client.fletchOtherTime = 1200;
            }
        }
        for (int b2 = 0; b2 < Constants.longbows.length; b2++) {
            if ((itemUsed == Constants.longbows[b2] || otherItem == Constants.longbows[b2])
                    && (itemUsed == 1777 || otherItem == 1777)) {
                client.resetAction();
                client.send(new SendString("" + client.GetItemName(Constants.longbow[b2]), 2799));
                client.sendFrame246(1746, 220, Constants.longbow[b2]);
                client.sendFrame164(4429);
                client.dialogInterface = Constants.longbow[b2];
                client.fletchOtherAmt = 1;
                client.fletchOtherId1 = itemUsed;
                client.fletchOtherId2 = otherItem;
                client.fletchOtherId3 = Constants.longbow[b2];
                client.fletchOtherXp = Constants.longexp[b2];
                client.fletchOtherTime = 1200;
            }
        }
        for (int h = 0; h < Constants.leathers.length; h++) {
            if ((itemUsed == 1733 || otherItem == 1733)
                    && (itemUsed == Constants.leathers[h] || otherItem == Constants.leathers[h])) {
                client.craftMenu(h);
                client.cIndex = h;
            }
        }

        if (itemUsed == 1755 || otherItem == 1755) {
            int gem = -1, slot = -1;
            if (itemUsed == 1755)
                gem = otherItem;
            else
                gem = itemUsed;
            for (int i11 = 0; i11 < Utils.uncutGems.length; i11++) {
                if (Utils.uncutGems[i11] == gem) {
                    slot = i11;
                }
            }
            if (slot < 0)
                return;
            if (Utils.gemReq[slot] > client.getLevel(Skill.CRAFTING)) {
                client.send(new SendMessage("You need a crafting level of " + Utils.gemReq[slot] + " to cut this."));
                return;
            }
            client.deleteItem(gem, 1);
            client.addItem(Utils.cutGems[slot], 1);
            client.giveExperience((int) (Utils.gemExp[slot] * 6), Skill.CRAFTING);
            client.send(new SendMessage("You cut the " + client.GetItemName(Utils.cutGems[slot]) + ""));
        }

        if (itemUsed == 1759 || otherItem == 1759) {
            int amulet;
            if (itemUsed == 1759)
                amulet = otherItem;
            else
                amulet = itemUsed;
            int strung = client.findStrungAmulet(amulet);
            if (strung < 0) {
                client.send(new SendMessage("You cannot string this item with wool!"));
                return;
            }
            client.deleteItem(amulet, 1);
            client.deleteItem(1759, 1);
            client.addItem(strung, 1);
            client.giveExperience(60, Skill.CRAFTING);
        }

        if (itemUsed == 590 && useWith == 1511 || itemUsed == 1511 && useWith == 590) {
            client.deleteItem(1511, 1);
            client.giveExperience(160, Skill.FIREMAKING);
            client.resetAction();
        } else if (itemUsed == 1521 && useWith == 590 || itemUsed == 590 && useWith == 1521) {
            if (client.getLevel(Skill.FIREMAKING) >= 15) {
                client.deleteItem(1521, 1);
                client.giveExperience(240, Skill.FIREMAKING);
                client.resetAction();
            } else {
                client.send(new SendMessage("You need a firemaking level of 15 to burn oak logs."));
            }
        } else if (itemUsed == 1519 && useWith == 590 || itemUsed == 590 && useWith == 1519) {
            if (client.getLevel(Skill.FIREMAKING) >= 30) {
                client.deleteItem(1519, 1);
                client.giveExperience(360, Skill.FIREMAKING);
                client.resetAction();
            } else {
                client.send(new SendMessage("You need a firemaking of 30 to burn willow logs."));
            }
        } else if (itemUsed == 1517 && useWith == 590 || itemUsed == 590 && useWith == 1517) {
            if (client.getLevel(Skill.FIREMAKING) >= 45) {
                client.deleteItem(1517, 1);
                client.giveExperience(540, Skill.FIREMAKING);
                client.resetAction();
            } else {
                client.send(new SendMessage("You need a firemaking level of 45 to burn maple logs."));
            }
        } else if (itemUsed == 1515 && useWith == 590 || itemUsed == 590 && useWith == 1515) {
            if (client.getLevel(Skill.FIREMAKING) >= 60) {
                client.deleteItem(1515, 1);
                client.giveExperience(812, Skill.FIREMAKING);
                client.resetAction();
            } else {
                client.send(new SendMessage("You need a firemaking of 60 to burn yew logs."));
            }
        } else if (itemUsed == 1513 && useWith == 590 || itemUsed == 590 && useWith == 1513) {
            if (client.getLevel(Skill.FIREMAKING) >= 75) {
                client.deleteItem(1513, 1);
                client.giveExperience(1216, Skill.FIREMAKING);
                client.resetAction();
            } else {
                client.send(new SendMessage("You need a firemaking level of 75 to burn magic logs."));
            }
        }
    }

}
