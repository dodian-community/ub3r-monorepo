package net.dodian.utilities;

import io.netty.buffer.ByteBuf;

public class Stream {

    public static int bitMaskOut[] = new int[32];

    private static final int frameStackSize = 10;

    static {
        for (int i = 0; i < 32; i++)
            bitMaskOut[i] = (1 << i) - 1;
    }

    public int bitPosition = 0;

    public byte buffer[] = null;

    public int currentOffset = 0;

    private int frameStack[] = new int[frameStackSize];

    private int frameStackPtr = -1;

    public ISAACCipher packetEncryption = null;

    public Stream() {
    }

    public Stream(byte abyte0[]) {
        buffer = abyte0;
        currentOffset = 0;
    }

    public Stream(ByteBuf buf, ISAACCipher cipher) {
        packetEncryption = cipher;
        byte[] bytes = new byte[buf.readableBytes()];
        int readerIndex = buf.readerIndex();
        buffer = buf.getBytes(readerIndex, bytes).array();
        currentOffset = 0;
    }

    public void createFrame(int id) {
        try {
            buffer[currentOffset++] = (byte) (id + packetEncryption
                    .getNextKey());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createFrameVarSize(int id) { // creates a variable sized frame
        buffer[currentOffset++] = (byte) (id + packetEncryption.getNextKey());
        buffer[currentOffset++] = 0; // place holder for size byte
        if (frameStackPtr >= frameStackSize - 1) {
            throw new RuntimeException("Stack overflow");
        } else
            frameStack[++frameStackPtr] = currentOffset;
    }

    public void createFrameVarSizeWord(int id) { // creates a variable sized
        // frame
        buffer[currentOffset++] = (byte) (id + packetEncryption.getNextKey());
        writeWord(0); // place holder for size word
        if (frameStackPtr >= frameStackSize - 1) {
            throw new RuntimeException("Stack overflow");
        } else
            frameStack[++frameStackPtr] = currentOffset;
    }

    public void endFrameVarSize() {// ends a variable sized frame
        if (frameStackPtr < 0)
            throw new RuntimeException("Stack empty");
        else
            writeFrameSize(currentOffset - frameStack[frameStackPtr--]);
    }

    public void endFrameVarSizeWord() { // ends a variable sized frame
        if (frameStackPtr < 0)
            throw new RuntimeException("Stack empty");
        else
            writeFrameSizeWord(currentOffset - frameStack[frameStackPtr--]);
    }

    public void finishBitAccess() {
        currentOffset = (bitPosition + 7) / 8;
    }

    public void initBitAccess() {
        bitPosition = currentOffset * 8;
    }

    public void readBytes(byte abyte0[], int i, int j) {
        for (int k = j; k < j + i; k++)
            abyte0[k] = buffer[currentOffset++];

    }

    public void readBytes_reverse(byte abyte0[], int i, int j) {
        for (int k = (j + i) - 1; k >= j; k--) {
            abyte0[k] = buffer[currentOffset++];

        }
    }

    public void readBytes_reverseA(byte abyte0[], int i, int j) {
        for (int k = (j + i) - 1; k >= j; k--)
            abyte0[k] = (byte) (buffer[currentOffset++] - 128);

    }

    public int readDWord() {
        currentOffset += 4;
        return ((buffer[currentOffset - 4] & 0xff) << 24)
                + ((buffer[currentOffset - 3] & 0xff) << 16)
                + ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] & 0xff);
    }

    public int readDWord_v1() {
        currentOffset += 4;
        return ((buffer[currentOffset - 2] & 0xff) << 24)
                + ((buffer[currentOffset - 1] & 0xff) << 16)
                + ((buffer[currentOffset - 4] & 0xff) << 8)
                + (buffer[currentOffset - 3] & 0xff);
    }

    public int readDWord_v2() {
        currentOffset += 4;
        return ((buffer[currentOffset - 3] & 0xff) << 24)
                + ((buffer[currentOffset - 4] & 0xff) << 16)
                + ((buffer[currentOffset - 1] & 0xff) << 8)
                + (buffer[currentOffset - 2] & 0xff);
    }

