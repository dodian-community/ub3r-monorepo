package net.dodian.uber.game.model.entity.player;

import java.util.Objects;

/** Immutable absolute-coordinate waypoint consumed by player movement. */
public final class MovementPoint {
    private final int x;
    private final int y;
    private final int z;

    public MovementPoint(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MovementPoint)) return false;
        MovementPoint point = (MovementPoint) other;
        return x == point.x && y == point.y && z == point.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "MovementPoint{x=" + x + ", y=" + y + ", z=" + z + '}';
    }
}
