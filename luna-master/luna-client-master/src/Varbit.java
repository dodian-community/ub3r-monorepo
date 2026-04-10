// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class Varbit {

	public static void unpack(Archive archive) {
		JagBuffer buf = new JagBuffer(archive.get("varbit.dat"));
		count = buf.getShort();

		if (varbitTable == null)
			varbitTable = new Varbit[count];

		for (int id = 0; id < count; id++) {
			if (varbitTable[id] == null)
				varbitTable[id] = new Varbit();
			varbitTable[id].init(id, buf);
			if (varbitTable[id].aBoolean829)
				Varp.varpTable[varbitTable[id].varpId].aBoolean716 = true;
		}

		if (buf.position != buf.buffer.length)
			System.out.println("varbit load mismatch");
	}

	public void init(int j, JagBuffer buf) {
		do {
			int attribute = buf.getByte();
			if (attribute == 0)
				return;
			if (attribute == 1) {
				varpId = buf.getShort();
				leastSignificantBit = buf.getByte();
				mostSignificantBit = buf.getByte();
			} else if (attribute == 10)
				aString825 = buf.getString();
			else if (attribute == 2)
				aBoolean829 = true;
			else if (attribute == 3)
				anInt830 = buf.getInt();
			else if (attribute == 4)
				anInt831 = buf.getInt();
			else if (attribute == 5)
				aBoolean832 = false;
			else
				System.out.println("Error unrecognised config code: " + attribute);
		} while (true);
	}

	public Varbit() {
		aBoolean829 = false;
		anInt830 = -1;
		aBoolean832 = true;
	}

	public int anInt822;
	public static int count;
	public static Varbit varbitTable[];
	public String aString825;
	public int varpId;
	public int leastSignificantBit;
	public int mostSignificantBit;
	public boolean aBoolean829;
	public int anInt830;
	public int anInt831;
	public boolean aBoolean832;
}
