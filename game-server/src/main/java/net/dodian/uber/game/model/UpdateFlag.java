package net.dodian.uber.game.model;

import net.dodian.uber.game.model.entity.Entity;

/**
 * Represents a single update flag with bit masks for both player and NPC updates.
 * Uses bitwise operations for efficient flag management.
 */
public enum UpdateFlag {
    /**
     * The order of these enums is important as it determines the bit position.
     * Each flag uses a unique bit position for efficient bitwise operations.
     */
    FACE_CHARACTER(0),    // 1 << 0 = 1
    FACE_COORDINATE(1),   // 1 << 1 = 2
    CHAT(2),              // 1 << 2 = 4
    HIT(3),               // 1 << 3 = 8
    ANIM(4),              // 1 << 4 = 16
    FORCED_CHAT(5),       // 1 << 5 = 32
    HIT2(6),              // 1 << 6 = 64
    GRAPHICS(7),          // 1 << 7 = 128
    APPEARANCE(8),        // 1 << 8 = 256
    FORCED_MOVEMENT(9),   // 1 << 9 = 512
    DUMMY(10);            // 1 << 10 = 1024
    
    // Add more flags as needed, up to 31 for int-based bitmask

    /**
     * The bit position for this flag.
     */
    private final int bitPosition;
    
    /**
     * The bit mask for this flag (1 << bitPosition).
     */
    private final int mask;
    
    /**
     * The player mask value for this flag.
     */
    private int playerMask;
    
    /**
     * The NPC mask value for this flag.
     */
    private int npcMask;

    /**
     * Creates a new UpdateFlag with the given bit position.
     * The mask values will be set to default (1 << bitPosition).
     * 
     * @param bitPosition The bit position for this flag (0-30).
     */
    UpdateFlag(int bitPosition) {
        this(bitPosition, 1 << bitPosition, 1 << bitPosition);
    }
    
    /**
     * Creates a new UpdateFlag with the given bit position and custom masks.
     * 
     * @param bitPosition The bit position for this flag.
     * @param playerMask The player mask value.
     * @param npcMask The NPC mask value.
     */
    UpdateFlag(int bitPosition, int playerMask, int npcMask) {
        if (bitPosition < 0 || bitPosition > 30) {
            throw new IllegalArgumentException("Bit position must be between 0 and 30 (inclusive)");
        }
        this.bitPosition = bitPosition;
        this.mask = 1 << bitPosition;
        this.playerMask = playerMask;
        this.npcMask = npcMask;
    }

    /**
     * Gets the bit position of this flag.
     * @return The bit position.
     */
    public int getBitPosition() {
        return bitPosition;
    }
    
    /**
     * Gets the bit mask for this flag.
     * @return The bit mask.
     */
    public int getMask() {
        return mask;
    }

    /**
     * Gets the appropriate mask for the given entity type.
     * 
     * @param type The entity type (PLAYER or NPC).
     * @return The mask value for the entity type.
     * @throws IllegalArgumentException if the entity type is not supported.
     * @throws IllegalStateException if the mask for the entity type is not set.
     */
    public int getMask(Entity.Type type) {
        if (type == Entity.Type.PLAYER) {
            if (playerMask == -1) {
                throw new IllegalStateException("Player mask not set for " + this);
            }
            return playerMask;
        } else if (type == Entity.Type.NPC) {
            if (npcMask == -1) {
                throw new IllegalStateException("NPC mask not set for " + this);
            }
            return npcMask;
        }
        throw new IllegalArgumentException("Unsupported entity type: " + type);
    }
    
    /**
     * Sets the mask values for this flag.
     * 
     * @param playerMask The player mask value.
     * @param npcMask The NPC mask value.
     */
    public void setMasks(int playerMask, int npcMask) {
        this.playerMask = playerMask;
        this.npcMask = npcMask;
    }
    
    // Initialize the masks for each flag
    static {
        FACE_CHARACTER.setMasks(0x1, 0x20);
        FACE_COORDINATE.setMasks(0x2, 0x4);
        CHAT.setMasks(0x80, -1);
        HIT.setMasks(0x20, 0x40);
        ANIM.setMasks(0x8, 0x10);
        FORCED_CHAT.setMasks(0x4, 0x1);
        HIT2.setMasks(0x200, 0x8);
        GRAPHICS.setMasks(0x100, 0x80);
        APPEARANCE.setMasks(0x10, 0x2);
        FORCED_MOVEMENT.setMasks(0x400, -1);
        DUMMY.setMasks(0, 0);
    }
}