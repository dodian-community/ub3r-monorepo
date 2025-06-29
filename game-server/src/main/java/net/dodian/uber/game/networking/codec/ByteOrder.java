package net.dodian.uber.game.networking.codec;


public enum ByteOrder {

    /**
     * Least significant byte is stored first and the most significant byte is stored last.
     */
    LITTLE,

    /**
     * Most significant byte is stored first and the least significant byte is stored last.
     */
    BIG,

    /**
     * Neither big endian nor little endian, the v1 order.
     */
    MIDDLE,

    /**
     * Neither big endian nor little endian, the v2 order.
     */
    INVERSE_MIDDLE
}
