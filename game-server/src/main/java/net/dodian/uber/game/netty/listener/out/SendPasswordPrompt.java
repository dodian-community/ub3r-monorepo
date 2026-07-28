package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.encode.TarnishOutboundPackets;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

/** Opens the masked account-services password entry prompt. */
public final class SendPasswordPrompt implements OutgoingPacket {
    private final int stage;
    private final String nonce;
    private final String title;

    public SendPasswordPrompt(int stage, String nonce, String title) {
        this.stage = stage;
        this.nonce = nonce;
        this.title = title;
    }

    @Override
    public void send(Client client) {
        client.send(new TarnishOutboundPackets.SendPasswordPrompt(stage, nonce, title).encode());
    }
}
