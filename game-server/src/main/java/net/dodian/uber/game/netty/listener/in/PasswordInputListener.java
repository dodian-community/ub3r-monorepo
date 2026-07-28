package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.ui.AccountServices;

/** Receives an RSA-encrypted account-services password response. */
@PacketHandler(opcodes = {62})
public final class PasswordInputListener implements PacketListener {
    private static final int MAX_RSA_BLOCK_BYTES = 512;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf payload = packet.payload();
        if (!client.viewingAccountServices || payload.readableBytes() < 2
                || payload.readableBytes() > MAX_RSA_BLOCK_BYTES) {
            AccountServices.cancelPasswordChange(client);
            return;
        }
        byte[] encrypted = new byte[payload.readableBytes()];
        payload.readBytes(encrypted);
        AccountServices.handleEncryptedPasswordInput(client, encrypted);
    }
}
