package net.dodian.utilities;

// a collection of misc methods

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    public static final char[] playerNameXlateTable = {'_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9'};

    public static String longToPlayerName(long l) {
        int i = 0;
        char[] ac = new char[12];
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

    public static String Hex(byte[] data, int offset, int len) {
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

    public static int HexToInt(byte[] data, int offset, int len) {
        int temp = 0;
        int i = 1000;
        for (int cntr = 0; cntr < len; cntr++) {
            int num = (data[offset + cntr] & 0xFF) * i;
            temp += num;
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
        char[] ac = new char[99];
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

    public static long playerNameToLong(String s) {
        long l = 0L;
        for (int i = 0; i < s.length() && i < 12; i++) {
            char c = s.charAt(i);
            l *= 37L;
            if (c >= 'A' && c <= 'Z') {
                l += (1 + c) - 65;
            } else if (c >= 'a' && c <= 'z') {
                l += (1 + c) - 97;
            } else if (c >= '0' && c <= '9') {
                l += (27 + c) - 48;
            }
        }
        for (; l % 37L == 0L && l != 0L; l /= 37L);
        return l;
    }

    private static final char[] decodeBuf = new char[4096];

    public static String textUnpack(byte[] packedData, int size) {
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

    public static char[] xlateTable = {' ', 'e', 't', 'a', 'o', 'i', 'h', 'n', 's', 'r', 'd', 'l', 'u', 'm', 'w', 'c',
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

    public static byte[] directionDeltaX = new byte[]{0, 1, 1, 1, 0, -1, -1, -1};
    public static byte[] directionDeltaY = new byte[]{1, 1, 0, -1, -1, -1, 0, 1};

    // translates our direction convention to the one used in the protocol
    public static byte[] xlateDirectionToClient = new byte[]{1, 2, 4, 7, 6, 5, 3, 0};

	public static void println_debug(String message) {
		if (!net.dodian.utilities.DotEnvKt.getClientPacketTraceEnabled()
				&& !net.dodian.utilities.DotEnvKt.getClientUiTraceEnabled()) {
			return;
		}
		String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		if (logger.isDebugEnabled()) {
			logger.debug("[{}] {}", timestamp, message);
		}
	}

	public static String capitalize(String str) {
	    if (str == null || str.isEmpty()) {
	        return str;
	    }
	    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
	}
}
