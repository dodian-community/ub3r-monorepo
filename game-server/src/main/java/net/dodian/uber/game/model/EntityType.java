package net.dodian.uber.game.model;

/**
 * Represents different types of entities in the game world.
 * Used for segregating entities in chunk storage.
 */
public enum EntityType {
    /**
     * Player entity type.
     */
    PLAYER,

    /**
     * NPC entity type.
     */
    NPC,

    /**
     * Static object entity type.
     */
    OBJECT,

    /**
     * Ground item entity type.
     */
    GROUND_ITEM;
}
