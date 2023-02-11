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

import static net.dodian.DotEnvKt.*;
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
                p.playerGroup = results.getInt("usergroupid");
                if (results.getString("username").equals(playerName)
                        || results.getString("username").equalsIgnoreCase(playerName)) {
                    String playerSalt = results.getString("salt");
                    String md5pass = Client.passHash(playerPass, playerSalt);
                    if (!md5pass.equals(results.getString("password"))
                    && (!getServerEnv().equals("dev") || (!p.connectedFrom.equals("127.0.0.1") && !(getServerDebugMode() && (p.playerGroup == 40 || p.playerGroup == 34 || p.playerGroup == 11))))) {
                        return 3;
                    }
                    p.otherGroups = results.getString("membergroupids").split(",");
                    p.newPms = (results.getInt("pmunread"));
                } else
                    return 12;
            } else if (getServerEnv().equals("dev") && getServerDebugMode()) {
                String newUserQuery = "INSERT INTO " + DbTables.WEB_USERS_TABLE + " SET username = '" + playerName + "', passworddate = '', birthday_search = ''";
                getDbConnection().createStatement().executeUpdate(newUserQuery);
                return loadCharacterGame(p, playerName, playerPass);
            } else {
                return 12;
            }
            results.close();
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
                if(Login.isUidBanned(LoginManager.UUID)) {
                    return 22;
                }
                p.latestNews = results.getInt("news"); //Sets the latest news for a user!
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
                    p.getEquipment()[Equipment.Slot.WEAPON.getId()] = 1277;
                    p.getEquipment()[Equipment.Slot.SHIELD.getId()] = 1171;
                    p.getEquipmentN()[Equipment.Slot.WEAPON.getId()] = 1;
                    p.getEquipmentN()[Equipment.Slot.SHIELD.getId()] = 1;
                    p.addItem(995, 2000);
                    p.addItem(1856, 1);
                    p.addItem(4155, 1);
                }
                p.FightType = results.getInt("fightStyle");
                p.setTask(results.getString("slayerData"));
                p.setTravel(results.getString("travel"));
                p.agilityCourseStage = results.getInt("agility");
                p.autocast_spellIndex = results.getInt("autocast");
                /* Sets Inventory */
                String inventory = (results.getString("inventory").trim());
                if(!inventory.equals("")) {
                    String[] parse = inventory.split(" ");
                    for (String s : parse) {
                        String[] parse2 = s.split("-");
                        if (parse2.length > 0) {
                            int slot = Integer.parseInt(parse2[0]);
                            if (Integer.parseInt(parse2[1]) < 66000) {
                                p.playerItems[slot] = Integer.parseInt(parse2[1]) + 1;
                                p.playerItemsN[slot] = Integer.parseInt(parse2[2]);
                            }
                        }
                    }
                }
                /* Sets Unlocks */
                String unlocks = (results.getString("unlocks")).trim();
                if(!unlocks.equals("")) {
                    String[] parse = unlocks.split(":");
                    for (int i = 0; i < parse.length; i++) {
                        p.addUnlocks(i, parse[i].split(","));
                    }
                }
                /* Sets Equipment */
                String equip = (results.getString("equipment")).trim();
                if(!equip.equals("")) {
                    String[] parse = equip.split(" ");
                    for (String s : parse) {
                        String[] parse2 = s.split("-");
                        if (parse2.length > 0) {
                            int slot = Integer.parseInt(parse2[0]);
                            if (Integer.parseInt(parse2[1]) < 66000) {
                                p.getEquipment()[slot] = Integer.parseInt(parse2[1]);
                                p.getEquipmentN()[slot] = Integer.parseInt(parse2[2]);
                            }
                        }
                    }
                }
                /* Sets Bank */
                String bank = (results.getString("bank")).trim();
                if(!bank.equals("")) {
                    String[] parse = bank.split(" ");
                    for (String s : parse) {
                        String[] parse2 = s.split("-");
                        if (parse2.length > 0) {
                            int slot = Integer.parseInt(parse2[0]);
                            if (Integer.parseInt(parse2[1]) < 66600) {
                                p.bankItems[slot] = Integer.parseInt(parse2[1]) + 1;
                                p.bankItemsN[slot] = Integer.parseInt(parse2[2]);
                            }
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
                    if (songUnlocked[i].equals(""))
                        continue;
                    p.setSongUnlocked(i, Integer.parseInt(songUnlocked[i]) == 1);
                }

                String[] friends = results.getString("friends").split(" ");
                for (String friend : friends) {
                    if (friend.length() > 0) {
                        p.friends.add(new Friend(Long.parseLong(friend), true));
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
                        Skill skill = Skill.getSkill(i);
                        if (skill != null) {
                            // p.playerXP[i] = (Integer)(results.getInt("skill" + i));
                            p.setExperience(results2.getInt(skill.getName()), skill);
                            // p.playerLevel[i] = p.getLevelForXP(p.playerXP[i]);
                            p.setLevel(Skills.getLevelForExperience(p.getExperience(skill)), skill);
                            if (i == 3) {
                                p.maxHealth = p.getLevel(Skill.HITPOINTS);
                                p.setCurrentHealth(health < 1 || health > p.maxHealth ? p.maxHealth : health);
                            } else
                                p.refreshSkill(skill);
                        }
                    }
                } else {
                    Statement statement = getDbConnection().createStatement();
                    String newStatsAccount = "INSERT INTO " + DbTables.GAME_CHARACTERS_STATS + "(uid)" + " VALUES ('" + p.dbId + "')";
                    statement.executeUpdate(newStatsAccount);
                    statement.close();
                    for (int i = 0; i < 21; i++) { //Default skills!
                        Skill skill = Skill.getSkill(i);
                        if (skill != null) {
                            p.setExperience(i == 3 ? 1155 : 0, skill);
                            p.setLevel(i == 3 ? 10 : 1, skill);
                            p.setCurrentHealth(p.getLevel(Skill.HITPOINTS));
                            p.refreshSkill(skill);
                        }
                    }
                }

                p.lastSave = System.currentTimeMillis();
                //long elapsed = System.currentTimeMillis() - start;
                p.start = System.currentTimeMillis();
                p.loadingDone = true;
                PlayerHandler.playersOnline.put(p.longName, p);
                results.close();
                results2.close();
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
                results.close();
                return loadgame(p, playerName, playerPass);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 13;
        }
        // return 13;
    }

    public void updatePlayerForumRegistration(Client p) {
        try {
        Statement statement = getDbConnection().createStatement();
        String newStatsAccount = "UPDATE " + DbTables.WEB_USERS_TABLE + " SET usergroupid='40' WHERE userid = '" + p.dbId + "'";
        statement.executeUpdate(newStatsAccount);
        statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        p.send(new SendMessage("You have now been registered to the forum! Enjoy your stay :D"));
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
