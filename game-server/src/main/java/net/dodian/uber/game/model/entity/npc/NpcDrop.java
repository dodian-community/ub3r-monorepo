/**
 *
 */
package net.dodian.uber.game.model.entity.npc;

import net.dodian.utilities.Misc;

/**
 * @author Owner
 *
 */
public class NpcDrop {

    private final int id;
    private final int minAmount;
    private final int maxAmount;
    private final double percent;
    private boolean rareShout;

    public NpcDrop(int id, int min, int max, double percent, boolean shout) {
        this.id = id;
        this.minAmount = min;
        this.maxAmount = max;
        this.percent = percent;
        this.rareShout = shout;
    }

    /**
     * Will this item drop?
     *
     * @return dropping or not
     */
    public boolean drop(boolean wealth) {
        return (Math.random() * 100) <= (wealth && percent <= 1.0 ? percent * 1.2 : percent < 10.0 ? percent * 1.1 : percent);
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the min and max amount dropped
     */
    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    /**
     * @return the amount
     */
    public int getAmount() {
        return minAmount == maxAmount ? minAmount : minAmount + Misc.random(maxAmount - minAmount);
    }

    /**
     * @return the chance
     */
    public double getChance() {
        return percent;
    }

    public boolean rareShout() {
        return rareShout;
    }

}
