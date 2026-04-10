// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class Animation {

	public static void unpack(Archive archive) {
		JagBuffer buf = new JagBuffer(archive.get("seq.dat"));
		count = buf.getShort();
		if (animations == null)
			animations = new Animation[count];
		for (int id = 0; id < count; id++) {
			if (animations[id] == null)
				animations[id] = new Animation();
			animations[id].init(buf);
		}
	}

	public int method205(int i, int j) {
		int k = anIntArray297[j];
		if (i != 0)
			return 1;
		if (k == 0) {
			Class21 class21 = Class21.method238(anIntArray295[j]);
			if (class21 != null)
				k = anIntArray297[j] = class21.anInt431;
		}
		if (k == 0)
			k = 1;
		return k;
	}

	public void init(JagBuffer buf) {
		do {
			int attribute = buf.getByte();
			if (attribute == 0)
				break;
			if (attribute == 1) {
				anInt294 = buf.getByte();
				anIntArray295 = new int[anInt294];
				anIntArray296 = new int[anInt294];
				anIntArray297 = new int[anInt294];
				for (int j = 0; j < anInt294; j++) {
					anIntArray295[j] = buf.getShort();
					anIntArray296[j] = buf.getShort();
					if (anIntArray296[j] == 65535)
						anIntArray296[j] = -1;
					anIntArray297[j] = buf.getShort();
				}

			} else if (attribute == 2)
				anInt298 = buf.getShort();
			else if (attribute == 3) {
				int k = buf.getByte();
				anIntArray299 = new int[k + 1];
				for (int l = 0; l < k; l++)
					anIntArray299[l] = buf.getByte();

				anIntArray299[k] = 0x98967f;
			} else if (attribute == 4)
				aBoolean300 = true;
			else if (attribute == 5)
				anInt301 = buf.getByte();
			else if (attribute == 6)
				anInt302 = buf.getShort();
			else if (attribute == 7)
				anInt303 = buf.getShort();
			else if (attribute == 8)
				anInt304 = buf.getByte();
			else if (attribute == 9)
				anInt305 = buf.getByte();
			else if (attribute == 10)
				anInt306 = buf.getByte();
			else if (attribute == 11)
				type = buf.getByte();
			else if (attribute == 12)
				anInt308 = buf.getInt();
			else
				System.out.println("Error unrecognised seq config code: " + attribute);
		} while (true);
		if (anInt294 == 0) {
			anInt294 = 1;
			anIntArray295 = new int[1];
			anIntArray295[0] = -1;
			anIntArray296 = new int[1];
			anIntArray296[0] = -1;
			anIntArray297 = new int[1];
			anIntArray297[0] = -1;
		}
		if (anInt305 == -1)
			if (anIntArray299 != null)
				anInt305 = 2;
			else
				anInt305 = 0;
		if (anInt306 == -1) {
			if (anIntArray299 != null) {
				anInt306 = 2;
				return;
			}
			anInt306 = 0;
		}
	}

	public Animation() {
		anInt298 = -1;
		aBoolean300 = false;
		anInt301 = 5;
		anInt302 = -1;
		anInt303 = -1;
		anInt304 = 99;
		anInt305 = -1;
		anInt306 = -1;
		type = 2;
	}

	public static int count;
	public static Animation animations[];
	public int anInt294;
	public int anIntArray295[];
	public int anIntArray296[];
	public int anIntArray297[];
	public int anInt298;
	public int anIntArray299[];
	public boolean aBoolean300;
	public int anInt301;
	public int anInt302;
	public int anInt303;
	public int anInt304;
	public int anInt305;
	public int anInt306;
	public int type;
	public int anInt308;
	public static int anInt309;

}
