package net.dodian.uber.game.model.entity.player;

import java.util.BitSet;

/**
 * Slot-indexed membership set for local player sync state.
 *
 * Uses slot bits instead of hash-based membership to avoid per-tick
 * allocation/iterator churn in synchronization hot paths.
 */
public final class PlayerSlotMembershipSet {
    private final BitSet slots;
    private int size;

    public PlayerSlotMembershipSet(int capacity) {
        this.slots = new BitSet(Math.max(1, capacity));
        this.size = 0;
    }

    public boolean add(Player player) {
        if (player == null) {
            return false;
        }
        int slot = player.getSlot();
        if (slot < 0) {
            return false;
        }
        if (slots.get(slot)) {
            return false;
        }
        slots.set(slot);
        size++;
        return true;
    }

    public boolean remove(Player player) {
        if (player == null) {
            return false;
        }
        int slot = player.getSlot();
        if (slot < 0 || !slots.get(slot)) {
            return false;
        }
        slots.clear(slot);
        size--;
        return true;
    }

    public boolean contains(Player player) {
        if (player == null) {
            return false;
        }
        int slot = player.getSlot();
        return slot >= 0 && slots.get(slot);
    }

    public void clear() {
        slots.clear();
        size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int getSize() {
        return size;
    }
}
