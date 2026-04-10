// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

import luna.Constants;
import luna.RsaParser;

import java.math.BigInteger;

public class JagBuffer extends QueueNode {

    public static JagBuffer allocate(int type) {
        synchronized (mediumBuffers) {
            JagBuffer buf = null;
            if (type == 0 && smallBufferCount > 0) {
                smallBufferCount--;
                buf = (JagBuffer) smallBuffers.removeFirst();
            } else if (type == 1 && mediumBufferCount > 0) {
                mediumBufferCount--;
                buf = (JagBuffer) mediumBuffers.removeFirst();
            } else if (type == 2 && largeBufferCount > 0) {
                largeBufferCount--;
                buf = (JagBuffer) largeBuffers.removeFirst();
            }
            if (buf != null) {
                buf.position = 0;
                return buf;
            }
        }
        JagBuffer vector = new JagBuffer();
        vector.position = 0;
        if (type == 0)
            vector.buffer = new byte[100];
        else if (type == 1)
            vector.buffer = new byte[5000];
        else
            vector.buffer = new byte[30000];
        return vector;
    }

    public JagBuffer() {
        // aBoolean1435 = false;
        // anInt1436 = 8;
        // aBoolean1437 = false;
        // aBoolean1438 = true;
        // aByte1439 = 5;
        // anInt1440 = -29290;
        // aBoolean1441 = false;
        // anInt1442 = 217;
        // anInt1443 = 236;
        // aBoolean1444 = false;
        // aByte1447 = 17;
        // aByte1448 = 89;
        // aByte1449 = -16;
        // aBoolean1450 = false;
    }

    public JagBuffer(byte abyte0[]) {
        // aBoolean1435 = false;
        // anInt1436 = 8;
        // aBoolean1437 = false;
        // aBoolean1438 = true;
        // aByte1439 = 5;
        // anInt1440 = -29290;
        // aBoolean1441 = false;
        // anInt1442 = 217;
        // anInt1443 = 236;
        // aBoolean1444 = false;
        // aByte1447 = 17;
        // aByte1448 = 89;
        // aByte1449 = -16;
        // aBoolean1450 = false;
        // anInt1452 = 1;
        buffer = abyte0;
        position = 0;
    }

    public void putOpcode(int opcode) {
        buffer[position++] = (byte) (opcode + random.nextInt());
    }

    public void putByte(int value) {
        buffer[position++] = (byte) value;
    }

    public void putShort(int value) {
        buffer[position++] = (byte) (value >> 8);
        buffer[position++] = (byte) value;
    }

    public void putLEShort(int value) {
        buffer[position++] = (byte) value;
        buffer[position++] = (byte) (value >> 8);
    }

    public void putTriByte(int value) {
        buffer[position++] = (byte) (value >> 16);
        buffer[position++] = (byte) (value >> 8);
        buffer[position++] = (byte) value;
    }

    public void putInt(int value) {
        buffer[position++] = (byte) (value >> 24);
        buffer[position++] = (byte) (value >> 16);
        buffer[position++] = (byte) (value >> 8);
        buffer[position++] = (byte) value;
    }

    public void putLEInt(int value) {
        buffer[position++] = (byte) value;
        buffer[position++] = (byte) (value >> 8);
        buffer[position++] = (byte) (value >> 16);
        buffer[position++] = (byte) (value >> 24);
    }

    public void putLong(long value) {
        buffer[position++] = (byte) (int) (value >> 56);
        buffer[position++] = (byte) (int) (value >> 48);
        buffer[position++] = (byte) (int) (value >> 40);
        buffer[position++] = (byte) (int) (value >> 32);
        buffer[position++] = (byte) (int) (value >> 24);
        buffer[position++] = (byte) (int) (value >> 16);
        buffer[position++] = (byte) (int) (value >> 8);
        buffer[position++] = (byte) (int) value;
    }

