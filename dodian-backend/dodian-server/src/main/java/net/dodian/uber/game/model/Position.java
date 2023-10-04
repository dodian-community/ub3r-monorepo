package net.dodian.uber.game.model;

import net.dodian.utilities.Misc;

public class Position {

    private int x, y, z;

    public Position(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Position(int x, int y) {
        this(x, y, 0);
    }

    public Position() {
        this(2611, 3093, 0);
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public double getDistance(Position position) {
        int difX = Math.abs(this.getX() - position.getX());
        int difY = Math.abs(this.getY() - position.getY());
        return Math.sqrt(Math.pow(difX, 2) + Math.pow(difY, 2));
    }

    public boolean isWithinRange(Position position, double threshold) {
        return getDistance(position) <= threshold;
    }

    public void moveTo(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void moveTo(int x, int y) {
        moveTo(x, y, this.z);
    }

    /**
     * Increments the {@code X}, {@code Y}, and {@code Z} coordinate values
     * within this container by {@code amountX}, {@code amountY}, and
     * {@code amountZ}.
     *
     * @param amountX the amount to increment the {@code X} coordinate by.
     * @param amountY the amount to increment the {@code Y} coordinate by.
     * @param amountZ the amount to increment the {@code Z} coordinate by.
     * @return an instance of this position.
     */
    public final Position move(int amountX, int amountY, int amountZ) {
        this.x += amountX;
        this.y += amountY;
        this.z += amountZ;
        return this;
    }

    /**
     * Increments the {@code X} and {@code Y} coordinate values within this
     * container by {@code amountX} and {@code amountY}.
     *
     * @param amountX the amount to increment the {@code X} coordinate by.
     * @param amountY the amount to increment the {@code Y} coordinate by.
     * @return an instance of this position.
     */
    public final Position move(int amountX, int amountY) {
        return move(amountX, amountY, 0);
    }

    /**
     * A substitute for {@link Object#clone()} that creates another 'copy' of
     * this instance. The created copy <i>safe</i> meaning it does not hold
     * <b>any</b> references to the original instance.
     *
     * @return the copy of this instance that does not hold any references.
     */
    public Position copy() {
        return new Position(x, y, z);
    }


    public boolean isPerpendicularTo(Position other) {
        Position delta = Misc.delta(this, other);
        return delta.getX() != delta.getY() && delta.getX() == 0 || delta.getY() == 0;
    }

    /**
     * Gets the local {@code X} coordinate relative to {@code base}.
     *
     * @param base the relative base position.
     * @return the local {@code X} coordinate.
     */
    public final int getLocalX(Position base) {
        return x - 8 * base.getRegionX();
    }

    /**
     * Gets the local {@code Y} coordinate relative to {@code base}.
     *
     * @param base the relative base position.
     * @return the local {@code Y} coordinate.
     */
    public final int getLocalY(Position base) {
        return y - 8 * base.getRegionY();
    }

    /**
     * Gets the local {@code X} coordinate relative to this position.
     *
     * @return the local {@code X} coordinate.
     */
    public final int getLocalX() {
        return getLocalX(this);
    }

    /**
     * Gets the local {@code Y} coordinate relative to this Position.
     *
     * @return the local {@code Y} coordinate.
     */
    public final int getLocalY() {
        return getLocalY(this);
    }

    /**
     * Gets the {@code X} coordinate of the region containing this position.
     *
     * @return the {@code X} coordinate of the region.
     */
    public final int getRegionX() {
        return (x >> 3) - 6;
    }

    /**
     * Gets the {@code Y} coordinate of the region containing this position.
     *
     * @return the {@code Y} coordinate of the region
     */
    public final int getRegionY() {
        return (y >> 3) - 6;
    }

    /**
     * Determines if this position is within {@code amount} distance of
     * {@code other}.
     *
     * @param other  the position to check the distance for.
     * @param amount the distance to check.
     * @return {@code true} if this position is within the distance,
     * {@code false} otherwise.
     */
    public final boolean withinDistance(Position other, int amount) {
        if (this.z != other.z)
            return false;
        return Math.abs(other.x - this.x) <= amount && Math.abs(other.y - this.y) <= amount;
    }

    /**
     * Returns the delta coordinates. Note that the returned position is not an
     * actual position, instead it's values represent the delta values between
     * the two arguments.
     *
     * @param a the first position.
     * @param b the second position.
     * @return the delta coordinates contained within a position.
     */
    public static Position delta(Position a, Position b) {
        return new Position(b.x - a.x, b.y - a.y);
    }

    @Override
    public boolean equals(Object o) {
        Position other = (Position) o;
        return other.getX() == this.getX() && other.getY() == this.getY() && other.getZ() == this.getZ();
    }

    public String toString() {
        return "x=" + this.x + " y=" + this.y + " z=" + this.z;
    }

}
