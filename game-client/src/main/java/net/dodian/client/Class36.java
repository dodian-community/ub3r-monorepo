package net.dodian.client;

public final class Class36 {

    public static java.util.Hashtable<Integer, Class36> frameList = new java.util.Hashtable<Integer, Class36>();

    public static void load(int file, byte[] array) {
        try {
            final Stream ay = new Stream(array);
            final Class18 b2 = new Class18(ay);
            final int n = ay.readUnsignedWord();
            ;
            animationlist[file] = new Class36[n * 3];
            final int[] array2 = new int[500];
            final int[] array3 = new int[500];
            final int[] array4 = new int[500];
            final int[] array5 = new int[500];
            for (int j = 0; j < n; ++j) {
                final int k = ay.readUnsignedWord();
                ;
                final Class36[] array6 = animationlist[file];
                final int n2 = k;
                final Class36 q = new Class36();
                array6[n2] = q;
                final Class36 q2 = q;
                q.aClass18_637 = b2;
                final int f = ay.readUnsignedByte();
                int c2 = 0;
                int n3 = -1;
                for (int l = 0; l < f; ++l) {
                    final int f2;
                    if ((f2 = ay.readUnsignedByte()) > 0) {
                        if (b2.anIntArray342[l] != 0) {
                            for (int n4 = l - 1; n4 > n3; --n4) {
                                if (b2.anIntArray342[n4] == 0) {
                                    array2[c2] = n4;
                                    array3[c2] = 0;
                                    array5[c2] = (array4[c2] = 0);
                                    ++c2;
                                    break;
                                }
                            }
                        }
                        array2[c2] = l;
                        int n4 = 0;
                        if (b2.anIntArray342[l] == 3) {
                            n4 = 128;
                        }
                        if ((f2 & 0x1) != 0x0) {
                            array3[c2] = ay.readShort2();
                        } else {
                            array3[c2] = n4;
                        }
                        if ((f2 & 0x2) != 0x0) {
                            array4[c2] = ay.readShort2();
                        } else {
                            array4[c2] = n4;
                        }
                        if ((f2 & 0x4) != 0x0) {
                            array5[c2] = ay.readShort2();
                        } else {
                            array5[c2] = n4;
                        }
                        n3 = l;
                        ++c2;
                    }
                }
                q2.anInt638 = c2;
                q2.anIntArray639 = new int[c2];
                q2.anIntArray640 = new int[c2];
                q2.anIntArray641 = new int[c2];
                q2.anIntArray642 = new int[c2];
                for (int l = 0; l < c2; ++l) {
                    q2.anIntArray639[l] = array2[l];
                    q2.anIntArray640[l] = array3[l];
                    q2.anIntArray641[l] = array4[l];
                    q2.anIntArray642[l] = array5[l];
                }
            }
        } catch (Exception ex) {
        }
    }

    public static Class36[][] animationlist;

    public static void nullLoader() {
        animationlist = null;
    }

    public static Class36 method531(int int1) {
        try {
            final String hexString;
            final int int2 = Integer.parseInt((hexString = Integer.toHexString(int1)).substring(0, hexString.length() - 4), 16);
            int1 = Integer.parseInt(hexString.substring(hexString.length() - 4), 16);
            if (animationlist[int2].length == 0) {
                Client.instance.onDemandFetcher.method558(1, int2);
                return null;
            }
            return animationlist[int2][int1];
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static boolean method532(int i) {
        return i == -1;
    }

    public Class36() {
    }

    public int anInt636;
    public Class18 aClass18_637;
    public int anInt638;
    public int anIntArray639[];
    public int anIntArray640[];
    public int anIntArray641[];
    public int anIntArray642[];

}
