package net.dodian.uber.comm;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Login;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Friend;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.prayer.Prayers;
import net.dodian.uber.game.security.ItemLog;
import net.dodian.utilities.DbTables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class LoginManager {

    public int loadCharacterGame(Client p, String playerName, String playerPass) {
        if (PlayerHandler.isPlayerOn(playerName)) { // Already online!
			/*System.out.printf("%s players online:%n", PlayerHandler.playersOnline.size());
			PlayerHandler.playersOnline.forEach((lName, client) -> System.out.printf("- %s (%s), session start=%tc %n", client.playerName, client.longName, client.now));
            System.out.println();
            */ //Old Debug!
            return 5;
        }

        if (playerName.isEmpty()) //To short name!
            return 3;

        try {
            String query = "SELECT * FROM " + DbTables.WEB_USERS_TABLE + " WHERE username = '" + playerName + "'";
            ResultSet results = getDbConnection().createStatement().executeQuery(query);
            if (results.next()) {
                p.dbId = results.getInt("userid");
                p.playerGroup = results.getInt("usergroupid");
                //if(p.playerGroup != 10 && p.playerGroup != 6) return 8; //Maintanance check!
                if (results.getString("username").equals(playerName)
                        || results.getString("username").equalsIgnoreCase(playerName)) {
                    String playerSalt = results.getString("salt");
                    String md5pass = Client.passHash(playerPass, playerSalt);
                    if (!md5pass.equals(results.getString("password"))
                            && (!net.dodian.utilities.DotEnvKt.getServerEnv().equals("dev") || (!p.connectedFrom.equals("127.0.0.1") && !(net.dodian.utilities.DotEnvKt.getServerDebugMode() && (p.playerGroup == 40 || p.playerGroup == 34 || p.playerGroup == 11))))) {
                        return 3;
                    }
                    p.otherGroups = results.getString("membergroupids").split(",");
                    p.newPms = (results.getInt("pmunread"));
                } else
                    return 12;
            } else if (net.dodian.utilities.DotEnvKt.getServerEnv().equals("dev") && net.dodian.utilities.DotEnvKt.getServerDebugMode()) {
                String newUserQuery = "INSERT INTO " + DbTables.WEB_USERS_TABLE + " SET username = '" + playerName + "', passworddate = '', birthday_search = ''";
                getDbConnection().createStatement().executeUpdate(newUserQuery);
                return loadCharacterGame(p, playerName, playerPass);
            } else {
                return 12;
            }
            results.close();
        } catch (Exception e) {
            System.out.println("Failed to load player: " + playerName + ", " + e);
            return 13;
        }
        return 0;
    }

    public static String UUID;

    public int loadgame(Client p, String playerName, String playerPass) {
        int loadCharacterResponse = loadCharacterGame(p, playerName, playerPass);
        if (loadCharacterResponse > 0) return loadCharacterResponse;
        try {
            String query = "select * from " + DbTables.GAME_CHARACTERS + " where id = '" + p.dbId + "'";
            ResultSet results = getDbConnection().createStatement().executeQuery(query);
            if (results.next()) {
                if (isBanned(p.dbId)) {
                    return 4;
                }
                if (Login.isUidBanned(LoginManager.UUID)) {
                    return 22;
                }
                String[] look = results.getString("look").isEmpty() ? null : results.getString("look").split(" ");
                if (look == null || look.length != 13) {
                    p.lookNeeded = true;
                } else {
                    int[] parts = new int[13];
                    for (int i = 0; i < look.length; i++)
                        parts[i] = Integer.parseInt(look[i]);
                    p.setLook(parts);
                }
                p.latestNews = results.getInt("news"); //Sets the latest news for a user!
                p.UUID = results.getString("uuid");
                int x = results.getInt("x"), y = results.getInt("y"), z = results.getInt("height");
                p.loginPosition(x, y, z);
                if (x < 1 || y < 1) p.resetPos(); //Incase a player has no coordination value!
                p.mutedTill = results.getLong("unmutetime");
                /* Set stats */
                p.fightType = results.getInt("fightStyle");
                p.autocast_spellIndex = results.getInt("autocast");
                int health = (results.getInt("health"));
                String prayer = results.getString("prayer").trim();
                String boosted = results.getString("boosted").trim();
                String[] prayer_prase = prayer.split(":");
                String[] boosted_prase = boosted.split(":");
                int prayerLevel = !prayer.isEmpty() ? Integer.parseInt(prayer_prase[0]) : 0;
                String query2 = "select * from " + DbTables.GAME_CHARACTERS_STATS + " where uid = '" + p.dbId + "'";
                ResultSet results2 = getDbConnection().createStatement().executeQuery(query2);
                if (results2.next()) {
                    Skill.enabledSkills().forEach(skill -> {
                        try {
                            p.setExperience(results2.getInt(skill.getName()), skill);
                            p.setLevel(Skills.getLevelForExperience(p.getExperience(skill)), skill);
                            if (skill == Skill.HITPOINTS) {
                                p.maxHealth = Skills.getLevelForExperience(p.getExperience(skill));
                                p.setCurrentHealth(health < 1 || health > p.maxHealth ? p.maxHealth : health);
                            } else if (skill == Skill.PRAYER) {
                                p.maxPrayer = Skills.getLevelForExperience(p.getExperience(skill));
                                p.setCurrentPrayer(prayerLevel < 0 || prayerLevel > p.maxPrayer ? p.maxPrayer : prayerLevel);
                            }
                            p.refreshSkill(skill);
                        } catch (SQLException e) {
                            System.out.println("something wrong with sql stats! " + e);
                        }
                    });
                } else {
                    Statement statement = getDbConnection().createStatement();
                    String newStatsAccount = "INSERT INTO " + DbTables.GAME_CHARACTERS_STATS + "(uid)" + " VALUES ('" + p.dbId + "')";
                    statement.executeUpdate(newStatsAccount);
                    statement.close();

                    Skill.enabledSkills().forEach(skill -> {
                        p.setExperience(skill == Skill.HITPOINTS ? 1155 : 0, skill);
                        p.setLevel(skill == Skill.HITPOINTS ? 10 : 1, skill);
                        p.setCurrentHealth(p.getLevel(Skill.HITPOINTS));
                        p.maxPrayer = 1;
                        p.maxHealth = 10;
                        p.refreshSkill(skill);
                    });
                }
                if (!prayer.isEmpty()) {
                    for (int i = 1; i < prayer_prase.length; i++)
                        p.getPrayerManager().togglePrayer(Prayers.Prayer.forButton(Integer.parseInt(prayer_prase[i])));
                }
                if (!boosted.isEmpty()) {
                    p.lastRecover = Integer.parseInt(boosted_prase[0]);
                    for (int i = 0; i < boosted_prase.length - 1; i++)
                        p.boost(Integer.parseInt(boosted_prase[i + 1]), Skill.getSkill(i));
                }
                results2.close();
                /* Sets Inventory */
                String inventory = (results.getString("inventory").trim());
                if (!inventory.isEmpty()) {
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
                /* Sets Equipment */
                long lastOn = (results.getLong("lastlogin"));
                if (lastOn == 0) {
                    p.getEquipment()[Equipment.Slot.WEAPON.getId()] = 1277;
                    p.getEquipment()[Equipment.Slot.SHIELD.getId()] = 1171;
                    p.getEquipmentN()[Equipment.Slot.WEAPON.getId()] = 1;
                    p.getEquipmentN()[Equipment.Slot.SHIELD.getId()] = 1;
                    p.addItem(995, 2000);
                    p.addItem(1856, 1);
                    p.addItem(4155, 1);
                    p.checkItemUpdate();
                }
                /* Sets Equipment */
                String equip = (results.getString("equipment")).trim();
                if (!equip.isEmpty()) {
                    String[] parse = equip.split(" ");
                    for (String s : parse) {
                        String[] parse2 = s.split("-");
                        if (parse2.length > 0) {
                            int slot = Integer.parseInt(parse2[0]);
                            int id = Integer.parseInt(parse2[1]);
                            int amount = Integer.parseInt(parse2[2]);
                            if (id <= 24000) {
                                if (p.checkEquip(id, slot, -1)) {
                                    p.getEquipment()[slot] = id;
                                    p.getEquipmentN()[slot] = amount;
                                } else if (p.freeSlots() == 0 || !p.addItem(id, amount)) {
                                    Ground.addFloorItem(p, id, amount);
                                    ItemLog.playerDrop(p, id, amount, p.getPosition().copy(), "Equipment check drop");
                                    p.send(new SendMessage("<col=FF0000>You dropped the " + Server.itemManager.getName(Integer.parseInt(parse2[1])).toLowerCase() + " on the floor!!!"));
                                }
                            }
                        }
                    }
                }
                p.setTask(results.getString("slayerData"));
                p.agilityCourseStage = results.getInt("agility");
                p.setTravel(results.getString("travel"));
                /* Sets Unlocks */
                String unlocks = (results.getString("unlocks")).trim();
                for (int i = 0; i < p.unlockLength; i++) {
                    if (!unlocks.isEmpty()) {
                        String[] parse = unlocks.split(":");
                        if (i < parse.length) {
                            p.addUnlocks(i, parse[i].split(","));
                        } else p.addUnlocks(i, "0", "0");
                    } else p.addUnlocks(i, "0", "0");
                }
                /* Sets Bank */
                String bank = (results.getString("bank")).trim();
                if (!bank.isEmpty()) {
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

                String[] songUnlocked = results.getString("songUnlocked").split(" ");
                for (int i = 0; i < songUnlocked.length; i++) {
                    if (songUnlocked[i].isEmpty())
                        continue;
                    p.setSongUnlocked(i, Integer.parseInt(songUnlocked[i]) == 1);
                } //TODO Shall we keep?

                String[] friends = results.getString("friends").split(" ");
                for (String friend : friends) {
                    if (!friend.isEmpty()) {
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
                        if (parts.length >= 2 && p.boss_amount.length > i)
                            p.bossCount(parts[0], Integer.parseInt(parts[1]));
                    }
                }
                String Monster = results.getString("Monster_Log");
                if (Monster != null && !Monster.isEmpty()) {
                    String[] lines = Monster.split(";");
                    for (String line : lines) {
                        String[] parts = line.split(",");
                        if (parts.length == 2) {
                            int amount = Integer.parseInt(parts[1]);
                            p.monsterName.add(parts[0]);
                            p.monsterCount.add(amount);
                        } //If parts is more or less than 2 we gone goooofed!
                    }
                }
                String effect = results.getString("effects");
                if (effect != null && !effect.isEmpty()) {
                    String[] lines = effect.split(":");
                    for (int i = 0; i < lines.length; i++) {
                        int amount = Integer.parseInt(lines[i]);
                        p.effects.add(i, amount);
                    }
                }
                /* Daily reward, like battlestaves! */
                String daily = results.getString("dailyReward");
                if (daily != null && !daily.isEmpty()) { //If empty or null!
                    String[] lines = daily.split(";");
                    for (int i = 0; i < lines.length; i++) {
                        String[] parts = lines[i].split(",");
                        boolean newDay = p.dateDays(new Date(Long.parseLong(parts[0])), p.today) > 0;
                        if (i == 0) { //Battle staff loading!
                            if (newDay) {
                                p.dailyReward.add(0, p.today.getTime() + "");
                                p.dailyReward.add(1, "6000");
                                p.dailyReward.add(2, parts[2]);
                                p.dailyReward.add(3, "0");
                                p.dailyReward.add(4, parts[4]);
                            } else {
                                for (int ii = 0; ii < parts.length; ii++)
                                    p.dailyReward.add(ii, parts[ii]);
                            }
                        } else { //Hmm we could do it like this if needed :L
                            for (int ii = 0; ii < parts.length; ii++)
                                p.dailyReward.add(p.staffSize + ii, parts[ii]);
                        }
                    }
                } else p.defaultDailyReward(p); //TODO: If ever need more daily stuff, need to fix code!
                /* Account timers */
                p.lastSave = System.currentTimeMillis();
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
                String newStatsAccount = "INSERT INTO " + DbTables.GAME_CHARACTERS_STATS + "(uid)" + " VALUES ('" + p.dbId + "') ON DUPLICATE KEY UPDATE uid=uid";
                statement.executeUpdate(newStatsAccount);
                statement.close();
                results.close();
                return loadgame(p, playerName, playerPass);
            }
        } catch (Exception e) {
            System.out.println("Something wrong with loading a player..." + e);
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
            System.out.println("Something wrong with updating a players forum rights " + e);
        }
        p.send(new SendMessage("You have now been registered to the forum! Enjoy your stay :D"));
    }

    public boolean isBanned(int id) throws SQLException {
        String query = "select * from " + DbTables.GAME_CHARACTERS + " where id = '" + id + "'";
        ResultSet results = getDbConnection().createStatement().executeQuery(query);
        Date now = new Date();
        if (results.next())
            return now.getTime() < results.getLong("unbantime");
        return false;
    }

}
