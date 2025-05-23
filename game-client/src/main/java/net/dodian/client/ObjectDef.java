package net.dodian.client;

import java.io.FileWriter;

public final class ObjectDef {

    public static ObjectDef forID(int i) {
        if (i > streamIndices.length)
            i = streamIndices.length - 1;
        /* Seers village fix */
		/*if (i == 25913)
			i = 15552;
		if (i == 25916 || i == 25926)
			i = 15553;
		if (i == 25917)
			i = 15554;*/
        for (int j = 0; j < 20; j++)
            if (cache[j].type == i)
                return cache[j];
        cacheIndex = (cacheIndex + 1) % 20;

        ObjectDef class46 = cache[cacheIndex];
        stream.currentOffset = streamIndices[i];
        class46.type = i;
        class46.setDefaults();
        class46.readValues(stream);
        customValues(class46);
        return class46;
    }

    public static void objectDump(int max) {
        try {
            FileWriter fw = new FileWriter(System.getProperty("user.home") + "/Object Dump.txt");
            for (int i = 0; i < max; i++) {
                ObjectDef def = ObjectDef.forID(i);
                if (def != null) {
                    fw.write("case " + i + ":");
                    fw.write(System.getProperty("line.separator"));
                    fw.write("itemDef.name = \"" + def.name + "\";");
                    if (def.actions != null) {
                        fw.write(System.getProperty("line.separator"));
                        fw.write("Object.actions = new String[" + def.actions.length + "]");
                        fw.write(System.getProperty("line.separator"));
                        for (int act = 0; act < def.actions.length && def.actions != null; act++) {
                            if (def.actions[act] != null) {
                                fw.write("Object.actions[" + act + "] = \"" + def.actions[act] + "\";");
                                fw.write(System.getProperty("line.separator"));
                            }
                        }
                    }
                    fw.write(System.getProperty("line.separator"));
                } else {
                    fw.write("case " + i + ":");
                    fw.write(System.getProperty("line.separator"));
                    fw.write("itemDef.name = \"NULL\";");
                }
            }
            System.out.println("Done dumping!");
            fw.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static void customValues(ObjectDef def) {
        if (def.type >= 15552 && def.type <= 15554) {
            def.aBoolean764 = true;
        }
        if (def.type == 2097) {
            def.actions = new String[5];
            def.actions[0] = "Smith";
        }
        if (def.type == 3994) {
            def.actions = new String[5];
            def.actions[0] = "Smelt";
            def.actions[1] = "Gold Craft";
        }
        if (def.type == 133) {
            def.name = "Dragon Ladder";
        }
        if (def.type == 7451 || def.type == 7484) {
            def.name = "Copper Rock";
        }
        if (def.type == 7452 || def.type == 7485) {
            def.name = "Tin Rock";
        }
        if (def.type == 7455 || def.type == 7488) {
            def.name = "Iron Rock";
        }
        if (def.type == 7456 || def.type == 7489) {
            def.name = "Coal Rock";
        }
        if (def.type == 7458 || def.type == 7491) {
            def.name = "Gold Rock";
        }
        if (def.type == 7459 || def.type == 7492) {
            def.name = "Mithril Rock";
        }
        if (def.type == 7460 || def.type == 7493) {
            def.name = "Adamant Rock";
        }
        if (def.type == 7461 || def.type == 7494) {
            def.name = "Runite Rock";
        }
        if (def.type == 16664 /*&& def.anInt744 == 2603 && anInt761 == 3078*/) {
            //anInt744 anInt761
            //def.actions[2] = "Boss Check";
        }
        if (def.type >= 26259 && def.type <= 26263) {
            def.name = "Hidden cave";
            def.actions = new String[5];
            def.actions[0] = "Enter";
        }
        if (def.type == 19038) {
            def.name = "Supertje's X-mas Tree";
            def.actions = new String[5];
            def.actions[0] = "Chop down";
        }
        if (def.type == 4528) {
            def.name = "The Oaktree Fountain";
            def.actions = new String[5];
        }
        if (def.type == 585) {
            def.name = "Statue of king Nozemi";
            def.actions = new String[5];
        }
        if (def.type == 586) {
            def.name = "Statue of the holy Pro Noob";
            def.actions = new String[5];
        }

    }

    public void setDefaults() {
        anIntArray773 = null;
        anIntArray776 = null;
        name = null;
        description = null;
        modifiedModelColors = null;
        originalModelColors = null;
        anInt744 = 1;
        anInt761 = 1;
        aBoolean767 = true;
        aBoolean757 = true;
        hasActions = false;
        aBoolean762 = false;
        aBoolean769 = false;
        aBoolean764 = false;
        anInt781 = -1;
        anInt775 = 16;
        aByte737 = 0;
        aByte742 = 0;
        actions = null;
        anInt746 = -1;
        anInt758 = -1;
        aBoolean751 = false;
        aBoolean779 = true;
        anInt748 = 128;
        anInt772 = 128;
        anInt740 = 128;
        anInt768 = 0;
        anInt738 = 0;
        anInt745 = 0;
        anInt783 = 0;
        aBoolean736 = false;
        aBoolean766 = false;
        anInt760 = -1;
        anInt774 = -1;
        anInt749 = -1;
        childrenIDs = null;
    }

    public void method574(OnDemandFetcher class42_sub1) {
        if (anIntArray773 == null)
            return;
        for (int j = 0; j < anIntArray773.length; j++)
            class42_sub1.method560(anIntArray773[j] & 0xffff, 0);
    }

    public static void nullLoader() {
        mruNodes1 = null;
        mruNodes2 = null;
        streamIndices = null;
        cache = null;
        stream = null;
    }

    public static void unpackConfig(StreamLoader streamLoader) {
        stream = new Stream(streamLoader.getDataForName("loc.dat"));
        Stream stream = new Stream(streamLoader.getDataForName("loc.idx"));
        int totalObjects = stream.readUnsignedWord();
        System.out.println(String.format("Loaded: %d objects", totalObjects));
        streamIndices = new int[totalObjects];
        int i = 2;
        for (int j = 0; j < totalObjects; j++) {
            streamIndices[j] = i;
            i += stream.readUnsignedWord();
        }
        cache = new ObjectDef[20];
        for (int k = 0; k < 20; k++)
            cache[k] = new ObjectDef();
        //objectDump(totalObjects);
    }

    public boolean method577(int i) {
        if (anIntArray776 == null) {
            if (anIntArray773 == null)
                return true;
            if (i != 10)
                return true;
            boolean flag1 = true;
            for (int k = 0; k < anIntArray773.length; k++)
                flag1 &= Model.method463(anIntArray773[k] & 0xffff);

            return flag1;
        }
        for (int j = 0; j < anIntArray776.length; j++)
            if (anIntArray776[j] == i)
                return Model.method463(anIntArray773[j] & 0xffff);

        return true;
    }

    public Model method578(int i, int j, int k, int l, int i1, int j1, int k1) {
        Model model = method581(i, k1, j);
        if (model == null)
            return null;
        if (aBoolean762 || aBoolean769)
            model = new Model(aBoolean762, aBoolean769, model);
        if (aBoolean762) {
            int l1 = (k + l + i1 + j1) / 4;
            for (int i2 = 0; i2 < model.anInt1626; i2++) {
                int j2 = model.anIntArray1627[i2];
                int k2 = model.anIntArray1629[i2];
                int l2 = k + ((l - k) * (j2 + 64)) / 128;
                int i3 = j1 + ((i1 - j1) * (j2 + 64)) / 128;
                int j3 = l2 + ((i3 - l2) * (k2 + 64)) / 128;
                model.anIntArray1628[i2] += j3 - l1;
            }

            model.method467();
        }
        return model;
    }

    public boolean method579() {
        if (anIntArray773 == null)
            return true;
        boolean flag1 = true;
        for (int i = 0; i < anIntArray773.length; i++)
            flag1 &= Model.method463(anIntArray773[i] & 0xffff);
        return flag1;
    }

    public ObjectDef method580() {
        int i = -1;
        if (anInt774 != -1) {
            VarBit varBit = VarBit.cache[anInt774];
            int j = varBit.anInt648;
            int k = varBit.anInt649;
            int l = varBit.anInt650;
            int i1 = Client.anIntArray1232[l - k];
            i = clientInstance.variousSettings[j] >> k & i1;
        } else if (anInt749 != -1)
            i = clientInstance.variousSettings[anInt749];
        if (i < 0 || i >= childrenIDs.length || childrenIDs[i] == -1)
            return null;
        else
            return forID(childrenIDs[i]);
    }

    public Model method581(int j, int k, int l) {
        Model model = null;
        long l1;
        if (anIntArray776 == null) {
            if (j != 10)
                return null;
            l1 = (long) ((type << 8) + (j << 3) + l) + ((long) (k + 1) << 32);
            Model model_1 = (Model) mruNodes2.insertFromCache(l1);
            if (model_1 != null)
                return model_1;
            if (anIntArray773 == null)
                return null;
            boolean flag1 = aBoolean751 ^ (l > 3);
            int k1 = anIntArray773.length;
            for (int i2 = 0; i2 < k1; i2++) {
                int l2 = anIntArray773[i2];
                if (flag1)
                    l2 += 0x10000;
                model = (Model) mruNodes1.insertFromCache(l2);
                if (model == null) {
                    model = Model.method462(l2 & 0xffff);
                    if (model == null)
                        return null;
                    if (flag1)
                        model.method477();
                    mruNodes1.removeFromCache(model, l2);
                }
                if (k1 > 1)
                    aModelArray741s[i2] = model;
            }

            if (k1 > 1)
                model = new Model(k1, aModelArray741s);
        } else {
            int i1 = -1;
            for (int j1 = 0; j1 < anIntArray776.length; j1++) {
                if (anIntArray776[j1] != j)
                    continue;
                i1 = j1;
                break;
            }

            if (i1 == -1)
                return null;
            l1 = (long) ((type << 8) + (i1 << 3) + l) + ((long) (k + 1) << 32);
            Model model_2 = (Model) mruNodes2.insertFromCache(l1);
            if (model_2 != null)
                return model_2;
            int j2 = anIntArray773[i1];
            boolean flag3 = aBoolean751 ^ (l > 3);
            if (flag3)
                j2 += 0x10000;
            model = (Model) mruNodes1.insertFromCache(j2);
            if (model == null) {
                model = Model.method462(j2 & 0xffff);
                if (model == null)
                    return null;
                if (flag3)
                    model.method477();
                mruNodes1.removeFromCache(model, j2);
            }
        }
        boolean flag;
        flag = anInt748 != 128 || anInt772 != 128 || anInt740 != 128;
        boolean flag2;
        flag2 = anInt738 != 0 || anInt745 != 0 || anInt783 != 0;
        Model model_3 = new Model(modifiedModelColors == null, Class36
                .method532(k), l == 0 && k == -1 && !flag && !flag2, model);
        if (k != -1) {
            model_3.method469();
            model_3.method470(k);
            model_3.anIntArrayArray1658 = null;
            model_3.anIntArrayArray1657 = null;
        }
        while (l-- > 0)
            model_3.method473();
        if (modifiedModelColors != null) {
            for (int k2 = 0; k2 < modifiedModelColors.length; k2++)
                model_3.method476(modifiedModelColors[k2],
                        originalModelColors[k2]);

        }
        if (flag)
            model_3.method478(anInt748, anInt740, anInt772);
        if (flag2)
            model_3.method475(anInt738, anInt745, anInt783);
        //model_3.method479(64, 768, -50, -10, -50, !aBoolean769);
        //model_3.method479(64 + aByte737, 768 + aByte742 * 5, -50, -10, -50, !aBoolean769);

        model_3.setLighting(84, 1500, -90, -280, -70, !aBoolean769);
        if (anInt760 == 1)
            model_3.anInt1654 = model_3.modelHeight;
        mruNodes2.removeFromCache(model_3, l1);
        return model_3;
    }

    public void readValues(Stream stream) {
        int flag = -1;
        do {
            int type = stream.readUnsignedByte();
            if (type == 0)
                break;
            if (type == 1) {
                int len = stream.readUnsignedByte();
                if (len > 0) {
                    if (anIntArray773 == null) {
                        anIntArray776 = new int[len];
                        anIntArray773 = new int[len];
                        for (int k1 = 0; k1 < len; k1++) {
                            anIntArray773[k1] = stream.readUnsignedWord();
                            anIntArray776[k1] = stream.readUnsignedByte();
                        }
                    } else {
                        stream.currentOffset += len * 3;
                    }
                }
            } else if (type == 2)
                name = stream.readString();
            else if (type == 5) {
                int len = stream.readUnsignedByte();
                if (len > 0) {
                    if (anIntArray773 == null) {
                        anIntArray776 = null;
                        anIntArray773 = new int[len];
                        for (int l1 = 0; l1 < len; l1++)
                            anIntArray773[l1] = stream.readUnsignedWord();
                    } else {
                        stream.currentOffset += len * 2;
                    }
                }
            } else if (type == 14)
                anInt744 = stream.readUnsignedByte();
            else if (type == 15)
                anInt761 = stream.readUnsignedByte();
            else if (type == 17)
                aBoolean767 = false;
            else if (type == 18)
                aBoolean757 = false;
            else if (type == 19)
                hasActions = (stream.readUnsignedByte() == 1);
            else if (type == 21)
                aBoolean762 = true;
            else if (type == 22)
                aBoolean769 = false;
            else if (type == 23)
                aBoolean764 = true;
            else if (type == 24) {
                anInt781 = stream.readUnsignedWord();
                if (anInt781 == 0xFFFF)
                    anInt781 = -1;
            } else if (type == 28)
                anInt775 = stream.readUnsignedByte();
            else if (type == 29)
                aByte737 = stream.readSignedByte();
            else if (type == 39)
                aByte742 = (byte) (stream.readSignedByte() * 25);
            else if (type >= 30 && type < 39) {
                if (actions == null)
                    actions = new String[5];
                actions[type - 30] = stream.readString();
                if (actions[type - 30].equalsIgnoreCase("Hidden"))
                    actions[type - 30] = null;
            } else if (type == 40) {
                int i1 = stream.readUnsignedByte();
                modifiedModelColors = new int[i1];
                originalModelColors = new int[i1];
                for (int i2 = 0; i2 < i1; i2++) {
                    modifiedModelColors[i2] = stream.readUnsignedWord();
                    originalModelColors[i2] = stream.readUnsignedWord();
                }
            } else if (type == 41) {
                int i1 = stream.readUnsignedByte();
                //modifiedModelColors = new int[i1];
                //originalModelColors = new int[i1];
                for (int i2 = 0; i2 < i1; i2++) {
                    stream.readUnsignedWord();
                    stream.readUnsignedWord();
                    //modifiedModelColors[i2] = stream.readUnsignedWord();
                    //originalModelColors[i2] = stream.readUnsignedWord();
                }
            } else if (type == 62)
                aBoolean751 = true;
            else if (type == 64)
                aBoolean779 = false;
            else if (type == 65)
                anInt748 = stream.readUnsignedWord();
            else if (type == 66)
                anInt772 = stream.readUnsignedWord();
            else if (type == 67)
                anInt740 = stream.readUnsignedWord();
            else if (type == 68)
                anInt758 = stream.readUnsignedWord();
            else if (type == 69)
                anInt768 = stream.readUnsignedByte();
            else if (type == 70)
                anInt738 = stream.readSignedWord();
            else if (type == 71)
                anInt745 = stream.readSignedWord();
            else if (type == 72)
                anInt783 = stream.readSignedWord();
            else if (type == 73)
                aBoolean736 = true;
            else if (type == 74)
                aBoolean766 = true;
            else if (type == 75)
                anInt760 = stream.readUnsignedByte();
            else if (type == 78) {
                stream.readUnsignedWord(); // ambient sound id
                stream.readUnsignedByte();
            } else if (type == 79) {
                stream.readUnsignedWord();
                stream.readUnsignedWord();
                stream.readUnsignedByte();
                int len = stream.readUnsignedByte();
                for (int i = 0; i < len; i++) {
                    stream.readUnsignedWord();
                }
            } else if (type == 81)
                anInt760 = stream.readUnsignedByte();
            else if (type == 82) {
                int test = stream.readUnsignedWord();
                if (test == 0xFFFF)
                    test = -1;
            } else if (type == 77 || type == 92) {
                anInt774 = stream.readUnsignedWord();
                if (anInt774 == 65535)
                    anInt774 = -1;
                anInt749 = stream.readUnsignedWord();
                if (anInt749 == 65535)
                    anInt749 = -1;
                int value = -1;
                if (type == 92) {
                    value = stream.readUnsignedWord();

                    if (value == 0xFFFF) {
                        value = -1;
                    }
                }
                /* Morphis */
                int j1 = stream.readUnsignedByte();
                childrenIDs = new int[j1 + 2];
                for (int j2 = 0; j2 <= j1; j2++) {
                    childrenIDs[j2] = stream.readUnsignedWord();
                    if (childrenIDs[j2] == 0xFFFF)
                        childrenIDs[j2] = -1;
                    childrenIDs[j2 + 1] = value;
                }
            }
        } while (true);
        if (flag == -1 && name != "null" && name != null) {
            hasActions = anIntArray773 != null
                    && (anIntArray776 == null || anIntArray776[0] == 10);
            if (actions != null)
                hasActions = true;
        }
        if (aBoolean766) {
            aBoolean767 = false;
            aBoolean757 = false;
        }
        if (anInt760 == -1)
            anInt760 = aBoolean767 ? 1 : 0;
    }

    public ObjectDef() {
        type = -1;
    }

    public boolean aBoolean736;
    public byte aByte737;
    public int anInt738;
    public String name;
    public int anInt740;
    public static final Model[] aModelArray741s = new Model[4];
    public byte aByte742;
    public int anInt744;
    public int anInt745;
    public int anInt746;
    public int[] originalModelColors;
    public int anInt748;
    public int anInt749;
    public boolean aBoolean751;
    public static boolean lowMem;
    public static Stream stream;
    public int type;
    public static int[] streamIndices;
    public boolean aBoolean757;
    public int anInt758;
    public int childrenIDs[];
    public int anInt760;
    public static int anInt761;
    public boolean aBoolean762;
    public boolean aBoolean764;
    public static Client clientInstance;
    public boolean aBoolean766;
    public boolean aBoolean767;
    public int anInt768;
    public boolean aBoolean769;
    public static int cacheIndex;
    public int anInt772;
    public int[] anIntArray773;
    public int anInt774;
    public int anInt775;
    public int[] anIntArray776;
    public byte description[];
    public boolean hasActions;
    public boolean aBoolean779;
    public static MRUNodes mruNodes2 = new MRUNodes(30);
    public int anInt781;
    public static ObjectDef[] cache;
    public int anInt783;
    public int[] modifiedModelColors;
    public static MRUNodes mruNodes1 = new MRUNodes(500);
    public String actions[];

}
