package net.dodian.uber.game.runtime.sync.util;

import java.util.Arrays;
import java.util.function.IntConsumer;

public final class IntHashSet {
    private static final float LOAD_FACTOR = 0.5f;

    private int[] keys;
    private int size;
    private int threshold;
    private int mask;

    public IntHashSet(int initialCapacity) {
        int capacity = capacityFor(initialCapacity);
        this.keys = new int[capacity];
        this.mask = capacity - 1;
        this.threshold = Math.max(1, (int) (capacity * LOAD_FACTOR));
    }

    public void clear() {
        Arrays.fill(keys, 0);
        size = 0;
    }

    public int size() {
        return size;
    }

    public boolean add(int key) {
        int stored = key + 1;
        if (size + 1 > threshold) {
            resize(keys.length << 1);
        }
        int index = mix(stored) & mask;
        while (true) {
            int existing = keys[index];
            if (existing == 0) {
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

    public boolean contains(int key) {
        int stored = key + 1;
        int index = mix(stored) & mask;
        while (true) {
            int existing = keys[index];
            if (existing == 0) {
                return false;
            }
            if (existing == stored) {
                return true;
            }
            index = (index + 1) & mask;
        }
    }

    public void forEach(IntConsumer consumer) {
        for (int stored : keys) {
            if (stored != 0) {
                consumer.accept(stored - 1);
            }
        }
    }

    private void resize(int newCapacity) {
        int[] oldKeys = keys;
        keys = new int[newCapacity];
        mask = newCapacity - 1;
        threshold = Math.max(1, (int) (newCapacity * LOAD_FACTOR));
        size = 0;
        for (int stored : oldKeys) {
            if (stored != 0) {
                add(stored - 1);
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

    private static int mix(int value) {
        int x = value;
        x ^= (x >>> 16);
        x *= 0x7feb352d;
        x ^= (x >>> 15);
        x *= 0x846ca68b;
        x ^= (x >>> 16);
        return x;
    }
}
