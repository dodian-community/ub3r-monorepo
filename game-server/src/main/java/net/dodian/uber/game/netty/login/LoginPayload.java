package net.dodian.uber.game.netty.login;

import io.netty.buffer.ByteBuf;

/**
 * Simple holder for the login payload bytes once fully received.
 * The {@link LoginPayloadDecoder} collects the payload and wraps it
 * in an instance of this class which is then passed to
 * {@link LoginProcessorHandler} for validation and login logic.
 */
public record LoginPayload(ByteBuf payload) {

    /**
     * The retained payload buffer. Callers must release when done.
     */
    @Override
    public ByteBuf payload() {
        return payload;
    }
}
