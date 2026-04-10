// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class IsaacRandom {

	public IsaacRandom(int seed[]) {
		memory = new int[256];
		results = new int[256];
		for (int j = 0; j < seed.length; j++)
			results[j] = seed[j];
		init();
		return;
	}

	public int nextInt() {
		if (result-- == 0) {
			isaac();
			result = 255;
		}
		return results[result];
	}

	public void isaac() {
		anInt526 += ++anInt527;
		for (int i = 0; i < 256; i++) {
			int j = memory[i];
			if ((i & 3) == 0)
				anInt525 ^= anInt525 << 13;
			else if ((i & 3) == 1)
				anInt525 ^= anInt525 >>> 6;
			else if ((i & 3) == 2)
				anInt525 ^= anInt525 << 2;
			else if ((i & 3) == 3)
				anInt525 ^= anInt525 >>> 16;
			anInt525 += memory[i + 128 & 0xff];
			int k;
			memory[i] = k = memory[(j & 0x3fc) >> 2] + anInt525 + anInt526;
			results[i] = anInt526 = memory[(k >> 8 & 0x3fc) >> 2] + j;
		}

	}

	public void init() {
		int i1;
		int j1;
		int k1;
		int l1;
		int i2;
		int j2;
		int k2;
		int l = i1 = j1 = k1 = l1 = i2 = j2 = k2 = 0x9e3779b9;
		for (int i = 0; i < 4; i++) {
			l ^= i1 << 11;
			k1 += l;
			i1 += j1;
			i1 ^= j1 >>> 2;
			l1 += i1;
			j1 += k1;
			j1 ^= k1 << 8;
			i2 += j1;
			k1 += l1;
			k1 ^= l1 >>> 16;
			j2 += k1;
			l1 += i2;
			l1 ^= i2 << 10;
			k2 += l1;
			i2 += j2;
			i2 ^= j2 >>> 4;
			l += i2;
			j2 += k2;
			j2 ^= k2 << 8;
			i1 += j2;
			k2 += l;
			k2 ^= l >>> 9;
			j1 += k2;
			l += i1;
		}

		for (int j = 0; j < 256; j += 8) {
			l += results[j];
			i1 += results[j + 1];
			j1 += results[j + 2];
			k1 += results[j + 3];
			l1 += results[j + 4];
			i2 += results[j + 5];
			j2 += results[j + 6];
			k2 += results[j + 7];
			l ^= i1 << 11;
			k1 += l;
			i1 += j1;
			i1 ^= j1 >>> 2;
			l1 += i1;
			j1 += k1;
			j1 ^= k1 << 8;
			i2 += j1;
			k1 += l1;
			k1 ^= l1 >>> 16;
			j2 += k1;
			l1 += i2;
			l1 ^= i2 << 10;
			k2 += l1;
			i2 += j2;
			i2 ^= j2 >>> 4;
			l += i2;
			j2 += k2;
			j2 ^= k2 << 8;
			i1 += j2;
			k2 += l;
			k2 ^= l >>> 9;
			j1 += k2;
			l += i1;
			memory[j] = l;
			memory[j + 1] = i1;
			memory[j + 2] = j1;
			memory[j + 3] = k1;
			memory[j + 4] = l1;
			memory[j + 5] = i2;
			memory[j + 6] = j2;
			memory[j + 7] = k2;
		}

		for (int k = 0; k < 256; k += 8) {
			l += memory[k];
			i1 += memory[k + 1];
			j1 += memory[k + 2];
			k1 += memory[k + 3];
			l1 += memory[k + 4];
			i2 += memory[k + 5];
			j2 += memory[k + 6];
			k2 += memory[k + 7];
			l ^= i1 << 11;
			k1 += l;
			i1 += j1;
			i1 ^= j1 >>> 2;
			l1 += i1;
			j1 += k1;
			j1 ^= k1 << 8;
			i2 += j1;
			k1 += l1;
			k1 ^= l1 >>> 16;
			j2 += k1;
			l1 += i2;
			l1 ^= i2 << 10;
			k2 += l1;
			i2 += j2;
			i2 ^= j2 >>> 4;
			l += i2;
			j2 += k2;
			j2 ^= k2 << 8;
			i1 += j2;
			k2 += l;
			k2 ^= l >>> 9;
			j1 += k2;
			l += i1;
			memory[k] = l;
			memory[k + 1] = i1;
			memory[k + 2] = j1;
			memory[k + 3] = k1;
			memory[k + 4] = l1;
			memory[k + 5] = i2;
			memory[k + 6] = j2;
			memory[k + 7] = k2;
		}

		isaac();
		result = 256;
	}

	public int result;
	public int results[];
	public int memory[];
	public int anInt525;
	public int anInt526;
	public int anInt527;
}
