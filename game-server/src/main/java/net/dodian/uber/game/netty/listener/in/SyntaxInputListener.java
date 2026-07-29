package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.engine.systems.net.PacketBankingService;

/**
 * Handles text syntax input (opcode 60).
 * Delegates bank-search flag checks and state mutation to PacketBankingService.
 */
@net.dodian.uber.game.netty.listener.PacketHandler(opcodes = {60})
public class SyntaxInputListener implements PacketListener {
    @Override
    public void handle(Client client, GamePacket packet) {
        net.dodian.uber.game.netty.game.decode.TarnishPackets.EncodedName msg = net.dodian.uber.game.netty.game.decode.TarnishPackets.EncodedName.decode(packet.payload());
        if (msg == null) {
            return;
        }

        long nameLong = msg.name();
        String input = net.dodian.utilities.Names.longToPlayerName(nameLong).trim();

        if (client.getContentRuntimeState().getPendingInputState()
                == net.dodian.uber.game.model.entity.player.PendingInputState.MODERATION_SEARCH) {
            client.getContentRuntimeState().clearPendingInputState();
            net.dodian.uber.game.social.moderation.ModerationService.open(client, input);
            return;
        }

        if (net.dodian.uber.game.engine.systems.net.PacketSocialService.handlePendingNameInput(client, nameLong)) {
            return;
        }

        if (net.dodian.uber.game.economy.PriceCheckerService.isSearchPending(client)) {
            net.dodian.uber.game.economy.PriceCheckerService.setSearchPending(client, false);
            net.dodian.uber.game.economy.PriceCheckerService.search(client, input);
            return;
        }

        if (PacketBankingService.hasPendingBankSearch(client)) {
            PacketBankingService.applyPendingBankSearch(client, input);
        }
    }
}
