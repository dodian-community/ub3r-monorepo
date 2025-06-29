package net.dodian.uber.game.networking.login;

import io.netty.buffer.ByteBuf;

/**
 * Simple value object representing a RuneScape login payload.
 * It carries the login type byte (world/world-switch etc.) and the payload
 * {@link ByteBuf}.  The downstream {@code LoginProcessorHandler} will parse it
 * and MUST release the {@link ByteBuf} when done.
 */
public final class LoginPayload {

    private final int loginType;
    private final ByteBuf payload;

    public LoginPayload(int loginType, ByteBuf payload) {
        this.loginType = loginType;
        this.payload = payload;
    }

    public int getLoginType() {
        return loginType;
    }

    public ByteBuf getPayload() {
        return payload;
    }
}
