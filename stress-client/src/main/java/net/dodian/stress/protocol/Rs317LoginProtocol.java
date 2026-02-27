package net.dodian.stress.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

public final class Rs317LoginProtocol {

    private static final int HANDSHAKE_OPCODE = 14;
    private static final int HANDSHAKE_BYTES = 17;
    private static final int RSA_MAGIC = 255;
    private static final int RSA_PACKET_ID = 10;
    private static final int LOGIN_TYPE_NEW = 16;
    private static final int LOGIN_TYPE_RECONNECT = 18;
    private static final int LOGIN_SUCCESS = 2;

    private Rs317LoginProtocol() {
    }

    public static LoginSession login(InputStream in,
                                     OutputStream out,
                                     String username,
                                     String password,
                                     boolean reconnecting,
                                     int clientVersion,
                                     boolean lowMemory) throws IOException {
        writeHandshake(out);

        byte[] handshake = readExactly(in, HANDSHAKE_BYTES);
        int handshakeCode = handshake[8] & 0xFF;
        if (handshakeCode != 0) {
            throw new IOException("Handshake rejected with code " + handshakeCode);
        }

        long serverSeed = ByteBuffer.wrap(handshake, 9, 8).getLong();
        long clientSeed = ThreadLocalRandom.current().nextLong();
        byte[] rsaBlock = buildRsaBlock(clientSeed, serverSeed, username, password);
        byte[] loginBlock = buildLoginBlock(reconnecting, clientVersion, lowMemory, rsaBlock);

        out.write(loginBlock);
        out.flush();

        int loginCode = in.read();
        if (loginCode < 0) {
            throw new EOFException("Server closed while waiting for login response");
        }
        if (loginCode != LOGIN_SUCCESS) {
            throw new IOException("Login rejected with code " + loginCode);
        }

        int rights = in.read();
        if (rights < 0) {
            throw new EOFException("Server closed before sending rights");
        }

        int[] seed = new int[]{
                (int) (clientSeed >>> 32),
                (int) clientSeed,
                (int) (serverSeed >>> 32),
                (int) serverSeed
        };
        Rs317IsaacCipher outCipher = new Rs317IsaacCipher(seed);

        return new LoginSession(rights, outCipher);
    }

    public static void writeKeepAlive(OutputStream out, Rs317IsaacCipher outCipher) throws IOException {
        int encryptedOpcode = (outCipher.getNextKey()) & 0xFF;
        int encryptedLength = (2 + outCipher.getNextKey()) & 0xFF;
        out.write(encryptedOpcode);
        out.write(encryptedLength);
        out.flush();
    }

    private static void writeHandshake(OutputStream out) throws IOException {
        out.write(HANDSHAKE_OPCODE);
        out.write(0); // name hash (ignored by server)
        out.flush();
    }

    private static byte[] buildRsaBlock(long clientSeed,
                                        long serverSeed,
                                        String username,
                                        String password) throws IOException {
        ByteArrayOutputStream rsa = new ByteArrayOutputStream(128);
        DataOutputStream rsaOut = new DataOutputStream(rsa);
        rsaOut.writeByte(RSA_PACKET_ID);
        rsaOut.writeLong(clientSeed);
        rsaOut.writeLong(serverSeed);
        rsaOut.writeByte(10); // empty server string terminator
        writeRuneString(rsaOut, username);
        writeRuneString(rsaOut, password == null ? "" : password);
        rsaOut.flush();
        return rsa.toByteArray();
    }

    private static byte[] buildLoginBlock(boolean reconnecting,
                                          int clientVersion,
                                          boolean lowMemory,
                                          byte[] rsaBlock) throws IOException {
        ByteArrayOutputStream login = new ByteArrayOutputStream(256);
        DataOutputStream loginOut = new DataOutputStream(login);
        loginOut.writeByte(reconnecting ? LOGIN_TYPE_RECONNECT : LOGIN_TYPE_NEW);

        int loginPacketSize = 1 + 2 + 1 + 36 + 1 + rsaBlock.length;
        loginOut.writeByte(loginPacketSize);
        loginOut.writeByte(RSA_MAGIC);
        loginOut.writeShort(clientVersion);
        loginOut.writeByte(lowMemory ? 1 : 0);
        for (int i = 0; i < 9; i++) {
            loginOut.writeInt(0);
        }
        loginOut.writeByte(rsaBlock.length);
        loginOut.write(rsaBlock);
        loginOut.flush();
        return login.toByteArray();
    }

    private static void writeRuneString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.ISO_8859_1);
        out.write(bytes);
        out.writeByte(10);
    }

    private static byte[] readExactly(InputStream in, int length) throws IOException {
        byte[] buffer = new byte[length];
        int read = 0;
        while (read < length) {
            int count = in.read(buffer, read, length - read);
            if (count < 0) {
                throw new EOFException("Expected " + length + " bytes, got " + read);
            }
            read += count;
        }
        return buffer;
    }

    public static final class LoginSession {
        private final int rights;
        private final Rs317IsaacCipher outCipher;

        public LoginSession(int rights, Rs317IsaacCipher outCipher) {
            this.rights = rights;
            this.outCipher = outCipher;
        }

        public int getRights() {
            return rights;
        }

        public Rs317IsaacCipher getOutCipher() {
            return outCipher;
        }
    }
}
