package net.dodian.uber.game.model;

import net.dodian.uber.game.model.entity.Entity;

import java.util.HashSet;
import java.util.Set;

/**
 * A class that manages update flags using bitwise operations for better performance.
 * This implementation is more efficient than using an EnumMap as it uses a single
 * integer to track all flags.
 */
public class UpdateFlags {
    
    /**
     * The bitmask containing all the flags.
     * Each bit represents the state of a flag (1 = set, 0 = not set).
     */
    private int flags;
    
    /**
     * Creates a new UpdateFlags instance with all flags cleared.
     */
    public UpdateFlags() {
        this.flags = 0;
    }
    
    /**
     * Checks if any flag is set.
     * 
     * @return true if any flag is set, false otherwise.
     */
    public boolean isUpdateRequired() {
        return flags != 0;
    }
    
    /**
     * Checks if the specified flag is set.
     * 
     * @param flag The flag to check.
     * @return true if the flag is set, false otherwise.
     */
    public boolean isRequired(UpdateFlag flag) {
        return (flags & flag.getMask()) != 0;
    }
    
    /**
     * Sets the specified flag to the given value.
     * 
     * @param flag The flag to set or clear.
     * @param required true to set the flag, false to clear it.
     */
    public void setRequired(UpdateFlag flag, boolean required) {
        if (required) {
            flags |= flag.getMask();
        } else {
            flags &= ~flag.getMask();
        }
    }
    
    /**
     * Sets the specified flag.
     * 
     * @param flag The flag to set.
     */
    public void set(UpdateFlag flag) {
        flags |= flag.getMask();
    }
    
    /**
     * Clears the specified flag.
     * 
     * @param flag The flag to clear.
     */
    public void clear(UpdateFlag flag) {
        flags &= ~flag.getMask();
    }
    
    /**
     * Toggles the specified flag.
     * 
     * @param flag The flag to toggle.
     */
    public void toggle(UpdateFlag flag) {
        flags ^= flag.getMask();
    }
    
    /**
     * Clears all flags.
     */
    public void clear() {
        flags = 0;
    }
    
    /**
     * Gets the raw flags value.
     * 
     * @return The raw flags value.
     */
    /**
     * Gets the raw flags value.
     * 
     * @return The raw flags value.
     */
    public int getValue() {
        return flags;
    }
    
    /**
     * Backward compatibility method to check if a flag is set.
     * 
     * @param flag The flag to check.
     * @return true if the flag is set, false otherwise.
     */
    public boolean get(UpdateFlag flag) {
        return isRequired(flag);
    }
    
    /**
     * Backward compatibility method to get all flags.
     * 
     * @return A set of all UpdateFlag values.
     */
    public Set<UpdateFlag> keySet() {
        Set<UpdateFlag> result = new HashSet<>();
        for (UpdateFlag flag : UpdateFlag.values()) {
            if (isRequired(flag)) {
                result.add(flag);
            }
        }
        return result;
    }
    
    /**
     * Gets the mask for the specified entity type.
     * 
     * @param type The entity type.
     * @return The mask for the entity type.
     */
    public int getMask(Entity.Type type) {
        int mask = 0;
        
        // Iterate through all flags and build the mask
        for (UpdateFlag flag : UpdateFlag.values()) {
            if (isRequired(flag)) {
                try {
                    mask |= flag.getMask(type);
                } catch (IllegalStateException e) {
                    // Skip flags that don't have a mask for this entity type
                    continue;
                }
            }
        }
        
        return mask;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("UpdateFlags[");
        boolean first = true;
        
        for (UpdateFlag flag : UpdateFlag.values()) {
            if (isRequired(flag)) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(flag.name());
                first = false;
            }
        }
        
        sb.append("]");
        return sb.toString();
    }
}
