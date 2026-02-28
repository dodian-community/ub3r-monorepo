package net.dodian.uber.game.model.chunk;

/**
 * Represents an 8x8 tile chunk in the game world.
 * Chunks are used for spatial partitioning to optimize entity lookups.
 *
 * Coordinate system:
 * - Chunk coordinates are relative to (-6, -6) origin
 * - Absolute world coordinates = SIZE * (chunk_coord + 6)
 * - SIZE = 8 tiles
 */
public final class Chunk {

    /**
     * Size of a chunk in tiles (8x8).
     */
    public static final int SIZE = 8;

    /**
     * Chunk X coordinate (relative to origin-6).
     */
    private final int x;

    /**
     * Chunk Y coordinate (relative to origin-6).
     */
    private final int y;

    /**
     * Creates a new chunk at the specified coordinates.
     *
     * @param x The chunk X coordinate
     * @param y The chunk Y coordinate
     */
    public Chunk(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Gets the chunk X coordinate.
     */
    public int getX() {
        return x;
    }

    /**
     * Gets the chunk Y coordinate.
     */
    public int getY() {
        return y;
    }

    /**
     * Gets the absolute world X coordinate of the chunk's bottom-left corner.
     */
    public int getAbsX() {
        return SIZE * (x + 6);
    }

    /**
     * Gets the absolute world Y coordinate of the chunk's bottom-left corner.
     */
    public int getAbsY() {
        return SIZE * (y + 6);
    }

    /**
     * Creates a new chunk translated by the given offsets.
     *
     * @param dx X offset
     * @param dy Y offset
     * @return New translated chunk
     */
    public Chunk translate(int dx, int dy) {
        return new Chunk(x + dx, y + dy);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Chunk)) return false;
        Chunk other = (Chunk) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    @Override
    public String toString() {
        return "Chunk(" + x + ", " + y + ")";
    }
}
