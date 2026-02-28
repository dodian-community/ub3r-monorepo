package net.dodian.stress.protocol;

public final class Rs317IsaacCipher {

    private final int[] results;
    private final int[] memory;

    private int accumulator;
    private int lastResult;
    private int counter;
    private int index;

    public Rs317IsaacCipher(int[] seed) {
        this.results = new int[256];
        this.memory = new int[256];
        for (int i = 0; i < seed.length; i++) {
            results[i] = seed[i];
        }
        init();
    }

    public int getNextKey() {
        if (index-- == 0) {
            isaac();
            index = 255;
        }
        return results[index];
    }

    private void isaac() {
        lastResult += ++counter;
        for (int i = 0; i < 256; i++) {
            int x = memory[i];
            switch (i & 3) {
                case 0 -> accumulator ^= accumulator << 13;
                case 1 -> accumulator ^= accumulator >>> 6;
                case 2 -> accumulator ^= accumulator << 2;
                default -> accumulator ^= accumulator >>> 16;
            }
            accumulator += memory[(i + 128) & 0xFF];
            int y = memory[(x & 0x3FC) >> 2] + accumulator + lastResult;
            memory[i] = y;
            lastResult = memory[((y >> 8) & 0x3FC) >> 2] + x;
            results[i] = lastResult;
        }
    }

    private void init() {
        int a = 0x9E3779B9;
        int b = 0x9E3779B9;
        int c = 0x9E3779B9;
        int d = 0x9E3779B9;
        int e = 0x9E3779B9;
        int f = 0x9E3779B9;
        int g = 0x9E3779B9;
        int h = 0x9E3779B9;

        for (int i = 0; i < 4; i++) {
            a ^= b << 11; d += a; b += c;
            b ^= c >>> 2; e += b; c += d;
            c ^= d << 8; f += c; d += e;
            d ^= e >>> 16; g += d; e += f;
            e ^= f << 10; h += e; f += g;
            f ^= g >>> 4; a += f; g += h;
            g ^= h << 8; b += g; h += a;
            h ^= a >>> 9; c += h; a += b;
        }

        for (int i = 0; i < 256; i += 8) {
            a += results[i];
            b += results[i + 1];
            c += results[i + 2];
            d += results[i + 3];
            e += results[i + 4];
            f += results[i + 5];
            g += results[i + 6];
            h += results[i + 7];

            a ^= b << 11; d += a; b += c;
            b ^= c >>> 2; e += b; c += d;
            c ^= d << 8; f += c; d += e;
            d ^= e >>> 16; g += d; e += f;
            e ^= f << 10; h += e; f += g;
            f ^= g >>> 4; a += f; g += h;
            g ^= h << 8; b += g; h += a;
            h ^= a >>> 9; c += h; a += b;

            memory[i] = a;
            memory[i + 1] = b;
            memory[i + 2] = c;
            memory[i + 3] = d;
            memory[i + 4] = e;
            memory[i + 5] = f;
            memory[i + 6] = g;
            memory[i + 7] = h;
        }

        for (int i = 0; i < 256; i += 8) {
            a += memory[i];
            b += memory[i + 1];
            c += memory[i + 2];
            d += memory[i + 3];
            e += memory[i + 4];
            f += memory[i + 5];
            g += memory[i + 6];
            h += memory[i + 7];

            a ^= b << 11; d += a; b += c;
            b ^= c >>> 2; e += b; c += d;
            c ^= d << 8; f += c; d += e;
            d ^= e >>> 16; g += d; e += f;
            e ^= f << 10; h += e; f += g;
            f ^= g >>> 4; a += f; g += h;
            g ^= h << 8; b += g; h += a;
            h ^= a >>> 9; c += h; a += b;

            memory[i] = a;
            memory[i + 1] = b;
            memory[i + 2] = c;
            memory[i + 3] = d;
            memory[i + 4] = e;
            memory[i + 5] = f;
            memory[i + 6] = g;
            memory[i + 7] = h;
        }

        isaac();
        index = 256;
    }
}
