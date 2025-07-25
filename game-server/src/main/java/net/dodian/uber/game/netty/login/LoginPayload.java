package net.dodian.uber.game.netty.login;

import io.netty.buffer.ByteBuf;

/**
 * Simple holder for the login payload bytes once fully received.
 * The {@link LoginPayloadDecoder} collects the payload and wraps it
 * in an instance of this class which is then passed to
 * {@link LoginProcessorHandler} for validation and login logic.
 */
public class LoginPayload {

    private final ByteBuf payload;

    public LoginPayload(ByteBuf payload) {
        this.payload = payload;
    }

    /**
     * The retained payload buffer. Callers must release when done.
     */
    public ByteBuf getPayload() {
        return payload;
    }
}
