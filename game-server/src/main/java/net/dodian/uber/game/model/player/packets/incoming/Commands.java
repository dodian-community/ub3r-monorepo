package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.ChatLine;
import net.dodian.uber.game.model.Login;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.npc.NpcData;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask;
import net.dodian.uber.game.model.player.packets.outgoing.*;
import net.dodian.uber.game.party.Balloons;
import net.dodian.uber.game.security.ChatLog;
import net.dodian.uber.game.security.CommandLog;
import net.dodian.utilities.DbTables;
import net.dodian.utilities.Misc;

import java.sql.Connection;
import java.sql.Statement;
import java.text.DecimalFormat;

import static net.dodian.uber.game.combat.ClientExtensionsKt.*;
import static net.dodian.utilities.DotEnvKt.getGameWorldId;
import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class Commands implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        String playerCommand = client.getInputStream().readString();
        if (!(playerCommand.indexOf("password") > 0) && !(playerCommand.indexOf("unstuck") > 0)) {
            if(!playerCommand.startsWith("examine"))
                client.println_debug("playerCommand: " + playerCommand);
        }
        if (client.validClient) {
            customCommand(client, playerCommand);
        } else {
            client.send(new SendMessage("Command ignored, please use another client"));
        }
    }

    public void customCommand(Client client, String command) {
        String[] cmd = command.split(" ");
        boolean specialRights = client.playerGroup == 6 || client.playerGroup == 10 || client.playerGroup == 35;
        try {
            if (specialRights) { //Special Rank Command
                if (cmd[0].equalsIgnoreCase("npca")) {
                    try {
                        int id = Integer.parseInt(cmd[1]);
                        Server.npcManager.getData(id).setAttackEmote(Integer.parseInt(cmd[2]));
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " npcid animationId"));
                    }
                }
                if (cmd[0].equalsIgnoreCase("loot")) {
                    client.instaLoot = !client.instaLoot;
                    client.send(new SendMessage("You turned insta loot " + (client.instaLoot ? "on" : "off") + "!"));
                }
                if (cmd[0].equalsIgnoreCase("death")) {
                    int type = Integer.parseInt(cmd[1]);
                    if(type > 0 && type <= Entity.hitType.values().length) {
                        System.out.println("wtf..." + client.getCurrentHealth());
                        client.dealDamage(null, client.getCurrentHealth(), Entity.hitType.values()[type - 1]);
                    } else client.send(new SendMessage("Only type 1 to " + Entity.hitType.values().length + " works!"));
                }
                if (cmd[0].equalsIgnoreCase("damage")) {
                    client.dealDamage(null, 20, Entity.hitType.CRIT);
                }
                if (cmd[0].equalsIgnoreCase("dharok")) {
                    client.dealDamage(null, client.getCurrentHealth() - 1, Entity.hitType.CRIT);
                }
                if (cmd[0].equalsIgnoreCase("heal")) {
                    int overHeal = (int)(client.getMaxHealth() * 0.15);
                    client.heal(client.getMaxHealth() + overHeal, overHeal);
                }
                if (cmd[0].equalsIgnoreCase("obja")) {
                    try { //497 = swing rope! -> 23132
                        int id = Integer.parseInt(cmd[1]);
                        int animation = Integer.parseInt(cmd[2]);
                        client.send(new ObjectAnimation(new GameObjectDef(id, 10, 2, new Position(client.getPosition().getX(), client.getPosition().getY() + 1, client.getPosition().getZ())), animation));
                        client.send(new SendMessage("Object "+id+" showing animation as " + animation));
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " objectId animationId"));
                    }
                }
                /*if(cmd[0].equalsIgnoreCase("input")) {
                    client.getOutputStream().createFrame(187);
                }*/
                if (cmd[0].equalsIgnoreCase("tobj")) {
                    try {
                        int id = Integer.parseInt(cmd[1]);
                        Position pos = client.getPosition().copy();
                        client.ReplaceObject(pos.getX(), pos.getY(), id, 0, 10);
                        client.send(new SendMessage("Object temporary spawned = " + id + ", at x = " + pos.getX()
                                + " y = " + pos.getY() + " with height " + pos.getZ()));
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " objectId"));
                    }
                }
                if (cmd[0].equalsIgnoreCase("varbit")) {
                    //173 = run config!
                    try {
                        int id = Integer.parseInt(cmd[1]);
                        int value = Integer.parseInt(cmd[2]);
                        client.varbit(id, value);
                        client.send(new SendMessage("You set varbit " + id + " with value " + value));
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " id value"));
                    }
                }
                if (cmd[0].equalsIgnoreCase("farm")) {
                    try {
                        boolean gotValue = true;
                        int value = Integer.parseInt(cmd[1]);
                        int config = 0;
                        switch(value) {
                            case 0: //Reset default value
                                client.varbit(529, config); //Patches
                                client.varbit(905, config); //Gout tuber patch!
                                client.varbit(1057, config); //Compost bin
                                break;
                            case 1: //Empty test for Trollheim
                                client.varbit(529, (3 + 2 + 295) | 2 << 6); //Herb patch for trollheim!
                                //client.varbit(529, (102 + 193) | 2 << 6); //Herb patch for trollheim!
                                //client.varbit(905, 3 + 2 + (1 << 6 | 1 << 7));
                                //client.varbit(905, 0); <- Gout tuber patch!
                                break;
                            case 2: //Compost bin
                                //client.varbit(1057, 30);
                                client.varbit(529,  (51 + 1 | 0 << 6 << 0 << 3) | (51 + 1 | 0 << 6 << 1 << 3)); //Allotment 1, Allotment 2, Flower, Herb
                                break;
                            case 3:
                                /* Herb + Flower + Allotment South  + Allotment North  */
                                int[] startConfig = {51, 5, 22, 3}; //Allotment, Allotment, flower, herb
                                int[] stage = {0, 1, 2, 2};
                                int[] patch = {0, 1, 2, 2};
                                for(int i = 0; i < startConfig.length; i++) {
                                    int check = patch[i] << 6;
                                    if(i == 3 && stage[i] > 1 && stage[i] < 5 && (patch[i] == 1 || patch[i] == 2)) {
                                        stage[i] = patch[i] == 2 ? stage[i] + 293 - (startConfig[i] - 3) : stage[i] + 290 - (startConfig[i] - 3);
                                        check = 2 << 6;
                                    } else if (i == 3) check = 0 << 6;
                                    config |= ((startConfig[i] + stage[i]) | check) << (i << 3);
                                }
                                client.varbit(529,  config);
                                break;
                            case 4: //Farm patch in tree gnome!
                                config = 0;
                                while(config < 2000) {
                                    try {
                                        client.send(new SendMessage("config = " + config));
                                        client.varbit(529,  config);
                                        config++;
                                        Thread.sleep(500);
                                    } catch(Exception e) {
                                        System.out.println("msg: " + e.getMessage());
                                    }
                                }
                                break;
                            case 5: //TODO test

                                break;
                            case 6: //TODO test2
                                break;
                            default: gotValue = false;
                        }
                        client.send(new SendMessage(gotValue ? "You set farming config to " + value : "Could not find a value!"));
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " patchId"));
                        System.out.println("send...." + e.toString());
                    }
                }
                if (cmd[0].equalsIgnoreCase("forcemove")) {
                    client.send(new SendMessage("force move!"));
                    client.appendForcemovement(client.getPosition(), new Position(3333, 3333), 10, 20, 3);
                }
                if (cmd[0].equalsIgnoreCase("plist")) {
                    System.out.println("test1..." + PlayerHandler.allOnline.toString());
                    System.out.println("test2..." + PlayerHandler.playersOnline.toString());
                }
                if (cmd[0].equalsIgnoreCase("goup")) {
                    int x = client.getPosition().getX(), y = client.getPosition().getY(), z = client.getPosition().getZ();
                    client.teleportTo(x, y, z + 1);
                    client.send(new SendMessage("You set your height to " + client.getPosition().getZ()));
                }
                if (cmd[0].equalsIgnoreCase("godown")) {
                    int x = client.getPosition().getX(), y = client.getPosition().getY(), z = client.getPosition().getZ();
                    client.teleportTo(x, y, Math.max(z - 1, 0));
                    client.send(new SendMessage("You set your height to " + client.getPosition().getZ()));
                }
                if (cmd[0].equalsIgnoreCase("tnpc") && getGameWorldId() > 1) {
                    try {
                        int id = Integer.parseInt(cmd[1]);
                        Position pos = client.getPosition().copy();
                        Server.npcManager.createNpc(id, pos, 0);
                        client.send(new SendMessage("Npc temporary spawned = " + id + ", at x = " + pos.getX()
                                + " y = " + pos.getY() + " with height " + pos.getZ()));
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " npcid"));
                    }
                }
                if (cmd[0].equalsIgnoreCase("immune")) {
                    client.immune = !client.immune;
                    client.send(new SendMessage("You set immune as " + client.immune));
                }
                if (cmd[0].equalsIgnoreCase("rehp")) {
                    client.reloadHp = !client.reloadHp;
                    client.send(new SendMessage("You set reload hp as " + client.reloadHp));
                }
                if (cmd[0].equalsIgnoreCase("face")) {
                    try {
                        int face = Integer.parseInt(cmd[1]);
                        Npc n = null;
                        for (Npc npc : Server.npcManager.getNpcs()) {
                            if (client.getPosition().equals(npc.getPosition())) n = npc;
                        }
                        if(n == null)
                            client.send(new SendMessage("Could not find a npc on this spot!"));
                        else {
                            int x = n.getPosition().getX(), y = n.getPosition().getY(), z = n.getPosition().getZ();
                            int faceCheck = n.getFace();
                            if(faceCheck != face) { //Update face on the sql aswell as ingame!
                                try {
                                    Statement stm = getDbConnection().createStatement();
                                    n.setFace(face);
                                    client.send(new SendMessage("You set the face of the npc from " + faceCheck + " to " + face + "!"));
                                    stm.executeUpdate("UPDATE uber3_spawn SET face='"+face+"' where x="+x+" && y="+y+" && height="+z);
                                    stm.close();
                                } catch (Exception e) {
                                    System.out.println("msg for face: " + e.getMessage());
                                }
                            } else
                                client.send(new SendMessage("'"+n.npcName()+"' is already facing the way you want it!"));
                        }
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " face"));
                    }
                }
                if (cmd[0].equalsIgnoreCase("split")) { //Magic armour split!
                    int chance = Integer.parseInt(cmd[1]);
                    double[] array = {0.14, 0.4, 0.3, 0.08, 0.08};
                    String[] parts = {"helm", "body", "legs", "feet", "boots"};
                    for(int i = 0; i < array.length; i++)
                        client.send(new SendMessage(chance+ " is started and of that " + parts[i] + " should be " + (int)(chance * array[i]) + " stats!"));
                }
                if ((cmd[0].equalsIgnoreCase("bank") || cmd[0].equalsIgnoreCase("b")) && client.playerRights > 1 && getGameWorldId() < 2) {
                    client.openUpBank();
                }
                if (cmd[0].equals("party")) {
                    Balloons.triggerPartyEvent(client);
                }
                if (cmd[0].equalsIgnoreCase("event")) {
                    Balloons.triggerBalloonEvent(client);
                }
                if (cmd[0].equals("gem")) {
                    /* Shilo village gem rock? */
                    double[] chance = new double[] {23.4, 11.7, 7.03, 3.91, 3.91, 3.12};
                    int[] gemId = new int[] {1625, 1627, 1629, 1623, 1621, 1619, 1617};
                    int rolledChance = 0, gem = -1, roll = Misc.chance(10000);
                    for (int i = 0; i < chance.length && gem == -1; i++) {
                        rolledChance += (int)(chance[i] * 100);
                        if (roll <= rolledChance) gem = gemId[i + 1];
                        else if (i + 1 == chance.length) gem = gemId[0];
                    }
                    client.send(new SendMessage("You found gem.." + client.GetItemName(gem) + "(" + gem + ")"));
                }
                if (cmd[0].equals("rune")) {
                    /* 1k cosmic rune convertion! */
                    int level = Integer.parseInt(cmd[1]);
                    int deleteOne = 0, deleteTwo = 0, deleteThree = 0, deleteFour = 0;
                    double[] value = {1.25, 2.5, 5};
                    for(int i = 0; i < 100; i++) {
                        if(Math.random() * 100 < (100D - (level / value[0])))
                            deleteFour++;
                        else if(Math.random() * 100 < (100D - (level / value[1])))
                            deleteThree++;
                        else if(Math.random() * 100 < (100D - (level / value[2])))
                            deleteTwo++;
                        else deleteOne++;
                    }
                    client.send(new SendMessage("At level "+level+" Runecraft, Four=" + deleteFour + ", Three=" + deleteThree + ", Two=" + deleteTwo + ", One=" + deleteOne + "."));
                }
                if (cmd[0].equals("boost_on")) {
                    client.boost(1337, Skill.STRENGTH);
                    client.boost(1337, Skill.DEFENCE);
                    client.boost(1337, Skill.ATTACK);
                    client.boost(1337, Skill.RANGED);
                    client.boost(1337, Skill.MAGIC);
                }
                if (cmd[0].equals("boost_off")) {
                    for(int i = 0; i < 7; i++)
                        if(i != 3 && i != 5) {
                            client.boostedLevel[i] = 0;
                            client.refreshSkill(Skill.getSkill(i));
                        }
                }
                if (command.startsWith("telemob")) {
                    int mobId = Integer.parseInt(cmd[1]);
                    for (Npc npc : Server.npcManager.getNpcs()) {
                        if(npc.getId() == mobId) {
                            client.triggerTele(npc.getPosition().getX(), npc.getPosition().getY(), npc.getPosition().getZ(),false);
                            return;
                        }
                    }
                }
                if (command.startsWith("findmob")) {
                    String npcName = command.substring(cmd[0].length() + 1).replaceAll("_", " ");
                    for (Npc npc : Server.npcManager.getNpcs()) {
                        String npcCheckName = npc.npcName().replaceAll("_", " ");
                        if(npcName.equalsIgnoreCase(npcCheckName)) {
                            client.send(new SendMessage("Found "+ npcCheckName +" ("+ npc.getId() +") at " + npc.getPosition().toString()));
                            return;
                        }
                    }
                }
                if (cmd[0].equals("rank")) {
                    try {
                        String rank = cmd[1];
                        int rankId = -1;
                        String name = command.substring(cmd[0].length() + cmd[1].length() + 2);
                        Client other = (Client) PlayerHandler.getPlayer(name);
                        switch (rank) {
                            case "normal":
                                rankId = 40;
                                break;
                            case "premium":
                                rankId = 11;
                                break;
                            case "mod":
                                rankId = 5;
                                break;
                            case "trial":
                                rankId = 9;
                                break;
                        }
                        if (rankId == -1) {
                            client.send(new SendMessage("Only available ranks: 'normal', 'premium', 'trial', 'mod'"));
                            return;
                        }
                        try {
                            Connection conn = getDbConnection();
                            Statement statement = conn.createStatement();
                            statement.executeUpdate("UPDATE " + DbTables.WEB_USERS_TABLE + " SET usergroupid='" + rankId + "' WHERE username ='" + name + "'");
                            statement.close();
                            if (other != null)
                                other.disconnected = true;
                            client.send(new SendMessage("You set " + name + " to a " + rank + "!"));
                        } catch (Exception e) {
                            client.send(new SendMessage("Sql issue! Contact a admin!"));
                        }
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " rank playername"));
                    }
                }
                if (cmd[0].equalsIgnoreCase("if")) {
                    int id = Integer.parseInt(cmd[1]);
                    client.showInterface(id);
                    client.send(new SendMessage("You open interface " + id));
                }
                if (cmd[0].equalsIgnoreCase("travel")) {
                    client.setTravelMenu();
                }
                if (cmd[0].equals("combat")) {
                    try {
                        int level = Integer.parseInt(cmd[1]);
                        client.customCombat = level > 255 || level < 0 ? -1 : level;
                        if(client.customCombat == -1)
                            client.send(new SendMessage("You reset back to normal combat!"));
                        else
                            client.send(new SendMessage("You set your own combat to level: " + level));
                        client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " combat"));
                    }
                }
                if (cmd[0].equals("remitem")) {
                    try {
                        int id = Integer.parseInt(cmd[1]);
                        int amt = Integer.parseInt(cmd[2]);
                        String user = command.substring(cmd[0].length() + cmd[1].length() + cmd[2].length() + 3);
                        client.removeItemsFromPlayer(user, id, amt);
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " id amount playername"));
                    }
                }
                if (cmd[0].equals("remskill")) {
                    try {
                        int id = cmd[1].matches(".*\\d.*") ? Integer.parseInt(cmd[1]) : client.getSkillId(cmd[1]);
                        if (id < 0 || id > 20) {
                            client.send(new SendMessage("Skills are between 0 - 20!"));
                            return;
                        }
                        int xp = Integer.parseInt(cmd[2]) < 1 ? 0 : Integer.parseInt(cmd[2]);
                        String user = command.substring(cmd[0].length() + cmd[1].length() + cmd[2].length() + 3);
                        client.removeExperienceFromPlayer(user, id, xp);
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " id xp(0 - " + Integer.MAX_VALUE + " playername"));
                    }
                }
                if (cmd[0].equals("pouch")) {
                    if (client.freeSlots() >= 4) {
                        client.addItem(5509, 1);
                        for (int i = 0; i < 3; i++)
                            client.addItem(5510 + (i * 2), 1);
                        client.checkItemUpdate();
                    } else
                        client.send(new SendMessage("Need 4 free slots!"));
                }
                if (cmd[0].equalsIgnoreCase("emote")) {
                    int id = Integer.parseInt(cmd[1]);
                    client.requestAnim(id, 0);
                    client.send(new SendMessage("You set animation to: " + id));
                }
                if (cmd[0].equalsIgnoreCase("heat")) {
                    client.UsingAgility = !client.UsingAgility;
                    client.walkBlock = System.currentTimeMillis() + (600 * 30); //30 ticks!
                    client.send(new SendMessage("You set agility to: " + client.UsingAgility));
                }
                if (cmd[0].equalsIgnoreCase("gfx")) {
                    int id = Integer.parseInt(cmd[1]);
                    client.animation(id, client.getPosition());
                    client.send(new SendMessage("You set gfx to: " + id));
                }
                if (command.startsWith("update") && command.length() > 7) {
                    Server.updateSeconds = (Integer.parseInt(command.substring(7)) + 1);
                    Server.updateRunning = true;
                    Server.updateStartTime = System.currentTimeMillis();
                    Server.trading = false;
                    Server.dueling = false;
                }
                if (cmd[0].equalsIgnoreCase("head")) {
                    int icon = Integer.parseInt(cmd[1]);
                    client.headIcon = icon;
                    client.send(new SendMessage("Head : " + icon));
                    client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
                }
                if (cmd[0].equalsIgnoreCase("skull") && client.playerRights > 1) {
                    int icon = Integer.parseInt(cmd[1]);
                    client.skullIcon = icon;
                    client.send(new SendMessage("Skull : " + icon));
                    client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
                }
                if (cmd[0].equalsIgnoreCase("sound") && client.playerRights > 1) {
                    int id = Integer.parseInt(cmd[1]);
                    client.send(new Sound(id));
                    client.send(new SendMessage("Sound playing..." + id));
                }
                if (cmd[0].equalsIgnoreCase("resetTask")) {
                    try {
                        String otherPName = command.substring(cmd[0].length() + 1);
                        int otherPIndex = PlayerHandler.getPlayerID(otherPName);

                        if (otherPIndex != -1) {
                            Client p = (Client) PlayerHandler.players[otherPIndex];
                            p.getSlayerData().set(3, 0);
                            p.send(new SendMessage(client.getPlayerName() + " have reset your task!"));
                            client.send(new SendMessage("You reset the task for " + p.getPlayerName() + "!"));
                        } else
                            client.send(new SendMessage("Player " + otherPName + " is not online!"));
                    } catch (Exception e) {
                        client.send(new SendMessage("Try entering a name you want to tele to.."));
                    }
                }
                if (cmd[0].equalsIgnoreCase("r_drops")) {
                    try {
                        int id = client.getPlayerNpc() < 1 ? Integer.parseInt(cmd[1]) : client.getPlayerNpc();
                        Server.npcManager.reloadDrops(client, id);
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " id"));
                    }
                }
                if (cmd[0].equalsIgnoreCase("d_drop")) {
                    if (client.getPlayerNpc() < 1) {
                        client.send(new SendMessage("please try to do ::pnpc id"));
                        return;
                    }
                    int itemid = Integer.parseInt(cmd[1]);
                    double chance = Double.parseDouble(cmd[2]);
                    try {
                        Connection conn = getDbConnection();
                        Statement statement = conn.createStatement();
                        String sql = "delete FROM " + DbTables.GAME_NPC_DROPS + " where npcid=" + client.getPlayerNpc() + " && itemid=" + itemid
                                + " && percent=" + chance;
                        if (statement.executeUpdate(sql) < 1)
                            client.send(new SendMessage(Server.npcManager.getName(client.getPlayerNpc())
                                    + " does not have the " + client.GetItemName(itemid) + " with the chance " + chance + "% !"));
                        else
                            client.send(new SendMessage("You deleted " + client.GetItemName(itemid) + " drop with the chance of " + chance + "% from "
                                    + Server.npcManager.getName(client.getPlayerNpc())));
                        statement.executeUpdate(sql);
                        statement.close();
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " itemid chance"));
                    }
                }
                if (cmd[0].equalsIgnoreCase("npc_data")) {
                    if (client.getPlayerNpc() < 1) {
                        client.send(new SendMessage("please try to do ::pnpc id"));
                        return;
                    }
                    try {
                        String data = cmd[1];
                        String value = command.substring(cmd[0].length() + cmd[1].length() + 2);
                        Server.npcManager.reloadNpcConfig(client, client.getPlayerNpc(), data, value);
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " config value"));
                    }
                }
                if (cmd[0].equalsIgnoreCase("a_drop")) {
                    if (client.getPlayerNpc() < 1) {
                        client.send(new SendMessage("please try to do ::pnpc id"));
                        return;
                    }
                    try {
                        int itemid = Integer.parseInt(cmd[1]);
                        int min = Integer.parseInt(cmd[2]);
                        int max = Integer.parseInt(cmd[3]);
                        DecimalFormat numberFormat = new DecimalFormat("###.###");
                        double first = !cmd[4].contains(":") ? 0.0 : Double.parseDouble(cmd[4].split(":")[0]);
                        double second = !cmd[4].contains(":") ? 0.0 : Double.parseDouble(cmd[4].split(":")[1]);
                        double chance = first != 0.0 || second != 0.0 ? Double.parseDouble(numberFormat.format((first / second) * 100)) : Double.parseDouble(cmd[4]);
                        chance = chance > 100.000 ? 100.0 : Math.max(chance, 0.001);
                        String rareShout = cmd.length >= 6 && (cmd[5].equalsIgnoreCase("false") || cmd[5].equalsIgnoreCase("true")) ? cmd[5].toLowerCase() : "false";
                        try {
                            Connection conn = getDbConnection();
                            Statement statement = conn.createStatement();
                            String sql = "INSERT INTO " + DbTables.GAME_NPC_DROPS + " SET npcid='" + client.getPlayerNpc() + "', percent='" + chance
                                    + "', itemid='" + itemid + "', amt_min='" + min + "', amt_max='" + max + "', rareShout='" + rareShout + "'";
                            statement.execute(sql);
                            client.send(new SendMessage("You added " + min + "-" + max + " " + client.GetItemName(itemid) + " to "
                                    + Server.npcManager.getName(client.getPlayerNpc()) + " with a chance of " + chance + "%" + (rareShout.equals("true") ? " with a yell!" : "")));
                            statement.close();
                        } catch (Exception e) {
                            if (e.getMessage().contains("Duplicate entry"))
                                client.send(new SendMessage(client.GetItemName(itemid) + " with the chance of " + chance + "% already exist for the " + Server.npcManager.getName(client.getPlayerNpc())));
                            else {
                                client.send(new SendMessage("Something bad happend with sql!"));
                                System.out.println("sql error: " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " itemid min max procent(x:y or %)"));
                    }
                }
                if (specialRights && (cmd[0].equalsIgnoreCase("bank") || cmd[0].equalsIgnoreCase("b"))) {
                    client.openUpBank();
                }
                if (cmd[0].equalsIgnoreCase("droptable")) {
                    if (client.getPlayerNpc() < 1) {
                        client.send(new SendMessage("please try to do ::pnpc id"));
                        return;
                    }
                    int npcId = client.getPlayerNpc();
                    NpcData npcData = Server.npcManager.getData(npcId);
                    if (!npcData.getDrops().isEmpty()) {
                        client.send(new SendMessage("-----------DROPS FOR "
                                + Server.npcManager.getName(client.getPlayerNpc()).toUpperCase() + "-----------"));
                        for (int i = 0; i < npcData.getDrops().size(); i++) {
                            int min = npcData.getDrops().get(i).getMinAmount();
                            int max = npcData.getDrops().get(i).getMaxAmount();
                            int itemId = npcData.getDrops().get(i).getId();
                            double chance = npcData.getDrops().get(i).getChance();
                            client.send(new SendMessage(
                                    min + " - " + max + " " + client.GetItemName(itemId) + "(" + itemId + ") " + chance + "%"));
                        }
                    } else
                        client.send(new SendMessage("Npc " + npcData.getName() + " (" + npcId + ") has no assigned drops!"));
                }
                if (cmd[0].equalsIgnoreCase("addxmastree")) {
                    try {
                        Connection conn = getDbConnection();
                        Statement statement = conn.createStatement();
                        statement
                                .executeUpdate("INSERT INTO " + DbTables.GAME_OBJECT_DEFINITIONS + " SET id = 1318, x = " + client.getPosition().getX()
                                        + ", y = " + client.getPosition().getY() + ", type = 2");
                        statement.close();
                        //Server.objects.add(new RS2Object(1318, client.getPosition().getX(), client.getPosition().getY(), 10));
                        client.send(new SendMessage("Object added, at x = " + client.getPosition().getX()
                                + " y = " + client.getPosition().getY()));
                    } catch (Exception e) {
                        System.out.println("something wrong with xmas tree: " + e.getMessage());
                    }
                }
                if (cmd[0].equalsIgnoreCase("addnpc")) {
                    try {
                        if (client.getPlayerNpc() < 1) {
                            client.send(new SendMessage("please try to do ::pnpc id"));
                            return;
                        }
                        if (Server.npcManager.getData(client.getPlayerNpc()) == null) {
                            client.send(new SendMessage("Does not exist in the database!"));
                            return;
                        }
                        Connection conn = getDbConnection();
                        Statement statement = conn.createStatement();
                        int health = Server.npcManager.getData(client.getPlayerNpc()).getHP();
                        statement
                                .executeUpdate("INSERT INTO " + DbTables.GAME_NPC_SPAWNS + " SET id = " + client.getPlayerNpc() + ", x=" + client.getPosition().getX()
                                        + ", y=" + client.getPosition().getY() + ", height=" + client.getPosition().getZ() + ", hitpoints="
                                        + health + ", live=1, face=0, rx=0,ry=0,rx2=0,ry2=0,movechance=0");
                        statement.close();
                        Server.npcManager.createNpc(client.getPlayerNpc(), new Position(client.getPosition().getX(), client.getPosition().getY(), client.getPosition().getZ()), 0);
                        client.send(new SendMessage("Npc added = " + client.getPlayerNpc() + ", at x = " + client.getPosition().getX()
                                + " y = " + client.getPosition().getY()));
                    } catch (Exception e) {
                        System.out.println("something wrong with adding npc: " + e.getMessage());
                    }
                }
                if (cmd[0].equalsIgnoreCase("reloaditems")) {
                    Server.itemManager.reloadItems();
                    client.send(new SendMessage("You reloaded all items!"));
                }
                if (cmd[0].equalsIgnoreCase("setlevel")) {
                    int skill = Integer.parseInt(cmd[1]);
                    int level = Integer.parseInt(cmd[2]);
                    if (level > 99 || level < 1) {
                        return;
                    }
                    client.setExperience(Skills.getXPForLevel(level), Skill.getSkill(skill));
                    client.setLevel(level, Skill.getSkill(skill));
                    client.refreshSkill(Skill.getSkill(skill));
                    if(skill == 3) { //refresh hp + prayer from this skill!
                        client.maxHealth = level;
                        client.heal(client.maxHealth);
                    } else if (skill == 5) {
                        client.maxPrayer = level;
                        client.setCurrentPrayer(client.maxPrayer);
                        client.drainPrayer(0);
                    }
                }
                if (cmd[0].equalsIgnoreCase("setxp")) {
                    int skill = Integer.parseInt(cmd[1]);
                    int xp = Integer.parseInt(cmd[2]);
                    if (xp + client.getExperience(Skill.getSkill(skill)) > 200000000 || xp < 1) {
                        return;
                    }
                    client.giveExperience(xp, Skill.getSkill(skill));
                    client.refreshSkill(Skill.getSkill(skill));
                }
                if (command.equalsIgnoreCase("reset") && client.playerRights > 1) {
                    Skill.enabledSkills().forEach(skill -> {
                        client.setExperience(skill == Skill.HITPOINTS ? 1155 : 0, skill);
                        client.setLevel(skill == Skill.HITPOINTS ? 10 : 1, skill);
                        client.refreshSkill(skill);
                    });
                }
                if (command.startsWith("master") && client.playerRights > 1) {
                    Skill.enabledSkills().forEach(skill -> client.giveExperience(14_000_000, skill));
                }
            } //End of Special rank commands
            if (client.playerRights > 0) {
                if (cmd[0].equalsIgnoreCase("pnpc") && specialRights) {
                    try {
                        int npcId = Integer.parseInt(cmd[1]);
                        if (npcId <= 8195) {
                            client.isNpc = npcId >= 0;
                            client.setPlayerNpc(npcId >= 0 ? npcId : -1);
                            client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
                        }
                        client.send(new SendMessage(npcId > 8195 ? "Maximum 8195 in npc id!" : npcId >= 0 ? "Setting npc to " + client.getPlayerNpc() : "Setting you normal!"));
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " npcid"));
                    }
                }
                if (cmd[0].equalsIgnoreCase("invis")) {
                    client.invis = !client.invis;
                    client.send(new SendMessage("You turn invis to " + client.invis));
                    client.transport(client.getPosition()); //Transport to the realm of invisibility or leave it! hehe :D
                    CommandLog.recordCommand(client, command);
                }
                if (cmd[0].equalsIgnoreCase("teleto")) {
                    try {
                        if (client.wildyLevel > 0 && !specialRights) {
                            client.send(new SendMessage("Command can't be used in the wilderness!"));
                            return;
                        }
                        String otherPName = command.substring(cmd[0].length() + 1);
                        int otherPIndex = PlayerHandler.getPlayerID(otherPName);

                        if (otherPIndex != -1) {
                            Client p = (Client) PlayerHandler.players[otherPIndex];
                            if (p.wildyLevel > 0 && !specialRights) {
                                client.send(new SendMessage("That player is in the wilderness!"));
                                return;
                            }
                            if (client.UsingAgility || p.UsingAgility || System.currentTimeMillis() < client.walkBlock) { //Agility course + potential travel!
                                return;
                            }
                            client.transport(p.getPosition().copy());
                            client.send(new SendMessage("Teleto: You teleport to " + p.getPlayerName()));
                            CommandLog.recordCommand(client, command);
                        } else {
                            client.send(new SendMessage("Player " + otherPName + " is not online!"));
                        }
                    } catch (Exception e) {
                        client.send(new SendMessage("Try entering a name you want to tele to.."));
                    }
                }
                if (cmd[0].equalsIgnoreCase("kick") && client.playerRights > 0) {
                    try {
                        String otherPName = command.substring(cmd[0].length() + 1);
                        int otherPIndex = PlayerHandler.getPlayerID(otherPName);
                        if (otherPIndex != -1) {
                            Client p = (Client) PlayerHandler.players[otherPIndex];
                            p.disconnected = true;
                            client.send(new SendMessage("Player " + p.getPlayerName() + " has been kicked!"));
                            CommandLog.recordCommand(client, command);
                        } else client.send(new SendMessage("Player " + otherPName + " is not online!"));
                    } catch (Exception e) {
                        client.send(new SendMessage("Try entering a name you wish to kick.."));
                        client.send(new SendMessage(e.getMessage()));
                    }
                }
                if (cmd[0].equalsIgnoreCase("teletome") && client.playerRights > 0) {
                    try {
                        if (client.wildyLevel > 0 && !specialRights) {
                            client.send(new SendMessage("Command can't be used in the wilderness"));
                            return;
                        }
                        String otherPName = command.substring(cmd[0].length() + 1);
                        int otherPIndex = PlayerHandler.getPlayerID(otherPName);
                        if (otherPIndex != -1) {
                            Client p = (Client) PlayerHandler.players[otherPIndex];
                            if (p.wildyLevel > 0 && !specialRights) {
                                client.send(new SendMessage("Can not teleport someone out of the wilderness! Contact a admin!"));
                                return;
                            }
                            if (client.UsingAgility || p.UsingAgility || System.currentTimeMillis() < client.walkBlock) { //Agility course + potential travel!
                                return;
                            }
                            p.transport(client.getPosition().copy());
                            CommandLog.recordCommand(client, command);
                        } else {
                            client.send(new SendMessage("Player " + otherPName + " is not online!"));
                        }
                    } catch (Exception e) {
                        client.send(new SendMessage("Try entering a name you want to tele to you.."));
                    }
                }
                if (cmd[0].equalsIgnoreCase("quest") && client.playerRights > 1) {
                    try {
                        int id = cmd[1].matches(".*\\d.*") ? Integer.parseInt(cmd[1]) : 0;
                        int amount = cmd.length > 2 && cmd[2].matches(".*\\d.*") ? Integer.parseInt(cmd[2]) : 1;
                        if(amount == 1)
                            client.send(new SendMessage("quests = " + ++client.quests[id]));
                        else {
                            client.quests[id] = amount;
                            client.send(new SendMessage("quests = " + client.quests[id]));
                        }
                    } catch (Exception e) {
                        client.send(new SendMessage("wrong usage! ::quest id amount or ::quest id"));
                        System.out.println(e.getMessage());
                    }
                }
                if (cmd[0].equalsIgnoreCase("quest_reward") && client.playerRights > 1) {
                    client.send(new SendString("Quest name here", 12144));
                    client.send(new SendString(" 99", 12147));
                    for(int i = 0; i < 6; i++)
                        client.send(new SendString(i + "", 12150 + i));
                    client.sendFrame246(12145, 250, 4151);
                    client.showInterface(12140);
                    client.stillgfx(199, client.getPosition().getY(), client.getPosition().getX());
                }
                if (cmd[0].equalsIgnoreCase("moooo") && client.playerRights > 1) {
                    client.clearQuestInterface();
                    client.send(new SendString("@str@testing something@str@", 8147));
                    client.send(new SendString("Yes", 8148));
                    client.send(new SendString("@369@Tits1@369@", 8149));
                    client.send(new SendString("@mon@Tits3@mon@", 8150));
                    client.send(new SendString("@lre@Tits3@lre@", 8151));
                    client.sendQuestSomething(8143);
                    client.showInterface(8134);
                }
                if (cmd[0].equalsIgnoreCase("staffzone")) {
                    if(client.inWildy()) {
                        client.send(new SendMessage("Cant use this in the wilderness!"));
                        return;
                    }
                    client.teleportTo(2936, 4688, 0);
                    client.send(new SendMessage("Welcome to the staff zone!"));
                }
                if (cmd[0].equalsIgnoreCase("test_area")) {
                    client.triggerTele(3260, 2784, 0, false);
                    client.send(new SendMessage("Welcome to the monster test area!"));
                }
                if (cmd[0].equalsIgnoreCase("busy") && client.playerRights > 1) {
                    client.busy = !client.busy;
                    client.send(new SendMessage(!client.busy ? "You are no longer busy!" : "You are now busy!"));
                }
                if (cmd[0].equalsIgnoreCase("camera")) {
                    client
                            .send(new SendCamera("rotation", client.getPosition().getX(), client.getPosition().getY(), 100, 2, 2, ""));
                }
                if (cmd[0].equalsIgnoreCase("creset")) {
                    client.send(new CameraReset());
                }
                if (cmd[0].equalsIgnoreCase("slots")) {
                    if (client.playerRights < 2) {
                        client.send(new SendMessage("Do not fool with yaaaaar!"));
                        return;
                    }
                    client.send(new RemoveInterfaces());
                    client.showInterface(671);
                    Server.slots.playSlots(client, -1);
                }
                if(client.playerRights > 0) { //Toggle commands!
                    cmd[0] = cmd[0].startsWith("toggle") ? cmd[0].replace("_", "") : cmd[0];
                    if (cmd[0].equalsIgnoreCase("toggleyell")) {
                        Server.chatOn = !Server.chatOn;
                        client.yell(Server.chatOn ? "[SERVER]: Yell has been enabled!" : "[SERVER]: Yell has been disabled!");
                        CommandLog.recordCommand(client, command);
                    }
                    if (cmd[0].equalsIgnoreCase("togglepvp")) {
                        Server.pking = !Server.pking;
                        client.yell(Server.pking ? "[SERVER]: Player Killing has been enabled!" : "[SERVER]: Player Killing  has been disabled!");
                        CommandLog.recordCommand(client, command);
                    }
                    if (cmd[0].equalsIgnoreCase("toggletrade")) {
                        Server.trading = !Server.trading;
                        client.yell(Server.trading ? "[SERVER]: Trading has been enabled!" : "[SERVER]: Trading has been disabled!");
                        CommandLog.recordCommand(client, command);
                    }
                    if (cmd[0].equalsIgnoreCase("toggleduel")) {
                        Server.dueling = !Server.dueling;
                        client.yell(Server.dueling ? "[SERVER]: Dueling has been enabled!" : "[SERVER]: Dueling has been disabled!");
                        CommandLog.recordCommand(client, command);
                    }
                    if (cmd[0].equalsIgnoreCase("toggledrop")) {
                        Server.dropping = !Server.dropping;
                        client.yell(Server.dropping ? "[SERVER]: Dropping items has been enabled!" : "[SERVER]: Dropping items has been disabled!");
                        CommandLog.recordCommand(client, command);
                    }
                    if (cmd[0].equalsIgnoreCase("toggleshop")) {
                        Server.shopping = !Server.shopping;
                        client.yell(Server.shopping ? "[SERVER]: Shops has been enabled!" : "[SERVER]: Shops has been disabled!");
                        CommandLog.recordCommand(client, command);
                    }
                    if (cmd[0].equalsIgnoreCase("togglebank")) {
                        Server.banking = !Server.banking;
                        client.yell(Server.banking ? "[SERVER]: The Bank has been enabled!" : "[SERVER]: The Bank has been disabled!");
                        if (!Server.banking) {
                            for (Player p : PlayerHandler.players) {
                                if (p == null) continue;
                                Client c = (Client) p;
                                if (c.IsBanking) {
                                    c.send(new RemoveInterfaces());
                                    c.IsBanking = false;
                                }
                            }
                        }
                        CommandLog.recordCommand(client, command);
                    }
                }
                if (cmd[0].equalsIgnoreCase("checkbank") && client.playerRights > 0) {
                    String player = command.substring(cmd[0].length() + 1);
                    client.openUpOtherBank(player);
                    CommandLog.recordCommand(client, command);
                }
                if (cmd[0].equalsIgnoreCase("checkinv") && client.playerRights > 0) {
                    String player = command.substring(cmd[0].length() + 1);
                    client.openUpOtherInventory(player);
                    CommandLog.recordCommand(client, command);
                }
                if (command.startsWith("banmac") && client.playerRights > 0) {
                    try {
                        String otherPName = command.substring(7);
                        int otherPIndex = PlayerHandler.getPlayerID(otherPName);

                        if (otherPIndex != -1) {
                            Client p = (Client) PlayerHandler.players[otherPIndex];
                            Login.addUidToFile(p.UUID);
                            System.out.println("mac ban: " + p.UUID);
                            p.logout();
                            CommandLog.recordCommand(client, command);
                        } else {
                            client.send(new SendMessage("Error MAC banning player. Name doesn't exist or player is offline."));
                        }
                    } catch (Exception e) {
                        client.send(new SendMessage("Invalid Syntax! Use as ::banmac PlayerName"));
                    }
                }
            }
            /* Regular player commands */
            if (cmd[0].equalsIgnoreCase("request")) {
                Player.openPage(client, "https://dodian.net/forumdisplay.php?f=83");
            }
            if (cmd[0].equalsIgnoreCase("report")) {
                Player.openPage(client, "https://dodian.net/forumdisplay.php?f=118");
            }
            if (cmd[0].equalsIgnoreCase("suggest")) {
                Player.openPage(client, "https://dodian.net/forumdisplay.php?f=4");
            }
            if (cmd[0].equalsIgnoreCase("bug")) {
                Player.openPage(client, "https://dodian.net/forumdisplay.php?f=120");
            }
            if (cmd[0].equalsIgnoreCase("rules")) {
                Player.openPage(client, "https://dodian.net/index.php?pageid=rules");
            }
            if (cmd[0].equalsIgnoreCase("droplist") || (cmd[0].equalsIgnoreCase("drops") && client.playerRights < 2)) {
                Player.openPage(client, "https://dodian.net/index.php?pageid=droplist");
            }
            if (cmd[0].equalsIgnoreCase("latestclient")) {
                Player.openPage(client, "https://dodian.net/client/DodianClient.jar");
            }
            if (cmd[0].equalsIgnoreCase("news")) {
                Player.openPage(client, "https://dodian.net/showthread.php?t="+client.latestNews);
            }
            if (cmd[0].equalsIgnoreCase("thread")) {
                try {
                    int page = Integer.parseInt(cmd[1]);
                    Player.openPage(client, "https://dodian.net/showthread.php?t=" + page);
                } catch (Exception e) {
                    client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " page"));
                }
            }
            if (cmd[0].equalsIgnoreCase("highscores")) {
                try {
                    String firstPerson = cmd.length < 2 ? "" : cmd[1].replace("_", "+");
                    String secondPerson = cmd.length < 3 ? "" : cmd[2].replace("_", "+");
                    Player.openPage(client, firstPerson.isEmpty() && secondPerson.isEmpty() ? "https://dodian.net/index.php?pageid=highscores" : !firstPerson.isEmpty() && secondPerson.isEmpty() ? "https://dodian.net/index.php?pageid=highscores&player1=" + firstPerson : "https://dodian.net/index.php?pageid=highscores&player1=" + firstPerson + "&player2=" + secondPerson);
                } catch (Exception e) {
                    client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " or ::" + cmd[0] + " First_name or"));
                    client.send(new SendMessage("::" + cmd[0] + " First_name second_name"));
                }
            }
            if (command.startsWith("noclip") && client.playerRights < 2 && getGameWorldId() == 1) {
                client.disconnected = true;
            }
            if (command.startsWith("slay_sim") && getGameWorldId() > 1) {
                int[] taskStreak = {1000, 500, 250, 100, 50, 10};
                int[] experience = {50, 30, 20, 11, 6, 2};
                int totalTimes = 0;
                for(int task = 1; task <= 1000; task++) {
                    int bonusXp = -1;
                    for(int i = 0; i < taskStreak.length && bonusXp == -1; i++)
                        if(task%taskStreak[i] == 0) {
                            totalTimes+= experience[i];
                            bonusXp = 0;
                        }
                }
                client.send(new SendMessage("Total amount of times: " + totalTimes + " out of 1000!"));
            }
            if (cmd[0].equalsIgnoreCase("price")) {
                String name = command.substring(cmd[0].length() + 1);
                Server.itemManager.getItemName(client, name);
            }
            if (cmd[0].equalsIgnoreCase("max")) {
                client.send(new SendMessage("<col=FF8000>Melee max hit: " + meleeMaxHit(client) + " (MeleeStr: " + client.playerBonus[10] + ")"));
                client.send(new SendMessage("<col=0B610B>Range max hit: " + rangedMaxHit(client) + " (RangeStr: " + getRangedStr(client) + ")"));
                if (client.autocast_spellIndex == -1)
                    client.send(new SendMessage("<col=292BA3>Magic max hit (smoke rush): " + (int)(client.baseDamage[0] * magicBonusDamage(client)) + " (Magic damage increase: " + String.format("%3.1f", (magicBonusDamage(client) - 1.0) * 100D) + "%)"));
                else
                    client.send(new SendMessage("<col=292BA3>Magic max hit (" + client.spellName[client.autocast_spellIndex]
                            + "): " + (int)(client.baseDamage[client.autocast_spellIndex] * magicBonusDamage(client)) + " (Magic damage increase: " + String.format("%3.1f", (magicBonusDamage(client) - 1.0) * 100D) + "%)"));
            }
            if ((command.startsWith("/") && !command.substring(1).isEmpty()) || (cmd[0].equalsIgnoreCase("yell") && command.length() > 5)) {
                if (!client.premium) {
                    client.send(new SendMessage("You must be a Premium Member to yell."));
                    client.send(new SendMessage("Use the Dodian.net Market Forums to post new threads to buy/sell."));
                    return;
                }
                if (!Server.chatOn && client.playerRights < 1) {
                    client.send(new SendMessage("Yell chat is disabled!"));
                    return;
                }
			/*if (System.currentTimeMillis() - client.lastYell < 10000 && client.playerRights < 1) {
				client.send(new SendMessage("You must wait " + (((lastYell + 10000) - System.currentTimeMillis()) / 1000)
						+ " more seconds before yelling again"));
				client.send(new SendMessage("Use the yell channel to congratulate members, buy and sell items,"));
				client.send(new SendMessage("ask questions about the server or to announce an event you are holding."));
				client.send(new SendMessage("Misuse of the yell channel is grounds for a 24 hour mute."));
				return;
			}
			client.lastYell = System.currentTimeMillis();*/ //TODO: Add timer if needed!
                String text = command.substring(command.startsWith("/") ? 1 : 5);
                text = text.replace("<col", "<moo");
                text = text.replace("<shad", "<moo");
                text = text.replace("b:", "<col=292BA3>");
                text = text.replace("r:", "<col=FF0000>");
                text = text.replace("p:", "<col=FF00FF>");
                text = text.replace("o:", "<col=FF8000>");
                text = text.replace("g:", "<col=0B610B>");
                text = text.replace("y:", "<col=FFFF00>");
                text = text.replace("d:", "<col=000000>");
                if (!client.isMuted()) {
                    String[] bad = {"chalreq", "duelreq", "tradereq"};
                    for (String s : bad) {
                        if (text.contains(s)) {
                            return;
                        }
                    }
                    String yell = Character.toUpperCase(text.charAt(0)) + text.substring(1);
                    Server.chat.add(new ChatLine(client.getPlayerName(), client.dbId, 1, yell, client.getPosition().getX(),
                            client.getPosition().getY()));
                    if (client.playerRights == 0) {
                        client.yell("[YELL]<col=000000>" + client.getPlayerName() + "<col=0000ff>: " + yell);
                    } else if (client.playerRights == 1) {
                        client.yell("<col=0B610B>" + client.getPlayerName() + "<col=000000>: <col=0B610B>" + yell + "@cr1@");
                    } else if (client.playerRights >= 2) {
                        client.yell("<col=FFFF00>" + client.getPlayerName() + "<col=000000>: <col=0B610B>" + yell + "@cr2@");
                    }
                    ChatLog.recordYellChat(client, yell);
                } else {
                    client.send(new SendMessage("You are currently muted!"));
                }
            }
            if (cmd[0].equalsIgnoreCase("examine")) {
                int definition = Integer.parseInt(cmd[1]);
                int id = Integer.parseInt(cmd[2]);
                switch(definition) {
                    case 1025: //Npc examine!
                        client.examineNpc(client, id);
                        break;
                    case 1448: //Ground item examine!
                    case 1125: //Item examine!
                        int amount = cmd.length < 4 ? 1 : Integer.parseInt(cmd[3]);
                        client.examineItem(client, id, amount);
                        break;
                    case 1226: //Object examine!
                        int x = Integer.parseInt(cmd[3]);
                        int y = Integer.parseInt(cmd[4]);
                        int z = Integer.parseInt(cmd[5]);
                        client.examineObject(client, id, new Position(x, y, z));
                        break;
                }
            }
            if (command.equalsIgnoreCase("players")) {
                client.send(new SendMessage("There are currently <col=006600>" + PlayerHandler.getPlayerCount() + "<col=0> players online!"));
                client.send(new SendString("@dre@                    Uber 3.0", 8144));
                client.clearQuestInterface();
                client.send(new SendString("@dbl@Online players: @blu@" + PlayerHandler.getPlayerCount(), 8145));
                int line = 8147;
                int count = 0;
                for (Player p : PlayerHandler.players) {
                    if (p != null && p.dbId >= 0) {
                        String title = "";
                        if (p.playerRights == 1 && p.playerGroup == 5)
                            title = "@blu@Mod ";
                        else if (p.playerRights == 1 && p.playerGroup == 9)
                            title = "@blu@Trial Mod ";
                        else if (p.playerRights == 2 && p.playerGroup == 10)
                            title = "@yel@Developer ";
                        else if (p.playerRights == 2)
                            title = "@yel@Admin ";
                        client.send(new SendString("@bla@" + title + "@dbl@" + p.getPlayerName() + " @bla@(Level-" + p.determineCombatLevel() + ") @bla@is " + p.getPositionName(), line));
                        line++;
                        count++;
                        if (line == 8196)
                            line = 12174;
                        if (count > 100)
                            break;
                    }
                }
                if (PlayerHandler.getPlayerCount() > 100) {
                    client.send(new SendMessage("Note: there are too many players online to list, 100 are shown"));
                }
                client.sendQuestSomething(8143);
                client.showInterface(8134);
            }
            /* Special commands for beta or regular world */
            if (cmd[0].equalsIgnoreCase("tele") && (specialRights || getGameWorldId() > 1)) {
                try {
                    int newPosX = Integer.parseInt(cmd[1]);
                    int newPosY = Integer.parseInt(cmd[2]);
                    int newHeight = cmd.length != 4 ? 0 : Integer.parseInt(cmd[3]);
                    client.transport(new Position(newPosX, newPosY, newHeight));
                    client.send(new SendMessage("Welcome to " + newPosX + ", " + newPosY + " at height " + newHeight));
                } catch (Exception e) {
                    client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " x y or ::" + cmd[0] + " x y height"));
                }
            }
            if ((command.equalsIgnoreCase("mypos") || command.equalsIgnoreCase("pos")) && (specialRights || getGameWorldId() > 1)) {
                client.send(new SendMessage(
                        "Your position is (" + client.getPosition().getX() + " , " + client.getPosition().getY() + ")"));
            }
            if (cmd[0].equalsIgnoreCase("forcetask") && (specialRights || getGameWorldId() > 1)) {
                try {
                    int taskId = Integer.parseInt(cmd[1]);
                    int length = SlayerTask.slayerTasks.values().length - 1;
                    if (taskId < 0 || taskId > length) {
                        client.send(new SendMessage("Task id out of bound! Can only be 0 - " + length));
                        return;
                    }
                    client.getSlayerData().set(0, client.getSlayerData().get(0) == -1 ? 402 : client.getSlayerData().get(0));
                    client.getSlayerData().set(1, taskId);
                    client.getSlayerData().set(2, 1337); //Current amt
                    client.getSlayerData().set(3, 1337); //Start amt
                    client.send(new SendMessage("[DEBUG]: You force the task to be 1337 of  " + SlayerTask.slayerTasks.getTask(taskId).getTextRepresentation() + " (" + SlayerTask.slayerTasks.getTask(taskId) + ")"));
                } catch (Exception e) {
                    client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " taskId"));
                }
            }
            if (command.equalsIgnoreCase("meeting") && client.playerRights > 1) {
                for (int i = 0; i < PlayerHandler.players.length; i++) {
                    if (client.validClient(i)) {
                        Client t = client.getClient(i);
                        if (t.playerRights > 0) {
                            t.send(new SendMessage("All of you belong to " + client.getPlayerName()));
                            t.triggerTele(2936, 4688, 0, false);
                        }
                    }
                }
            }
            if (command.equalsIgnoreCase("alltome") && client.playerRights > 1) {
                for (int i = 0; i < PlayerHandler.players.length; i++) {
                    if (client.validClient(i)) {
                        Client t = client.getClient(i);
                        if (t == client) continue;
                        t.send(new SendMessage("<col=cc0000>A force moved you towards a location!"));
                        t.triggerTele(client.getPosition().getX(), client.getPosition().getY(), client.getPosition().getZ(), false);
                    }
                }
                client.send(new SendMessage("You teleported all online to you!"));
            }
            if (command.startsWith("mod") && client.playerRights > 0) {
                String text = command.substring(4);
                text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
                client.modYell(
                        "[STAFF] " + client.getPlayerName() + ": " + text);
                ChatLog.recordModChat(client, text); //We record this instead of command!
            }
            if (command.startsWith("tradelock") && client.playerRights > 0) {
                try {
                    if (client.wildyLevel > 0) {
                        client.send(new SendMessage("Command can't be used in the wilderness"));
                        return;
                    }
                    String otherPName = command.substring(cmd[0].length() + 1);
                    int otherPIndex = PlayerHandler.getPlayerID(otherPName);
                    if (otherPIndex != -1) {
                        Client p = (Client) PlayerHandler.players[otherPIndex];
                        p.tradeLocked = true;
                        client.send(new SendMessage("You have just tradelocked " + otherPName));
                        CommandLog.recordCommand(client, command);
                    } else {
                        client.send(new SendMessage("The name doesnt exist."));
                    }
                } catch (Exception e) {
                    client.send(new SendMessage("Try entering a name you want to tradelock.."));
                }
            }
            if (command.startsWith("bosspawn") && (getGameWorldId() > 1 || specialRights)) {
                String npcName = command.substring(cmd[0].length() + 1).replaceAll(" ", "_");
                if(npcName.equalsIgnoreCase(client.boss_name[0])) //Dad
                    client.respawnBoss(4130);
                else if(npcName.equalsIgnoreCase(client.boss_name[1]) || npcName.equalsIgnoreCase("abyssal")) //Abyssal guardian
                    client.respawnBoss(2585);
                else if(npcName.equalsIgnoreCase(client.boss_name[2]) || npcName.equalsIgnoreCase("san")) //San Tajalon
                    client.respawnBoss(3964);
                else if(npcName.equalsIgnoreCase(client.boss_name[3]) || npcName.equalsIgnoreCase("bkt")) //Black knight titan
                    client.respawnBoss(4067);
                else if(npcName.equalsIgnoreCase(client.boss_name[4]) || npcName.equalsIgnoreCase("jungle")) //Jungle demon
                    client.respawnBoss(1443);
                else if(npcName.equalsIgnoreCase(client.boss_name[5])) //Ungadulu
                    client.respawnBoss(3957);
                else if(npcName.equalsIgnoreCase(client.boss_name[6]) || npcName.equalsIgnoreCase("nech")) //Nechrayel
                    client.respawnBoss(8);
                else if(npcName.equalsIgnoreCase(client.boss_name[7]) || npcName.equalsIgnoreCase("queen")) //Ice queen
                    client.respawnBoss(4922);
                else if(npcName.equalsIgnoreCase(client.boss_name[8]) || npcName.equalsIgnoreCase("kbd")) //King black dragon
                    client.respawnBoss(239);
                else if(npcName.equalsIgnoreCase(client.boss_name[9]) || npcName.equalsIgnoreCase("mourner") || npcName.equalsIgnoreCase("head")) //Head mourner
                    client.respawnBoss(5311);
                else if(npcName.equalsIgnoreCase(client.boss_name[10]) || npcName.equalsIgnoreCase("black")) //Black demon
                    client.respawnBoss(1432);
                else if(npcName.equalsIgnoreCase(client.boss_name[11]) || npcName.equalsIgnoreCase("prime")) //Dagannoth prime
                    client.respawnBoss(2266);
                else if(npcName.equalsIgnoreCase(client.boss_name[12])) //Dwayne
                    client.respawnBoss(2261);
                else if(npcName.equalsIgnoreCase(client.boss_name[13]) || npcName.equalsIgnoreCase("jad")) //TzTok-Jad
                    client.respawnBoss(3127);
                else if(npcName.equalsIgnoreCase(client.boss_name[14]) || npcName.equalsIgnoreCase("kq")) //Kalphite queen
                    client.respawnBoss(4303);
                else if(npcName.equalsIgnoreCase(client.boss_name[15]) || npcName.equalsIgnoreCase("kk")) //Kalphite king
                    client.respawnBoss(4304);
                else if(npcName.equalsIgnoreCase(client.boss_name[16])) //Venenatis
                    client.respawnBoss(6610);
                else client.send(new SendMessage("Could not find boss: " + npcName));
            }
            if (command.startsWith("bosstele") && (getGameWorldId() > 1 || specialRights)) {
                String npcName = command.substring(cmd[0].length() + 1).replaceAll(" ", "_");
                if(npcName.equalsIgnoreCase(client.boss_name[0])) //Dad
                    client.triggerTele(2543, 3091, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[1]) || npcName.equalsIgnoreCase("abyssal")) //Abyssal guardian
                    client.triggerTele(2626, 3084, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[2]) || npcName.equalsIgnoreCase("san")) //San Tajalon
                    client.triggerTele(2613, 9521, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[3]) || npcName.equalsIgnoreCase("bkt")) //Black knight titan
                    client.triggerTele(2566, 9507, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[4]) || npcName.equalsIgnoreCase("jungle")) //Jungle demon
                    client.triggerTele(2572, 9529, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[5])) //Ungadulu
                    client.triggerTele(2889, 3426, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[6]) || npcName.equalsIgnoreCase("nech")) //Nechrayel
                    client.triggerTele(2698, 9771, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[7]) || npcName.equalsIgnoreCase("queen")) //Ice Queen
                    client.triggerTele(2866, 9951, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[8]) || npcName.equalsIgnoreCase("kbd")) //King black dragon
                    client.triggerTele(3315, 9374, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[9]) || npcName.equalsIgnoreCase("mourner") || npcName.equalsIgnoreCase("head")) //Head mourner
                    client.triggerTele(2554, 3278, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[10]) || npcName.equalsIgnoreCase("black")) //Black demon
                    client.triggerTele(2907, 9805, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[11]) || npcName.equalsIgnoreCase("prime")) //Dagannoth prime
                    client.triggerTele(2905, 9727, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[12])) //Dwayne
                    client.triggerTele(2776, 3206, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[13]) || npcName.equalsIgnoreCase("jad")) //TzTok-Jad
                    client.triggerTele(2393, 5090, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[14]) || npcName.equalsIgnoreCase("kq")) //Kalphite queen
                    client.triggerTele(3489, 9495, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[15]) || npcName.equalsIgnoreCase("kk")) //Kalphite king
                    client.triggerTele(1713, 9843, 0,false);
                else if(npcName.equalsIgnoreCase(client.boss_name[16])) //Venenatis
                    client.triggerTele(3218, 9934, 0,false);
                else client.send(new SendMessage("Could not find boss spawn for: " + npcName));
            }
            /* Beta commands*/
            if (getGameWorldId() > 1) {
                if (cmd[0].equalsIgnoreCase("rehp") && !specialRights) {
                    client.reloadHp = !client.reloadHp;
                    client.send(new SendMessage("You set reload hp as " + client.reloadHp));
                }
                if (cmd[0].equalsIgnoreCase("debug")) {
                    client.debug = !client.debug;
                    client.send(new SendMessage("You set debug as " + client.debug));
                }
                if (cmd[0].equals("boost")) {
                    client.boost(5 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.STRENGTH)) * 0.15), Skill.STRENGTH);
                    client.boost(5 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.DEFENCE)) * 0.15), Skill.DEFENCE);
                    client.boost(5 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.ATTACK)) * 0.15), Skill.ATTACK);
                    client.boost(4 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.RANGED)) * 0.12), Skill.RANGED);
                }
                if ((cmd[0].equalsIgnoreCase("tool") || cmd[0].equalsIgnoreCase("potato")) && client.playerRights > 0) {
                    client.addItem(5733, 1);
                    client.checkItemUpdate();
                    client.send(new SendMessage("Here is your dev potato!"));
                }
                if (cmd[0].equalsIgnoreCase("pnpc") && client.playerRights > 0) {
                    try {
                        int npcId = Integer.parseInt(cmd[1]);
                        if (npcId <= 8195) {
                            client.isNpc = npcId >= 0;
                            client.setPlayerNpc(npcId >= 0 ? npcId : -1);
                            client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
                        }
                        client.send(new SendMessage(npcId > 8195 ? "Maximum 8195 in npc id!" : npcId >= 0 ? "Setting npc to " + client.getPlayerNpc() : "Setting you normal!"));
                    } catch (Exception e) {
                        client.send(new SendMessage("Wrong usage.. ::" + cmd[0] + " npcid"));
                    }
                }
                if (cmd[0].equalsIgnoreCase("item")) {
                    int newItemID = Integer.parseInt(cmd[1]);
                    int newItemAmount = Integer.parseInt(cmd[2]);
                    if (newItemID < 1 || newItemID > 22376) {
                        client.send(new SendMessage("Stop pulling a River! Maximum itemid = 22376!"));
                        return;
                    }
                    if (Server.itemManager.isStackable(newItemID))
                        if (client.freeSlots() <= 0 && !client.playerHasItem(newItemID))
                            client.send(new SendMessage("Not enough space in your inventory."));
                        else
                            client.addItem(newItemID, newItemAmount);
                    else {
                        newItemAmount = Math.min(newItemAmount, client.freeSlots());
                        if (newItemAmount > 0)
                            for (int i = 0; i < newItemAmount; i++)
                                client.addItem(newItemID, 1);
                        else
                            client.send(new SendMessage("Not enough space in your inventory."));
                    }
                    client.checkItemUpdate();
                }
                if (!specialRights && (cmd[0].equalsIgnoreCase("bank") || cmd[0].equalsIgnoreCase("b"))) {
                    client.openUpBank();
                }
                if (cmd[0].equalsIgnoreCase("perk")) {
                    client.send(new SendMessage("Checking perk...."));
                    boolean equipCheck = client.skillcapePerk(Skill.WOODCUTTING, false);
                    boolean invCheck = client.skillcapePerk(Skill.WOODCUTTING, true);
                    client.send(new SendMessage("result = " + equipCheck + ", " + invCheck));
                }
                if (cmd[0].equals("plunder")) {
                    client.getPlunder.startPlunder();
                }
                if (cmd[0].equals("p_start")) {
                    client.transport(Server.entryObject.start);
                }
                if (cmd[0].equals("p_tele")) {
                    client.transport(Server.entryObject.currentDoor);
                }
                if (cmd[0].equals("p_next")) {
                    client.getPlunder.nextRoom();
                    client.send(new SendMessage("You are now at floor " + (client.getPlunder.getRoomNr() + 1)));
                }
                if (cmd[0].equals("barrows")) {
                    for(int i = 0; i < 8; i++)
                        client.addItem(4746 + (i * 2), 1);
                    for(int i = 0; i < 16; i++)
                        client.addItem(4709 + (i * 2), 1);
                    client.checkItemUpdate();
                    client.send(new SendMessage("Here is your barrows pieces!"));
                }
                if(cmd[0].equalsIgnoreCase("setup")) {
                    for(int i = 0; i < 7; i++) {
                        int level = i == 3 ? 78 : 75;
                        client.setExperience(Skills.getXPForLevel(level), Skill.getSkill(i));
                        client.setLevel(level, Skill.getSkill(i));
                        if(i == 3) {
                            client.maxHealth = level;
                            client.heal(level);
                        } else if (i == 5) {
                            client.maxPrayer = level;
                            client.pray(level);
                        } else client.refreshSkill(Skill.getSkill(i));
                    }
                    client.getEquipment()[0] = 3751;
                    client.getEquipmentN()[0] = 1;
                    client.getEquipment()[1] = 1007;
                    client.getEquipmentN()[1] = 1;
                    client.getEquipment()[2] = 1725;
                    client.getEquipmentN()[2] = 1;
                    client.getEquipment()[3] = 4587;
                    client.getEquipmentN()[3] = 1;
                    client.getEquipment()[4] = 3140;
                    client.getEquipmentN()[4] = 1;
                    client.getEquipment()[5] = 1540;
                    client.getEquipmentN()[5] = 1;
                    client.getEquipment()[7] = 4087;
                    client.getEquipmentN()[7] = 1;
                    client.getEquipment()[9] = 7459;
                    client.getEquipmentN()[9] = 1;
                    client.getEquipment()[10] = 4129;
                    client.getEquipmentN()[10] = 1;
                    for(int i = 0; i < 14; i++)
                        client.setEquipment(client.getEquipment()[i], client.getEquipmentN()[i], i);
                    client.addItem(5733, 1);
                    for(int i = 0; i < 27; i++)
                        client.addItem(385, 1);
                    client.checkItemUpdate();
                    client.send(new SendMessage("Your setup is done!"));
                }
            }
        } catch (Exception e) { //end of commands!
            // client.send(new SendMessage("sends this!")); //Invalid command!
        }
    }

}