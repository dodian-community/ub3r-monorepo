package net.dodian.uber.comm;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Login;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Friend;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.persistence.PlayerSaveCoordinator;
import net.dodian.uber.game.model.player.skills.prayer.Prayers;
import net.dodian.uber.game.security.ItemLog;
import net.dodian.utilities.DbTables;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class LoginManager {

    public int loadCharacterGame(Client p, String playerName, String playerPass) {
        int returnCode = 0;
        if (PlayerHandler.isPlayerOn(playerName))
            return 5; //Already logged in, do not attempt check!
        if (playerName.isEmpty()) //Name is empty!
            return 3;
        
        try (java.sql.Connection conn = getDbConnection();
             Statement stmt = conn.createStatement()) {
            String query = "SELECT * FROM " + DbTables.WEB_USERS_TABLE + " WHERE username = '" + playerName + "'";
            ResultSet results = stmt.executeQuery(query);
            if (results.next()) {
                /* Add data value to check a user for */
                p.dbId = results.getInt("userid");
                if (PlayerSaveCoordinator.isFinalSavePending(p.dbId)) {
                    return 5;
                }
                p.playerGroup = results.getInt("usergroupid");
                p.otherGroups = results.getString("membergroupids").split(",");
                /* if(p.playerGroup != 10 && p.playerGroup != 6) { //Maintanance check!
                    results.close();
                    return 8;
                }*/
                if (results.getString("username").equals(playerName)
                        || results.getString("username").equalsIgnoreCase(playerName)) {
                    String playerSalt = results.getString("salt");
                    String md5pass = Client.passHash(playerPass, playerSalt);
                    if (!md5pass.equals(results.getString("password"))
                            && (!net.dodian.utilities.DotEnvKt.getServerEnv().equals("dev") || (!p.connectedFrom.equals("127.0.0.1") && !(net.dodian.utilities.DotEnvKt.getServerDebugMode() && (p.playerGroup == 40 || p.playerGroup == 34 || p.playerGroup == 11))))) {
                        returnCode = 3;
                    } else { //Values from forum!
                        p.newPms = (results.getInt("pmunread"));
                    }
                } else returnCode = 12;
            } else if (net.dodian.utilities.DotEnvKt.getServerEnv().equals("dev") && net.dodian.utilities.DotEnvKt.getServerDebugMode()) {
                String newUserQuery = "INSERT INTO " + DbTables.WEB_USERS_TABLE + " SET username = '" + playerName + "', passworddate = '', birthday_search = ''";
                try (Connection conn2 = getDbConnection();
                     Statement stmt2 = conn2.createStatement()) {
                    stmt2.executeUpdate(newUserQuery);
                }
                return loadCharacterGame(p, playerName, playerPass);
            } else returnCode = 12;
        } catch (Exception e) {
            System.out.println("Failed to load player: " + playerName + ", " + e);
            returnCode = 13;
        }
        return returnCode;
    }

    public int loadgame(Client p, String playerName, String playerPass) {
        int loadCharacterResponse = loadCharacterGame(p, playerName, playerPass);
        if (loadCharacterResponse > 0) {
            return loadCharacterResponse;
        }
        if (p.playerGroup == 3) {
            return 12; // Not registered users!
        }

        try {
            String query = "SELECT * FROM " + DbTables.GAME_CHARACTERS + " WHERE id = '" + p.dbId + "'";

            // The main try-with-resources for the character lookup.
            // 'stmt' and 'results' will be closed automatically.
            try (java.sql.Connection conn = getDbConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet results = stmt.executeQuery(query)) {

                if (results.next()) { // --- CHARACTER EXISTS, LOAD THEIR DATA ---
                    if (isBanned(p.dbId)) return 4;
                    if (Login.isUidBanned(p.UUID)) return 22;

                    // Set look
                    String[] look = results.getString("look").isEmpty() ? null : results.getString("look").split(" ");
                    if (look == null || look.length != 13) {
                        p.lookNeeded = true;
                    } else {
                        int[] parts = new int[13];
                        for (int i = 0; i < look.length; i++)
                            parts[i] = Integer.parseInt(look[i]);
                        p.setLook(parts);
                    }

                    p.latestNews = results.getInt("news");
                    int x = results.getInt("x"), y = results.getInt("y"), z = results.getInt("height");
                    p.loginPosition(x, y, z);
                    if (x < 1 || y < 1) p.resetPos();
                    p.mutedTill = results.getLong("unmutetime");
                    p.fightType = results.getInt("fightStyle");
                    p.autocast_spellIndex = results.getInt("autocast");
                    int health = results.getInt("health");
                    String prayer = results.getString("prayer").trim();
                    String boosted = results.getString("boosted").trim();
                    String[] prayer_prase = prayer.split(":");
                    String[] boosted_prase = boosted.split(":");
                    int prayerLevel = !prayer.isEmpty() ? Integer.parseInt(prayer_prase[0]) : 0;

                    // Nested try-with-resources for the stats lookup.
                    String query2 = "SELECT * FROM " + DbTables.GAME_CHARACTERS_STATS + " WHERE uid = '" + p.dbId + "'";
                    try (java.sql.Connection conn2 = getDbConnection();
                         Statement stmt2 = conn2.createStatement();
                         ResultSet results2 = stmt2.executeQuery(query2)) {

                        if (results2.next()) {
                            final ResultSet finalResults2 = results2;
                            final int finalHealth = health;
                            final int finalPrayerLevel = prayerLevel;
                            Skill.enabledSkills().forEach(skill -> {
                                try {
                                    p.setExperience(finalResults2.getInt(skill.getName()), skill);
                                    p.setLevel(Skills.getLevelForExperience(p.getExperience(skill)), skill);
                                    if (skill == Skill.HITPOINTS) {
                                        p.maxHealth = Skills.getLevelForExperience(p.getExperience(skill));
                                        p.setCurrentHealth(finalHealth < 1 || finalHealth > p.maxHealth ? p.maxHealth : finalHealth);
                                    } else if (skill == Skill.PRAYER) {
                                        p.maxPrayer = Skills.getLevelForExperience(p.getExperience(skill));
                                        p.setCurrentPrayer(finalPrayerLevel < 0 || finalPrayerLevel > p.maxPrayer ? p.maxPrayer : finalPrayerLevel);
                                    }
                                } catch (SQLException e) {
                                    System.out.println("Error reading player stats: " + e);
                                }
                            });
                        } else { // Stats row missing, create it and set defaults.
                            try (java.sql.Connection tempConn = getDbConnection();
                                 Statement tempStmt = tempConn.createStatement()) {
                                String newStatsAccount = "INSERT INTO " + DbTables.GAME_CHARACTERS_STATS + "(uid) VALUES ('" + p.dbId + "')";
                                tempStmt.executeUpdate(newStatsAccount);
                            }
                            Skill.enabledSkills().forEach(skill -> {
                                p.setExperience(skill == Skill.HITPOINTS ? 1155 : 0, skill);
                                p.setLevel(skill == Skill.HITPOINTS ? 10 : 1, skill);
                            });
                            p.maxHealth = 10;
                            p.setCurrentHealth(10);
                            p.maxPrayer = 1;
                            p.setCurrentPrayer(1);
                        }
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

                    String inventory = (results.getString("inventory").trim());
                    if (!inventory.isEmpty()) {
                        for (String s : inventory.split(" ")) {
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

                    String equip = (results.getString("equipment")).trim();
                    if (!equip.isEmpty()) {
                        for (String s : equip.split(" ")) {
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
                                        p.send(new SendMessage("<col=FF0000>You dropped the " + Server.itemManager.getName(id).toLowerCase() + " on the floor!!!"));
                                    }
                                }
                            }
                        }
                    }

                    p.setTask(results.getString("slayerData"));
                    p.agilityCourseStage = results.getInt("agility");
                    p.setTravel(results.getString("travel"));

                    String unlocks = (results.getString("unlocks")).trim();
                    for (int i = 0; i < p.unlockLength; i++) {
                        if (!unlocks.isEmpty()) {
                            String[] parse = unlocks.split(":");
                            if (i < parse.length) {
                                p.addUnlocks(i, parse[i].split(","));
                            } else p.addUnlocks(i, "0", "0");
                        } else p.addUnlocks(i, "0", "0");
                    }

                    String bank = (results.getString("bank")).trim();
                    if (!bank.isEmpty()) {
                        for (String s : bank.split(" ")) {
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
                        if (songUnlocked[i].isEmpty()) continue;
                        p.setSongUnlocked(i, Integer.parseInt(songUnlocked[i]) == 1);
                    }

                    String[] friendsList = results.getString("friends").split(" ");
                    for (String friend : friendsList) {
                        if (!friend.isEmpty()) {
                            p.friends.add(new Friend(Long.parseLong(friend), true));
                        }
                    }

                    String bossLog = results.getString("Boss_Log");
                    if (bossLog == null) {
                        for (int i = 0; i < p.boss_name.length; i++) p.boss_amount[i] = 0;
                    } else {
                        for (String line : bossLog.split(" ")) {
                            String[] parts = line.split(":");
                            if (parts.length >= 2) p.bossCount(parts[0], Integer.parseInt(parts[1]));
                        }
                    }

                    String monsterLog = results.getString("Monster_Log");
                    if (monsterLog != null && !monsterLog.isEmpty()) {
                        for (String line : monsterLog.split(";")) {
                            String[] parts = line.split(",");
                            if (parts.length == 2) {
                                p.monsterName.add(parts[0]);
                                p.monsterCount.add(Integer.parseInt(parts[1]));
                            }
                        }
                    }

                    String effect = results.getString("effects");
                    if (effect != null && !effect.isEmpty()) {
                        String[] lines = effect.split(":");
                        for (int i = 0; i < lines.length; i++) {
                            p.effects.add(i, Integer.parseInt(lines[i]));
                        }
                    }

                    String daily = results.getString("dailyReward");
                    if (daily != null && !daily.isEmpty()) {
                        String[] lines = daily.split(";");
                        for (int i = 0; i < lines.length; i++) {
                            String[] parts = lines[i].split(",");
                            boolean newDay = p.dateDays(new Date(Long.parseLong(parts[0])), p.today) > 0;
                            if (i == 0) {
                                if (newDay) {
                                    p.dailyReward.add(0, p.today.getTime() + "");
                                    p.dailyReward.add(1, "6000");
                                    p.dailyReward.add(2, parts[2]);
                                    p.dailyReward.add(3, "0");
                                    p.dailyReward.add(4, parts[4]);
                                } else {
                                    for (int ii = 0; ii < parts.length; ii++) p.dailyReward.add(ii, parts[ii]);
                                }
                            } else {
                                for (int ii = 0; ii < parts.length; ii++) p.dailyReward.add(p.staffSize + ii, parts[ii]);
                            }
                        }
                    } else {
                        p.defaultDailyReward(p);
                    }

                    String farmingData = results.getString("farming");
                    if (farmingData != null && !farmingData.equals("[]")) {
                        p.farmingJson.farmingLoad(farmingData);
                    } else {
                        p.farmingJson.farmingLoad("");
                    }

                    // Give new players default items if they have an old account with no last login time
                    if (results.getLong("lastlogin") == 0) {
                        p.getEquipment()[Equipment.Slot.WEAPON.getId()] = 1277;
                        p.getEquipment()[Equipment.Slot.SHIELD.getId()] = 1171;
                        p.getEquipmentN()[Equipment.Slot.WEAPON.getId()] = 1;
                        p.getEquipmentN()[Equipment.Slot.SHIELD.getId()] = 1;
                        p.addItem(995, 2000);
                        p.addItem(1856, 1);
                        p.addItem(4155, 1);
                        p.checkItemUpdate();
                    }

                } else { // --- CHARACTER DOES NOT EXIST, CREATE A NEW ONE ---
                    // Use try-with-resources for the INSERT statements.
                    try (java.sql.Connection insertConn = getDbConnection();
                         Statement statement = insertConn.createStatement()) {
                        String newAccount = "INSERT INTO " + DbTables.GAME_CHARACTERS + "(id, name, equipment, inventory, bank, friends, songUnlocked)" + " VALUES ('"
                                + p.dbId + "', '" + playerName + "', '', '', '', '', '0')";
                        statement.executeUpdate(newAccount);
                        String newStatsAccount = "INSERT INTO " + DbTables.GAME_CHARACTERS_STATS + "(uid)" + " VALUES ('" + p.dbId + "') ON DUPLICATE KEY UPDATE uid=uid";
                        statement.executeUpdate(newStatsAccount);
                    }

                    // Now, set up the new player's default state directly on the player object.
                    p.lookNeeded = true;
                    p.resetPos();

                    // Set default stats
                    Skill.enabledSkills().forEach(skill -> {
                        p.setExperience(skill == Skill.HITPOINTS ? 1155 : 0, skill);
                        p.setLevel(skill == Skill.HITPOINTS ? 10 : 1, skill);
                    });
                    p.maxHealth = 10;
                    p.setCurrentHealth(10);
                    p.maxPrayer = 1;
                    p.setCurrentPrayer(1);

                    // Give default items
                    p.getEquipment()[Equipment.Slot.WEAPON.getId()] = 1277;
                    p.getEquipment()[Equipment.Slot.SHIELD.getId()] = 1171;
                    p.getEquipmentN()[Equipment.Slot.WEAPON.getId()] = 1;
                    p.getEquipmentN()[Equipment.Slot.SHIELD.getId()] = 1;
                    p.addItem(995, 2000);
                    p.addItem(1856, 1);
                    p.addItem(4155, 1);
                    p.checkItemUpdate();
                    p.defaultDailyReward(p);
                    p.farmingJson.farmingLoad("");
                }
            }

            // This finalization logic applies to both loaded and newly created players.
            p.lastSave = System.currentTimeMillis();
            p.start = System.currentTimeMillis();
            p.loadingDone = true;
            return 0; // Success

        } catch (Exception e) {
            System.out.println("A critical error occurred while loading player: " + playerName + ". Exception: " + e);
            e.printStackTrace();
            return 13; // Return a generic error code
        }

    }
    public void updatePlayerForumRegistration(Client p) {
        try (java.sql.Connection conn = getDbConnection();
             Statement statement = conn.createStatement()) {
            String newStatsAccount = "UPDATE " + DbTables.WEB_USERS_TABLE + " SET usergroupid='40' WHERE userid = '" + p.dbId + "'";
            statement.executeUpdate(newStatsAccount);
        } catch (Exception e) {
            System.out.println("Something wrong with updating a players forum rights " + e);
        }
        p.send(new SendMessage("You have now been registered to the forum! Enjoy your stay :D"));
    }

    public boolean isBanned(int id) throws SQLException {
        String query = "SELECT unbantime FROM " + DbTables.GAME_CHARACTERS + " WHERE id = '" + id + "'";

        try (java.sql.Connection conn = getDbConnection();
             Statement stmt = conn.createStatement();
             ResultSet results = stmt.executeQuery(query)) {

            // Check if a record was found
            if (results.next()) {
                long unbanTime = results.getLong("unbanTime");
                // A ban is active if the unban time is in the future.
                // (Assumes unbantime is 0 or in the past if not banned).
                return System.currentTimeMillis() < unbanTime;
            }
        }
        // If no record was found for the id, they are not banned.
        return false;
    }

}
