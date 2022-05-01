package net.dodian.uber.comm;

import net.dodian.uber.game.model.Login;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Friend;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.utilities.DbTables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import static net.dodian.DotEnvKt.getServerDebugMode;
import static net.dodian.DotEnvKt.getServerEnv;
import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class LoginManager {

    public int loadCharacterGame(Client p, String playerName, String playerPass) {
        if (PlayerHandler.isPlayerOn(playerName)) //Already online!
            return 5;
        if (playerName.length() < 1) //To short name!
            return 3;
        try {
            String query = "SELECT * FROM " + DbTables.WEB_USERS_TABLE + " WHERE username = '" + playerName + "'";
            ResultSet results = getDbConnection().createStatement().executeQuery(query);
            if (results.next()) {
                p.dbId = results.getInt("userid");
                if (results.getString("username").equals(playerName)
                        || results.getString("username").equalsIgnoreCase(playerName)) {
                    String playerSalt = results.getString("salt");
                    String md5pass = Client.passHash(playerPass, playerSalt);
                    if (!md5pass.equals(results.getString("password"))
                    && (!getServerEnv().equals("dev") || !getServerDebugMode())) {
                        return 3;
                    }
                    p.playerGroup = results.getInt("usergroupid");
                    p.otherGroups = results.getString("membergroupids").split(",");
                    p.newPms = (results.getInt("pmunread"));
                    boolean specialTreatment = p.playerGroup == 6 || p.playerGroup == 10 || p.playerGroup == 35 || p.playerGroup == 9 || p.playerGroup == 5;
    	  /*if(!specialTreatment)
    	    return 8;*/
          /*int hosts = Server.totalHostConnection(p.connectedFrom);
    	  if(hosts > 2 && !specialTreatment)
    		  return 9;*/
                } else {
                    return 12;
                }
            } else if (getServerEnv().equals("dev") && getServerDebugMode()) {
                String newUserQuery = "INSERT INTO " + DbTables.WEB_USERS_TABLE + " SET username = '" + playerName + "', passworddate = '', birthday_search = ''";
                getDbConnection().createStatement().executeUpdate(newUserQuery);
                return loadCharacterGame(p, playerName, playerPass);
            } else {
                return 12;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to load player: " + playerName);
            return 13;
        }
        return 0;
    }

    public static String UUID;

    public int loadgame(Client p, String playerName, String playerPass) {
        int loadCharacterResponse = loadCharacterGame(p, playerName, playerPass);
        if (loadCharacterResponse > 0) return loadCharacterResponse;

        //long start = System.currentTimeMillis();
        try {
            String query = "select * from " + DbTables.GAME_CHARACTERS + " where id = '" + p.dbId + "'";
            ResultSet results = getDbConnection().createStatement().executeQuery(query);
            if (results.next()) {
                if (isBanned(p.dbId)) {
                    return 4;
                }
//        if (isSibling(p.dbId)) {
//        	return 23;
//        }
//        if(Login.isUidBanned(LoginManager.UUID)) {
//			return 22;
//		}
                p.setLastVote(results.getLong("lastvote"));
                Client.isSibling = results.getInt("sibling");
                p.UUID = results.getString("uuid");
                p.moveTo(results.getInt("x"), results.getInt("y"), results.getInt("height"));
                if (p.getPosition().getX() == -1 || p.getPosition().getY() == -1) {
                    p.moveTo(2606, 3102, 0);
                }
                int health = (results.getInt("health"));
                p.mutedTill = results.getInt("unmutetime");
                Date now = new Date();
                p.rightNow = now.getTime();
                if (p.mutedTill * 1000 > p.rightNow) {
                    p.muted = true;
                }
                long lastOn = (results.getLong("lastlogin"));
                if (lastOn == 0) {
                    if (!Login.hasRecieved1stStarter(PlayerHandler.players[p.getSlot()].connectedFrom)) {
                        p.getEquipment()[Equipment.Slot.WEAPON.getId()] = 1277;
                        p.getEquipment()[Equipment.Slot.SHIELD.getId()] = 1171;
                        p.getEquipmentN()[Equipment.Slot.WEAPON.getId()] = 1;
                        p.getEquipmentN()[Equipment.Slot.SHIELD.getId()] = 1;
                        p.addItem(995, 10000);
                        p.addItem(1856, 1);
                        Login.addIpToStarterList1(PlayerHandler.players[p.getSlot()].connectedFrom);
                        Login.addIpToStarter1(PlayerHandler.players[p.getSlot()].connectedFrom);
                        p.send(new SendMessage(("You have recieved 1 out of 2 starter packages on this IP address.")));
                    } else if (Login.hasRecieved1stStarter(PlayerHandler.players[p.getSlot()].connectedFrom) && !Login.hasRecieved2ndStarter(PlayerHandler.players[p.getSlot()].connectedFrom)) {
                        p.getEquipment()[Equipment.Slot.WEAPON.getId()] = 1277;
                        p.getEquipment()[Equipment.Slot.SHIELD.getId()] = 1171;
                        p.getEquipmentN()[Equipment.Slot.WEAPON.getId()] = 1;
                        p.getEquipmentN()[Equipment.Slot.SHIELD.getId()] = 1;
                        p.addItem(995, 10000);
                        p.addItem(1856, 1);
                        p.send(new SendMessage(("You have recieved 2 out of 2 starter packages on this IP address.")));
                        Login.addIpToStarterList2(PlayerHandler.players[p.getSlot()].connectedFrom);
                        Login.addIpToStarter2(PlayerHandler.players[p.getSlot()].connectedFrom);
                    } else if (Login.hasRecieved1stStarter(PlayerHandler.players[p.getSlot()].connectedFrom) && Login.hasRecieved2ndStarter(PlayerHandler.players[p.getSlot()].connectedFrom)) {
                        p.send(new SendMessage(("You have already recieved 2 starters!")));
                    }
                }
                int Style = (Integer) (results.getInt("fightStyle"));
                p.FightType = Style;
                p.CalculateMaxHit();
                p.setTask(results.getString("slayerData"));
                p.agilityCourseStage = results.getInt("agility");
                p.autocast_spellIndex = results.getInt("autocast");
                String inventory = (results.getString("inventory").trim());
                String[] parse = inventory.split(" ");
                for (int i = 0; i < parse.length; i++) {
                    String[] parse2 = parse[i].split("-");
                    if (parse2.length > 0) {
                        try {
                            int slot = Integer.parseInt(parse2[0]);
                            if (Integer.parseInt(parse2[1]) < 66000) {
                                p.playerItems[slot] = Integer.parseInt(parse2[1]) + 1;
                                p.playerItemsN[slot] = Integer.parseInt(parse2[2]);
                            }
                        } catch (Exception e) {
                        }
                    }
                }
                String equip = (results.getString("equipment")).trim();
                parse = equip.split(" ");
                for (int i = 0; i < parse.length; i++) {
                    String[] parse2 = parse[i].split("-");
                    if (parse2.length > 0) {
                        try {
                            int slot = Integer.parseInt(parse2[0]);
                            if (Integer.parseInt(parse2[1]) < 66000) {
                                p.getEquipment()[slot] = Integer.parseInt(parse2[1]);
                                p.getEquipmentN()[slot] = Integer.parseInt(parse2[2]);
                            }
                        } catch (Exception e) {
                        }
                    }
                }

                String bank = (results.getString("bank")).trim();
                parse = bank.split(" ");
                for (int i = 0; i < parse.length; i++) {
                    String[] parse2 = parse[i].split("-");
                    if (parse2.length > 0) {
                        try {
                            int slot = Integer.parseInt(parse2[0]);
                            if (Integer.parseInt(parse2[1]) < 66600) {
                                p.bankItems[slot] = Integer.parseInt(parse2[1]) + 1;
                                p.bankItemsN[slot] = Integer.parseInt(parse2[2]);
                            }
                        } catch (Exception e) {
                        }
                    }
                }

                String[] pouches = results.getString("essence_pouch").split(":");
                for (int i = 0; i < pouches.length; i++)
                    p.runePouchesAmount[i] = Integer.parseInt(pouches[i]);

                String[] look = results.getString("look").length() == 0 ? null : results.getString("look").split(" ");
                if (look == null || look.length != 13) {
                    p.lookNeeded = true;
                } else {
                    int[] parts = new int[13];
                    for (int i = 0; i < look.length; i++)
                        parts[i] = Integer.parseInt(look[i]);
                    p.setLook(parts);
                }

                String[] songUnlocked = results.getString("songUnlocked").split(" ");
                for (int i = 0; i < songUnlocked.length; i++) {
                    if (songUnlocked[i] == "")
                        continue;
                    p.setSongUnlocked(i, Integer.parseInt(songUnlocked[i]) == 1 ? true : false);
                }

                String[] friends = results.getString("friends").split(" ");
                for (int i = 0; i < friends.length; i++) {
                    if (friends[i].length() > 0) {
                        p.friends.add(new Friend(Long.parseLong(friends[i]), true));
                    }
                }
                String Boss = results.getString("Boss_Log");
                if (Boss == null) {
                    for (int i = 0; i < p.boss_name.length; i++)
                        p.boss_amount[i] = 0;
                } else {
                    String[] lines = Boss.split(" ");
                    for (int i = 0; i < lines.length; i++) {
                        String[] parts = lines[i].split(":");
                        if (parts.length >= 2 && p.boss_amount.length > i) {
                            int amount = Integer.parseInt(parts[1]);
                            p.boss_amount[i] = amount;
                        }
                    }
                }
                String query2 = "select * from " + DbTables.GAME_CHARACTERS_STATS + " where uid = '" + p.dbId + "'";
                ResultSet results2 = getDbConnection().createStatement().executeQuery(query2);
                if (results2.next()) {
                    for (int i = 0; i < 21; i++) {
                        // p.playerXP[i] = (Integer)(results.getInt("skill" + i));
                        p.setExperience(results2.getInt(Skill.getSkill(i).getName()), Skill.getSkill(i));
                        // p.playerLevel[i] = p.getLevelForXP(p.playerXP[i]);
                        p.setLevel(Skills.getLevelForExperience(p.getExperience(Skill.getSkill(i))), Skill.getSkill(i));
                        if (health == 0 && i == 3) {
                            p.setCurrentHealth(p.getLevel(Skill.HITPOINTS));
                        } else if (health > 0) {
                            p.setCurrentHealth(health);
                        }
                        if (i != 3)
                            p.refreshSkill(Skill.getSkill(i));
                            // p.setSkillLevel(i, p.playerLevel[i], p.playerXP[i]);
                        else {
                            p.refreshSkill(Skill.getSkill(i));
                            // p.setSkillLevel(i, p.currentHealth, p.playerXP[i]);
                            p.maxHealth = p.getLevel(Skill.HITPOINTS);
                        }
                    }
                } else {
                    Statement statement = getDbConnection().createStatement();
                    String newStatsAccount = "INSERT INTO " + DbTables.GAME_CHARACTERS_STATS + "(uid)" + " VALUES ('" + p.dbId + "')";
                    statement.executeUpdate(newStatsAccount);
                    statement.close();
                    for (int i = 0; i < 21; i++) { //Default skills!
                        p.setExperience(i == 3 ? 1155 : 0, Skill.getSkill(i));
                        p.setLevel(i == 3 ? 10 : 1, Skill.getSkill(i));
                        p.setCurrentHealth(p.getLevel(Skill.HITPOINTS));
                        p.refreshSkill(Skill.getSkill(i));
                    }
                }

                p.lastSave = System.currentTimeMillis();
                //long elapsed = System.currentTimeMillis() - start;
                p.start = System.currentTimeMillis();
                p.loadingDone = true;
                PlayerHandler.playersOnline.put(p.longName, p);
                results.close();
                //p.println("Loading Process Completed  [" + p.playerRights + ", " + p.dbId + ", " + elapsed + "]");
                return 0;
            } else {
                Statement statement = getDbConnection().createStatement();
                String newAccount = "INSERT INTO " + DbTables.GAME_CHARACTERS + "(id, name, equipment, inventory, bank, friends, songUnlocked)" + " VALUES ('"
                        + p.dbId + "', '" + playerName + "', '', '', '', '', '0')";
                statement.executeUpdate(newAccount);
                String newStatsAccount = "INSERT INTO " + DbTables.GAME_CHARACTERS_STATS + "(uid)" + " VALUES ('" + p.dbId + "') ON CONFLICT (uid) DO NOTHING";
                statement.executeUpdate(newStatsAccount);
                statement.close();
                return loadgame(p, playerName, playerPass);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 13;
        }
        // return 13;
    }

    public boolean isBanned(int id) throws SQLException {
        String query = "select * from " + DbTables.GAME_CHARACTERS + " where id = '" + id + "'";
        ResultSet results = getDbConnection().createStatement().executeQuery(query);
        Date now = new Date();
        if (results.next()) {
            return now.getTime() < results.getLong("unbantime");
        }
        return false;
    }

}
