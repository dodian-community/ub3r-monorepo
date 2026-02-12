package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.content.dialogue.DialogueService;
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation of legacy {@code Dialogue} (opcode 40).
 * The packet carries no useful payload; behaviour depends solely on client state fields.
 */
public class DialogueListener implements PacketListener {

    static { PacketListenerManager.register(40, new DialogueListener()); }

    private static final Logger logger = LoggerFactory.getLogger(DialogueListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        if (DialogueService.onContinue(client)) {
            return;
        }

        // No fields to decode; just replicate legacy behaviour
        ByteBuf buf = packet.getPayload();
        if (buf.isReadable()) {
            buf.skipBytes(buf.readableBytes()); // discard if any
        }

        logger.debug("DialogueListener triggered: npcDialogue={} nextDiag={}", client.NpcDialogue, client.nextDiag);

        switch (client.NpcDialogue) {
            case 1:
            case 3:
            case 5:
            case 21:
                client.NpcDialogue += 1;
                client.NpcDialogueSend = false;
                break;
            case 6:
            case 7:
                client.NpcDialogue = 0;
                client.NpcDialogueSend = false;
                client.send(new RemoveInterfaces());
                break;
            case 23:
                client.NpcDialogue += 2;
                client.NpcDialogueSend = false;
                break;
            default:
                if (client.nextDiag > 0) {
                    client.NpcDialogue = client.nextDiag;
                    client.NpcDialogueSend = false;
                    client.nextDiag = -1;
                } else {
                    if (client.NpcDialogue != 48054)
                        client.send(new RemoveInterfaces());
                }
                break;
        }
    }
}