    public long readQWord() {
        long l = readDWord() & 0xffffffffL;
        long l1 = readDWord() & 0xffffffffL;
        return (l << 32) + l1;
    }

    public byte readSignedByte() {
        return buffer[currentOffset++];
    }

    public byte readSignedByteA() {
        return (byte) (buffer[currentOffset++] - 128);
    }

    public byte readSignedByteC() {
        return (byte) (-buffer[currentOffset++]);
    }

    public byte readSignedByteS() {
        return (byte) (128 - buffer[currentOffset++]);
    }

    public int readSignedWord() {
        currentOffset += 2;
        int i = ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] & 0xff);
        if (i > 32767) {
            i -= 0x10000;
        }
        return i;
    }

    public int readSignedWordA() {
        currentOffset += 2;
        int i = ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] - 128 & 0xff);
        if (i > 32767) {
            i -= 0x10000;
        }
        return i;
    }

    public int readSignedWordBigEndian() {
        currentOffset += 2;
        int i = ((buffer[currentOffset - 1] & 0xff) << 8)
                + (buffer[currentOffset - 2] & 0xff);
        if (i > 32767) {
            i -= 0x10000;
        }
        return i;
    }

    public int readSignedWordBigEndianA() {
        currentOffset += 2;
        int i = ((buffer[currentOffset - 1] & 0xff) << 8)
                + (buffer[currentOffset - 2] - 128 & 0xff);
        if (i > 32767)
            i -= 0x10000;
        return i;
    }

    public java.lang.String readString() {
        int i = currentOffset;
        while (buffer[currentOffset++] != 10)
            ;
        return new String(buffer, i, currentOffset - i - 1);
    }

    public int readUnsignedByte() {
        return buffer[currentOffset++] & 0xff;
    }

    public int readUnsignedByteA() {
        return buffer[currentOffset++] - 128 & 0xff;
    }

    public int readUnsignedByteC() {
        return -buffer[currentOffset++] & 0xff;
    }

    public int readUnsignedByteS() {
        return 128 - buffer[currentOffset++] & 0xff;
    }

    public int readUnsignedWord() {
        currentOffset += 2;
        return ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] & 0xff);
    }

    public int readUnsignedWordA() {
        currentOffset += 2;
        return ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] - 128 & 0xff);
    }

    public int readUnsignedWordBigEndian() {
        currentOffset += 2;
        return ((buffer[currentOffset - 1] & 0xff) << 8)
                + (buffer[currentOffset - 2] & 0xff);
    }

    public int readUnsignedWordBigEndianA() {
        currentOffset += 2;
        return ((buffer[currentOffset - 1] & 0xff) << 8)
                + (buffer[currentOffset - 2] - 128 & 0xff);
    }

    public void write3Byte(int i) {
        buffer[currentOffset++] = (byte) (i >> 16);
        buffer[currentOffset++] = (byte) (i >> 8);
        buffer[currentOffset++] = (byte) i;
    }

    public void writeBits(int numBits, int value) {
        int bytePos = bitPosition >> 3;
        int bitOffset = 8 - (bitPosition & 7);
        bitPosition += numBits;

        for (; numBits > bitOffset; bitOffset = 8) {
            buffer[bytePos] &= ~bitMaskOut[bitOffset]; // mask out the
            // desired area
            buffer[bytePos++] |= (value >> (numBits - bitOffset))
                    & bitMaskOut[bitOffset];

            numBits -= bitOffset;
        }
        if (numBits == bitOffset) {
            buffer[bytePos] &= ~bitMaskOut[bitOffset];
            buffer[bytePos] |= value & bitMaskOut[bitOffset];
        } else {
            buffer[bytePos] &= ~(bitMaskOut[numBits] << (bitOffset - numBits));
            buffer[bytePos] |= (value & bitMaskOut[numBits]) << (bitOffset - numBits);
        }
    }

    public void writeByte(int i) {
        buffer[currentOffset++] = (byte) i;
    }

    public void writeByteA(int i) {
        buffer[currentOffset++] = (byte) (i + 128);
    }

    public void writeByteC(int i) {
        buffer[currentOffset++] = (byte) (-i);
    }

    public void writeBytes(byte abyte0[], int i, int j) {
        for (int k = j; k < j + i; k++)
            buffer[currentOffset++] = abyte0[k];

    }

    public void writeByteS(int i) {
        buffer[currentOffset++] = (byte) (128 - i);
    }

    public void writeBytes_reverse(byte abyte0[], int i, int j) {
        for (int k = (j + i) - 1; k >= j; k--)
            buffer[currentOffset++] = abyte0[k];

    }

    public void writeBytes_reverseA(byte abyte0[], int i, int j) {
        for (int k = (j + i) - 1; k >= j; k--)
            buffer[currentOffset++] = (byte) (abyte0[k] + 128);

    }

    public void writeDWord(int i) {
        buffer[currentOffset++] = (byte) (i >> 24);
        buffer[currentOffset++] = (byte) (i >> 16);
        buffer[currentOffset++] = (byte) (i >> 8);
        buffer[currentOffset++] = (byte) i;
    }

    public void writeDWord_v1(int i) {
        buffer[currentOffset++] = (byte) (i >> 8);
        buffer[currentOffset++] = (byte) i;
        buffer[currentOffset++] = (byte) (i >> 24);
        buffer[currentOffset++] = (byte) (i >> 16);
    }

    public void writeDWord_v2(int i) {
        buffer[currentOffset++] = (byte) (i >> 16);
        buffer[currentOffset++] = (byte) (i >> 24);
        buffer[currentOffset++] = (byte) i;
        buffer[currentOffset++] = (byte) (i >> 8);
    }

    public void writeDWordBigEndian(int i) {
        buffer[currentOffset++] = (byte) i;
        buffer[currentOffset++] = (byte) (i >> 8);
        buffer[currentOffset++] = (byte) (i >> 16);
        buffer[currentOffset++] = (byte) (i >> 24);
    }

    public void writeFrameSize(int i) {
        buffer[currentOffset - i - 1] = (byte) i;
    }

    public void writeFrameSizeWord(int i) {
        buffer[currentOffset - i - 2] = (byte) (i >> 8);
        buffer[currentOffset - i - 1] = (byte) i;
    }

    public void writeQWord(long l) {
        buffer[currentOffset++] = (byte) (int) (l >> 56);
        buffer[currentOffset++] = (byte) (int) (l >> 48);
        buffer[currentOffset++] = (byte) (int) (l >> 40);
        buffer[currentOffset++] = (byte) (int) (l >> 32);
        buffer[currentOffset++] = (byte) (int) (l >> 24);
        buffer[currentOffset++] = (byte) (int) (l >> 16);
        buffer[currentOffset++] = (byte) (int) (l >> 8);
        buffer[currentOffset++] = (byte) (int) l;
    }

    @SuppressWarnings("deprecation")
    public void writeString(java.lang.String s) {
        s.getBytes(0, s.length(), buffer, currentOffset);
        currentOffset += s.length();
        buffer[currentOffset++] = 10;
    }

    public void writeWord(int i) {
        buffer[currentOffset++] = (byte) (i >> 8);
        buffer[currentOffset++] = (byte) i;
    }

    public void writeWordA(int i) {
        buffer[currentOffset++] = (byte) (i >> 8);
        buffer[currentOffset++] = (byte) (i + 128);
    }

    public void writeWordBigEndian(int i) {
        buffer[currentOffset++] = (byte) i;
        buffer[currentOffset++] = (byte) (i >> 8);
    }

    public void writeWordBigEndian_dup(int i) {
        buffer[currentOffset++] = (byte) i;
        buffer[currentOffset++] = (byte) (i >> 8);
    }

    public void writeWordBigEndianA(int i) {
        buffer[currentOffset++] = (byte) (i + 128);
        buffer[currentOffset++] = (byte) (i >> 8);
    }

}
