package net.dodian.utilities;

// a collection of misc methods

import java.text.NumberFormat;

public class Utils {
    public static int[] buttons_smelting = {15147, 15146, 10247, 9110, 15151, 15150, 15149, 15148, 15155, 15154, 15153,
            15152, 15159, 15158, 15157, 15156, 15163, 15162, 15161, 15160, 29017, 29016, 24253, 16062, 29022, 29020, 29019,
            29018, 29026, 29025, 29024, 29023};
    public static int[] smelt_frame = {2405, 2406, 2407, 2409, 2410, 2411, 2412, 2413};
    public static int[][] smelt_bars = {{2349, 30}, {2351, 65}, {2355, 70}, {2353, 90}, {2357, 115},
            {2359, 150}, {2361, 190}, {2363, 250}};

    public static int[] herbs = {249, 253, 257, 259, 263, 265, 267,};
    public static int[] secondary = {221, 225, 239, 221, 225, 239, 245};
    public static int[] unfinished = {91, 95, 99, 101, 105, 107, 109};
    public static int[] finished = {121, 115, 133, 145, 157, 163, 169};
    public static int[] req = {1, 10, 30, 45, 55, 65, 75};
    public static int[] potexp = {400, 550, 650, 1200, 1300, 1400, 1650};
    public static int[] grimy_herbs = {199, 203, 207, 209, 213, 215, 217};
    public static int[] grimy_herbs_lvl = {1, 10, 30, 45, 55, 65, 75};
    public static int[] grimy_herbs_xp = {3, 5, 7, 9, 11, 13, 15, 17, 19};

    public static int[] pot_1_dose = {119, 125, 137, 143, 149, 161, 167, 173};
    public static int[] pot_2_dose = {117, 123, 135, 141, 147, 159, 165, 171};
    public static int[] pot_3_dose = {115, 121, 133, 139, 145, 157, 163, 169};
    public static int[] pot_4_dose = {113, 2428, 2432, 2434, 2436, 2440, 2442, 2444};

    public static int[] shortExp = {80, 120, 160, 200, 250, 320};
    public static int[] longExp = {90, 135, 180, 230, 290, 360};
    // cooper tin iron coal mith addy
    public static int[] rocks = {7471, 7451, 7484, 7452, 7485, 7455, 7488, 7456, 7489, 7458, 7491, 7459, 7492, 7460, 7493, 7461, 7494};
    public static int[] rockLevels = {1, 1, 1, 1, 1, 15, 15, 30, 30, 40, 40, 55, 55, 70, 70, 85, 85};
    public static long[] mineTimes = {900, 2000, 2000, 2000, 2000, 3000, 3000, 5000, 5000, 6000, 6000, 7000, 7000, 9000, 9000, 35000, 35000};
    public static int[] ore = {1436, 436, 436, 438, 438, 440, 440, 453, 453, 444, 444, 447, 447, 449, 449, 451, 451};
    public static int[] oreExp = {50, 110, 110, 110, 110, 280, 280, 420, 420, 510, 510, 620, 620, 780, 780, 3100, 3100};
    public static int[] picks = {1265, 1267, 1269, 1273, 1271, 1275};
    public static double[] pickBonus = {0.03, 0.035, 0.045, 0.06, 0.08, 0.2};
    public static int[] pickReq = {1, 1, 6, 21, 31, 41};

    // Tree timer here soon!
    public static int[] axes = {1351, 1349, 1353, 1355, 1357, 1359, 6739};
    public static double[] axeBonus = {0.03, 0.035, 0.045, 0.06, 0.08, 0.12, 0.18};
    public static int[] axeReq = {1, 1, 6, 21, 31, 41, 61};

    public static int[] fishSpots = {1510, 1510, 1511, 1511, 1514, 1514, 1517, 1517};
    public static int[] fishId = {317, 335, 377, 371, 7944, 383, 395, 389};
    public static int[] fishAnim = {621, 622, 619, 618, 621, 618, 619, 618};
    public static int[] fishReq = {1, 20, 40, 50, 60, 70, 85, 95};
    public static int[] fishTime = {1200, 1800, 2400, 3000, 2400, 4200, 4800, 5400};
    public static int[] fishTool = {303, 309, 301, 311, 303, 311, 301, 311};
    public static int[] fishExp = {110, 200, 440, 650, 780, 1100, 1450, 1900};
    public static int[] cookIds = {317, 335, 331, 377, 371, 7944, 383, 395, 389};
    public static int[] cookedIds = {315, 333, 329, 379, 373, 7946, 385, 397, 391};
    public static int[] burnId = {323, 343, 343, 381, 375, 7948, 387, 399, 393};
    public static int[] cookExp = {150, 250, 350, 500, 720, 870, 1220, 1600, 2100};
    public static int[] cookLevel = {1, 20, 30, 40, 50, 60, 70, 85, 95};

