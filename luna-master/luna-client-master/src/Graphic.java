// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class Graphic extends Entity {

	public void method604(byte byte0, int i) {
		anInt1738 += i;
		if (byte0 == 1)
			byte0 = 0;
		else
			return;
		while (anInt1738 > aClass27_1739.animation.method205(0, anInt1737)) {
			anInt1738 -= aClass27_1739.animation.method205(0, anInt1737);
			anInt1737++;
			if (anInt1737 >= aClass27_1739.animation.anInt294
					&& (anInt1737 < 0 || anInt1737 >= aClass27_1739.animation.anInt294)) {
				anInt1737 = 0;
				aBoolean1736 = true;
			}
		}
	}

	public Graphic(int i, int j, int k, int l, int i1, int j1, int k1, int l1) {
		aBoolean1735 = true;
		aBoolean1736 = false;
		aClass27_1739 = SpotAnimation.spotAnimations[i1];
		anInt1731 = j;
		anInt1732 = i;
		anInt1733 = k1;
		if (l1 != 10709) {
			for (int i2 = 1; i2 > 0; i2++);
		}
		anInt1734 = k;
		anInt1740 = j1 + l;
		aBoolean1736 = false;
	}

	@Override
	public Model getModel() {
		Model class50_sub1_sub4_sub4 = aClass27_1739.getModel();
		if (class50_sub1_sub4_sub4 == null)
			return null;
		int i = aClass27_1739.animation.anIntArray295[anInt1737];
		Model class50_sub1_sub4_sub4_1 = new Model(false, false, true,
				class50_sub1_sub4_sub4, Class21.method239(i));
		if (!aBoolean1736) {
			class50_sub1_sub4_sub4_1.method584(7);
			class50_sub1_sub4_sub4_1.method585(i, (byte) 6);
			class50_sub1_sub4_sub4_1.anIntArrayArray1679 = null;
			class50_sub1_sub4_sub4_1.anIntArrayArray1678 = null;
		}
		if (aClass27_1739.anInt561 != 128 || aClass27_1739.anInt562 != 128)
			class50_sub1_sub4_sub4_1.method593(aClass27_1739.anInt562, aClass27_1739.anInt561, 9,
					aClass27_1739.anInt561);
		if (aClass27_1739.anInt563 != 0) {
			if (aClass27_1739.anInt563 == 90)
				class50_sub1_sub4_sub4_1.method588(true);
			if (aClass27_1739.anInt563 == 180) {
				class50_sub1_sub4_sub4_1.method588(true);
				class50_sub1_sub4_sub4_1.method588(true);
			}
			if (aClass27_1739.anInt563 == 270) {
				class50_sub1_sub4_sub4_1.method588(true);
				class50_sub1_sub4_sub4_1.method588(true);
				class50_sub1_sub4_sub4_1.method588(true);
			}
		}
		class50_sub1_sub4_sub4_1.method594(64 + aClass27_1739.anInt564, 850 + aClass27_1739.anInt565, -30, -50, -30,
				true);
		return class50_sub1_sub4_sub4_1;
	}

	public int anInt1731;
	public int anInt1732;
	public int anInt1733;
	public int anInt1734;
	public boolean aBoolean1735;
	public boolean aBoolean1736;
	public int anInt1737;
	public int anInt1738;
	public SpotAnimation aClass27_1739;
	public int anInt1740;
}
