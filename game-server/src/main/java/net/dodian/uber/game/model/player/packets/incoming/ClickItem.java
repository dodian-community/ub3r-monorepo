package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.herblore.Herblore;
import net.dodian.uber.game.model.player.skills.prayer.Prayer;
import net.dodian.utilities.Database;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

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
                Connection conn = Database.conn;
                Statement statement = conn.createStatement();
                ResultSet rs = statement.executeQuery("SELECT 1 FROM uber3_spawn where id='" + client.getPlayerNpc() + "' && x='" + client.getPosition().getX() + "' && y='" + client.getPosition().getY() + "' && height='" + client.getPosition().getZ() + "'");
                if (rs.next()) {
                    client.send(new SendMessage("You already got a spawn on this position!"));
                    return;
                }
                int health = Server.npcManager.getData(client.getPlayerNpc()).getHP();
                statement
                        .executeUpdate("INSERT INTO uber3_spawn SET id = " + client.getPlayerNpc() + ", x=" + client.getPosition().getX()
                                + ", y=" + client.getPosition().getY() + ", height=" + client.getPosition().getZ() + ", hitpoints="
                                + health + ", live=1, face=0, rx=0,ry=0,rx2=0,ry2=0,movechance=0");
                statement.close();
                Server.npcManager.createNpc(client.getPlayerNpc(), new Position(client.getPosition().getX(), client.getPosition().getY(), client.getPosition().getZ()), 0);
                client.send(new SendMessage("Npc added = " + client.getPlayerNpc() + ", at x = " + client.getPosition().getX()
                        + " y = " + client.getPosition().getY() + ""));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        if (itemId == 2528) {
            client.openGenie();
            return;
        }
        if (System.currentTimeMillis() - client.lastAction >= 600) {
            clickItem(client, itemSlot, itemId);
            client.lastAction = System.currentTimeMillis();
            // client.actionTimer = 10;
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
        if (Herblore.cleanHerb(client, item, slot)) {
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
                    for (int i = 0; i < Utils.grimy_herbs.length; i++) {
                        if (client.getLevel(Skill.HERBLORE) < Utils.grimy_herbs_lvl[i]) {
                            client
                                    .send(new SendMessage("You need level " + Utils.grimy_herbs_lvl[i] + " herblore to clean this herb."));
                            return;
                        }
                        client.addExperience(Utils.grimy_herbs_xp[i] * 5, Skill.HERBLORE);
                    }
                    break;
                case 315:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(0x33D, 0);
                    client.animationReset = System.currentTimeMillis() + 800;
                    client.setCurrentHealth(client.getCurrentHealth() + 3);
                    if (client.getCurrentHealth() > client.getLevel(Skill.HITPOINTS)) {
                        client.setCurrentHealth(client.getLevel(Skill.HITPOINTS));
                    }
                    client.send(new SendMessage("You eat the shrimps"));
                    client.refreshSkill(Skill.HITPOINTS);
                    break;
                case 333:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(0x33D, 0);
                    client.animationReset = System.currentTimeMillis() + 800;
                    client.setCurrentHealth(client.getCurrentHealth() + 5);
                    if (client.getCurrentHealth() > client.getLevel(Skill.HITPOINTS)) {
                        client.setCurrentHealth(client.getLevel(Skill.HITPOINTS));
                    }
                    client.send(new SendMessage("You eat the trout"));
                    client.refreshSkill(Skill.HITPOINTS);
                    break;
                case 329:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(0x33D, 0);
                    client.animationReset = System.currentTimeMillis() + 800;
                    client.setCurrentHealth(client.getCurrentHealth() + 7);
                    if (client.getCurrentHealth() > client.getLevel(Skill.HITPOINTS)) {
                        client.setCurrentHealth(client.getLevel(Skill.HITPOINTS));
                    }
                    client.send(new SendMessage("You eat the salmon"));
                    client.refreshSkill(Skill.HITPOINTS);
                    break;
                case 379:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(0x33D, 0);
                    client.animationReset = System.currentTimeMillis() + 800;
                    client.setCurrentHealth(client.getCurrentHealth() + 12);
                    if (client.getCurrentHealth() > client.getLevel(Skill.HITPOINTS)) {
                        client.setCurrentHealth(client.getLevel(Skill.HITPOINTS));
                    }
                    client.send(new SendMessage("You eat the lobster"));
                    client.refreshSkill(Skill.HITPOINTS);
                    break;
                case 373:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(0x33D, 0);
                    client.animationReset = System.currentTimeMillis() + 800;
                    client.setCurrentHealth(client.getCurrentHealth() + 14);
                    if (client.getCurrentHealth() > client.getLevel(Skill.HITPOINTS)) {
                        client.setCurrentHealth(client.getLevel(Skill.HITPOINTS));
                    }
                    client.send(new SendMessage("You eat the swordfish"));
                    client.refreshSkill(Skill.HITPOINTS);
                    break;
                case 7946:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(0x33D, 0);
                    client.animationReset = System.currentTimeMillis() + 800;
                    client.setCurrentHealth(client.getCurrentHealth() + 16);
                    if (client.getCurrentHealth() > client.getLevel(Skill.HITPOINTS)) {
                        client.setCurrentHealth(client.getLevel(Skill.HITPOINTS));
                    }
                    client.send(new SendMessage("You eat the monkfish"));
                    client.refreshSkill(Skill.HITPOINTS);
                    break;
                case 385:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(0x33D, 0);
                    client.animationReset = System.currentTimeMillis() + 800;
                    client.setCurrentHealth(client.getCurrentHealth() + 20);
                    if (client.getCurrentHealth() > client.getLevel(Skill.HITPOINTS)) {
                        client.setCurrentHealth(client.getLevel(Skill.HITPOINTS));
                    }
                    client.send(new SendMessage("You eat the shark"));
                    client.refreshSkill(Skill.HITPOINTS);
                    break;
                case 397:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(0x33D, 0);
                    client.animationReset = System.currentTimeMillis() + 800;
                    client.setCurrentHealth(client.getCurrentHealth() + 22);
                    if (client.getCurrentHealth() > client.getLevel(Skill.HITPOINTS)) {
                        client.setCurrentHealth(client.getLevel(Skill.HITPOINTS));
                    }
                    client.send(new SendMessage("You eat the sea turtle"));
                    client.refreshSkill(Skill.HITPOINTS);
                    break;
                case 391:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(0x33D, 0);
                    client.animationReset = System.currentTimeMillis() + 800;
                    client.setCurrentHealth(client.getCurrentHealth() + 24);
                    if (client.getCurrentHealth() > client.getLevel(Skill.HITPOINTS)) {
                        client.setCurrentHealth(client.getLevel(Skill.HITPOINTS));
                    }
                    client.send(new SendMessage("You eat the manta ray"));
                    client.refreshSkill(Skill.HITPOINTS);
                    break;
                case 2309:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(0x33D, 0);
                    client.animationReset = System.currentTimeMillis() + 800;
                    client.setCurrentHealth(client.getCurrentHealth() + 5);
                    if (client.getCurrentHealth() > client.getLevel(Skill.HITPOINTS)) {
                        client.setCurrentHealth(client.getLevel(Skill.HITPOINTS));
                    }
                    client.send(new SendMessage("You eat the bread"));
                    client.refreshSkill(Skill.HITPOINTS);
                    break;
                case 1959:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(0x33D, 0);
                    client.animationReset = System.currentTimeMillis() + 800;
                    client.setCurrentHealth(client.getCurrentHealth() + 2);
                    if (client.getCurrentHealth() > client.getLevel(Skill.HITPOINTS)) {
                        client.setCurrentHealth(client.getLevel(Skill.HITPOINTS));
                    }
                    client.send(new SendMessage("You eat the pumpkin"));
                    client.refreshSkill(Skill.HITPOINTS);
                    break;
                case 1961:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(0x33D, 0);
                    client.animationReset = System.currentTimeMillis() + 800;
                    client.setCurrentHealth(client.getCurrentHealth() + 2);
                    if (client.getCurrentHealth() > client.getLevel(Skill.HITPOINTS)) {
                        client.setCurrentHealth(client.getLevel(Skill.HITPOINTS));
                    }
                    client.send(new SendMessage("You eat the easter egg"));
                    client.refreshSkill(Skill.HITPOINTS);
                    break;
                case 121: // regular attack potion
                case 123:
                case 125:
                case 2428:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.animationReset = System.currentTimeMillis() + 750;
                    client.send(new SendMessage("You drink the attack potion"));
                    client.attackPot = 12.5;
                    client.refreshSkill(Skill.ATTACK);
                    if (item < 125) {
                        nextId = item + 2;
                    } else if (item == 2428) {
                        nextId = 121;
                    } else {
                        nextId = 229;
                    }
                    break;
                case 757:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.send(new SendMessage("You drink the attack potion"));
                    client.attackPot = 30;
                    client.refreshSkill(Skill.ATTACK);
                    break;
                case 113:
                case 115: // regular str
                case 117:
                case 119:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.animationReset = System.currentTimeMillis() + 750;
                    client.send(new SendMessage("You drink the strength potion"));
                    client.strengthPot = 12.5;
                    client.refreshSkill(Skill.STRENGTH);
                    if (item < 119) {
                        nextId = item + 2;
                    } else {
                        nextId = 229;
                    }
                    client.CalculateMaxHit();
                    break;
                case 2432:
                case 133: // regular def
                case 135:
                case 137:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.animationReset = System.currentTimeMillis() + 750;
                    client.send(new SendMessage("You drink the defense potion"));
                    client.defensePot = 12.5;
                    client.refreshSkill(Skill.DEFENCE);
                    if (item < 137) {
                        nextId = item + 2;
                    } else if (item == 2432) {
                        nextId = 133;
                    } else {
                        nextId = 229;
                    }
                    break;
                case 2440:
                case 157:
                case 159:
                case 161:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.animationReset = System.currentTimeMillis() + 750;
                    client.send(new SendMessage("You drink the super strength potion"));
                    client.strengthPot = 20.0;
                    client.refreshSkill(Skill.STRENGTH);
                    if (item < 161) {
                        nextId = item + 2;
                    } else if (item == 2440) {
                        nextId = 157;
                    } else {
                        nextId = 229;
                    }
                    break;
                case 2436:
                case 145:
                case 147:
                case 149:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.animationReset = System.currentTimeMillis() + 750;
                    client.send(new SendMessage("You drink the super attack potion"));
                    client.attackPot = 20.0;
                    client.refreshSkill(Skill.ATTACK);
                    if (item < 149) {
                        nextId = item + 2;
                    } else if (item == 2436) {
                        nextId = 145;
                    } else {
                        nextId = 229;
                    }
                    break;
                case 169://ranging potion
                case 171:
                case 173:
                case 2444:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.animationReset = System.currentTimeMillis() + 750;
                    client.send(new SendMessage("You drink the ranging potion"));
                    client.rangePot = 20.0;
                    client.refreshSkill(Skill.RANGED);
                    if (item < 173) {
                        nextId = item + 2;
                    } else if (item == 2444) {
                        nextId = 169;
                    } else {
                        nextId = 229;
                    }
                    break;
                case 2442:
                case 163:
                case 165:
                case 167:
                    if (client.deathStage > 0 || client.deathTimer > 0) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.animationReset = System.currentTimeMillis() + 750;
                    client.send(new SendMessage("You drink the super defense potion"));
                    client.defensePot = 20.0;
                    client.refreshSkill(Skill.DEFENCE);
                    if (item < 167) {
                        nextId = item + 2;
                    } else if (item == 2442) {
                        nextId = 163;
                    } else {
                        nextId = 229;
                    }
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
                    client.deleteItem(11877, 1);
                    client.addItem(230, 100);
                    used = false;
                    break;
                case 11879:
                    client.deleteItem(11879, 1);
                    client.addItem(228, 100);
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
                    int rs = Utils.random(idss.length) - 1;
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
                    for (int i = 0; i < xPresents.length; i++)
                        client.addItem(xPresents[i], 3 + Misc.random(6));
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
    }

}
