// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public class ObjectDefinition {

    public static ObjectDefinition forId(int id) {
        for (int j = 0; j < 20; j++)
            if (cache[j].id == id)
                return cache[j];

        cachePos = (cachePos + 1) % 20;
        ObjectDefinition def = cache[cachePos];
        buf.position = indices[id];
        def.id = id;
        def.reset();
        def.init(buf);
        return def;
    }

    @Override
    public String toString() {
        return "{OBJECT DEF} id " + id + ", name " + name + ", varbitId " + varbitId + ", animationId: " + animationId;
    }

    public ObjectDefinition method424(int i) {
        if (i != 0)
            anInt788 = 445;
        int j = -1;
        if (varbitId != -1) {
            Varbit class49 = Varbit.varbitTable[varbitId];
            int k = class49.varpId;
            int lsb = class49.leastSignificantBit;
            int msb = class49.mostSignificantBit;
            int bit = client.BITFIELD_MAX_VALUES[msb - lsb];
            j = aClient770.localVarps[k] >> lsb & bit;
        } else if (anInt781 != -1)
            j = aClient770.localVarps[anInt781];
        if (j < 0 || j >= anIntArray805.length || anIntArray805[j] == -1)
            return null;
        else
            return forId(anIntArray805[j]);
    }

    public void method425(OnDemandFetcher class32_sub1, int i) {
        if (anIntArray763 == null)
            return;
        for (int j = 0; j < anIntArray763.length; j++)
            class32_sub1.method337(anIntArray763[j] & 0xffff, 0, aByte793);

        if (i >= 0) {
            for (int k = 1; k > 0; k++) ;
        }
    }

    public static void unpack(Archive archive) {
        buf = new JagBuffer(archive.get("loc.dat"));
        JagBuffer buf = new JagBuffer(archive.get("loc.idx"));
        count = buf.getShort();
        indices = new int[count];
        int pos = 2;
        for (int id = 0; id < count; id++) {
            indices[id] = pos;
            pos += buf.getShort();
        }

        cache = new ObjectDefinition[20];
        for (int k = 0; k < 20; k++)
            cache[k] = new ObjectDefinition();

    }

    public Model method427(int i, int j, int k, int l) {
        Model class50_sub1_sub4_sub4 = null;
        long l1;
        if (anIntArray789 == null) {
            if (l != 10)
                return null;
            l1 = ((id << 6) + i) + ((long) (j + 1) << 32);
            Model class50_sub1_sub4_sub4_1 = (Model) aClass33_762.get(l1);
            if (class50_sub1_sub4_sub4_1 != null)
                return class50_sub1_sub4_sub4_1;
            if (anIntArray763 == null)
                return null;
            boolean flag1 = aBoolean798 ^ (i > 3);
            int k1 = anIntArray763.length;
            for (int i2 = 0; i2 < k1; i2++) {
                int l2 = anIntArray763[i2];
                if (flag1)
                    l2 += 0x10000;
                class50_sub1_sub4_sub4 = (Model) aClass33_779.get(l2);
                if (class50_sub1_sub4_sub4 == null) {
                    class50_sub1_sub4_sub4 = Model.forId(l2 & 0xffff);
                    if (class50_sub1_sub4_sub4 == null)
                        return null;
                    if (flag1)
                        class50_sub1_sub4_sub4.method592(0);
                    aClass33_779.put(class50_sub1_sub4_sub4, l2);
                }
                if (k1 > 1)
                    aClass50_Sub1_Sub4_Sub4Array771[i2] = class50_sub1_sub4_sub4;
            }

            if (k1 > 1)
                class50_sub1_sub4_sub4 = new Model(k1, aClass50_Sub1_Sub4_Sub4Array771);
        } else {
            int i1 = -1;
            for (int j1 = 0; j1 < anIntArray789.length; j1++) {
                if (anIntArray789[j1] != l)
                    continue;
                i1 = j1;
                break;
            }

            if (i1 == -1)
                return null;
            l1 = ((id << 6) + (i1 << 3) + i) + ((long) (j + 1) << 32);
            Model class50_sub1_sub4_sub4_2 = (Model) aClass33_762.get(l1);
            if (class50_sub1_sub4_sub4_2 != null)
                return class50_sub1_sub4_sub4_2;
            int j2 = anIntArray763[i1];
            boolean flag3 = aBoolean798 ^ (i > 3);
            if (flag3)
                j2 += 0x10000;
            class50_sub1_sub4_sub4 = (Model) aClass33_779.get(j2);
            if (class50_sub1_sub4_sub4 == null) {
                class50_sub1_sub4_sub4 = Model.forId(j2 & 0xffff);
                if (class50_sub1_sub4_sub4 == null)
                    return null;
                if (flag3)
                    class50_sub1_sub4_sub4.method592(0);
                aClass33_779.put(class50_sub1_sub4_sub4, j2);
            }
        }
        boolean flag;
        if (anInt780 != 128 || anInt760 != 128 || anInt796 != 128)
            flag = true;
        else
            flag = false;
        boolean flag2;
        if (anInt761 != 0 || anInt785 != 0 || anInt766 != 0)
            flag2 = true;
        else
            flag2 = false;
        Model class50_sub1_sub4_sub4_3 = new Model(i == 0 && j == -1 && !flag
                && !flag2, false, modifiedModelColors == null, class50_sub1_sub4_sub4, Class21.method239(j));
        if (k != 0)
            anInt768 = 487;
        if (j != -1) {
            class50_sub1_sub4_sub4_3.method584(7);
            class50_sub1_sub4_sub4_3.method585(j, (byte) 6);
            class50_sub1_sub4_sub4_3.anIntArrayArray1679 = null;
            class50_sub1_sub4_sub4_3.anIntArrayArray1678 = null;
        }
        while (i-- > 0)
            class50_sub1_sub4_sub4_3.method588(true);
        if (modifiedModelColors != null) {
            for (int k2 = 0; k2 < modifiedModelColors.length; k2++)
                class50_sub1_sub4_sub4_3.replaceColor(modifiedModelColors[k2], originalModelColors[k2]);

        }
        if (flag)
            class50_sub1_sub4_sub4_3.method593(anInt760, anInt796, 9, anInt780);
        if (flag2)
            class50_sub1_sub4_sub4_3.method590(anInt761, anInt766, false, anInt785);
        class50_sub1_sub4_sub4_3.method594(64 + aByte784, 768 + aByte787 * 5, -50, -10, -50, !aBoolean804);
        if (anInt794 == 1)
            class50_sub1_sub4_sub4_3.anInt1675 = ((Entity) (class50_sub1_sub4_sub4_3)).height;
        aClass33_762.put(class50_sub1_sub4_sub4_3, l1);
        return class50_sub1_sub4_sub4_3;
    }

    public boolean method428(int i) {
        if (anIntArray763 == null)
            return true;
        boolean flag = true;
        while (i >= 0)
            anInt768 = 347;
        for (int j = 0; j < anIntArray763.length; j++)
            flag &= Model.isDownloaded(anIntArray763[j] & 0xffff);

        return flag;
    }

    public void reset() {
        anIntArray763 = null;
        anIntArray789 = null;
        name = "null";
        description = null;
        modifiedModelColors = null;
        originalModelColors = null;
        anInt801 = 1;
        anInt775 = 1;
        aBoolean810 = true;
        aBoolean809 = true;
        aBoolean759 = false;
        aBoolean769 = false;
        aBoolean804 = false;
        aBoolean797 = false;
        animationId = -1;
        anInt802 = 16;
        aByte784 = 0;
        aByte787 = 0;
        options = null;
        anInt806 = -1;
        anInt795 = -1;
        aBoolean798 = false;
        aBoolean807 = true;
        anInt780 = 128;
        anInt760 = 128;
        anInt796 = 128;
        anInt764 = 0;
        anInt761 = 0;
        anInt785 = 0;
        anInt766 = 0;
        aBoolean765 = false;
        aBoolean791 = false;
        anInt794 = -1;
        varbitId = -1;
        anInt781 = -1;
        anIntArray805 = null;
    }

    public void init(JagBuffer buf) {
        int i = -1;
        label0:
        do {
            int attribute;
            do {
                attribute = buf.getByte();
                if (attribute == 0)
                    break label0;
                if (attribute == 1) {
                    int k = buf.getByte();
                    if (k > 0)
                        if (anIntArray763 == null || lowMemory) {
                            anIntArray789 = new int[k];
                            anIntArray763 = new int[k];
                            for (int k1 = 0; k1 < k; k1++) {
                                anIntArray763[k1] = buf.getShort();
                                anIntArray789[k1] = buf.getByte();
                            }

                        } else {
                            buf.position += k * 3;
                        }
                } else if (attribute == 2)
                    name = buf.getString();
                else if (attribute == 3)
                    description = buf.getStringBytes();
                else if (attribute == 5) {
                    int l = buf.getByte();
                    if (l > 0)
                        if (anIntArray763 == null || lowMemory) {
                            anIntArray789 = null;
                            anIntArray763 = new int[l];
                            for (int l1 = 0; l1 < l; l1++)
                                anIntArray763[l1] = buf.getShort();

                        } else {
                            buf.position += l * 2;
                        }
                } else if (attribute == 14)
                    anInt801 = buf.getByte();
                else if (attribute == 15)
                    anInt775 = buf.getByte();
                else if (attribute == 17)
                    aBoolean810 = false;
                else if (attribute == 18)
                    aBoolean809 = false;
                else if (attribute == 19) {
                    i = buf.getByte();
                    if (i == 1)
                        aBoolean759 = true;
                } else if (attribute == 21)
                    aBoolean769 = true;
                else if (attribute == 22)
                    aBoolean804 = true;
                else if (attribute == 23)
                    aBoolean797 = true;
                else if (attribute == 24) {
                    animationId = buf.getShort();
                    if (animationId == 65535)
                        animationId = -1;
                } else if (attribute == 28)
                    anInt802 = buf.getByte();
                else if (attribute == 29)
                    aByte784 = buf.getSignedByte();
                else if (attribute == 39)
                    aByte787 = buf.getSignedByte();
                else if (attribute >= 30 && attribute < 39) {
                    if (options == null)
                        options = new String[5];
                    options[attribute - 30] = buf.getString();
                    if (options[attribute - 30].equalsIgnoreCase("hidden"))
                        options[attribute - 30] = null;
                } else if (attribute == 40) {
                    int i1 = buf.getByte();
                    modifiedModelColors = new int[i1];
                    originalModelColors = new int[i1];
                    for (int i2 = 0; i2 < i1; i2++) {
                        modifiedModelColors[i2] = buf.getShort();
                        originalModelColors[i2] = buf.getShort();
                    }

                } else if (attribute == 60)
                    anInt806 = buf.getShort();
                else if (attribute == 62)
                    aBoolean798 = true;
                else if (attribute == 64)
                    aBoolean807 = false;
                else if (attribute == 65)
                    anInt780 = buf.getShort();
                else if (attribute == 66)
                    anInt760 = buf.getShort();
                else if (attribute == 67)
                    anInt796 = buf.getShort();
                else if (attribute == 68)
                    anInt795 = buf.getShort();
                else if (attribute == 69)
                    anInt764 = buf.getByte();
                else if (attribute == 70)
                    anInt761 = buf.getSignedShort();
                else if (attribute == 71)
                    anInt785 = buf.getSignedShort();
                else if (attribute == 72)
                    anInt766 = buf.getSignedShort();
                else if (attribute == 73)
                    aBoolean765 = true;
                else if (attribute == 74) {
                    aBoolean791 = true;
                } else {
                    if (attribute != 75)
                        continue;
                    anInt794 = buf.getByte();
                }
                continue label0;
            } while (attribute != 77);
            varbitId = buf.getShort();
            if (varbitId == 65535)
                varbitId = -1;
            anInt781 = buf.getShort();
            if (anInt781 == 65535)
                anInt781 = -1;
            int j1 = buf.getByte();
            anIntArray805 = new int[j1 + 1];
            for (int j2 = 0; j2 <= j1; j2++) {
                anIntArray805[j2] = buf.getShort();
                if (anIntArray805[j2] == 65535)
                    anIntArray805[j2] = -1;
            }

        } while (true);
        if (i == -1) {
            aBoolean759 = false;
            if (anIntArray763 != null && (anIntArray789 == null || anIntArray789[0] == 10))
                aBoolean759 = true;
            if (options != null)
                aBoolean759 = true;
        }
        if (aBoolean791) {
            aBoolean810 = false;
            aBoolean809 = false;
        }
        if (anInt794 == -1)
            anInt794 = aBoolean810 ? 1 : 0;
    }

    public Model method431(int i, int j, int k, int l, int i1, int j1, int k1) {
        Model class50_sub1_sub4_sub4 = method427(j, k1, 0, i);
        if (class50_sub1_sub4_sub4 == null)
            return null;
        if (aBoolean769 || aBoolean804)
            class50_sub1_sub4_sub4 = new Model(aBoolean769, aBoolean804, 0, class50_sub1_sub4_sub4);
        if (aBoolean769) {
            int l1 = (k + l + i1 + j1) / 4;
            for (int i2 = 0; i2 < class50_sub1_sub4_sub4.anInt1648; i2++) {
                int j2 = class50_sub1_sub4_sub4.anIntArray1649[i2];
                int k2 = class50_sub1_sub4_sub4.anIntArray1651[i2];
                int l2 = k + ((l - k) * (j2 + 64)) / 128;
                int i3 = j1 + ((i1 - j1) * (j2 + 64)) / 128;
                int j3 = l2 + ((i3 - l2) * (k2 + 64)) / 128;
                class50_sub1_sub4_sub4.anIntArray1650[i2] += j3 - l1;
            }

            class50_sub1_sub4_sub4.method582(6);
        }
        return class50_sub1_sub4_sub4;
    }

    public boolean method432(int i, int j) {
        if (i != 26261)
            aBoolean786 = !aBoolean786;
        if (anIntArray789 == null) {
            if (anIntArray763 == null)
                return true;
            if (j != 10)
                return true;
            boolean flag = true;
            for (int l = 0; l < anIntArray763.length; l++)
                flag &= Model.isDownloaded(anIntArray763[l] & 0xffff);

            return flag;
        }
        for (int k = 0; k < anIntArray789.length; k++)
            if (anIntArray789[k] == j)
                return Model.isDownloaded(anIntArray763[k] & 0xffff);

        return true;
    }

    public static void method433(boolean flag) {
        aClass33_779 = null;
        aClass33_762 = null;
        indices = null;
        if (flag) {
            for (int i = 1; i > 0; i++) ;
        }
        cache = null;
        buf = null;
    }

    public ObjectDefinition() {
        anInt768 = -992;
        id = -1;
        aBoolean774 = true;
        name = "null";
        aBoolean786 = true;
        aByte793 = -113;
    }

    public static int indices[];
    public boolean aBoolean759;
    public int anInt760;
    public int anInt761;
    public static LruHashTable aClass33_762 = new LruHashTable(40);
    public int anIntArray763[];
    public int anInt764;
    public boolean aBoolean765;
    public int anInt766;
    public static JagBuffer buf;
    public int anInt768;
    public boolean aBoolean769;
    public static client aClient770;
    public static Model aClass50_Sub1_Sub4_Sub4Array771[] = new Model[4];
    public static boolean lowMemory;
    public int id;
    public boolean aBoolean774;
    public int anInt775;
    public String name;
    public static int cachePos;
    public int varbitId;
    public static LruHashTable aClass33_779 = new LruHashTable(500);
    public int anInt780;
    public int anInt781;
    public static ObjectDefinition cache[];
    public byte description[];
    public byte aByte784;
    public int anInt785;
    public boolean aBoolean786;
    public byte aByte787;
    public int anInt788;
    public int anIntArray789[];
    public String options[];
    public boolean aBoolean791;
    public int originalModelColors[];
    public byte aByte793;
    public int anInt794;
    public int anInt795;
    public int anInt796;
    public boolean aBoolean797;
    public boolean aBoolean798;
    public int modifiedModelColors[];
    public int anInt801;
    public int anInt802;
    public int animationId;
    public boolean aBoolean804;
    public int anIntArray805[];
    public int anInt806;
    public boolean aBoolean807;
    public static int count;
    public boolean aBoolean809;
    public boolean aBoolean810;

}
