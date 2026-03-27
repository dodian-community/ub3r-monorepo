package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.persistence.player.PlayerSaveSegment;

final class PlayerStats {
    private final Player owner;
    private final int[] playerLevel = new int[21];
    private final int[] playerXP = new int[21];

    PlayerStats(Player owner) {
        this.owner = owner;
    }

    int getLevel(Skill skill) {
        return playerLevel[skill.getId()];
    }

    int getExperience(Skill skill) {
        return playerXP[skill.getId()];
    }

    void addExperience(int experience, Skill skill) {
        playerXP[skill.getId()] += experience;
    }

    void setLevel(int level, Skill skill) {
        playerLevel[skill.getId()] = level;
    }

    void setExperience(int experience, Skill skill) {
        playerXP[skill.getId()] = experience;
    }

    int getCurrentHealth() {
        return owner.currentHealth;
    }

    int getMaxHealth() {
        return owner.maxHealth;
    }

    void setCurrentHealth(int currentHealth) {
        owner.currentHealth = currentHealth;
    }

    void heal(int healing) {
        heal(healing, 0);
    }

    void heal(int healing, int overHeal) {
        Client c = (Client) owner;
        int maxLevel = getMaxHealth() + overHeal;
        setCurrentHealth(Math.min(getCurrentHealth() + healing, maxLevel));
        c.refreshSkill(Skill.HITPOINTS);
    }

    void eat(int healing, int removeId, int removeSlot) {
        Client c = (Client) owner;
        if (c.deathStage > 0 || c.deathTimer > 0 || c.getCurrentHealth() < 1) {
            return;
        }
        if (getCurrentHealth() < getMaxHealth()) {
            c.performAnimation(829, 0);
            c.deleteItem(removeId, removeSlot, 1);
            c.send(new net.dodian.uber.game.netty.listener.out.SendMessage("You eat the " + Server.itemManager.getName(removeId).toLowerCase() + "."));
            heal(healing);
        } else {
            c.send(new net.dodian.uber.game.netty.listener.out.SendMessage("You have full health already, so you spare the " + Server.itemManager.getName(removeId).toLowerCase() + " for later."));
        }
    }

    void boost(int boosted, Skill skill) {
        if (skill == null || skill == Skill.HITPOINTS || skill == Skill.PRAYER) {
            return;
        }

        Client c = (Client) owner;
        int lvl = Skills.getLevelForExperience(getExperience(skill));
        int currentLevel = c.getLevel(skill);
        boosted = currentLevel >= lvl + boosted ? currentLevel - lvl : boosted;
        owner.boostedLevel[skill.getId()] = boosted;
        c.refreshSkill(skill);
        c.markSaveDirty(PlayerSaveSegment.STATS.getMask());
    }

    int getCurrentPrayer() {
        return owner.currentPrayer;
    }

    int getMaxPrayer() {
        return owner.maxPrayer;
    }

    void setCurrentPrayer(int amount) {
        owner.currentPrayer = amount;
    }

    void drainPrayer(int amount) {
        pray(-amount);
        if (getCurrentPrayer() <= 0) {
            setCurrentPrayer(0);
            owner.prayers.reset();
            ((Client) owner).send(new net.dodian.uber.game.netty.listener.out.SendMessage("<col=8B8000>Your prayer has ran out! Please recharge at a nearby altar!"));
        }
    }

    void pray(int healing) {
        Client c = (Client) owner;
        int maxLevel = getMaxPrayer();
        setCurrentPrayer(Math.min(getCurrentPrayer() + healing, maxLevel));
        c.refreshSkill(Skill.PRAYER);
    }

    int determineCombatLevel() {
        int magLvl = Skills.getLevelForExperience(getExperience(Skill.MAGIC));
        int ranLvl = Skills.getLevelForExperience(getExperience(Skill.RANGED));
        int attLvl = Skills.getLevelForExperience(getExperience(Skill.ATTACK));
        int strLvl = Skills.getLevelForExperience(getExperience(Skill.STRENGTH));
        int defLvl = Skills.getLevelForExperience(getExperience(Skill.DEFENCE));
        int hitLvl = Skills.getLevelForExperience(getExperience(Skill.HITPOINTS));
        int prayLvl = Skills.getLevelForExperience(getExperience(Skill.PRAYER));
        double mag = magLvl * 1.5;
        double ran = ranLvl * 1.5;
        double attstr = attLvl + strLvl;
        double combatLevel;
        if (ran > attstr && ran > mag) {
            combatLevel = ((double) defLvl * 0.25) + ((double) hitLvl * 0.25) + ((double) (prayLvl / 2) * 0.25) + ((double) ranLvl * 0.4875);
        } else if (mag > attstr) {
            combatLevel = ((double) defLvl * 0.25) + ((double) hitLvl * 0.25) + ((double) (prayLvl / 2) * 0.25) + ((double) magLvl * 0.4875);
        } else {
            combatLevel = ((double) defLvl * 0.25) + ((double) hitLvl * 0.25) + ((double) (prayLvl / 2) * 0.25) + ((double) attLvl * 0.325) + ((double) strLvl * 0.325);
        }
        return owner.customCombat != -1 ? owner.customCombat : (int) combatLevel;
    }

    int getSkillLevel(Skill skill) {
        return Skills.getLevelForExperience(getExperience(skill));
    }
}
