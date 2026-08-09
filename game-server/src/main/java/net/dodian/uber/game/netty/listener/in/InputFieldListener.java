package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.content.DropDisplay;
import net.dodian.uber.game.ui.bank.PlayerBankService;

/**
 * Handles live input-field streaming (opcode 142).
 * Packet format: int componentId, null-terminated string text.
 * Sent every keystroke for input fields with updatesEveryInput=true (e.g. bank search 60019).
 */
@net.dodian.uber.game.netty.listener.PacketHandler(opcodes = {142})
public class InputFieldListener implements PacketListener {

    private static final int BANK_SEARCH_COMPONENT = 60019;
    private static final int DROP_VIEWER_ITEM_SEARCH = 54506;
    private static final int DROP_VIEWER_NPC_SEARCH = 54507;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < 5) {
            return;
        }

        int componentId = ByteBufReader.readInt(buf);
        if (!buf.isReadable()) {
            return;
        }
        String text = ByteBufReader.readTerminatedString(buf, 256).trim();

        if (componentId == BANK_SEARCH_COMPONENT) {
            PlayerBankService.applyBankSearch(client, text);
        } else if (componentId == DROP_VIEWER_ITEM_SEARCH) {
            if (!text.isEmpty()) {
                DropDisplay.search(client, text, DropDisplay.DropType.ITEM);
            }
        } else if (componentId == DROP_VIEWER_NPC_SEARCH) {
            if (!text.isEmpty()) {
                DropDisplay.search(client, text, DropDisplay.DropType.NPC);
            }
        }
    }
}