package net.dodian.uber.game.netty;

import java.math.BigInteger;


public final class NetworkConstants {

    /**
     * Maximum number of inbound packets a single client is allowed to have
     * processed on the game thread per tick (600ms).
     */
    public static final int PACKET_PROCESS_LIMIT_PER_TICK = 25;

    /**
     * Maximum number of inbound packets a single client is allowed to enqueue
     * within a sliding 600ms window (enforced on the Netty event loop).
     */
    public static final int PACKET_RATE_LIMIT_PER_WINDOW = 200;

    // TODO: Add RSA key configuration
    public static BigInteger RSA_MODULUS;

    public static BigInteger RSA_EXPONENT;

    private NetworkConstants() {
        
    }
}
