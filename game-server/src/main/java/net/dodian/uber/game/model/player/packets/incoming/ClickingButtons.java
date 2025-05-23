package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.combat.impl.CombatStyleHandler;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Emotes;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.player.content.Skillcape;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.RemoveInterfaces;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.packets.outgoing.SendString;
import net.dodian.uber.game.model.player.quests.QuestSend;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.prayer.Prayers;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

import static net.dodian.uber.game.model.player.skills.Skill.*;
import static net.dodian.utilities.DotEnvKt.getServerDebugMode;

public class ClickingButtons implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int actionButton = Utils.HexToInt(client.getInputStream().buffer, 0, packetSize);
        if (getServerDebugMode()) {
            client.println("button=" + actionButton);
        }
        if (System.currentTimeMillis() - client.lastButton < 600 || !client.validClient) { //To prevent some shiez!
            client.lastButton = System.currentTimeMillis();
            return;
        }
        if(!(actionButton >= 9157 && actionButton <= 9194))
            client.actionButtonId = actionButton;
        if(actionButton != 10239 && actionButton != 10238 && actionButton != 6212 && actionButton != 6211) ////10239, 10238, 6212, 6211
            client.resetAction(false);
        if (client.duelButton(actionButton)) {
            return;
        }
        Prayers.Prayer prayer = Prayers.Prayer.forButton(actionButton);
        if (prayer != null) {
            client.getPrayerManager().togglePrayer(prayer);
            return;
        }
        if(QuestSend.questMenu(client, actionButton)) {
            return;
        }
        if(client.refundSlot != -1) { //Refund code!
            int size = client.rewardList.size();
            int checkSlot = 1;
            int position = size - client.refundSlot;
            if(actionButton == 9158 || actionButton == 9168 || actionButton == 9179 || actionButton == 9191)
                checkSlot = 2;
            else if(actionButton == 9169 || actionButton == 9180 || actionButton == 9192)
                checkSlot = 3;
            else if(actionButton == 9181 || actionButton == 9193)
                checkSlot = 4;
            else if(actionButton == 9194)
                checkSlot = 5;
            if(client.refundSlot == 0 && ((size > 3 && checkSlot == 5) || (size == 3 && checkSlot == 4) || (size == 1 && checkSlot == 2) || (size == 2 && checkSlot == 3))) { //Close!
                client.refundSlot = -1;
                client.send(new RemoveInterfaces());
            } else if((position > 3) && checkSlot == 4)
                client.refundSlot += 3;
            else if(client.refundSlot != 0 && ((position <= 3 && checkSlot == position + 1) || (position > 3 && checkSlot == 5)))
                client.refundSlot -= 3;
            else client.reclaim(checkSlot);
            if(!client.rewardList.isEmpty())
                client.setRefundOptions();
            return;
        }
        if(client.herbMaking != -1) { //Herb making code
            int size = client.herbOptions.size();
            int checkSlot = 1;
            int position = size - client.herbMaking;
            if(actionButton == 9158 || actionButton == 9168 || actionButton == 9179 || actionButton == 9191)
                checkSlot = 2;
            else if(actionButton == 9169 || actionButton == 9180 || actionButton == 9192)
                checkSlot = 3;
            else if(actionButton == 9181 || actionButton == 9193)
                checkSlot = 4;
            else if(actionButton == 9194)
                checkSlot = 5;
            if(client.herbMaking == 0 && ((size > 3 && checkSlot == 5) || (size == 3 && checkSlot == 4) || (size == 1 && checkSlot == 2) || (size == 2 && checkSlot == 3))) { //Close!
                client.herbMaking = -1;
                client.send(new RemoveInterfaces());
            } else if((position > 3) && checkSlot == 4)
                client.herbMaking += 3;
            else if(client.refundSlot != 0 && ((position <= 3 && checkSlot == position + 1) || (position > 3 && checkSlot == 5)))
                client.herbMaking -= 3;
            else if (client.herbMaking + checkSlot <= size) { //When press button show us x amount interface
                client.send(new RemoveInterfaces());
                client.XinterfaceID = 4753;
                client.XremoveSlot = client.herbMaking + checkSlot;
                client.herbMaking = -1;
                client.getOutputStream().createFrame(27);
            }
            client.setHerbOptions();
            return;
        }
        Emotes.doEmote(actionButton, client);
        switch (actionButton) {
            case 58073:
                client.send(new SendMessage("Visit the Dodian.net UserCP and click edit pin to remove your pin"));
                break;
            case 151:
                client.NpcDialogue = 27;
                client.NpcDialogueSend = false;
                break;
            case 150:
                client.NpcDialogue = 26;
                client.NpcDialogueSend = false;
                break;
            case 8198:
                Balloons.acceptItems(client);
                break;
            case 83093:
                client.sendFrame248(21172, 3213);
                break;
            case 83051:
            case 9118:
            case 19022:
                client.send(new RemoveInterfaces());
                break;
            case 24136:
                client.yellOn = true;
                client.send(new SendMessage("You enabled the boss yell messages."));
                break;
            case 24137:
                client.yellOn = false;
                client.send(new SendMessage("You disabled the boss yell messages."));
                break;
            case 89223:
                for (int i = 0; i < client.playerItems.length; i++) {
                    client.bankItem(client.playerItems[i], i, client.playerItemsN[i]);
                }
		    break;
            case 3056: //Small tree
            case 3057: //Big Tree
            case 3058: //Mountain
            case 3059: //Castle
            case 3060: //Tent
            case 48054: //totem!
                int pos = client.skillX == 2772 && client.skillY == 3235 ? 5:
                client.skillX == 2864 && client.skillY == 2971 ? 4:
                client.skillX == 3511 && client.skillY == 3505 ? 2: 0;
                client.travelTrigger(pos);
                break;
            case 75010:
            case 84237: //Home teleport aka Yanille
                client.triggerTele(2604 + Misc.random(6), 3101 + Misc.random(3), 0, false);
                break;
            case 4143: //Normal spellbook!
            case 50235: //Seers
                client.triggerTele(2722 + Misc.random(6), 3484 + Misc.random(2), 0, false);
                break;
            case 4146: //Normal spellbook!
            case 50245: //Ardougne
                client.triggerTele(2660 + Misc.random(4), 3306 + Misc.random(4), 0, false);
                break;
            case 4150: //Normal spellbook!
            case 50253: // Catherby
                client.triggerTele(2802 + Misc.random(4), 3432 + Misc.random(3), 0, false);
                break;
            case 6004: //Normal spellbook!
            case 51005: //Legends guild
                client.triggerTele(2726 + Misc.random(5), 3346 + Misc.random(2), 0, false);
                break;
            case 6005: //Normal spellbook!
            case 51013: //Taverly
                client.triggerTele(2893 + Misc.random(4), 3454 + Misc.random(3), 0, false);
                break;
            case 29031: //Normal spellbook!
            case 51023: //Fishing guild
                client.triggerTele(2596 + Misc.random(3), 3406 + Misc.random(4), 0, true);
                break;
            case 72038:
            case 51031: //Gnome village
                client.triggerTele(2472 + Misc.random(6), 3436 + Misc.random(3), 0, false);
                break;
            case 4140: //Normal spell book pvp teleport!
            case 51039: //Edgeville teleport
                client.triggerTele(3085 + Misc.random(4), 3488 + Misc.random(4), 0, false);
                break;
            case 74212:
            case 49047: // old magic on
            case 49046: // old magic off
                if (client.ancients == 1) {
                    client.setSidebarInterface(6, 1151); // magic tab (ancient =
                    // 12855);
                    client.ancients = 0;
                    client.send(new SendMessage("Normal magic enabled"));
                } else {
                    client.setSidebarInterface(6, 12855); // magic tab (ancient =
                    // 12855);
                    client.ancients = 1;
                    client.send(new SendMessage("Ancient magic enabled"));
                }
                break;
            case 26076:
                // frame36(6575, 1);
                break;
            case 53245:
            case 53246:
            case 53247:
            case 53248:
            case 53249:
            case 53250:
            case 53251:
            case 53252:
            case 53253:
            case 53254:
            case 53255:
                client.duelButton2(client.actionButtonId - 53245);
                break;
            case 54074:
                Server.slots.playSlots(client, -1);
                break;
            case 25120:
                if (System.currentTimeMillis() - client.lastButton < 1000) {
                    client.lastButton = System.currentTimeMillis();
                    break;
                } else {
                    client.lastButton = System.currentTimeMillis();
                }
                Client dw = client.getClient(client.duel_with);
                /*
                 * Danno: Sometimes dcs a player. So we break if other player is null.
                 */
                if (dw == null)
                    break;
                client.canOffer = false;
                if (!client.validClient(client.duel_with)) {
                    client.declineDuel();
                }
                if (client.duelConfirmed2) {
                    break;
                }
                client.duelConfirmed2 = true;
                if (dw.duelConfirmed2) {
                    client.removeEquipment();
                    dw.removeEquipment();
                    client.startDuel();
                    dw.startDuel();
                } else {
                    client.send(new SendString("Waiting for other player...", 6571));
                    dw.send(new SendString("Other player has accepted", 6571));
                }
                break;
            case 15147: // bronze
            case 15146:
            case 10247:
            case 9110:
            case 15151: // iron
            case 15150:
            case 15149:
            case 15148:
            case 15155: // silver
            case 15154:
            case 15153:
            case 15152:
            case 15159: // steel
            case 15158:
            case 15157:
            case 15156:
            case 15163: // gold
            case 15162:
            case 15161:
            case 15160:
            case 29017: // mithril
            case 29016:
            case 24253:
            case 16062:
            case 29022: // addy
            case 29020:
            case 29019:
            case 29018:
            case 29026: // rune
            case 29025:
            case 29024:
            case 29023:
                client.startSmelt(client.actionButtonId);
                break;
            case 34185:
            case 34184: // vamps
            case 34183:
            case 34182:
            case 34189: // chaps
            case 34188:
            case 34187:
            case 34186:
            case 34193:
            case 34192:
            case 34191:
            case 34190:
                client.startHideCraft(client.actionButtonId);
                break;
            case 33187: // armor
            case 33186:
            case 33185:
            case 33190: // gloves
            case 33189:
            case 33188:
            case 33193: // boots
            case 33192:
            case 33191:
            case 33196: // vamps
            case 33195:
            case 33194:
            case 33199: // chaps
            case 33198:
            case 33197:
            case 33202: // coif
            case 33201:
            case 33200:
            case 33205:// cowl
            case 33204:
            case 33203:
                client.startCraft(client.actionButtonId);
            break;
            case 57225:
                client.startTan(1, 0);
            break;
            case 57217:
                client.startTan(5, 0);
                break;
            case 57201:
            case 57209:
                client.startTan(27, 0);
            break;
            case 57229: //Hard leather!
                client.startTan(1, 1);
            break;
            case 57221:
                client.startTan(5, 1);
            break;
            case 57205:
            case 57213:
                client.startTan(27, 1);
            break;
            case 57227:
                client.startTan(1, 2);
                break;
            case 57219:
                client.startTan(5, 2);
                break;
            case 57211:
            case 57203:
                client.startTan(27, 2);
                break;
            case 57228:
                client.startTan(1, 3);
                break;
            case 57220:
                client.startTan(5, 3);
                break;
            case 57212:
            case 57204:
                client.startTan(27, 3);
                break;
            case 57231:
                client.startTan(1, 4);
                break;
            case 57223:
                client.startTan(5, 4);
                break;
            case 57215:
            case 57207:
                client.startTan(27, 4);
                break;
            case 57232:
                client.startTan(1, 5);
                break;
            case 57224:
                client.startTan(5, 5);
                break;
            case 57216:
            case 57208:
                client.startTan(27, 5);
                break;
            case 10239: //make stuff 1
                if(client.playerSkillAction.isEmpty()) break;
                client.send(new RemoveInterfaces());
                client.skillActionCount = 1;
                client.skillActionTimer = client.playerSkillAction.get(7);
                break;
            case 10238: //make stuff 5
                if(client.playerSkillAction.isEmpty()) break;
                client.send(new RemoveInterfaces());
                client.skillActionCount = 5;
                client.skillActionTimer = client.playerSkillAction.get(7);
                break;
            case 6212: //make stuff 10 (x)
                if(client.playerSkillAction.isEmpty()) break;
                client.send(new RemoveInterfaces());
                client.skillActionCount = 10;
                client.skillActionTimer = client.playerSkillAction.get(7);
                break;
            case 6211: //make stuff 28 (all)
                if(client.playerSkillAction.isEmpty()) break;
                client.send(new RemoveInterfaces());
                client.skillActionCount = 28;
                client.skillActionTimer = client.playerSkillAction.get(7);
                break;
            case 44210: //Make one vial
            case 44209: //Make 5
            case 44208: //Make 10
            case 44207: //Make all (27)
                client.send(new RemoveInterfaces());
                int[] craftVialAmount = {27, 10, 5, 1};
                client.setSkill(CRAFTING.getId(), 229,  1, 1775, -1, 80, 884, 3);
                client.skillActionCount = craftVialAmount[actionButton - 44207];
                client.skillActionTimer = client.playerSkillAction.get(7);
            break;
            case 48108: //Make one empty cup
            case 48107: //Make 5
            case 48106: //Make 10
            case 48105: //Make all (27)
                client.send(new RemoveInterfaces());
                if(client.getLevel(CRAFTING) < 18) {
                    client.send(new SendMessage("You need level 18 crafting to craft a empty cup."));
                    break;
                }
                int[] craftCupAmount = new int[]{27, 10, 5, 1};
                client.setSkill(CRAFTING.getId(), 1980,  1, 1775, -1, 120, 884, 3);
                client.skillActionCount = craftCupAmount[actionButton - 48105];
                client.skillActionTimer = client.playerSkillAction.get(7);
                break;
            case 48112: //Make one fishbowl
            case 48111: //Make 5
            case 48110: //Make 10
            case 48109: //Make all (27)
                client.send(new RemoveInterfaces());
                if(client.getLevel(CRAFTING) < 32) {
                    client.send(new SendMessage("You need level 32 crafting to craft a fishbowl."));
                    break;
                }
                int[] craftFishAmount = new int[]{27, 10, 5, 1};
                client.setSkill(CRAFTING.getId(), 6667,  1, 1775, -1, 160, 884, 3);
                client.skillActionCount = craftFishAmount[actionButton - 48109];
                client.skillActionTimer = client.playerSkillAction.get(7);
                break;
            case 48116: //Make one unpowered orb
            case 48115: //Make 5
            case 48114: //Make 10
            case 48113: //Make all (27)
                client.send(new RemoveInterfaces());
                if(client.getLevel(CRAFTING) < 48) {
                    client.send(new SendMessage("You need level 48 crafting to craft a unpowered orb."));
                    break;
                }
                int[] craftOrbAmount = new int[]{27, 10, 5, 1};
                client.setSkill(CRAFTING.getId(), 567,  1, 1775, -1, 240, 884, 3);
                client.skillActionCount = craftOrbAmount[actionButton - 48113];
                client.skillActionTimer = client.playerSkillAction.get(7);
                break;
            case 34170:
                client.fletching.fletchBow(client, true, 1);
                break;
            case 34169:
                client.fletching.fletchBow(client, true, 5);
                break;
            case 34168:
                client.fletching.fletchBow(client, true, 10);
                break;
            case 34167:
                client.fletching.fletchBow(client, true, 27);
                break;
            case 34174: // 1
                client.fletching.fletchBow(client, false, 1);
                break;
            case 34173: // 5
                client.fletching.fletchBow(client, false, 5);
                break;
            case 34172: // 10
                client.fletching.fletchBow(client, false, 10);
                break;
            case 34171:
                client.fletching.fletchBow(client, false, 27);
                break;
            case 10252:
            case 11000:
            case 10253:
            case 11001:
            case 10254:
            case 10255:
            case 11002:
            case 11011:
            case 11013:
            case 11014:
            case 11010:
            case 11012:
            case 11006:
            case 11009:
            case 11008:
            case 11004:
            case 11003:
            case 11005:
            case 47002:
            case 54090:
            case 11007:
                if (client.genie) {
                    int[] skillTrain = {
                            10252, 11000, 10253, 11001, 10254, 11002, 10255, 11011,
                            11013, 11014, 11010, 11012, 11006, 11009, 11008, 11004,
                            11003, 11005, 47002, 54090, 11007
                    };
                    client.send(new RemoveInterfaces());
                    client.genie = false;
                    if (client.isBusy() || client.checkBankInterface || !client.playerHasItem(2528)) //To prevent stuff!
                        break;
                    for (int i = 0; i < skillTrain.length; i++) {
                        Skill trainedSkill = Skill.getSkill(i);
                        if (trainedSkill != null && skillTrain[i] == client.actionButtonId) {
                            if (client.actionButtonId != 54090) {
                                client.deleteItem(2528, 1);
                                client.checkItemUpdate();
                                int level = Skills.getLevelForExperience(client.getExperience(trainedSkill));
                                int experience = 100 * level;
                                client.giveExperience(experience, trainedSkill);
                                client.send(new SendMessage("You rub the lamp and gained " + experience + " experience in " + trainedSkill.getName() + "."));
                            } else
                                client.send(new SendMessage("Experience for " + trainedSkill.getName() + " is disabled until 10th of July!"));
                        }
                    }
                } else if (client.antique) {
                    int[] skillTrain = {
                            10252, 11000, 10253, 11001, 10254, 11002, 10255, 11011,
                            11013, 11014, 11010, 11012, 11006, 11009, 11008, 11004,
                            11003, 11005, 47002, 54090, 11007
                    };
                    client.send(new RemoveInterfaces());
                    client.antique = false;
                    if (client.inDuel || client.duelFight || client.IsBanking || client.checkBankInterface || !client.playerHasItem(6543)) //To prevent stuff!
                        break;
                    for (int i = 0; i < skillTrain.length; i++) {
                        Skill trainedSkill = Skill.getSkill(i);
                        if (trainedSkill != null && skillTrain[i] == client.actionButtonId) {
                                client.deleteItem(6543, 1);
                                client.checkItemUpdate();
                                int level = Skills.getLevelForExperience(client.getExperience(trainedSkill));
                                int experience = 250 * level;
                                client.giveExperience(experience, trainedSkill);
                                client.send(new SendMessage("You rub the lamp and gained " + experience + " experience in " + trainedSkill.getName() + "."));
                        }
                    }
                } else if (client.randomed && client.actionButtonId == client.statId[client.random_skill]) {
                    client.randomed = false;
                    client.resetTabs();
                    client.send(new RemoveInterfaces());
                    if (!client.addItem(2528, 1)) {
                        Ground.addFloorItem(client, 2528, 1);
                        client.send(new SendMessage("You dropped the lamp on the floor!"));
                    } else client.checkItemUpdate();
                }
                break;
            case 4130: //Autocast on normal spellbook
            break;
            case 1097:
            case 1094:
            case 1093:
                client.autocast_spellIndex = -1; //Reset autocast!
                client.resetAttack(); //Reset attack when picking new autocast?
                client.setSidebarInterface(0, 1689);
                break;
            case 51133:
            case 51185:
            case 51091:
            case 24018:
            case 51159:
            case 51211:
            case 51111:
            case 51069:
            case 51146:
            case 51198:
            case 51102:
            case 51058:
            case 51172:
            case 51224:
            case 51122:
            case 51080:
                for (int index = 0; index < client.ancientButton.length && client.autocast_spellIndex == -1; index++) {
                    if (client.actionButtonId == client.ancientButton[index])
                        client.autocast_spellIndex = index;
                }
                //client.setSidebarInterface(0, 328);
                CombatStyleHandler.setWeaponHandler(client); //We need this apperently!
                break;
            case 24017:
                CombatStyleHandler.setWeaponHandler(client);
                break;

            case 2171: // Retribution
                break;

            case 14067: //Apperance accepted!
                client.send(new RemoveInterfaces());
                client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
                break;

            case 152:
                client.buttonOnRun = false;
                break;
            case 153:
                client.buttonOnRun = true;
                break;

            case 130: // close interface
                client.println_debug("Closing Interface");
                break;

            case 3014: //Unhandled weapon buttons?
            case 3017:
            case 3016:
                // fightType = fightStyle.POUND;
            break;

            case 1177: // Gmaul!
            case 1080: // bash (staff)
            case 14218: //?
            case 22228: // punch (unarmed)
            case 48010: // flick (whip)
            case 21200: // spike (pickaxe)
            case 6221: // accurate (shortbow)
            case 6236: // accurate (long bow)
            case 17102: // accurate (darts)
            case 8234: // stab (dagger)
            case 30088: // Chop claws
            case 18103: // Chop 2h
            case 9125: // Chop longsword & Scimitar
            case 6168: // chop (axe)
                client.weaponStyle = actionButton == 1177 || actionButton == 1080 || actionButton == 14218 ? Player.fightStyle.POUND :
                actionButton == 22228 ? Player.fightStyle.PUNCH : actionButton == 48010 ? Player.fightStyle.FLICK : actionButton == 21200 ? Player.fightStyle.SPIKE :
                actionButton == 6221 || actionButton == 6236 || actionButton == 17102 ? Player.fightStyle.ACCURATE :
                actionButton == 8234 ? Player.fightStyle.STAB : Player.fightStyle.CHOP;
                client.fightType = 0;
                CombatStyleHandler.setWeaponHandler(client);
                if(actionButton == 1080 && client.autocast_spellIndex != -1) {
                    client.resetAttack(); //Swapping from magic to melee so stop combat!
                    client.autocast_spellIndex = -1; //Reset due to change of combat style
                }
                break;

            case 1175: // Gmaul!
            case 22229: // block (unarmed)
            case 1078: // focus - block (staff)
            case 3015: // ??
            case 33019: // fend (hally)
            case 6169: // block (axe)
            case 8235: // block (dagger)
            case 9126: // Defensive
            case 18078: // block (spear)
            case 21201: // block (pickaxe)
            case 48008: // deflect (whip)
            case 14219:
            case 6219: // longrange (shortbow)
            case 6234: // longrange (long bow)
            case 17100: // longrange (darts)
                client.weaponStyle = actionButton == 1175 || actionButton == 22229 ? Player.fightStyle.BLOCK_THREE :
                actionButton == 33019 ? Player.fightStyle.FEND : actionButton == 48008 ? Player.fightStyle.DEFLECT :
                actionButton == 6219 || actionButton == 6234 || actionButton == 17100 ? Player.fightStyle.LONGRANGE : Player.fightStyle.BLOCK;
                client.fightType = 1; //Defensive xp!
                CombatStyleHandler.setWeaponHandler(client);
                if(actionButton == 1078 && client.autocast_spellIndex != -1) {
                    client.resetAttack(); //Swapping from magic to melee so stop combat!
                    client.autocast_spellIndex = -1; //Reset due to change of combat style
                }
                break;

            case 14220: //Mace spike!
            case 33018: // jab (hally)
            case 48009: // lash (whip)
            case 9127: // Controlled
            case 18077: // lunge (spear)
            case 18080: // swipe (spear)
            case 18079: // pound (spear)
                client.weaponStyle = actionButton == 14220 ? Player.fightStyle.SPIKE : actionButton == 33018 ? Player.fightStyle.JAB :
                actionButton == 18077 ? Player.fightStyle.LUNGE : actionButton == 18079 ? Player.fightStyle.POUND_CON : actionButton == 18080 ? Player.fightStyle.SWIPE :
                actionButton == 9127 ? Player.fightStyle.CONTROLLED : Player.fightStyle.LASH;
                client.fightType = 3;
                CombatStyleHandler.setWeaponHandler(client);
            break;

            case 1079: // pound (staff)
            case 1176: // Gmaul!
            case 14221: //Mace pummel!
            case 18106: // slash 2h
            case 30091: // Slash claws
            case 22230: // kick (unarmed)
            case 21203: // impale (pickaxe)
            case 21202: // smash (pickaxe)
            case 18105: //Smash 2h
            case 9128: // Aggressive
            case 6170: // smash (axe)
            case 6171: // hack (axe)
            case 33020: // swipe (hally)
            case 6220: // Rapid (shortbow)
            case 6235: // rapid (long bow)
            case 17101: // repid (darts)
            case 8237: // lunge (dagger)
            case 8236: // slash (dagger)
                client.weaponStyle = actionButton == 1079 || actionButton == 1176 || actionButton == 14221 ? Player.fightStyle.PUMMEL :
                actionButton == 9128 || actionButton == 18106 || actionButton == 30091 || actionButton == 8236 ? Player.fightStyle.SLASH :
                actionButton == 22230 ? Player.fightStyle.KICK : actionButton == 21203 ? Player.fightStyle.IMPALE :
                actionButton == 6170 || actionButton == 21202 || actionButton == 18105 ? Player.fightStyle.SMASH :
                actionButton == 6171 ? Player.fightStyle.HACK : actionButton == 33020 ? Player.fightStyle.SWIPE :
                actionButton == 6220 || actionButton == 6235 || actionButton == 17101 ? Player.fightStyle.RAPID : Player.fightStyle.LUNGE_STR;
                client.fightType = 2;
                CombatStyleHandler.setWeaponHandler(client);
                if(actionButton == 1079 && client.autocast_spellIndex != -1) {
                    client.resetAttack(); //Swapping from magic to melee so stop combat!
                    client.autocast_spellIndex = -1; //Reset due to change of combat style
                }
                break;

            case 9154: // Log out
                if (System.currentTimeMillis() < client.walkBlock && !client.UsingAgility) {
                    client.send(new SendMessage("You are unable to logout right now."));
                    break;
                }
                if (client.isInCombat()) {
                    int seconds = (int)(client.getLastCombat() * 0.6);
                    client.send(new SendMessage("You must wait "+(seconds + 1)+" seconds before you can logout!"));
                    break;
                }
                if (System.currentTimeMillis() - client.lastPlayerCombat <= 30000 && client.inWildy()) {
                    client.send(new SendMessage("You must wait 30 seconds after combat in the wilderness to logout."));
                    client.send(new SendMessage("If you X out or disconnect you will stay online for up to a minute"));
                    break;
                }
                // if(currentHealth > 0)
                client.logout();
                break;

            case 21011:
                if(client.IsBanking) {
                    client.takeAsNote = !client.takeAsNote;
                    client.send(new SendString(client.takeAsNote ? "No Note" : "Note", 5389));
                    client.send(new SendMessage(client.takeAsNote ? "You can now note items." : "You can no longer note items."));
                }
                break;
            case 21010:
                if(client.IsBanking) {
                    if (client.freeSlots() < 28) {
                        for (int i = 0; i < 28; i++)
                            if (client.playerItems[i] > 0)
                                client.bankItem(client.playerItems[i] - 1, i, client.playerItemsN[i]);
                        client.send(new SendMessage("You bank all your items!"));
                        client.checkItemUpdate();
                    } else
                        client.send(new SendMessage("You do not have anything that can be banked!"));
                }
                break;

            case 13092:
                try {
                    Client other = client.getClient(client.trade_reqId);
                    if (other == null || !client.validClient(client.trade_reqId) || System.currentTimeMillis() - client.lastButton < 600 || !client.inTrade) {
                        break;
                    }
                    client.lastButton = System.currentTimeMillis();
                    if (client.inTrade && !client.tradeConfirmed) {
                        client.tradeConfirmed = true;
                        if (other.tradeConfirmed) {
                            if (other.hasTradeSpace() || client.hasTradeSpace()) {
                                client.send(new SendMessage(client.failer));
                                other.send(new SendMessage(client.failer));
                                client.declineTrade();
                                return;
                            }
                            client.confirmScreen();
                            other.confirmScreen();
                            break;
                        }
                        client.send(new SendString("Waiting for other player...", 3431));
                        if (client.validClient(client.trade_reqId)) {
                            other.send(new SendString("Other player has accepted", 3431));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Trade button issue! " + e);
                }
                break;

            case 13218:
                try {
                    Client other = client.getClient(client.trade_reqId);
                    if (other == null || !client.validClient(client.trade_reqId) || System.currentTimeMillis() - client.lastButton < 600 || !client.inTrade) {
                        break;
                    }
                    client.lastButton = System.currentTimeMillis();
                    if (client.inTrade && client.tradeConfirmed && other.tradeConfirmed && !client.tradeConfirmed2) {
                        client.tradeConfirmed2 = true;
                        if (other.tradeConfirmed2) {
                            client.giveItems();
                            other.giveItems();
                            break;
                        }
                        other.send(new SendString("Other player has accepted.", 3535));
                        client.send(new SendString("Waiting for other player...", 3535));
                    }
                } catch (Exception e) {
                    System.out.println("Trade button issue! " + e);
                }
                break;

            case 9157:
                if(client.discord) { //Yes
                    client.send(new RemoveInterfaces());
                    Player.openPage(client, "https://discord.gg/WZP5mByJ8e");
                    client.discord = false;
                }
                client.triggerChat(1);
                if (client.NpcDialogue == 2) {
                    client.NpcDialogue = 0;
                    client.NpcDialogueSend = false;
                    client.openUpBank();
                } else if (client.NpcDialogue == 4) { // Aubury
                    client.NpcDialogue = 0;
                    client.NpcDialogueSend = false;
                    client.openUpShop(2);
                } else if (client.NpcDialogue == 1001) { // Aubury
                    client.getOutputStream().createFrame(27);
                } else if (client.NpcDialogue == 22) { // Makeover Mage
                    client.NpcDialogue = 23;
                    client.NpcDialogueSend = false;
                } else if (client.NpcDialogue == 27) {
                    client.yellOn = true;
                    client.send(new SendMessage("You have enabled boss yell messages."));
                    client.send(new RemoveInterfaces());
                    client.NpcDialogue = 0;
                    client.NpcDialogueSend = false;
                } else if (client.NpcDialogue == 9) { // mage arena
                    if (client.determineCombatLevel() >= 80) {
                        client.moveTo(3105, 3933, 0);
                        client.send(new RemoveInterfaces());
                    } else {
                        client.send(new SendMessage("You need to be level 80 or above to enter the mage arena."));
                        client.send(new SendMessage("The skeletons at the varrock castle are a good place until then."));
                    }
                }
                break;

            case 9158:
                if(client.discord) { //No
                    client.send(new RemoveInterfaces());
                    client.discord = false;
                }
                client.triggerChat(2);
                if (client.NpcDialogue == 2) {
                    client.NpcDialogue = 0;
                    client.NpcDialogueSend = false;
                } else if (client.NpcDialogue == 4) {
                    client.NpcDialogue = 5;
                    client.NpcDialogueSend = false;
                } else if (client.NpcDialogue == 22) { // Makeover Mage
                    client.NpcDialogue = 24;
                    client.NpcDialogueSend = false;
                } else if (client.NpcDialogue == 1001) { // dice
                    client.setInterfaceWalkable(-1);
                    client.send(new RemoveInterfaces());
                } else if (client.NpcDialogue == 27) {
                    client.yellOn = false;
                    client.send(new SendMessage("You have disabled boss yell messages."));
                    client.send(new RemoveInterfaces());
                    client.NpcDialogue = 0;
                    client.NpcDialogueSend = false;
                }
                break;

            case 9167:
            case 9178:
            case 9190:
                client.triggerChat(1);
                break;
            case 9168:
            case 9179:
            case 9191:
                client.triggerChat(2);
                break;
            case 9169:
            case 9180:
            case 9192:
                client.triggerChat(3);
                break;
            case 9181:
            case 9193:
                client.triggerChat(4);
                break;
            case 9194:
                client.triggerChat(5);
                break;

            case 7212:
                client.setSidebarInterface(0, 328);
                break;
            case 26018:
                if (!client.inDuel || !client.validClient(client.duel_with)) {
                    break;
                }
                Client o = client.getClient(client.duel_with);
                boolean sendMsgToOther = client.getMaxHealth() - client.getCurrentHealth() == 0 && o.getMaxHealth() - o.getCurrentHealth() != 0;
                if (o.getMaxHealth() - o.getCurrentHealth() != 0 || client.getMaxHealth() - client.getCurrentHealth() != 0) {
                    client.send(new SendMessage(sendMsgToOther ? "Your opponent is low on health!" : "You are low on health, so please heal up!"));
                    if(sendMsgToOther)
                        o.send(new SendMessage("You are low on health, so please heal up!"));
                    break;
                }
                if (System.currentTimeMillis() - client.lastButton < 1000) {
                    client.lastButton = System.currentTimeMillis();
                    break;
                } else {
                    client.lastButton = System.currentTimeMillis();
                }
                if (client.duelConfirmed) {
                    break;
                }
                client.duelConfirmed = true;
                client.canOffer = false;
                if (o.duelConfirmed) {
                    /*
                     * Danno: Fix; stop a duel with all combat styles disabled.
                     */
                    if (client.duelRule[0] && client.duelRule[1] && client.duelRule[2]) {
                        client.declineDuel();
                        client.send(new SendMessage("At least one combat style must be enabled!"));
                        o.send(new SendMessage("At least one combat style must be enabled!"));
                        return;
                    }
                    if (client.hasEnoughSpace() || o.hasEnoughSpace()) {
                        client.send(new SendMessage(client.failer));
                        o.send(new SendMessage(client.failer));
                        client.declineDuel();
                        return;
                    }
                    client.canOffer = false;
                    o.canOffer = false;
                    client.confirmDuel();
                    o.confirmDuel();
                } else {
                    client.send(new SendString("Waiting for other player...", 6684));
                    o.send(new SendString("Other player has accepted.", 6684));
                }

                break;

            case 33206:
            case 94167:
                    client.showSkillMenu(ATTACK.getId(), 0);
                break;
            case 33207:
            case 94168:
                    client.showSkillMenu(HITPOINTS.getId(), 0);
                break;
            case 33208:
            case 94169:
                    client.showSkillMenu(MINING.getId(), 0);
                break;
            case 33209:
            case 94170:
                    client.showSkillMenu(STRENGTH.getId(), 0);
                break;
            case 33210:
            case 94171:
                    client.showSkillMenu(AGILITY.getId(), 0);
                break;
            case 33212:
            case 94173:
                    client.showSkillMenu(DEFENCE.getId(), 0);
                break;
            case 33215:
            case 94176:
                    client.showSkillMenu(RANGED.getId(), 0);
                break;
            //case 33213:
            case 94179:
                client.showSkillMenu(PRAYER.getId(), 0);
            break;
            case 33216:
            case 94177:
                    client.showSkillMenu(THIEVING.getId(), 0);
                break;
            case 33213:
            case 94174:
                    client.showSkillMenu(HERBLORE.getId(), 0);
                break;
            case 33219: //Crafting
            case 94180:
                    client.showSkillMenu(CRAFTING.getId(), 0);
                break;
            case 33211: //Smithing
            case 94172:
                    client.showSkillMenu(SMITHING.getId(), 0);
                break;
            case 33220:
            case 94184:
                    client.showSkillMenu(WOODCUTTING.getId(), 0);
                break;
            case 33221:
            case 94182:
                    client.showSkillMenu(MAGIC.getId(), 0);
                break;
            case 33222:
            case 94181:
                    client.showSkillMenu(FIREMAKING.getId(), 0);
                break;
            case 33223:
            case 94178:
                    client.showSkillMenu(COOKING.getId(), 0);
                break;
            case 33224:
            case 95053:
                    client.showSkillMenu(RUNECRAFTING.getId(), 0);
                break;
            case 33214:
            case 94183:
                    client.showSkillMenu(FLETCHING.getId(), 0);
                break;
            case 33217:
            case 94175:
                    client.showSkillMenu(FISHING.getId(), 0);
                break;
            case 34142:
                    if (client.currentSkill < 2)
                        client.showSkillMenu(ATTACK.getId(), 0);
                    else
                        client.showSkillMenu(client.currentSkill, 0);
                break;
            case 34119:
                    if (client.currentSkill < 2)
                        client.showSkillMenu(DEFENCE.getId(), 0);
                    else
                        client.showSkillMenu(client.currentSkill, 1);
                break;
            case 34120:
                if (client.currentSkill < 2)
                        client.showSkillMenu(RANGED.getId(), 0);
                else
                        client.showSkillMenu(client.currentSkill, 2);
                break;
            case 34123:
                if (client.currentSkill < 2)
                    client.send(new SendMessage("Coming soon!"));
                else
                        client.showSkillMenu(client.currentSkill, 3);
                break;
            case 34133:
                    client.showSkillMenu(client.currentSkill, 4);
                break;
            case 34136:
                    client.showSkillMenu(client.currentSkill, 5);
                break;
            case 34139:
                    client.showSkillMenu(client.currentSkill, 6);
                break;
            case 34155:
                    client.showSkillMenu(client.currentSkill, 7);
                break;
            case 47130:
            case 95061:
                    client.showSkillMenu(SLAYER.getId(), 0);
                break;
            case 95068: //Farming
                    client.showSkillMenu(FARMING.getId(), 0);
                break;
            case 88060: //Idea
                client.requestAnim(4276, 0);
                client.gfx0(712);
                break;
            case 88061: //Stomp
                client.requestAnim(4278, 0);
                client.gfx0(713);
                break;
            case 88062: //Flap
                client.requestAnim(4280, 0);
                break;
            case 88063: //Slap head
                client.requestAnim(4275, 0);
                break;
            case 59062: //Scared
                client.requestAnim(2836, 0);
                break;
            case 72254: //Bunny hop
                client.requestAnim(6111, 0);
                break;
            case 72033: //Zombie dance
                client.requestAnim(3543, 0);
                break;
            case 72032: //Zombie walk
                client.requestAnim(3544, 0);
            break;
            case 74108:
                Skillcape skillcape = Skillcape.getSkillCape(client.getEquipment()[Equipment.Slot.CAPE.getId()]);
                if (skillcape != null) {
                    client.requestAnim(skillcape.getEmote(), 0);
                    client.gfx0(skillcape.getGfx());
                } else if (client.getEquipment()[Equipment.Slot.CAPE.getId()] == 9813) { //Questpoint cape
                    client.requestAnim(4945, 0);
                    client.gfx0(816);
                } else if (client.GetItemName(client.getEquipment()[Equipment.Slot.CAPE.getId()]).toLowerCase().contains("max cape")) { //Max cape
                    skillcape = Skillcape.getRandomCape();
                    client.requestAnim(skillcape.getEmote(), 0);
                    client.gfx0(skillcape.getGfx());
                } else {
                    client.send(new SendMessage("You need to be wearing a skillcape to do that!"));
                }
                break;
            case 83097:
                client.questPage = client.questPage == 0 ? 1 : 0;
            break;
            case 23132: //Morph shiet!
            if(client.morph) {
                client.unMorph();
            }
            break;
            default:
                // System.out.println("Player stands in: X="+absX+" Y="+absY);
                if (client.playerRights > 1) {
                    client.println_debug("Case 185: Action Button: " + client.actionButtonId);
                }
                break;
        }
    }

}
