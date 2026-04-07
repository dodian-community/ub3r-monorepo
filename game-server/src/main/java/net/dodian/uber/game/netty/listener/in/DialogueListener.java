package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.systems.ui.dialogue.DialogueService;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.engine.event.GameEventBus;
import net.dodian.uber.game.events.widget.DialogueContinueEvent;

/**
 * Netty implementation of legacy {@code Dialogue} (opcode 40).
 * The packet carries no useful payload; behaviour depends solely on client state fields.
 */
public class DialogueListener implements PacketListener {

    static { PacketListenerManager.register(40, new DialogueListener()); }

    @Override
    public void handle(Client client, GamePacket packet) {
        if (GameEventBus.postWithResult(new DialogueContinueEvent(client))) {
            return;
        }

        if (DialogueService.onContinue(client)) {
            return;
        }

        // No fields to decode; just replicate legacy behaviour
        ByteBuf buf = packet.payload();
        if (buf.isReadable()) {
            buf.skipBytes(buf.readableBytes()); // discard if any
        }

        if (DialogueService.onIndexedContinue(client)) {
            return;
        }
    }
}
