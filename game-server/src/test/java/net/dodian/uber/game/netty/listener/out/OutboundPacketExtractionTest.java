package net.dodian.uber.game.netty.listener.out;

import io.netty.channel.embedded.EmbeddedChannel;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OutboundPacketExtractionTest {

    @Test
    void mapRegionUpdateEncodesOpcodeAndPayload() {
        EmbeddedChannel channel = new EmbeddedChannel();
        Client client = new Client(channel, 1);

        new MapRegionUpdate(3200, 3201).send(client);

        ByteMessage message = channel.readOutbound();
        assertNotNull(message);
        assertEquals(73, message.getOpcode());
        assertEquals(MessageType.FIXED, message.getType());
        assertEquals((3200 + 6) >> 8, message.getBuffer().getUnsignedByte(0));
        assertEquals(((3200 + 6) + 128) & 0xFF, message.getBuffer().getUnsignedByte(1));
        assertEquals((3201 + 6) >> 8, message.getBuffer().getUnsignedByte(2));
        assertEquals((3201 + 6) & 0xFF, message.getBuffer().getUnsignedByte(3));
        message.releaseAll();
        channel.finishAndReleaseAll();
    }

    @Test
    void systemUpdateTimerEncodesOpcodeAndTicks() {
        EmbeddedChannel channel = new EmbeddedChannel();
        Client client = new Client(channel, 1);

        new SystemUpdateTimer(250).send(client);

        ByteMessage message = channel.readOutbound();
        assertNotNull(message);
        assertEquals(114, message.getOpcode());
        assertEquals(MessageType.FIXED, message.getType());
        assertEquals(0, message.getBuffer().getUnsignedByte(0));
        assertEquals(250, message.getBuffer().getUnsignedByte(1));
        message.releaseAll();
        channel.finishAndReleaseAll();
    }
}
