// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class IdentityKit {

	public static void unpack(Archive archive) {
		JagBuffer buf = new JagBuffer(archive.get("idk.dat"));
		count = buf.getShort();
		if (identityKits == null)
			identityKits = new IdentityKit[count];
		for (int id = 0; id < count; id++) {
			if (identityKits[id] == null)
				identityKits[id] = new IdentityKit();
			identityKits[id].decode(buf);
		}
	}

	public void decode(JagBuffer buf) {
		do {
			int attribute = buf.getByte();
			if (attribute == 0)
				return;
			if (attribute == 1)
				part = buf.getByte();
			else if (attribute == 2) {
				int count = buf.getByte();
				bodyModelIds = new int[count];
				for (int k = 0; k < count; k++)
					bodyModelIds[k] = buf.getShort();
			} else if (attribute == 3)
				notSelectable = true;
			else if (attribute >= 40 && attribute < 50)
				srcColors[attribute - 40] = buf.getShort();
			else if (attribute >= 50 && attribute < 60)
				destColours[attribute - 50] = buf.getShort();
			else if (attribute >= 60 && attribute < 70)
				headModelIds[attribute - 60] = buf.getShort();
			else
				System.out.println("Error unrecognised config code: " + attribute);
		} while (true);
	}

	public boolean isBodyDownloaded() {
		if (bodyModelIds == null)
			return true;

		boolean downloaded = true;
		for (int j = 0; j < bodyModelIds.length; j++)
			if (!Model.isDownloaded(bodyModelIds[j]))
				downloaded = false;

		return downloaded;
	}

	public Model getBodyModel() {
		if (bodyModelIds == null)
			return null;

		Model subModels[] = new Model[bodyModelIds.length];
		for (int i = 0; i < bodyModelIds.length; i++)
			subModels[i] = Model.forId(bodyModelIds[i]);

		Model model;
		if (subModels.length == 1)
			model = subModels[0];
		else
			model = new Model(subModels.length, subModels);

		for (int j = 0; j < 6; j++) {
			if (srcColors[j] == 0)
				break;
			model.replaceColor(srcColors[j], destColours[j]);
		}

		return model;
	}

	public boolean isHeadDownloaded() {
		boolean downloaded = true;
		for (int j = 0; j < 5; j++)
			if (headModelIds[j] != -1 && !Model.isDownloaded(headModelIds[j]))
				downloaded = false;

		return downloaded;
	}

	public Model getHeadModel() {
		Model subModels[] = new Model[5];

		int count = 0;
		for (int j = 0; j < 5; j++)
			if (headModelIds[j] != -1)
				subModels[count++] = Model.forId(headModelIds[j]);

		Model model = new Model(count, subModels);
		for (int i = 0; i < 6; i++) {
			if (srcColors[i] == 0)
				break;
			model.replaceColor(srcColors[i], destColours[i]);
		}

		return model;
	}

	public IdentityKit() {
		part = -1;
		srcColors = new int[6];
		destColours = new int[6];
		notSelectable = false;
	}

	public static int count;
	public static IdentityKit identityKits[];
	public int part;
	public int bodyModelIds[];
	public int srcColors[];
	public int destColours[];
	public int headModelIds[] = { -1, -1, -1, -1, -1 };
	public boolean notSelectable;

}
