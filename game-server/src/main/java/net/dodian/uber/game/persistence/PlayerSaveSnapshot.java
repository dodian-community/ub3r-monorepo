package net.dodian.uber.game.persistence;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Friend;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.prayer.Prayers;
import net.dodian.utilities.DbTables;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class PlayerSaveSnapshot {

    private final long sequence;
    private final long createdAt;
    private final int dbId;
    private final String playerName;
    private final PlayerSaveReason reason;
    private final boolean updateProgress;
    private final boolean finalSave;

    private final String statsUpdateSql;
    private final String statsProgressInsertSql;
    private final String characterUpdateSql;

    private PlayerSaveSnapshot(long sequence,
                               long createdAt,
                               int dbId,
                               String playerName,
                               PlayerSaveReason reason,
                               boolean updateProgress,
                               boolean finalSave,
                               String statsUpdateSql,
                               String statsProgressInsertSql,
                               String characterUpdateSql) {
        this.sequence = sequence;
        this.createdAt = createdAt;
        this.dbId = dbId;
        this.playerName = playerName;
        this.reason = reason;
        this.updateProgress = updateProgress;
        this.finalSave = finalSave;
        this.statsUpdateSql = statsUpdateSql;
        this.statsProgressInsertSql = statsProgressInsertSql;
        this.characterUpdateSql = characterUpdateSql;
    }

    public static PlayerSaveSnapshot fromClient(Client client,
                                                long sequence,
                                                PlayerSaveReason reason,
                                                boolean updateProgress,
                                                boolean finalSave) {
        long allXp = Skill.enabledSkills()
                .mapToInt(client::getExperience)
                .sum();

        int totalLevel = client.totalLevel();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        StringBuilder statsQuery = new StringBuilder("UPDATE " + DbTables.GAME_CHARACTERS_STATS +
                " SET total=" + totalLevel + ", combat=" + client.determineCombatLevel() + ", ");
        StringBuilder progressQuery = new StringBuilder("INSERT INTO " + DbTables.GAME_CHARACTERS_STATS_PROGRESS +
                " SET updated='" + timeStamp + "', total=" + totalLevel + ", combat=" +
                client.determineCombatLevel() + ", uid=" + client.dbId + ", ");

        Skill.enabledSkills().forEach(skill -> {
            statsQuery.append(skill.getName()).append("=").append(client.getExperience(skill)).append(", ");
            progressQuery.append(skill.getName()).append("=").append(client.getExperience(skill)).append(", ");
        });

        statsQuery.append("totalxp=").append(allXp).append(" WHERE uid=").append(client.dbId);
        progressQuery.append("totalxp=").append(allXp);

        StringBuilder inventory = new StringBuilder();
        StringBuilder equipment = new StringBuilder();
        StringBuilder bank = new StringBuilder();
        StringBuilder list = new StringBuilder();
        StringBuilder bossLog = new StringBuilder();
        StringBuilder monsterLog = new StringBuilder();
        StringBuilder effect = new StringBuilder();
        StringBuilder dailyReward = new StringBuilder();
        StringBuilder prayer = new StringBuilder();
        StringBuilder boosted = new StringBuilder();

        for (int i = 0; i < client.playerItems.length; i++) {
            if (client.playerItems[i] > 0) {
                inventory.append(i)
                        .append("-")
                        .append(client.playerItems[i] - 1)
                        .append("-")
                        .append(client.playerItemsN[i])
                        .append(" ");
            }
        }

        for (int i = 0; i < client.bankItems.length; i++) {
            if (client.bankItems[i] > 0) {
                bank.append(i)
                        .append("-")
                        .append(client.bankItems[i] - 1)
                        .append("-")
                        .append(client.bankItemsN[i])
                        .append(" ");
            }
        }

        for (int i = 0; i < client.getEquipment().length; i++) {
            if (client.getEquipment()[i] > 0) {
                equipment.append(i)
                        .append("-")
                        .append(client.getEquipment()[i])
                        .append("-")
                        .append(client.getEquipmentN()[i])
                        .append(" ");
            }
        }

        for (int i = 0; i < client.boss_name.length; i++) {
            if (client.boss_amount[i] >= 0) {
                bossLog.append(client.boss_name[i]).append(":").append(client.boss_amount[i]).append(" ");
            }
        }

        for (int i = 0; i < client.effects.size(); i++) {
            effect.append(client.effects.get(i)).append(i == client.effects.size() - 1 ? "" : ":");
        }

        for (int i = 0; i < client.monsterName.size(); i++) {
            monsterLog.append(client.monsterName.get(i))
                    .append(",")
                    .append(client.monsterCount.get(i))
                    .append(i == client.monsterName.size() - 1 ? "" : ";");
        }

        for (int i = 0; i < client.staffSize; i++) {
            dailyReward.append(client.dailyReward.get(i)).append(
                    i == client.staffSize - 1 && client.dailyReward.size() <= client.staffSize
                            ? ""
                            : i == client.staffSize - 1 ? ";" : ",");
        }

        prayer.append(client.getCurrentPrayer());
        for (Prayers.Prayer pray : Prayers.Prayer.values()) {
            if (client.getPrayerManager().isPrayerOn(pray)) {
                prayer.append(":").append(pray.getButtonId());
            }
        }

        boosted.append(client.lastRecover);
        for (int boost : client.boostedLevel.clone()) {
            boosted.append(":").append(boost);
        }

        int num = 0;
        for (Friend f : client.friends) {
            if (f.name > 0 && num < 200) {
                list.append(f.name).append(" ");
                num++;
            }
        }

        String last = "";
        long elapsed = System.currentTimeMillis() - client.session_start;
        if (elapsed > 10000) {
            last = ", lastlogin = '" + System.currentTimeMillis() + "'";
        }

        String characterQuery = "UPDATE " + DbTables.GAME_CHARACTERS +
                " SET pkrating=" + 1500 +
                ", health=" + client.getCurrentHealth() +
                ", equipment='" + equipment + "', inventory='" + inventory + "', bank='" + bank +
                "', friends='" + list + "', fightStyle = " + client.fightType +
                ", slayerData='" + client.saveTaskAsString() + "', essence_pouch='" + client.getPouches() +
                "', effects='" + effect + "'" +
                ", autocast=" + client.autocast_spellIndex +
                ", news=" + client.latestNews +
                ", agility = '" + client.agilityCourseStage + "', height = " + client.getPosition().getZ() +
                ", x = " + client.getPosition().getX() +
                ", y = " + client.getPosition().getY() +
                ", lastlogin = '" + System.currentTimeMillis() + "', Monster_Log='" + monsterLog +
                "', farming = '" + client.farmingJson.farmingSave() + "', dailyReward = '" + dailyReward +
                "',Boss_Log='" + bossLog +
                "', songUnlocked='" + client.getSongUnlockedSaveText() +
                "', travel='" + client.saveTravelAsString() +
                "', look='" + client.getLook() +
                "', unlocks='" + client.saveUnlocksAsString() + "'" +
                ", prayer='" + prayer + "', boosted='" + boosted + "'" + last +
                " WHERE id = " + client.dbId;

        return new PlayerSaveSnapshot(
                sequence,
                System.currentTimeMillis(),
                client.dbId,
                client.getPlayerName(),
                reason,
                updateProgress,
                finalSave,
                statsQuery.toString(),
                updateProgress ? progressQuery.toString() : null,
                characterQuery
        );
    }

    public static PlayerSaveSnapshot forSql(long sequence,
                                            int dbId,
                                            String playerName,
                                            PlayerSaveReason reason,
                                            boolean updateProgress,
                                            boolean finalSave,
                                            String statsUpdateSql,
                                            String statsProgressInsertSql,
                                            String characterUpdateSql) {
        return new PlayerSaveSnapshot(
                sequence,
                System.currentTimeMillis(),
                dbId,
                playerName,
                reason,
                updateProgress,
                finalSave,
                statsUpdateSql,
                statsProgressInsertSql,
                characterUpdateSql
        );
    }

    public long getSequence() {
        return sequence;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getDbId() {
        return dbId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public PlayerSaveReason getReason() {
        return reason;
    }

    public boolean isUpdateProgress() {
        return updateProgress;
    }

    public boolean isFinalSave() {
        return finalSave;
    }

    public String getStatsUpdateSql() {
        return statsUpdateSql;
    }

    public String getStatsProgressInsertSql() {
        return statsProgressInsertSql;
    }

    public String getCharacterUpdateSql() {
        return characterUpdateSql;
    }
}
