/**
 *
 */
package net.dodian.uber.game.model.entity.npc;

/**
 * @author Owner
 *
 */
public class NpcDrop {

    private final int id;
    private final int minAmount;
    private final int maxAmount;
    private final double percent;
    private final boolean rareShout;

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
        return NpcDropMath.shouldDrop(percent, wealth);
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
        return NpcDropMath.rollAmount(minAmount, maxAmount);
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