    public void putString(String str) {
        byte[] bytes = new byte[str.length()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) str.charAt(i);
        }
        System.arraycopy(bytes, 0, buffer, position, bytes.length);
        position += str.length();
        buffer[position++] = 10;
    }

    public void putBytes(byte bytes[], int start, int length) {
        for (int pos = start; pos < start + length; pos++)
            buffer[position++] = bytes[pos];
    }

    public void putLength(int length) {
        buffer[position - length - 1] = (byte) length;
    }

    public int getByte() {
        return buffer[position++] & 0xff;
    }

    public byte getSignedByte() {
        return buffer[position++];
    }

    public int getShort() {
        position += 2;
        return ((buffer[position - 2] & 0xff) << 8) + (buffer[position - 1] & 0xff);
    }

    public int getSignedShort() {
        position += 2;
        int i = ((buffer[position - 2] & 0xff) << 8) + (buffer[position - 1] & 0xff);
        if (i > 32767)
            i -= 0x10000;
        return i;
    }

    public int getTriByte() {
        position += 3;
        return ((buffer[position - 3] & 0xff) << 16) + ((buffer[position - 2] & 0xff) << 8)
                + (buffer[position - 1] & 0xff);
    }

    public int getInt() {
        position += 4;
        return ((buffer[position - 4] & 0xff) << 24) + ((buffer[position - 3] & 0xff) << 16)
                + ((buffer[position - 2] & 0xff) << 8) + (buffer[position - 1] & 0xff);
    }

    public long getLong() {
        long l = getInt() & 0xffffffffL;
        long l1 = getInt() & 0xffffffffL;
        return (l << 32) + l1;
    }

    public String getString() {
        int start = position;
        while (buffer[position++] != 10) ;
        return new String(buffer, start, position - start - 1);
    }

    public byte[] getStringBytes() {
        int start = position;
        while (buffer[position++] != 10) ;
        byte bytes[] = new byte[position - start - 1];
        for (int pos = start; pos < position - 1; pos++)
            bytes[pos - start] = buffer[pos];
        return bytes;
    }

    public void getBytes(byte bytes[], int start, int len) {
        for (int pos = start; pos < start + len; pos++)
            bytes[pos] = buffer[position++];
    }

    public void initBitAccess() {
        bitPosition = position * 8;
    }

    public int getBits(int numBits) {
        int k = bitPosition >> 3;
        int l = 8 - (bitPosition & 7);
        int value = 0;
        bitPosition += numBits;
        for (; numBits > l; l = 8) {
            value += (buffer[k++] & BIT_MASKS[l]) << numBits - l;
            numBits -= l;
        }

        if (numBits == l)
            value += buffer[k] & BIT_MASKS[l];
        else
            value += buffer[k] >> l - numBits & BIT_MASKS[numBits];
        return value;
    }

    public void finishBitAccess() {
        position = (bitPosition + 7) / 8;
    }

    public int getSignedSmart() {
        int peek = buffer[position] & 0xff;
        if (peek < 128)
            return getByte() - 64;
        else
            return getShort() - 49152;
    }

    public int getSmart() {
        int peek = buffer[position] & 0xff;
        if (peek < 128)
            return getByte();
        else
            return getShort() - 32768;
    }

    public void rsa(BigInteger modulus, BigInteger key) {
        int len = position;
        position = 0;
        byte rawBytes[] = new byte[len];
        getBytes(rawBytes, 0, len);
        BigInteger rawInteger = new BigInteger(rawBytes);
        BigInteger encryptedInteger;
        if (Constants.DECODE_RSA) {
            encryptedInteger = rawInteger.modPow(RsaParser.getExponent(), RsaParser.getModulus());
        } else {
            encryptedInteger = rawInteger;
        }
        byte encryptedBytes[] = encryptedInteger.toByteArray();
        position = 0;
        putByte(encryptedBytes.length);
        putBytes(encryptedBytes, 0, encryptedBytes.length);
    }

    public void putByteAdded(int value) {
        buffer[position++] = (byte) (value + 128);
    }

    public void putByteNegated(int value) {
        buffer[position++] = (byte) (-value);
    }

    public void putByteSubtracted(int value) {
        buffer[position++] = (byte) (128 - value);
    }

    public int getByteAdded() {
        return buffer[position++] - 128 & 0xff;
    }

    public int getByteNegated() {
        return -buffer[position++] & 0xff;
    }

    public int getByteSubtracted() {
        return 128 - buffer[position++] & 0xff;
    }

    public byte getSignedByteAdded() {
        return (byte) (buffer[position++] - 128);
    }

    public byte getSignedByteNegated() {
        return (byte) (-buffer[position++]);
    }

    public byte getSignedByteSubtracted() {
        return (byte) (128 - buffer[position++]);
    }

    // TODO should we remove the duplication?
    public void putLEShortDup(int value) {
        buffer[position++] = (byte) value;
        buffer[position++] = (byte) (value >> 8);
    }

    public void putShortAdded(int value) {
        buffer[position++] = (byte) (value >> 8);
        buffer[position++] = (byte) (value + 128);
    }

    public void putLEShortAdded(int value) {
        buffer[position++] = (byte) (value + 128);
        buffer[position++] = (byte) (value >> 8);
    }

    public int getLEShort() {
        position += 2;
        return ((buffer[position - 1] & 0xff) << 8) + (buffer[position - 2] & 0xff);
    }

    public int getShortAdded() {
        position += 2;
        return ((buffer[position - 2] & 0xff) << 8) + (buffer[position - 1] - 128 & 0xff);
    }

    public int getLEShortA() {
        position += 2;
        return ((buffer[position - 1] & 0xff) << 8) + (buffer[position - 2] - 128 & 0xff);
    }

    public int method552() {
        position += 2;
        int j = ((buffer[position - 1] & 0xff) << 8) + (buffer[position - 2] & 0xff);
        if (j > 32767)
            j -= 0x10000;
        return j;
    }

    public int method553() {
        position += 2;
        int i = ((buffer[position - 2] & 0xff) << 8) + (buffer[position - 1] - 128 & 0xff);
        if (i > 32767)
            i -= 0x10000;
        return i;
    }

    public int method554() {
        position += 3;
        return ((buffer[position - 2] & 0xff) << 16) + ((buffer[position - 3] & 0xff) << 8)
                + (buffer[position - 1] & 0xff);
    }

    public int getLittleInt() {
        position += 4;
        return ((buffer[position - 1] & 0xff) << 24) + ((buffer[position - 2] & 0xff) << 16)
                + ((buffer[position - 3] & 0xff) << 8) + (buffer[position - 4] & 0xff);
    }

    public int method556() {
        position += 4;
        return ((buffer[position - 2] & 0xff) << 24) + ((buffer[position - 1] & 0xff) << 16)
                + ((buffer[position - 4] & 0xff) << 8) + (buffer[position - 3] & 0xff);
    }

    public int getIMInt() {
        position += 4;
        return ((buffer[position - 3] & 0xff) << 24) + ((buffer[position - 4] & 0xff) << 16)
                + ((buffer[position - 1] & 0xff) << 8) + (buffer[position - 2] & 0xff);
    }

    public void getBytesReverse(byte bytes[], int start, int len) {
        for (int pos = (start + len) - 1; pos >= start; pos--)
            bytes[pos] = buffer[position++];
    }

    public void getBytesAdded(byte bytes[], int start, int len) {
        for (int pos = start; pos < start + len; pos++)
            bytes[pos] = (byte) (buffer[position++] - 128);
    }

    // public boolean aBoolean1435;
    // public int anInt1436;
    // public boolean aBoolean1437;
    // public boolean aBoolean1438;
    // public byte aByte1439;
    // public int anInt1440;
    // public boolean aBoolean1441;
    // public int anInt1442;
    // public int anInt1443;
    // public boolean aBoolean1444;
    // public int anInt1445;
    // public int anInt1446;
    // public byte aByte1447;
    // public byte aByte1448;
    // public byte aByte1449;
    // public boolean aBoolean1450;
    // public static boolean aBoolean1451 = true;
    // public int anInt1452;
    public byte buffer[];
    public int position;
    public int bitPosition;
    public static int CRC32_TABLE[];
    public static final int BIT_MASKS[] = {0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383,
            32767, 65535, 0x1ffff, 0x3ffff, 0x7ffff, 0xfffff, 0x1fffff, 0x3fffff, 0x7fffff, 0xffffff, 0x1ffffff,
            0x3ffffff, 0x7ffffff, 0xfffffff, 0x1fffffff, 0x3fffffff, 0x7fffffff, -1};
    public IsaacRandom random;
    public static int smallBufferCount;
    public static int mediumBufferCount;
    public static int largeBufferCount;
    public static LinkedList smallBuffers = new LinkedList();
    public static LinkedList mediumBuffers = new LinkedList();
    public static LinkedList largeBuffers = new LinkedList();
    // public static char aCharArray1465[] = {
    // 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
    // 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
    // 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
    // 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
    // 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
    // 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
    // '8', '9', '+', '/'
    // };
    // public static boolean aBoolean1466;

    static {
        CRC32_TABLE = new int[256];
        for (int pos = 0; pos < 256; pos++) {
            int value = pos;
            for (int pass = 0; pass < 8; pass++)
                if ((value & 1) == 1)
                    value = value >>> 1 ^ 0xedb88320;
                else
                    value >>>= 1;
            CRC32_TABLE[pos] = value;
        }

    }
}
