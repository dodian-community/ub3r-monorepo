package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.prayer.Prayer;
import net.dodian.utilities.DbTables;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class ClickItem implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        client.getInputStream().readSignedWordBigEndianA();
        int itemSlot = client.getInputStream().readUnsignedWordA();
        int itemId = client.getInputStream().readUnsignedWordBigEndian();
        if (client.fillEssencePouch(itemId)) {
            return;
        }
        if (itemId == 5733) {
            try {
                if (client.getPlayerNpc() < 1) {
                    client.send(new SendMessage("please try to do ::pnpc id"));
                    return;
                }
                if (Server.npcManager.getData(client.getPlayerNpc()) == null) {
                    Server.npcManager.reloadNpcConfig(client, client.getPlayerNpc(), "New Npc", "-1");
                    return;
                }
                Connection conn = getDbConnection();
                Statement statement = conn.createStatement();
                ResultSet rs = statement.executeQuery("SELECT 1 FROM " + DbTables.GAME_NPC_SPAWNS + " where id='" + client.getPlayerNpc() + "' && x='" + client.getPosition().getX() + "' && y='" + client.getPosition().getY() + "' && height='" + client.getPosition().getZ() + "'");
                if (rs.next()) {
                    client.send(new SendMessage("You already got a spawn on this position!"));
                    return;
                }
                int health = Server.npcManager.getData(client.getPlayerNpc()).getHP();
                statement
                        .executeUpdate("INSERT INTO " + DbTables.GAME_NPC_SPAWNS + " SET id = " + client.getPlayerNpc() + ", x=" + client.getPosition().getX()
                                + ", y=" + client.getPosition().getY() + ", height=" + client.getPosition().getZ() + ", hitpoints="
                                + health + ", live=1, face=0, rx=0,ry=0,rx2=0,ry2=0,movechance=0");
                statement.close();
                Server.npcManager.createNpc(client.getPlayerNpc(), new Position(client.getPosition().getX(), client.getPosition().getY(), client.getPosition().getZ()), 0);
                client.send(new SendMessage("Npc added = " + client.getPlayerNpc() + ", at x = " + client.getPosition().getX()
                        + " y = " + client.getPosition().getY() + "."));
            } catch (Exception e) {
                System.out.println("Potato sql error " + e);
            }
            return;
        }
        if (itemId == 2528) {
            client.openGenie();
            return;
        }
        if (itemId == 6543) {
            client.openAntique();
            return;
        }
        if((itemId >= 199 && itemId <= 219) || itemId == 3049 || itemId == 3051)
            clickItem(client, itemSlot, itemId);
        else if (System.currentTimeMillis() - client.lastAction > 100) { //Due to how system handles time need this for 1 tick delay! Perhaps better way to do it?
            clickItem(client, itemSlot, itemId);
            client.lastAction = System.currentTimeMillis();
        }
    }

    public void clickItem(Client client, int slot, int id) {
        int item = client.playerItems[slot] - 1;
        if (item != id) {
            return; // might prevent stuff
        }
        if (client.duelRule[7] && client.inDuel && client.duelFight) {
            client.send(new SendMessage("Food has been disabled for this duel"));
            return;
        }
        boolean used = true;
        int nextId = -1;
        if (client.inDuel || client.duelFight || client.duelConfirmed || client.duelConfirmed2) {
            client.send(new SendMessage("RewardItem cannot be used in a duel!"));
            return;
        }
        if (Prayer.buryBones(client, item, slot)) {
            return;
        }
        if (client.playerHasItem(item)) {
            switch (item) {
                case 1856:
                    used = false;
                    client.guideBook();
                break;
                case 199:
                case 203:
                case 207:
                case 209:
                case 213:
                case 215:
                case 217:
                case 219:
                case 3049: //Toadflax
                case 3051: //Snapdragon
                    for (int i = 0; i < Utils.grimy_herbs.length && used; i++) {
                        if(Utils.grimy_herbs[i] == item) {
                            used = false;
                            if (Skills.getLevelForExperience(client.getExperience(Skill.HERBLORE)) < Utils.grimy_herbs_lvl[i]) {
                                client.send(new SendMessage("You need level " + Utils.grimy_herbs_lvl[i] + " herblore to clean this herb."));
                            } else {
                                client.giveExperience(Utils.grimy_herbs_xp[i], Skill.HERBLORE);
                                client.deleteItem(item, slot, 1);
                                client.addItemSlot(item == 3051 || item == 3049 ? item - 51 : item + 50, 1, slot);
                                client.send(new SendMessage("You clean the "+client.GetItemName(item)+"."));
                            }
                        }
                    }
                break;
                case 315: //Shrimp
                case 2142: //Meat
                    client.eat(3, item, slot);
                    used = false;
                break;
                case 2309: //Bread
                    client.eat(5, item, slot);
                    used = false;
                break;
                case 3369: //Thin Snail
                    client.eat(7, item, slot);
                    used = false;
                break;
                case 333: //Trout
                    client.eat(8, item, slot);
                    used = false;
                break;
                case 329: //Salmon
                    client.eat(10, item, slot);
                    used = false;
                break;
                case 379: //Lobster
                    client.eat(12, item, slot);
                    used = false;
                break;
                case 373: //Swordfish
                    client.eat(14, item, slot);
                    used = false;
                break;
                case 7946: //Monkfish
                    client.eat(16, item, slot);
                    used = false;
                break;
                case 385: //Shark
                    client.eat(20, item, slot);
                    used = false;
                break;
                case 397: //Sea turtle
                    client.eat(22, item, slot);
                    used = false;
                break;
                case 391: //Manta ray
                    client.eat(24, item, slot);
                    used = false;
                break;
                case 1959: //Pumpkin
                case 1961: //Easter egg
                    client.eat(2, item, slot);
                    used = false;
                break;
                case 121: // regular attack potion
                case 123:
                case 125:
                case 2428:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(3 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.ATTACK)) * 0.1), Skill.ATTACK);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the attack potion." : "You drink the attack potion."));
                    break;
                case 113:
                case 115: // regular str
                case 117:
                case 119:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(3 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.STRENGTH)) * 0.1), Skill.STRENGTH);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the strength potion." : "You drink the strength potion."));
                    break;
                case 2432:
                case 133: // regular def
                case 135:
                case 137:
                    if (client.deathStage > 0 || client.deathTimer > 0 ||client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(3 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.DEFENCE)) * 0.1), Skill.DEFENCE);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the defense potion." : "You drink the defense potion."));
                    break;
                case 2436:
                case 145:
                case 147:
                case 149:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(5 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.ATTACK)) * 0.15), Skill.ATTACK);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the super attack potion." : "You drink the super attack potion."));
                    break;
                case 2440:
                case 157:
                case 159:
                case 161:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(5 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.STRENGTH)) * 0.15), Skill.STRENGTH);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the super strength potion." : "You drink the super strength potion."));
                    break;
                case 2442:
                case 163:
                case 165:
                case 167:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(5 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.DEFENCE)) * 0.15), Skill.DEFENCE);
                    client.refreshSkill(Skill.DEFENCE);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the super defense potion." : "You drink the super defense potion."));
                    break;
                case 2444: //4 dose
                case 169://ranging potion
                case 171:
                case 173:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(4 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.RANGED)) * 0.12), Skill.RANGED);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the ranging potion." : "You drink the ranging potion."));
                    break;
                case 139://prayer potion
                case 141:
                case 143:
                case 2434: //4dose
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.pray(8 + (int)(client.getMaxPrayer() * 0.25));
                    client.refreshSkill(Skill.PRAYER);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the prayer potion." : "You drink the prayer potion."));
                    break;
                case 3026://Super restore potion
                case 3028:
                case 3030:
                case 3024: //4dose
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.pray(10 + (int)(client.getMaxPrayer() * 0.28));
                    client.refreshSkill(Skill.PRAYER);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the restore potion." : "You drink the restore potion."));
                    break;
                case 12695: //Super combat potion
                case 12697:
                case 12699:
                case 12701:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    for(int skill = 0; skill < 3; skill++)
                        client.boost(5 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.ATTACK)) * 0.15), Skill.getSkill(skill));
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the super combat potion." : "You drink the super combat potion."));
                    break;
                case 11730: //Overload
                case 11731:
                case 11732:
                case 11733:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel || client.getCurrentHealth() < 11) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.dealDamage(null, 10, Entity.hitType.CRIT);
                    for(int skill = 0; skill < 4; skill++) {
                        skill = skill == 3 ? 4 : skill;
                        client.boost(5 + (int) (Skills.getLevelForExperience(client.getExperience(Objects.requireNonNull(Skill.getSkill(skill)))) * 0.15), Skill.getSkill(skill));
                    }
                    int ticks = (1 + Skills.getLevelForExperience(client.getExperience(Skill.HERBLORE))) * 2;
                    client.addEffectTime(2, 200 + ticks); //200 ticks = 120 seconds = 2 minutes!, max ticks = (99 + 1) * 2 = 200 aka 2 minute for a total of 4 minutes.
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the overload potion." : "You drink the overload potion."));
                break;
                case 2452:
                case 2454:
                case 2456:
                case 2458:
                    if((client.effects.size() > 1 && client.effects.get(1) > 50) || client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.addEffectTime(1, 500); //500 ticks = 300 seconds = 5 minutes!
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the anti-fire potion." : "You drink the anti-fire potion."));
                break;
                case 4155:
                    if (client.inTrade || client.inDuel)
                        break;
                    client.NpcDialogue = 15;
                    client.NpcDialogueSend = false;
                    client.nextDiag = -1;
                    used = false;
                    break;
                case 11877:
                    client.deleteItem(11877, slot, 1);
                    if(!client.playerHasItem(230))
                        client.addItemSlot(230,100, slot);
                    else
                        client.addItem(230, 100);
                    used = false;
                    break;
                case 11879:
                    client.deleteItem(11879, slot, 1);
                    if(!client.playerHasItem(228))
                        client.addItemSlot(228,100, slot);
                    else
                        client.addItem(228, 100);
                    used = false;
                    break;
                case 12859:
                    client.deleteItem(12859, slot,1);
                    if(!client.playerHasItem(222))
                        client.addItemSlot(222,100, slot);
                    else
                        client.addItem(222, 100);
                    used = false;
                    break;
//      case 3062:
//          if (client.freeSlots() < 1) {
//              client.send(new SendMessage("Not enough space in your inventory!"));
//              return;
//            }
//          if (client.hasVoted()) {
//        	  return;        	  
//          }
//    	  double roll = Math.random() * 100;
//    	  if (roll < 0.3) {
//    		  int[] items = { 3481, 3483, 3486, 3488, 2633, 2635, 2637 };
//    		  int r = (int) (Math.random() * items.length);
//    		  client.send(new SendMessage("You have recieved a " + client.GetItemName(items[r]) + "!"));
//    		  client.addItem(items[r], 1);
//    		  client.yell("[Server] - " + client.getPlayerName() + " has just received a " + client.GetItemName(items[r])
//    		   + " from voting!");
//    	  } else {
//    		  client.send(new SendMessage("You get 50,000 coins from voting!"));
//    		  client.addItem(995, 50000);
//    	  }
//    	  break;
                case 6199: //Something here!
                    int[] idss = {6856, 6857, 6859, 6861, 6860, 6858};
                    int rs = Utils.random(idss.length - 1);
                    client.deleteItem(6199, 1);
                    client.addItem(idss[rs], 1);
                    client.send(new SendMessage("Thank you for waiting patiently on us, take this as a token of gratitude!"));
                    used = false;
                    break;
                case 12854:
                    used = false;
                    int[] xPresents = {6542, 11996, 13345, 13346};
                    int slotNeeded = 3;
                    /* Check so we got enough slots! */
                    for (int i = 0; i < xPresents.length && slotNeeded > 0; i++) //check if we got the items or not!
                        if (client.playerHasItem(xPresents[i]))
                            slotNeeded--;
                    if (client.freeSlots() < slotNeeded) {
                        client.send(new SendMessage("You need atleast " + slotNeeded + " free slot to open this!"));
                        break;
                    }
                    /* Delete item and add stuff! */
                    client.deleteItem(item, 1);
                    for (int xPresent : xPresents)
                        client.addItem(xPresent, 3 + Misc.random(6));
                    break;
                case 6542:
                case 11996:
                case 13345:
                case 13346:
                    used = false;
                    if (client.freeSlots() < 1) {
                        client.send(new SendMessage("You need atleast one free slot to open this!"));
                        break;
                    }
                    int[] randomEventItem = {12887, 12888, 12889, 12890, 12891, 13343, 13344, 13203};
                    client.deleteItem(item, 1);
                    client.addItem(11997, 55 + Misc.random(500));
                    int chance = Misc.chance(1000);
                    if (chance == 1) {
                        int eventItemId = randomEventItem[Misc.random(randomEventItem.length - 1)];
                        client.addItem(eventItemId, 1);
                        client.send(new SendMessage("You found something of interest!"));
                        client.yell(client.getPlayerName() + " just found " + client.GetItemName(eventItemId).toLowerCase() + " in a " + client.GetItemName(item).toLowerCase() + "!");
                    }
                    break;
                case 11918:
                    used = false;
                    if (client.freeSlots() < 1) {
                        client.send(new SendMessage("You need atleast one free slot to open this!"));
                        break;
                    }
                    int[] halloweenMasks = {1053, 1055, 1057};
                    client.deleteItem(item, 1);
                    int itemId = halloweenMasks[Misc.random(halloweenMasks.length - 1)];
                    client.addItem(itemId, 1);
                    client.send(new SendMessage("You found a " + client.GetItemName(itemId).toLowerCase() + "!"));
                    client.yell(client.getPlayerName() + " just found " + client.GetItemName(itemId).toLowerCase() + " in a " + client.GetItemName(item).toLowerCase() + "!");
                    break;
                default:
                    // client.send(new SendMessage("Nothing interesting happens"));
                    used = false;
                    break;
            }
        }
        if (used) {
            client.deleteItem(item, slot, 1);
        }
        if (nextId > 0) {
            client.addItemSlot(nextId, 1, slot);
        }
        client.checkItemUpdate();
    }

}
