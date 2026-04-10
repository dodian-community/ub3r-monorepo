// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class SpotAnimation {

	public static void unpack(Archive archive) {
		JagBuffer buf = new JagBuffer(archive.get("spotanim.dat"));
		count = buf.getShort();
		if (spotAnimations == null)
			spotAnimations = new SpotAnimation[count];
		for (int id = 0; id < count; id++) {
			if (spotAnimations[id] == null)
				spotAnimations[id] = new SpotAnimation();
			spotAnimations[id].id = id;
			spotAnimations[id].init(buf);
		}

	}

	public void init(JagBuffer buf) {
		do {
			int attribute = buf.getByte();
			if (attribute == 0)
				return;
			if (attribute == 1)
				modelId = buf.getShort();
			else if (attribute == 2) {
				animId = buf.getShort();
				if (Animation.animations != null)
					animation = Animation.animations[animId];
			} else if (attribute == 4)
				anInt561 = buf.getShort();
			else if (attribute == 5)
				anInt562 = buf.getShort();
			else if (attribute == 6)
				anInt563 = buf.getShort();
			else if (attribute == 7)
				anInt564 = buf.getByte();
			else if (attribute == 8)
				anInt565 = buf.getByte();
			else if (attribute >= 40 && attribute < 50)
				srcColors[attribute - 40] = buf.getShort();
			else if (attribute >= 50 && attribute < 60)
				destColors[attribute - 50] = buf.getShort();
			else
				System.out.println("Error unrecognised spotanim config code: " + attribute);
		} while (true);
	}

	public Model getModel() {
		Model model = (Model) models.get(id);
		if (model != null)
			return model;

		model = Model.forId(modelId);
		if (model == null)
			return null;

		for (int i = 0; i < 6; i++)
			if (srcColors[0] != 0)
				model.replaceColor(srcColors[i], destColors[i]);

		models.put(model, id);
		return model;
	}

	public SpotAnimation() {
		animId = -1;
		srcColors = new int[6];
		destColors = new int[6];
		anInt561 = 128; // TODO: maybe width and height in units? or model scale?
		anInt562 = 128;
	}

	public static int count;
	public static SpotAnimation spotAnimations[];
	public int id;
	public int modelId;
	public int animId;
	public Animation animation;
	public int srcColors[];
	public int destColors[];
	public int anInt561;
	public int anInt562;
	public int anInt563;
	public int anInt564;
	public int anInt565;
	public static LruHashTable models = new LruHashTable(30);

}
