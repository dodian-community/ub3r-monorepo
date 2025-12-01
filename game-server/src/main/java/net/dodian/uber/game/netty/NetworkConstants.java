package net.dodian.uber.game.netty;

import java.math.BigInteger;


public final class NetworkConstants {

    /**
     * Maximum number of packets a single client is allowed to have processed
     * within a single time window (600ms) (configured in GamePacketHandler).
     */
    public static final int PACKET_PROCESS_LIMIT = 50;

    // TODO: Add RSA key configuration
    public static BigInteger RSA_MODULUS;

    public static BigInteger RSA_EXPONENT;

    private NetworkConstants() {
        
    }
}