    // Crafting
    public static int[] uncutGems = {1623, 1621, 1619, 1617, 1631, 6571};
    public static int[] cutGems = {1607, 1605, 1603, 1601, 1615, 6573};
    public static int[] gemReq = {20, 27, 34, 43, 55, 67};
    public static int[] gemExp = {50, 68, 85, 108, 137, 168};

    public static final char playerNameXlateTable[] = {'_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9'};

    public static String longToPlayerName(long l) {
        int i = 0;
        char ac[] = new char[12];
        while (l != 0L) {
            long l1 = l;
            l /= 37L;
            ac[11 - i++] = playerNameXlateTable[(int) (l1 - l * 37L)];
        }
        return new String(ac, 12 - i, i);
    }

    public static void println(String str) {
        System.out.println(str);
    }

    public static String Hex(byte data[], int offset, int len) {
        String temp = "";
        for (int cntr = 0; cntr < len; cntr++) {
            int num = data[offset + cntr] & 0xFF;
            String myStr;
            if (num < 16)
                myStr = "0";
            else
                myStr = "";
            temp += myStr + Integer.toHexString(num) + " ";
        }
        return temp.toUpperCase().trim();
    }

    public static int HexToInt(byte data[], int offset, int len) {
        int temp = 0;
        int i = 1000;
        for (int cntr = 0; cntr < len; cntr++) {
            int num = (data[offset + cntr] & 0xFF) * i;
            temp += (int) num;
            if (i > 1)
                i = i / 1000;
        }
        return temp;
    }

    public static int random(int range) { // 0 till range (range INCLUDED)
        return (int) (java.lang.Math.random() * (range + 1));
    }

    public static double dRandom(int range) { // 0 till range (range INCLUDED)
        return (java.lang.Math.random() * (range + 1));
    }

    public static int random2(int range) { // 1 till range
        return (int) ((java.lang.Math.random() * range) + 1);
    }

    public static double dRandom2(double range) { // 1 till range
        return (java.lang.Math.random() * range) + 1;
    }

    public static int random3(int range) { // 0 till range
        return (int) (java.lang.Math.random() * range);
    }

    public static String longToName(long l) {
        int i = 0;
        char ac[] = new char[99];
        while (l != 0L) {
            long l1 = l;
            l /= 37L;
            ac[11 - i++] = playerNameXlateTable[(int) (l1 - l * 37L)];
        }
        return new String(ac, 12 - i, i);
    }

    public static long playerNameToInt64(String s) {
        long l = 0L;
        for (int i = 0; i < s.length() && i < 12; i++) {
            char c = s.charAt(i);
            l *= 37L;
            if (c >= 'A' && c <= 'Z')
                l += (1 + c) - 65;
            else if (c >= 'a' && c <= 'z')
                l += (1 + c) - 97;
            else if (c >= '0' && c <= '9')
                l += (27 + c) - 48;
        }
        while (l % 37L == 0L && l != 0L)
            l /= 37L;
        return l;
    }

    private static char decodeBuf[] = new char[4096];

    public static String textUnpack(byte packedData[], int size) {
        int idx = 0, highNibble = -1;
        for (int i = 0; i < size * 2; i++) {
            int val = packedData[i / 2] >> (4 - 4 * (i % 2)) & 0xf;
            if (highNibble == -1) {
                if (val < 13)
                    decodeBuf[idx++] = xlateTable[val];
                else
                    highNibble = val;
            } else {
                decodeBuf[idx++] = xlateTable[((highNibble << 4) + val) - 195];
                highNibble = -1;
            }
        }

        return new String(decodeBuf, 0, idx);
    }

