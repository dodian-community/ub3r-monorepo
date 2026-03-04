package net.dodian.uber.game.model;

import net.dodian.uber.game.model.chunk.Chunk;
import net.dodian.utilities.Misc;

public class Position {

    private static final int X_BITS = 14;
    private static final int Y_BITS = 14;
    private static final int Z_BITS = 4;
    private static final int Y_SHIFT = Z_BITS;
    private static final int X_SHIFT = Y_SHIFT + Y_BITS;
    private static final int X_MASK = (1 << X_BITS) - 1;
    private static final int Y_MASK = (1 << Y_BITS) - 1;
    private static final int Z_MASK = (1 << Z_BITS) - 1;
    private static final int MIN_COORDINATE = -1;
    private static final int MAX_COORDINATE = X_MASK - 1;
    private static final int MIN_HEIGHT = 0;
    private static final int MAX_HEIGHT = Z_MASK;

    private int packed;

    public Position(int x, int y, int z) {
        this.packed = pack(x, y, z);
    }

    public Position(int x, int y) {
        this(x, y, 0);
    }

    public Position() {
        this(2611, 3093, 0);
    }

    public int getX() {
        return unpackCoordinate(packed >> X_SHIFT);
    }

    public int getY() {
        return unpackCoordinate(packed >> Y_SHIFT);
    }

    public int getZ() {
        return packed & Z_MASK;
    }

    public void setZ(int z) {
        packed = pack(getX(), getY(), z);
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
        packed = pack(x, y, z);
    }

    public void moveTo(int x, int y) {
        moveTo(x, y, getZ());
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
        packed = pack(getX() + amountX, getY() + amountY, getZ() + amountZ);
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
        return new Position(getX(), getY(), getZ());
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
        return getX() - 8 * base.getRegionX();
    }

    /**
     * Gets the local {@code Y} coordinate relative to {@code base}.
     *
     * @param base the relative base position.
     * @return the local {@code Y} coordinate.
     */
    public final int getLocalY(Position base) {
        return getY() - 8 * base.getRegionY();
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
        return (getX() >> 3) - 6;
    }

    /**
     * Gets the {@code Y} coordinate of the region containing this position.
     *
     * @return the {@code Y} coordinate of the region
     */
    public final int getRegionY() {
        return (getY() >> 3) - 6;
    }

    /**
     * Gets the chunk that contains this position.
     * Chunk coordinates are calculated as: (pos / 8) - 6
     *
     * @return The chunk containing this position
     */
    public Chunk getChunk() {
        return new Chunk(getChunkX(), getChunkY());
    }

    public int getChunkX() {
        return (getX() >> 3) - 6;
    }

    public int getChunkY() {
        return (getY() >> 3) - 6;
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
        int thisX = getX();
        int thisY = getY();
        int thisZ = getZ();
        int otherX = other.getX();
        int otherY = other.getY();
        if (thisZ != other.getZ()) {
            return false;
        }
        return Math.abs(otherX - thisX) <= amount && Math.abs(otherY - thisY) <= amount;
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
        return new Position(b.getX() - a.getX(), b.getY() - a.getY());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Position)) {
            return false;
        }
        Position other = (Position) o;
        return packed == other.packed;
    }

    @Override
    public int hashCode() {
        return packed;
    }

    public String toString() {
        return "x=" + getX() + " y=" + getY() + " z=" + getZ();
    }

    private static int pack(int x, int y, int z) {
        int encodedX = encodeCoordinate("x", x);
        int encodedY = encodeCoordinate("y", y);
        int encodedZ = encodeHeight(z);
        return (encodedX << X_SHIFT) | (encodedY << Y_SHIFT) | encodedZ;
    }

    private static int encodeCoordinate(String axis, int value) {
        if (value < MIN_COORDINATE || value > MAX_COORDINATE) {
            throw new IllegalArgumentException("Position " + axis + " out of range: " + value);
        }
        return value + 1;
    }

    private static int unpackCoordinate(int encoded) {
        return (encoded & X_MASK) - 1;
    }

    private static int encodeHeight(int value) {
        if (value < MIN_HEIGHT || value > MAX_HEIGHT) {
            throw new IllegalArgumentException("Position z out of range: " + value);
        }
        return value;
    }

}
