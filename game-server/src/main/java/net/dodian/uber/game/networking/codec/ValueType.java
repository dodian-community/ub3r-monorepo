package net.dodian.uber.game.networking.codec;

public enum ValueType {

    /**
     * Do nothing to the value.
     */
    NORMAL,

    /**
     * Add {@code 128} to the value.
     */
    ADD,

    /**
     * Invert the sign of the value.
     */
    NEGATE,

    /**
     * Subtract {@code 128} from the value.
     */
    SUBTRACT
}