// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class Player extends Actor {

	public Model getHeadModel() {
		if (!visible)
			return null;
		if (npc != null)
			return npc.getHeadModel();
		boolean flag1 = false;
		for (int i = 0; i < 12; i++) {
			int j = equipment[i];
			if (j >= 256 && j < 512 && !IdentityKit.identityKits[j - 256].isHeadDownloaded())
				flag1 = true;
			if (j >= 512 && !ItemDefinition.forId(j - 512).method211(gender))
				flag1 = true;
		}

		if (flag1)
			return null;
		Model subModels[] = new Model[12];
		int k = 0;
		for (int l = 0; l < 12; l++) {
			int i1 = equipment[l];
			if (i1 >= 256 && i1 < 512) {
				Model model = IdentityKit.identityKits[i1 - 256].getHeadModel();
				if (model != null)
					subModels[k++] = model;
			}
			if (i1 >= 512) {
				Model model = ItemDefinition.forId(i1 - 512).getGenderModel(gender);
				if (model != null)
					subModels[k++] = model;
			}
		}

		Model model = new Model(k, subModels);
		for (int j1 = 0; j1 < 5; j1++)
			if (colors[j1] != 0) {
				model.replaceColor(client.anIntArrayArray1008[j1][0],
						client.anIntArrayArray1008[j1][colors[j1]]);
				if (j1 == 1)
					model.replaceColor(client.anIntArray1268[0], client.anIntArray1268[colors[j1]]);
			}

		return model;
	}

	public Model method571(byte byte0) {
		if (npc != null) {
			int i = -1;
			if (super.currentAnimation >= 0 && super.animationDelay == 0)
				i = Animation.animations[super.currentAnimation].anIntArray295[super.animationFrame];
			else if (super.anInt1588 >= 0)
				i = Animation.animations[super.anInt1588].anIntArray295[super.anInt1589];
			Model class50_sub1_sub4_sub4 = npc.method362(i, -1, 0, null);
			return class50_sub1_sub4_sub4;
		}
		long l = appearanceHash;
		int j = -1;
		int k = -1;
		int i1 = -1;
		int j1 = -1;
		if (byte0 != 122)
			aBoolean1767 = !aBoolean1767;
		if (super.currentAnimation >= 0 && super.animationDelay == 0) {
			Animation class14 = Animation.animations[super.currentAnimation];
			j = class14.anIntArray295[super.animationFrame];
			if (super.anInt1588 >= 0 && super.anInt1588 != super.anInt1634)
				k = Animation.animations[super.anInt1588].anIntArray295[super.anInt1589];
			if (class14.anInt302 >= 0) {
				i1 = class14.anInt302;
				l += i1 - equipment[5] << 40;
			}
			if (class14.anInt303 >= 0) {
				j1 = class14.anInt303;
				l += j1 - equipment[3] << 48;
			}
		} else if (super.anInt1588 >= 0)
			j = Animation.animations[super.anInt1588].anIntArray295[super.anInt1589];
		Model class50_sub1_sub4_sub4_1 = (Model) aClass33_1761.get(l);
		if (class50_sub1_sub4_sub4_1 == null) {
			boolean flag = false;
			for (int k1 = 0; k1 < 12; k1++) {
				int i2 = equipment[k1];
				if (j1 >= 0 && k1 == 3)
					i2 = j1;
				if (i1 >= 0 && k1 == 5)
					i2 = i1;
				if (i2 >= 256 && i2 < 512 && !IdentityKit.identityKits[i2 - 256].isBodyDownloaded())
					flag = true;
				if (i2 >= 512 && !ItemDefinition.forId(i2 - 512).method216(-861, gender))
					flag = true;
			}

			if (flag) {
				if (aLong1749 != -1L)
					class50_sub1_sub4_sub4_1 = (Model) aClass33_1761.get(aLong1749);
				if (class50_sub1_sub4_sub4_1 == null)
					return null;
			}
		}
		if (class50_sub1_sub4_sub4_1 == null) {
			Model aclass50_sub1_sub4_sub4[] = new Model[12];
			int l1 = 0;
			for (int j2 = 0; j2 < 12; j2++) {
				int k2 = equipment[j2];
				if (j1 >= 0 && j2 == 3)
					k2 = j1;
				if (i1 >= 0 && j2 == 5)
					k2 = i1;
				if (k2 >= 256 && k2 < 512) {
					Model class50_sub1_sub4_sub4_3 = IdentityKit.identityKits[k2 - 256]
							.getBodyModel();
					if (class50_sub1_sub4_sub4_3 != null)
						aclass50_sub1_sub4_sub4[l1++] = class50_sub1_sub4_sub4_3;
				}
				if (k2 >= 512) {
					Model class50_sub1_sub4_sub4_4 = ItemDefinition.forId(k2 - 512).method213(
							(byte) -98, gender);
					if (class50_sub1_sub4_sub4_4 != null)
						aclass50_sub1_sub4_sub4[l1++] = class50_sub1_sub4_sub4_4;
				}
			}

			class50_sub1_sub4_sub4_1 = new Model(l1, aclass50_sub1_sub4_sub4);
			for (int l2 = 0; l2 < 5; l2++)
				if (colors[l2] != 0) {
					class50_sub1_sub4_sub4_1.replaceColor(client.anIntArrayArray1008[l2][0],
							client.anIntArrayArray1008[l2][colors[l2]]);
					if (l2 == 1)
						class50_sub1_sub4_sub4_1.replaceColor(client.anIntArray1268[0], client.anIntArray1268[colors[l2]]);
				}

			class50_sub1_sub4_sub4_1.method584(7);
			class50_sub1_sub4_sub4_1.method594(64, 850, -30, -50, -30, true);
			aClass33_1761.put(class50_sub1_sub4_sub4_1, l);
			aLong1749 = l;
		}
		if (aBoolean1763)
			return class50_sub1_sub4_sub4_1;
		Model class50_sub1_sub4_sub4_2 = Model.aClass50_Sub1_Sub4_Sub4_1643;
		class50_sub1_sub4_sub4_2.method579(Class21.method239(j) & Class21.method239(k),
				class50_sub1_sub4_sub4_1, 1244);
		if (j != -1 && k != -1)
			class50_sub1_sub4_sub4_2.method586(k, 0, j, Animation.animations[super.currentAnimation].anIntArray299);
		else if (j != -1)
			class50_sub1_sub4_sub4_2.method585(j, (byte) 6);
		class50_sub1_sub4_sub4_2.method581(anInt1772);
		class50_sub1_sub4_sub4_2.anIntArrayArray1679 = null;
		class50_sub1_sub4_sub4_2.anIntArrayArray1678 = null;
		return class50_sub1_sub4_sub4_2;
	}

	@Override
	public boolean isVisible() {
		return visible;
	}

	@Override
	public Model getModel() {
		if (!visible)
			return null;
		Model class50_sub1_sub4_sub4 = method571((byte) 122);
		if (class50_sub1_sub4_sub4 == null)
			return null;
		super.anInt1594 = ((Entity) (class50_sub1_sub4_sub4)).height;
		class50_sub1_sub4_sub4.aBoolean1680 = true;
		if (aBoolean1763)
			return class50_sub1_sub4_sub4;
		if (super.anInt1614 != -1 && super.anInt1615 != -1) {
			SpotAnimation class27 = SpotAnimation.spotAnimations[super.anInt1614];
			Model class50_sub1_sub4_sub4_2 = class27.getModel();
			if (class50_sub1_sub4_sub4_2 != null) {
				Model class50_sub1_sub4_sub4_3 = new Model(false, false, true,
						class50_sub1_sub4_sub4_2, Class21.method239(super.anInt1615));
				class50_sub1_sub4_sub4_3.method590(0, 0, false, -super.anInt1618);
				class50_sub1_sub4_sub4_3.method584(7);
				class50_sub1_sub4_sub4_3.method585(class27.animation.anIntArray295[super.anInt1615], (byte) 6);
				class50_sub1_sub4_sub4_3.anIntArrayArray1679 = null;
				class50_sub1_sub4_sub4_3.anIntArrayArray1678 = null;
				if (class27.anInt561 != 128 || class27.anInt562 != 128)
					class50_sub1_sub4_sub4_3.method593(class27.anInt562, class27.anInt561, 9, class27.anInt561);
				class50_sub1_sub4_sub4_3.method594(64 + class27.anInt564, 850 + class27.anInt565, -30, -50, -30, true);
				Model aclass50_sub1_sub4_sub4_1[] = { class50_sub1_sub4_sub4, class50_sub1_sub4_sub4_3 };
				class50_sub1_sub4_sub4 = new Model(2, true, 0, aclass50_sub1_sub4_sub4_1);
			}
		}
		if (aClass50_Sub1_Sub4_Sub4_1746 != null) {
			if (client.pulseCycle >= anInt1765)
				aClass50_Sub1_Sub4_Sub4_1746 = null;
			if (client.pulseCycle >= anInt1764 && client.pulseCycle < anInt1765) {
				Model class50_sub1_sub4_sub4_1 = aClass50_Sub1_Sub4_Sub4_1746;
				class50_sub1_sub4_sub4_1.method590(anInt1743 - super.unitX, anInt1745 - super.unitY, false,
						anInt1744 - anInt1750);
				if (super.anInt1584 == 512) {
					class50_sub1_sub4_sub4_1.method588(true);
					class50_sub1_sub4_sub4_1.method588(true);
					class50_sub1_sub4_sub4_1.method588(true);
				} else if (super.anInt1584 == 1024) {
					class50_sub1_sub4_sub4_1.method588(true);
					class50_sub1_sub4_sub4_1.method588(true);
				} else if (super.anInt1584 == 1536)
					class50_sub1_sub4_sub4_1.method588(true);
				Model aclass50_sub1_sub4_sub4[] = { class50_sub1_sub4_sub4, class50_sub1_sub4_sub4_1 };
				class50_sub1_sub4_sub4 = new Model(2, true, 0, aclass50_sub1_sub4_sub4);
				if (super.anInt1584 == 512)
					class50_sub1_sub4_sub4_1.method588(true);
				else if (super.anInt1584 == 1024) {
					class50_sub1_sub4_sub4_1.method588(true);
					class50_sub1_sub4_sub4_1.method588(true);
				} else if (super.anInt1584 == 1536) {
					class50_sub1_sub4_sub4_1.method588(true);
					class50_sub1_sub4_sub4_1.method588(true);
					class50_sub1_sub4_sub4_1.method588(true);
				}
				class50_sub1_sub4_sub4_1.method590(super.unitX - anInt1743, super.unitY - anInt1745, false,
						anInt1750 - anInt1744);
			}
		}
		class50_sub1_sub4_sub4.aBoolean1680 = true;
		return class50_sub1_sub4_sub4;
	}

	public void updateAppearance(JagBuffer buf, int i) {
		buf.position = 0;
		gender = buf.getByte();
		skullIcon = buf.getSignedByte();
		prayerIcon = buf.getSignedByte();
		npc = null;
		team = 0;
		for (int slot = 0; slot < 12; slot++) {
			int upperByte = buf.getByte();
			if (upperByte == 0) {
				equipment[slot] = 0;
				continue;
			}
			int lowerByte = buf.getByte();
			equipment[slot] = (upperByte << 8) + lowerByte;
			if (slot == 0 && equipment[0] == 65535) {
				npc = NpcDefinition.forId(buf.getShort());
				break;
			}
			if (equipment[slot] >= 512 && equipment[slot] - 512 < ItemDefinition.count) {
				int itemTeam = ItemDefinition.forId(equipment[slot] - 512).team;
				if (itemTeam != 0)
					team = itemTeam;
			}
		}

		for (int l = 0; l < 5; l++) {
			int j1 = buf.getByte();
			if (j1 < 0 || j1 >= client.anIntArrayArray1008[l].length)
				j1 = 0;
			colors[l] = j1;
		}

		super.anInt1634 = buf.getShort();
		if (super.anInt1634 == 65535)
			super.anInt1634 = -1;
		super.anInt1635 = buf.getShort();
		if (super.anInt1635 == 65535)
			super.anInt1635 = -1;
		super.anInt1619 = buf.getShort();
		if (super.anInt1619 == 65535)
			super.anInt1619 = -1;
		super.anInt1620 = buf.getShort();
		if (super.anInt1620 == 65535)
			super.anInt1620 = -1;
		super.anInt1621 = buf.getShort();
		if (super.anInt1621 == 65535)
			super.anInt1621 = -1;
		super.anInt1622 = buf.getShort();
		if (super.anInt1622 == 65535)
			super.anInt1622 = -1;
		super.anInt1629 = buf.getShort();
		if (super.anInt1629 == 65535)
			super.anInt1629 = -1;
		username = StringUtils.formatPlayerName(StringUtils.decodeBase37(buf.getLong()));
		anInt1753 = buf.getByte();
		anInt1759 = buf.getShort();
		visible = true;
		appearanceHash = 0L;
		int k1 = equipment[5];
		int i2 = equipment[9];
		if (i != 0)
			return;
		equipment[5] = i2;
		equipment[9] = k1;
		for (int j2 = 0; j2 < 12; j2++) {
			appearanceHash <<= 4;
			if (equipment[j2] >= 256)
				appearanceHash += equipment[j2] - 256;
		}

		if (equipment[0] >= 256)
			appearanceHash += equipment[0] - 256 >> 4;
		if (equipment[1] >= 256)
			appearanceHash += equipment[1] - 256 >> 8;
		equipment[5] = k1;
		equipment[9] = i2;
		for (int k2 = 0; k2 < 5; k2++) {
			appearanceHash <<= 3;
			appearanceHash += colors[k2];
		}

		appearanceHash <<= 1;
		appearanceHash += gender;
	}

	public Player() {
		prayerIcon = -1;
		aLong1749 = -1L;
		equipment = new int[12];
		skullIcon = -1;
		visible = false;
		colors = new int[5];
		aBoolean1762 = true;
		aBoolean1763 = false;
		aBoolean1767 = false;
		anInt1772 = 932;
	}

	public int anInt1743;
	public int anInt1744;
	public int anInt1745;
	public Model aClass50_Sub1_Sub4_Sub4_1746;
	public int prayerIcon;
	public long aLong1749;
	public int anInt1750;
	public String username;
	public int equipment[];
	public int anInt1753;
	public long appearanceHash;
	public int gender;
	public int skullIcon;
	public NpcDefinition npc;
	public boolean visible;
	public int anInt1759;
	public int colors[];
	public static LruHashTable aClass33_1761 = new LruHashTable(260);
	public boolean aBoolean1762;
	public boolean aBoolean1763;
	public int anInt1764;
	public int anInt1765;
	public int team;
	public boolean aBoolean1767;
	public int anInt1768;
	public int anInt1769;
	public int anInt1770;
	public int anInt1771;
	public int anInt1772;

}
