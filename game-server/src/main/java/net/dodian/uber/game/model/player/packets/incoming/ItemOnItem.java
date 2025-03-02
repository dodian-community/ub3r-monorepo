package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
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
        if(usedWithSlot > 28 || usedWithSlot < 0 || itemUsedSlot > 28 || itemUsedSlot < 0) { //No need to go out of scope!
            client.disconnected = true;
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
        client.farming.saplingMaking(client, useWith, usedWithSlot, itemUsed, itemUsedSlot);
        int otherItem = client.playerItems[usedWithSlot] - 1;
        boolean knife = (useWith == 946 || itemUsed == 946) || (useWith == 5605 || itemUsed == 5605);
        if ((itemUsed == 2383 && useWith == 2382) || (itemUsed == 2382 && useWith == 2383)) {
            if (client.getSkillLevel(Skill.CRAFTING) >= 60) {
                client.deleteItem(itemUsed, itemUsedSlot, 1);
                client.deleteItem(otherItem, usedWithSlot, 1);
                client.addItem(989, 1);
                client.send(new SendMessage("You have crafted the crystal key!  I wonder what it's for?"));
            } else {
                client.send(new SendMessage("You need 60 crafting to make the crystal key"));
            }
        }
        if(itemUsed >= 6157 && itemUsed <= 6161 && useWith >= 6157 && useWith <= 6161) {
            client.NpcDialogueSend = false;
            client.NpcDialogue = 10000;
        }
        /* Unfinish potion making! */
        for (int h = 0; h < Utils.herbs.length; h++) {
            if ((itemUsed == Utils.herbs[h] && otherItem == 227) || (itemUsed == 227 && otherItem == Utils.herbs[h])) {
                if (!client.premium && h > 2) {
                    client.send(new SendMessage("Need premium to mix these pots!"));
                    return;
                }
                if (client.getSkillLevel(Skill.HERBLORE) < Utils.grimy_herbs_lvl[h]) {
                    client.send(new SendMessage("Requires herblore level " + Utils.grimy_herbs_lvl[h]));
                    return;
                }
                int xp = 0;
                if (client.premium)
                    xp = Utils.grimy_herbs_xp[h];
                client.setSkillAction(Skill.HERBLORE.getId(), Utils.herb_unf[h], 1, itemUsed, otherItem, xp, 363, 1);
                client.skillMessage = "You mix the " + client.GetItemName(itemUsed != 227 ? itemUsed : otherItem).toLowerCase() + " herb with the vial of water.";
                break;
            }
        }
        /* Finish Potions */
        for (int h = 0; h < Utils.unf_potion.length; h++) {
            if ((itemUsed == Utils.secondary[h] && otherItem == Utils.unf_potion[h])
                    || (itemUsed == Utils.unf_potion[h] && otherItem == Utils.secondary[h])) {
                if (!client.premium && h > 3) {
                    client.send(new SendMessage("Need premium to mix these pots!"));
                    return;
                }
                if (client.getLevel(Skill.HERBLORE) < Utils.req[h]) {
                    client.send(new SendMessage("Requires herblore level " + Utils.req[h]));
                    return;
                }
                client.setSkillAction(Skill.HERBLORE.getId(), Utils.finished[h], 1, itemUsed, otherItem, Utils.potexp[h], 363, 3);
                client.skillMessage = "You mix the " + client.GetItemName(Utils.secondary[h]) + " into your potion.";
                break;
            }
        }
        /* Super combat potion */
        boolean checkPotionUsed = false, checkOtherPotionUsed = false;
        int[] potionItems = {269, 2436, 2440, 2442};
        for (int potionItem : potionItems)
            if (potionItem == 269 && (itemUsed == 111 || itemUsed == potionItem) || itemUsed == potionItem) {
                checkPotionUsed = true;
                break;
            }
        for (int potionItem : potionItems)
            if ((potionItem == 269 && (otherItem == 111 || otherItem == potionItem)) || otherItem == potionItem) {
                checkOtherPotionUsed = true;
                break;
            }
        if (checkPotionUsed && checkOtherPotionUsed) { //Overload making
            boolean hasAllItem = true;
            for(int i = 0; i < potionItems.length && hasAllItem; i++) {
                if(potionItems[i] == 269 && (!client.playerHasItem(269) && !client.playerHasItem(111))) hasAllItem = false;
                else if(potionItems[i] != 269 && !client.playerHasItem(potionItems[i])) hasAllItem = false;
            }
            if(hasAllItem) {
                if(client.getSkillLevel(Skill.HERBLORE) >= 88) {
                    //Succeeed!
                    client.setSkillAction(Skill.HERBLORE.getId(), 12695, 1, 2436, -1, 600, 363, 3);
                    client.skillMessage = "You mix the ingredients together and made a super combat potion.";
                } else client.send(new SendMessage("You need level 88 herblore to mix a super combat potion!"));
            } else client.send(new SendMessage("You need a torstol herb or (unf) potion, super attack, strength and defence potion!"));
        }
        /* Overload potion */
        potionItems = new int[]{12695, 2444, 5978};
        checkPotionUsed = false;
        checkOtherPotionUsed = false;
        for (int potionItem : potionItems)
            if (itemUsed == potionItem) {
                checkPotionUsed = true;
                break;
            }
        for (int potionItem : potionItems)
            if (otherItem == potionItem) {
                checkOtherPotionUsed = true;
                break;
            }
        if (checkPotionUsed && checkOtherPotionUsed) { //Overload making
            boolean hasAllItem = true;
            for(int i = 0; i < potionItems.length && hasAllItem; i++)
                if(!client.playerHasItem(potionItems[i])) hasAllItem = false;
            if(hasAllItem) {
                if(client.getSkillLevel(Skill.HERBLORE) >= 93) {
                    //Succeeed!
                    client.setSkillAction(Skill.HERBLORE.getId(), 11730, 1, 5978, -1, 800, 363, 3);
                    client.skillMessage = "You mix the ingredients together and made a Overload potion.";
                } else client.send(new SendMessage("You need level 93 herblore to mix a overload potion!"));
            } else client.send(new SendMessage("You need a coconut, super combat potion and a ranging potion!"));
        }
        /* Mix dose potions */
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
        /* Shiny keys */
        if (itemUsed == 85 || otherItem == 85) {
            int otherId = itemUsed == 85 ? otherItem : itemUsed;
            if(otherId == 1543 || otherId == 1544) {
                if(!client.checkItem(otherId + 1)) {
                    client.deleteItem(85, 1);
                    client.addItem(otherId + 1, 1);
                    client.send(new SendMessage("Your key shine bright and turned your " + client.GetItemName(85).toLowerCase() + " into a " + client.GetItemName(otherId + 1).toLowerCase()));
                } else
                    client.send(new SendMessage("I already have a " + client.GetItemName(otherId + 1).toLowerCase() + " in " + (client.playerHasItem(otherId + 1) ? "my inventory!" : "my bank!")));
            } else if (otherId == 2382 || otherId == 2383) {
                if(!client.checkItem(989) && (!client.checkItem(2382) || !client.checkItem(2383))) {
                    client.deleteItem(85, 1);
                    client.addItem(otherId == 2382 ? 2383 : 2382, 1);
                    client.send(new SendMessage("Your key shine bright and turned your " + client.GetItemName(85).toLowerCase() + " into a " + client.GetItemName(otherId == 2382 ? 2383 : 2382).toLowerCase()));
                } else if (!client.checkItem(989) && client.checkItem(2382) && client.checkItem(2383))
                    client.send(new SendMessage("You already have the crystals, perhaps you should combine them?"));
                else
                    client.send(new SendMessage("I already have a " + client.GetItemName(989).toLowerCase() + " in " + (client.playerHasItem(989) ? "my inventory!" : "my bank!")));
            }
        }
        boolean rainbowHat = Server.itemManager.getName(itemUsed).endsWith("partyhat") && Server.itemManager.getName(otherItem).endsWith("partyhat") && Server.itemManager.isNote(itemUsed) && Server.itemManager.isNote(otherItem);
        if(rainbowHat) {
            boolean hasItems = true;
            for(int i = 1038; i <= 1048 && hasItems; i += 2)
                if(!client.playerHasItem(i))
                    hasItems = false;
            if(hasItems) {
                for(int i = 1038; i <= 1048; i += 2)
                    client.deleteItem(i, 1);
                client.addItemSlot(11863, 1, itemUsedSlot);
            } else client.send(new SendMessage("You need one of each partyhat to combine it into the rainbow partyhat!"));
        }
        if (knife && (itemUsed == 11863 || otherItem == 11863)) {
            int slotRemain = 5 - client.getFreeSpace();
            if(slotRemain <= 0) {
                client.deleteItem(11863, 1);
                for(int i = 1038; i <= 1048; i += 2)
                    client.addItem(i, 1);
                client.send(new SendMessage("You gentle used the knife on the paper hat and cut it into different color partyhats."));
            } else client.send(new SendMessage("You need to have " + (slotRemain == 1 ? "one" : slotRemain) + " empty slot"+ (slotRemain != 1 ? "s" : "")+" to tear the rainbow partyhat apart."));
        }
        if((itemUsed == 962 || otherItem == 962) && (itemUsed == 11863 || otherItem == 11863)) {
            client.deleteItem(962, itemUsed == 962 ? itemUsedSlot : usedWithSlot, 1);
            client.deleteItem(11863, itemUsed == 11863 ? itemUsedSlot : usedWithSlot, 1);
            client.addItemSlot(11862, 1, itemUsed == 11863 ? itemUsedSlot : usedWithSlot);
        }
        if (knife && (itemUsed == 11862 || otherItem == 11862)) {
            if(client.getFreeSpace() > 0) {
                client.deleteItem(11862, itemUsed == 11862 ? itemUsedSlot : usedWithSlot, 1);
                client.addItemSlot(11863, 1, itemUsed == 11862 ? itemUsedSlot : usedWithSlot);
                client.addItem(962, 1);
                client.send(new SendMessage("You gentle used the knife on the paper hat and cut it into a cracker and a rainbow partyhat."));
            } else client.send(new SendMessage("You need to have atleast one space to tear the black partyhat apart!"));
        }
        int[][] dyes = {{-1, 1019}, {1763, 1007}, {1765, 1023}, {1767, 1021}, {1769, 1031}, {1771, 1027}, {1773, 1029}}; //Black, Red, yellow, blue, orange, green, purple
        for (int[] ints : dyes)
            if (itemUsed == ints[0] || otherItem == ints[0]) {
                for (int[] value : dyes)
                    if ((itemUsed == ints[0] && otherItem == value[1]) || (otherItem == ints[0] && itemUsed == value[1])) {
                        if (value[1] != ints[1]) {
                            client.deleteItem(itemUsed, itemUsedSlot, 1);
                            client.deleteItem(otherItem, usedWithSlot, 1);
                            client.addItemSlot(ints[1], 1, itemUsed == value[1] ? itemUsedSlot : usedWithSlot);
                        } else client.send(new SendMessage("There is no point in using the same color as the cape!"));
                        break;
                    }
            }
        /* Slayer helmet creation! */
        int[] slayerHelmItems = {4155, 4156, 4164, 4166, 4168, 4551, 6720, 8923, 11784, 8921};
        //4155-gem, 4156-mirror shield, 4164-face mask, 4166-earmuffs, 4168-nosepeg, 4551-spiny helm, 6720-slayer gloves, 8923-witchwood icon
        boolean checkItemUsed = false;
        for (int slayerHelmItem : slayerHelmItems)
            if (itemUsed == slayerHelmItem) {
                checkItemUsed = true;
                break;
            }
        boolean checkOtherItem = false;
        for (int slayerHelmItem : slayerHelmItems)
            if (otherItem == slayerHelmItem) {
                checkOtherItem = true;
                break;
            }
        if (checkItemUsed && checkOtherItem) { //Slayer helmet making!
            boolean hasAllItem = true;
            for(int i = 0; i < slayerHelmItems.length - 2 && hasAllItem; i++)
                if(!client.playerHasItem(slayerHelmItems[i])) hasAllItem = false;
            if(!hasAllItem) {
                client.send(new SendMessage("You need a enchanted gem, mirror shield, face mask, earmuffs, nosepeg, spiny helm,"));
                client.send(new SendMessage("slayer gloves, witchwood icon and black mask or black mask (i)"));
            } else if(client.playerHasItem(slayerHelmItems[slayerHelmItems.length - 1]) || client.playerHasItem(slayerHelmItems[slayerHelmItems.length - 2])) {
                if(client.getSkillLevel(Skill.CRAFTING) >= 70) {
                    int slayerHelm = client.playerHasItem(slayerHelmItems[slayerHelmItems.length - 2]) ? 11865 : 11864; //Trim : untrimmed
                    for(int i = 0; i < slayerHelmItems.length - 2; i++)
                        client.deleteItem(slayerHelmItems[i], 1);
                    client.deleteItem(slayerHelm == 11865 ? slayerHelmItems[slayerHelmItems.length - 2] : slayerHelmItems[slayerHelmItems.length - 1], 1);
                    client.addItem(slayerHelm, 1);
                    client.send(new SendMessage("You assemble the items together and made a "+client.GetItemName(slayerHelm).toLowerCase()+"."));
                } else client.send(new SendMessage("You need level 70 crafting to assemble these items together."));
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
            client.setSkillAction(Skill.FLETCHING.getId(), 53, 15, itemUsed, otherItem, 5, -1, 2);
        }
        for (int d = 0; d < Constants.darttip.length; d++) {
            if ((itemUsed == Constants.darttip[d] || otherItem == Constants.darttip[d])
                    && (itemUsed == 314 || otherItem == 314)) {
                client.resetAction();
                if (client.getLevel(Skill.FLETCHING) < Constants.darttip_required[d]) {
                    client.send(new SendMessage("You need level " + Constants.darttip_required[d] + " fletcing to make " + client.GetItemName(Constants.darts[d]).toLowerCase()));
                    return;
                }
                if (!client.playerHasItem(Constants.darts[d]) && client.freeSlots() < 1) {
                    client.send(new SendMessage("Your inventory is full!")); //Might send this if all else fails
                    return;
                }
                client.setSkillAction(Skill.FLETCHING.getId(), Constants.darts[d], 10, itemUsed, otherItem, Constants.darttip_xp[d] / 2, -1, 3);
                break;
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
                if (!client.playerHasItem(Constants.arrows[h]) && client.freeSlots() < 1) {
                    client.send(new SendMessage("Your inventory is full!")); //Might send this if all else fails
                    return;
                }
                client.setSkillAction(Skill.FLETCHING.getId(), Constants.arrows[h], 15, itemUsed, otherItem, Constants.xp[h], -1, 3);
                client.skillMessage = "You fletched some " + client.GetItemName(Constants.arrows[h]).toLowerCase() + ".";
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
                if (client.getLevel(Skill.FLETCHING) < Constants.shortreq[id1]) {
                    client.send(new SendMessage("Requires level " + Constants.shortreq[id1]
                            + " fletching"));
                    return;
                }
                client.setSkillAction(Skill.FLETCHING.getId(), Constants.shortbow[id1], 1, itemUsed, otherItem, Constants.shortexp[id1], 6679 + id1, 2);
                client.skillMessage = "You string your " + client.GetItemName(Constants.shortbows[id1]).toLowerCase() + " into a "+client.GetItemName(Constants.shortbow[id1]).toLowerCase()+".";
                break;
            }
        }
        for (int b2 = 0; b2 < Constants.longbows.length; b2++) {
            if ((itemUsed == Constants.longbows[b2] || otherItem == Constants.longbows[b2])
                    && (itemUsed == 1777 || otherItem == 1777)) {
                client.resetAction();
                if (client.getLevel(Skill.FLETCHING) < Constants.longreq[b2]) {
                    client.send(new SendMessage("Requires level " + Constants.longreq[b2]
                            + " fletching"));
                    return;
                }
                client.setSkillAction(Skill.FLETCHING.getId(), Constants.longbow[b2], 1, itemUsed, otherItem, Constants.longexp[b2], 6685 + b2, 2);
                client.skillMessage = "You string your " + client.GetItemName(Constants.longbows[b2]).toLowerCase() + " into a "+client.GetItemName(Constants.longbow[b2]).toLowerCase()+".";
                break;
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
            int gem = itemUsed == 1755 ? otherItem : itemUsed, slot = -1;
            for (int i11 = 0; i11 < Utils.uncutGems.length; i11++)
                if (Utils.uncutGems[i11] == gem) slot = i11;

            if(slot >= 0) {
                if (Utils.gemReq[slot] > client.getLevel(Skill.CRAFTING)) {
                    client.send(new SendMessage("You need a crafting level of " + Utils.gemReq[slot] + " to cut this."));
                    return;
                }
                client.setSkillAction(Skill.CRAFTING.getId(), Utils.cutGems[slot], 1, gem, -1, Utils.gemExp[slot] * 5, Utils.gemEmote[slot], 3);
                client.skillMessage = "You cut the " + client.GetItemName(Utils.cutGems[slot]);
            }
        }
        if (itemUsed == 1391 || otherItem == 1391) {
            int orb = itemUsed == 1391 ? otherItem : itemUsed, slot = -1;
            for (int i = 0; i < Utils.orbs.length; i++)
                if (Utils.orbs[i] == orb) slot = i;

            if(slot >= 0) {
                if (Utils.orbLevel[slot] > client.getLevel(Skill.CRAFTING)) {
                    client.send(new SendMessage("You need a crafting level of " + Utils.orbLevel[slot] + " to make this."));
                    return;
                }
                client.setSkillAction(Skill.CRAFTING.getId(), Utils.staves[slot], 1, orb, 1391, Utils.orbXp[slot], -1, 3);
                client.skillMessage = "You put the " + client.GetItemName(orb).toLowerCase() + " onto the battlestaff and made a " + client.GetItemName(Utils.staves[slot]).toLowerCase() + ".";
            }
        }
        if((itemUsed == 1785 && otherItem == 1775) || (itemUsed == 1775 && otherItem == 1785)) {
            //884 = blowing glass emote!
            String jump = "\\n\\n\\n";
            client.sendFrame246(11465, 160, 229);
            client.send(new SendString(jump + "Vial", 11474));
            client.sendFrame246(11466, 180, 1980);
            client.send(new SendString(jump + "Empty cup", 12396));
            client.sendFrame246(11467, 150, 6667);
            client.send(new SendString(jump + "Fishbowl", 12400));
            client.sendFrame246(11468, 150, 567);
            client.send(new SendString(jump + "Orb", 12404));
            client.sendFrame246(11469, 190, -1);
            client.send(new SendString(jump, 12408));
            client.sendFrame246(11470, 190, -1);
            client.send(new SendString(jump, 12412));
            client.sendFrame246(6199, 190, -1);
            client.send(new SendString(jump, 6203));
            client.showInterface(11462);
        }
        if((itemUsed == 6667 && otherItem == 1755) || (itemUsed == 1755 && otherItem == 6667)) {
            int slot = itemUsed == 6667 ? itemUsedSlot : usedWithSlot;
            client.deleteItem(6667, slot, 1);
            client.addItemSlot(7534, 1, slot);
            client.checkItemUpdate();
            client.giveExperience(60, Skill.CRAFTING);
            client.send(new SendMessage("You chisel the fishbowl into a helmet."));
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
            client.setSkillAction(Skill.CRAFTING.getId(), strung, 1, amulet, 1759, 60, -1, 2);
            client.skillMessage = "You put the wool onto the " + client.GetItemName(strung).toLowerCase() + ".";
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
        /* Karils Crossbow */
        if (itemUsed == 4938 && useWith == 4212 || itemUsed == 4212 && useWith == 4938) {
            client.send(new SendMessage("WIP: Repair!"));
        } else if (itemUsed == 4936 && useWith == 6724 || itemUsed == 6724 && useWith == 4936) {
            client.send(new SendMessage("WIP: Repair!"));
        } else if(itemUsed == 4938 || useWith == 4938)
            client.send(new SendMessage("You need a Crystal bow to repair the Karil crossbow half way."));
        else if(itemUsed == 4936 || useWith == 4936)
            client.send(new SendMessage("You need a Seercull bow to repair the Karil crossbow into full strength."));
        /* Ahrim Staff */
        if (itemUsed == 4866 && useWith == 4675 || itemUsed == 4675 && useWith == 4866) {
            client.send(new SendMessage("WIP: Repair!"));
        } else if (itemUsed == 4864 && useWith == 6914 || itemUsed == 6914 && useWith == 4864) {
            client.send(new SendMessage("WIP: Repair!"));
        } else if(itemUsed == 4866 || useWith == 4866)
            client.send(new SendMessage("You need a Ancient staff to repair the Ahrim staff half way."));
        else if(itemUsed == 4864 || useWith == 4864)
            client.send(new SendMessage("You need a Master wand to repair the Ahrim staff into full strength."));
        /*
        - Karil weapon 1 seercull and 1 crystal bow, armour require 1 spinned of each piece (helm for helm etc.)
- Ahrim weapon 1 ancient staff and 1 master wand, armour require 1 Infinity of each piece (hat for helm etc.)
- All other barrows gear require 2 dragon weapons of a choice for the weapon and armour require 1 dragon/obsidian/rock-shell of each piece (medium helm or helm for helm etc.)
         */
        client.checkItemUpdate();
    }

}
