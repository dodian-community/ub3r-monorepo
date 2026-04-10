// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class IndexedSprite extends Drawable {

	public IndexedSprite(Archive class2, String s, int i) {
		anInt1509 = 3;
		aBoolean1510 = true;
		anInt1512 = -235;
		aByte1513 = 5;
		anInt1514 = -3539;
		aBoolean1515 = true;
		JagBuffer class50_sub1_sub2 = new JagBuffer(class2.get(s + ".dat"));
		JagBuffer class50_sub1_sub2_1 = new JagBuffer(class2.get("index.dat"));
		class50_sub1_sub2_1.position = class50_sub1_sub2.getShort();
		anInt1522 = class50_sub1_sub2_1.getShort();
		anInt1523 = class50_sub1_sub2_1.getShort();
		int j = class50_sub1_sub2_1.getByte();
		anIntArray1517 = new int[j];
		for (int k = 0; k < j - 1; k++)
			anIntArray1517[k + 1] = class50_sub1_sub2_1.getTriByte();

		for (int l = 0; l < i; l++) {
			class50_sub1_sub2_1.position += 2;
			class50_sub1_sub2.position += class50_sub1_sub2_1.getShort() * class50_sub1_sub2_1.getShort();
			class50_sub1_sub2_1.position++;
		}

		anInt1520 = class50_sub1_sub2_1.getByte();
		anInt1521 = class50_sub1_sub2_1.getByte();
		anInt1518 = class50_sub1_sub2_1.getShort();
		anInt1519 = class50_sub1_sub2_1.getShort();
		int i1 = class50_sub1_sub2_1.getByte();
		int j1 = anInt1518 * anInt1519;
		aByteArray1516 = new byte[j1];
		if (i1 == 0) {
			for (int k1 = 0; k1 < j1; k1++)
				aByteArray1516[k1] = class50_sub1_sub2.getSignedByte();

			return;
		}
		if (i1 == 1) {
			for (int l1 = 0; l1 < anInt1518; l1++) {
				for (int i2 = 0; i2 < anInt1519; i2++)
					aByteArray1516[l1 + i2 * anInt1518] = class50_sub1_sub2.getSignedByte();

			}

		}
	}

	public void method485(int i) {
		anInt1522 /= 2;
		anInt1523 /= 2;
		byte abyte0[] = new byte[anInt1522 * anInt1523];
		int j = 0;
		if (i != 0)
			return;
		for (int k = 0; k < anInt1519; k++) {
			for (int l = 0; l < anInt1518; l++)
				abyte0[(l + anInt1520 >> 1) + (k + anInt1521 >> 1) * anInt1522] = aByteArray1516[j++];

		}

		aByteArray1516 = abyte0;
		anInt1518 = anInt1522;
		anInt1519 = anInt1523;
		anInt1520 = 0;
		anInt1521 = 0;
	}

	public void method486(boolean flag) {
		if (anInt1518 == anInt1522 && anInt1519 == anInt1523)
			return;
		byte abyte0[] = new byte[anInt1522 * anInt1523];
		int i = 0;
		for (int j = 0; j < anInt1519; j++) {
			for (int k = 0; k < anInt1518; k++)
				abyte0[k + anInt1520 + (j + anInt1521) * anInt1522] = aByteArray1516[i++];

		}

		aByteArray1516 = abyte0;
		anInt1518 = anInt1522;
		if (!flag) {
			return;
		} else {
			anInt1519 = anInt1523;
			anInt1520 = 0;
			anInt1521 = 0;
			return;
		}
	}

	public void method487(int i) {
		byte abyte0[] = new byte[anInt1518 * anInt1519];
		int j = 0;
		for (int k = 0; k < anInt1519; k++) {
			for (int l = anInt1518 - 1; l >= 0; l--)
				abyte0[j++] = aByteArray1516[l + k * anInt1518];

		}

		aByteArray1516 = abyte0;
		if (i != 0) {
			return;
		} else {
			anInt1520 = anInt1522 - anInt1518 - anInt1520;
			return;
		}
	}

	public void method488(byte byte0) {
		byte abyte0[] = new byte[anInt1518 * anInt1519];
		int i = 0;
		if (byte0 != 7)
			aBoolean1515 = !aBoolean1515;
		for (int j = anInt1519 - 1; j >= 0; j--) {
			for (int k = 0; k < anInt1518; k++)
				abyte0[i++] = aByteArray1516[k + j * anInt1518];

		}

		aByteArray1516 = abyte0;
		anInt1521 = anInt1523 - anInt1519 - anInt1521;
	}

	public void method489(int i, int j, int k, int l) {
		for (int i1 = 0; i1 < anIntArray1517.length; i1++) {
			int j1 = anIntArray1517[i1] >> 16 & 0xff;
			j1 += k;
			if (j1 < 0)
				j1 = 0;
			else if (j1 > 255)
				j1 = 255;
			int k1 = anIntArray1517[i1] >> 8 & 0xff;
			k1 += j;
			if (k1 < 0)
				k1 = 0;
			else if (k1 > 255)
				k1 = 255;
			int l1 = anIntArray1517[i1] & 0xff;
			l1 += i;
			if (l1 < 0)
				l1 = 0;
			else if (l1 > 255)
				l1 = 255;
			anIntArray1517[i1] = (j1 << 16) + (k1 << 8) + l1;
		}

		if (l == anInt1512)
			;
	}

	public void method490(int i, int j, int k) {
		j += anInt1520;
		i += anInt1521;
		while (k >= 0) {
			for (int l = 1; l > 0; l++);
		}
		int i1 = j + i * Drawable.width;
		int j1 = 0;
		int k1 = anInt1519;
		int l1 = anInt1518;
		int i2 = Drawable.width - l1;
		int j2 = 0;
		if (i < Drawable.anInt1427) {
			int k2 = Drawable.anInt1427 - i;
			k1 -= k2;
			i = Drawable.anInt1427;
			j1 += k2 * l1;
			i1 += k2 * Drawable.width;
		}
		if (i + k1 > Drawable.anInt1428)
			k1 -= (i + k1) - Drawable.anInt1428;
		if (j < Drawable.anInt1429) {
			int l2 = Drawable.anInt1429 - j;
			l1 -= l2;
			j = Drawable.anInt1429;
			j1 += l2;
			i1 += l2;
			j2 += l2;
			i2 += l2;
		}
		if (j + l1 > Drawable.anInt1430) {
			int i3 = (j + l1) - Drawable.anInt1430;
			l1 -= i3;
			j2 += i3;
			i2 += i3;
		}
		if (l1 <= 0 || k1 <= 0) {
			return;
		} else {
			method491(j1, Drawable.anIntArray1424, aByteArray1516, j2, anIntArray1517, k1, l1, i1, false, i2);
			return;
		}
	}

	public void method491(int i, int ai[], byte abyte0[], int j, int ai1[], int k, int l, int i1, boolean flag, int j1) {
		int k1 = -(l >> 2);
		l = -(l & 3);
		if (flag)
			anInt1511 = 264;
		for (int l1 = -k; l1 < 0; l1++) {
			for (int i2 = k1; i2 < 0; i2++) {
				byte byte0 = abyte0[i++];
				if (byte0 != 0)
					ai[i1++] = ai1[byte0 & 0xff];
				else
					i1++;
				byte0 = abyte0[i++];
				if (byte0 != 0)
					ai[i1++] = ai1[byte0 & 0xff];
				else
					i1++;
				byte0 = abyte0[i++];
				if (byte0 != 0)
					ai[i1++] = ai1[byte0 & 0xff];
				else
					i1++;
				byte0 = abyte0[i++];
				if (byte0 != 0)
					ai[i1++] = ai1[byte0 & 0xff];
				else
					i1++;
			}

			for (int j2 = l; j2 < 0; j2++) {
				byte byte1 = abyte0[i++];
				if (byte1 != 0)
					ai[i1++] = ai1[byte1 & 0xff];
				else
					i1++;
			}

			i1 += j1;
			i += j;
		}

	}

	public int anInt1509;
	public boolean aBoolean1510;
	public int anInt1511;
	public int anInt1512;
	public byte aByte1513;
	public int anInt1514;
	public boolean aBoolean1515;
	public byte aByteArray1516[];
	public int anIntArray1517[];
	public int anInt1518;
	public int anInt1519;
	public int anInt1520;
	public int anInt1521;
	public int anInt1522;
	public int anInt1523;
}
