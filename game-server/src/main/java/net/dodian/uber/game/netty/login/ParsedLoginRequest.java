package net.dodian.uber.game.netty.login;

import net.dodian.utilities.ISAACCipher;

/** Immutable result of the CPU-bound Tarnish login block preparation. */
public final class ParsedLoginRequest {
    public enum Mode { LOGIN, CREATE_ACCOUNT }

    private final Mode mode;
    private final String username;
    private final String password;
    private final String discordAuthCode;
    private final ISAACCipher inboundCipher;
    private final ISAACCipher outboundCipher;
    private final long parseDurationMs;

    ParsedLoginRequest(
            Mode mode,
            String username,
            String password,
            String discordAuthCode,
            ISAACCipher inboundCipher,
            ISAACCipher outboundCipher,
            long parseDurationMs
    ) {
        this.mode = mode;
        this.username = username;
        this.password = password;
        this.discordAuthCode = discordAuthCode != null ? discordAuthCode : "";
        this.inboundCipher = inboundCipher;
        this.outboundCipher = outboundCipher;
        this.parseDurationMs = parseDurationMs;
    }

    public Mode getMode() { return mode; }
    public boolean isAccountCreationRequested() { return mode == Mode.CREATE_ACCOUNT; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDiscordAuthCode() { return discordAuthCode; }
    public ISAACCipher getInboundCipher() { return inboundCipher; }
    public ISAACCipher getOutboundCipher() { return outboundCipher; }
    public long getParseDurationMs() { return parseDurationMs; }
}
