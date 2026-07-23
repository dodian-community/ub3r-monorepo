package net.dodian.uber.game.model.entity.player;

import java.util.Arrays;

/**
 * Slot-indexed membership set for local player sync state.
 *
 * Uses slot bits instead of hash-based membership to avoid per-tick
 * allocation/iterator churn in synchronization hot paths.
 */
public final class PlayerSlotMembershipSet {
    private final long[] slots;
    private int size;

    public PlayerSlotMembershipSet(int capacity) {
        this.slots = new long[(Math.max(1, capacity) + 63) >>> 6];
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
        int word = slot >>> 6;
        if (word >= slots.length) {
            return false;
        }
        long bit = 1L << (slot & 63);
        if ((slots[word] & bit) != 0L) {
            return false;
        }
        slots[word] |= bit;
        size++;
        return true;
    }

    public boolean remove(Player player) {
        if (player == null) {
            return false;
        }
        int slot = player.getSlot();
        int word = slot >>> 6;
        if (slot < 0 || word >= slots.length) {
            return false;
        }
        long bit = 1L << (slot & 63);
        if ((slots[word] & bit) == 0L) {
            return false;
        }
        slots[word] &= ~bit;
        size--;
        return true;
    }

    public boolean contains(Player player) {
        if (player == null) {
            return false;
        }
        int slot = player.getSlot();
        int word = slot >>> 6;
        return slot >= 0 && word < slots.length && (slots[word] & (1L << (slot & 63))) != 0L;
    }

    public void clear() {
        Arrays.fill(slots, 0L);
        size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int getSize() {
        return size;
    }
}
