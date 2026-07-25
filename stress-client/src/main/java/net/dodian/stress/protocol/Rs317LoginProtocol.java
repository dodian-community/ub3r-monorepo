package net.dodian.stress.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
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

    private static final BigInteger RSA_MODULUS = new BigInteger("102353038900255891527619367941460634639078944277149869534765441701765061915480193910291695742706042386340616731973380032288127455494356031646220980795852675234525031620306539656965685802100384909448780766960597664159328648803794286947984198753216591499378109000984639229430631686267432671373106838769133939913");
    private static final BigInteger RSA_EXPONENT = new BigInteger("65537");

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
        byte[] rsaBlockEncrypted = buildRsaBlock(clientSeed, serverSeed, username, password);
        byte[] loginBlock = buildLoginBlock(reconnecting, clientVersion, lowMemory, rsaBlockEncrypted);

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
        int encryptedOpcode = (0 + outCipher.getNextKey()) & 0xFF;
        out.write(encryptedOpcode);
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
        ByteArrayOutputStream rsa = new ByteArrayOutputStream(256);
        DataOutputStream rsaOut = new DataOutputStream(rsa);
        rsaOut.writeByte(RSA_PACKET_ID);
        rsaOut.writeLong(clientSeed);
        rsaOut.writeLong(serverSeed);
        rsaOut.writeInt(0); // legacy UID
        rsaOut.writeInt(0); // matching game-client Client.java line 11850
        writeRuneString(rsaOut, ""); // discordAuthCode
        writeRuneString(rsaOut, "00:00:00:00:00:00"); // MAC address
        writeRuneString(rsaOut, username);
        writeRuneString(rsaOut, password == null ? "" : password);
        rsaOut.flush();

        byte[] rawRsa = rsa.toByteArray();
        BigInteger encrypted = new BigInteger(1, rawRsa).modPow(RSA_EXPONENT, RSA_MODULUS);
        return encrypted.toByteArray();
    }

    private static byte[] buildLoginBlock(boolean reconnecting,
                                          int clientVersion,
                                          boolean lowMemory,
                                          byte[] rsaBlockEncrypted) throws IOException {
        ByteArrayOutputStream login = new ByteArrayOutputStream(512);
        DataOutputStream loginOut = new DataOutputStream(login);
        loginOut.writeByte(reconnecting ? LOGIN_TYPE_RECONNECT : LOGIN_TYPE_NEW);

        // Matching game-client: declared block length is rsaBlockEncrypted.length + 38
        int declaredSize = rsaBlockEncrypted.length + 38;
        loginOut.writeByte(declaredSize);
        loginOut.writeByte(RSA_MAGIC);
        loginOut.writeByte(clientVersion); // 1 byte version
        loginOut.writeByte(lowMemory ? 1 : 0);
        for (int i = 0; i < 9; i++) {
            loginOut.writeInt(0);
        }
        loginOut.writeByte(rsaBlockEncrypted.length);
        loginOut.write(rsaBlockEncrypted);
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
