package net.dodian.uber.game.model.player.skills;

import java.util.Arrays;

public class Skills {
    public static int getLevelForExperience(int exp) {
        double output = 0;
        int playerLevel = 0;
        for (int lvl = 2; lvl <= 100 && (int) output <= exp; lvl++) {
            output += (Math.floor((lvl - 1) + 300 * Math.pow(2.0, (double) (lvl - 1) / 7.0))) / 4.0;
            playerLevel++;
        }
        return playerLevel;
    }

    public static int getXPForLevel(int level) {
        double points = 0.0;
        int output = 0;
        for (int lvl = 1; lvl < level; lvl++) {
            points += Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
            output = (int) Math.floor(points / 4);
        }
        return output;
    }

    public static int maxTotalLevel() {
        return (((int) Skill.enabledSkills().count()) * 99) + (int) Skill.disabledSkills().count();
    }
}