    public static char xlateTable[] = {' ', 'e', 't', 'a', 'o', 'i', 'h', 'n', 's', 'r', 'd', 'l', 'u', 'm', 'w', 'c',
            'y', 'f', 'g', 'p', 'b', 'v', 'k', 'x', 'j', 'q', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ' ', '!',
            '?', '.', ',', ':', ';', '(', ')', '-', '&', '*', '\\', '\'', '@', '#', '+', '=', '\243', '$', '%', '"', '[',
            ']'};

    // gets the direction between the two given points
    // valid directions are N:0, NE:2, E:4, SE:6, S:8, SW:10, W:12, NW:14
    // the invalid (inbetween) direction are 1,3,5,7,9,11,13,15 i.e. odd integers
    // returns -1, if src and dest are the same
    public static int direction(int srcX, int srcY, int destX, int destY) {
        int dx = destX - srcX, dy = destY - srcY;
        // a lot of cases that have to be considered here ... is there a more
        // sophisticated (and quick!) way?
        if (dx < 0) {
            if (dy < 0) {
                if (dx < dy)
                    return 11;
                else if (dx > dy)
                    return 9;
                else
                    return 10; // dx == dy
            } else if (dy > 0) {
                if (-dx < dy)
                    return 15;
                else if (-dx > dy)
                    return 13;
                else
                    return 14; // -dx == dy
            } else { // dy == 0
                return 12;
            }
        } else if (dx > 0) {
            if (dy < 0) {
                if (dx < -dy)
                    return 7;
                else if (dx > -dy)
                    return 5;
                else
                    return 6; // dx == -dy
            } else if (dy > 0) {
                if (dx < dy)
                    return 1;
                else if (dx > dy)
                    return 3;
                else
                    return 2; // dx == dy
            } else { // dy == 0
                return 4;
            }
        } else { // dx == 0
            if (dy < 0) {
                return 8;
            } else if (dy > 0) {
                return 0;
            } else { // dy == 0
                return -1; // src and dest are the same
            }
        }
    }

    private static final char[] validChars = {' ', 'e', 't', 'a', 'o', 'i', 'h', 'n', 's', 'r', 'd', 'l', 'u', 'm', 'w',
            'c', 'y', 'f', 'g', 'p', 'b', 'v', 'k', 'x', 'j', 'q', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ' ',
            '!', '?', '.', ',', ':', ';', '(', ')', '-', '&', '*', '\\', '\'', '@', '#', '+', '=', '\243', '$', '%', '"', '[',
            ']'};

    public static byte[] encodePm(String s) {
        byte[] buffer = new byte[100];
        int currentOffset = 0;
        if (s.length() > 80)
            s = s.substring(0, 80);
        s = s.toLowerCase();
        int i = -1;
        for (int j = 0; j < s.length(); j++) {
            char c = s.charAt(j);
            int k = 0;
            for (int l = 0; l < validChars.length; l++) {
                if (c != validChars[l])
                    continue;
                k = l;
                break;
            }

            if (k > 12)
                k += 195;
            if (i == -1) {
                if (k < 13)
                    i = k;
                else
                    // stream.writeWordBigEndian(k);
                    buffer[currentOffset++] = (byte) k;
            } else if (k < 13) {
                // stream.writeWordBigEndian((i << 4) + k);
                buffer[currentOffset++] = (byte) ((i << 4) + k);
                i = -1;
            } else {
                // stream.writeWordBigEndian((i << 4) + (k >> 4));
                buffer[currentOffset++] = (byte) ((i << 4) + (k >> 4));
                i = k & 0xf;
            }
        }
        if (i != -1)
            // stream.writeWordBigEndian(i << 4);
            buffer[currentOffset++] = (byte) (i << 4);
        return buffer;
    }

    public static String format(int num) {
        return NumberFormat.getInstance().format(num);
    }

    public static int getDistance(int coordX1, int coordY1, int coordX2, int coordY2) {
        int deltaX = coordX2 - coordX1;
        int deltaY = coordY2 - coordY1;
        return ((int) Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2)));
    }

    public static byte directionDeltaX[] = new byte[]{0, 1, 1, 1, 0, -1, -1, -1};
    public static byte directionDeltaY[] = new byte[]{1, 1, 0, -1, -1, -1, 0, 1};

    // translates our direction convention to the one used in the protocol
    public static byte xlateDirectionToClient[] = new byte[]{1, 2, 4, 7, 6, 5, 3, 0};
}