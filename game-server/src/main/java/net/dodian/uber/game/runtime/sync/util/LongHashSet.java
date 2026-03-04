package net.dodian.uber.game.runtime.sync.util;

import java.util.Arrays;
import java.util.function.LongConsumer;

public final class LongHashSet {
    private static final float LOAD_FACTOR = 0.5f;

    private long[] keys;
    private int size;
    private int threshold;
    private int mask;

    public LongHashSet(int initialCapacity) {
        int capacity = capacityFor(initialCapacity);
        this.keys = new long[capacity];
        this.mask = capacity - 1;
        this.threshold = Math.max(1, (int) (capacity * LOAD_FACTOR));
    }

    public void clear() {
        Arrays.fill(keys, 0L);
        size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean add(long key) {
        long stored = key + 1L;
        if (size + 1 > threshold) {
            resize(keys.length << 1);
        }
        int index = mix(stored) & mask;
        while (true) {
            long existing = keys[index];
            if (existing == 0L) {
                keys[index] = stored;
                size++;
                return true;
            }
            if (existing == stored) {
                return false;
            }
            index = (index + 1) & mask;
        }
    }

    public boolean contains(long key) {
        long stored = key + 1L;
        int index = mix(stored) & mask;
        while (true) {
            long existing = keys[index];
            if (existing == 0L) {
                return false;
            }
            if (existing == stored) {
                return true;
            }
            index = (index + 1) & mask;
        }
    }

    public void forEach(LongConsumer consumer) {
        for (long stored : keys) {
            if (stored != 0L) {
                consumer.accept(stored - 1L);
            }
        }
    }

    private void resize(int newCapacity) {
        long[] oldKeys = keys;
        keys = new long[newCapacity];
        mask = newCapacity - 1;
        threshold = Math.max(1, (int) (newCapacity * LOAD_FACTOR));
        size = 0;
        for (long stored : oldKeys) {
            if (stored != 0L) {
                add(stored - 1L);
            }
        }
    }

    private static int capacityFor(int initialCapacity) {
        int desired = Math.max(2, (int) (initialCapacity / LOAD_FACTOR) + 1);
        int capacity = 1;
        while (capacity < desired) {
            capacity <<= 1;
        }
        return capacity;
    }

    private static int mix(long value) {
        long x = value;
        x ^= (x >>> 33);
        x *= 0xff51afd7ed558ccdL;
        x ^= (x >>> 33);
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= (x >>> 33);
        return (int) x;
    }
}
