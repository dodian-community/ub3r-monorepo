package net.dodian.uber.game.model.player.packets.incoming;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.networking.game.ByteBufPacket;

/**
 * ByteBuf wrapper for the legacy {@link ChangeAppearance} handler (opcode 101).
 * <p>
 * Payload is 13 signed bytes:
 *   gender, head, jaw, torso, arms, hands, legs, feet,
 *   hairColour, torsoColour, legsColour, feetColour, skinColour
 * <p>
 * We simply copy the payload into the client's legacy Stream buffer and invoke
 * the existing handler so that the substantial appearance-setting logic
 * remains unchanged.
 */
public class ChangeAppearanceBuf implements ByteBufPacket {

    private static final ChangeAppearance DELEGATE = new ChangeAppearance();

    @Override
    public void process(Client client, int opcode, int size, ByteBuf payload) {
        byte[] data = new byte[size];
        payload.readBytes(data);
        client.getInputStream().currentOffset = 0;
        client.getInputStream().buffer = data;

        DELEGATE.ProcessPacket(client, opcode, size);
    }
}
