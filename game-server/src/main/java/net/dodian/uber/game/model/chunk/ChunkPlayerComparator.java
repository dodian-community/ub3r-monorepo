package net.dodian.uber.game.model.chunk;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Player;

import java.util.Comparator;

/**
 * Comparator for prioritizing players when nearby lists exceed limits.
 * Currently weights by Chebyshev distance (closer players first).
 * Additional weighting (friends/size/combat) can be layered later.
 */
public final class ChunkPlayerComparator implements Comparator<Player> {

    /**
     * The player whose surroundings are being prioritized.
     */
    private final Player viewer;

    /**
     * Creates a new comparator for a given viewer.
     *
     * @param viewer Player whose proximity determines ordering
     */
    public ChunkPlayerComparator(Player viewer) {
        this.viewer = viewer;
    }

    @Override
    public int compare(Player left, Player right) {
        if (left == right) {
            return 0;
        }

        int leftWeight = 0;
        int rightWeight = 0;

        Position viewerPos = viewer != null ? viewer.getPosition() : null;

        int leftDistance = computeLongestDistance(viewerPos, left.getPosition());
        int rightDistance = computeLongestDistance(viewerPos, right.getPosition());

        if (leftDistance < rightDistance) {
            leftWeight += 3;
        } else if (rightDistance < leftDistance) {
            rightWeight += 3;
        }

        int weightCompare = Integer.compare(rightWeight, leftWeight);
        if (weightCompare != 0) {
            // Higher weight = higher priority = earlier in ordering.
            return weightCompare;
        }

        // Stable tie-breaker so sorted sets/lists keep deterministic ordering.
        return Integer.compare(left.getSlot(), right.getSlot());
    }

    /**
     * Computes Chebyshev (max axis) distance between two positions.
     */
    private int computeLongestDistance(Position a, Position b) {
        if (a == null || b == null) {
            return Integer.MAX_VALUE;
        }

        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());

        return Math.max(dx, dy);
    }
}
