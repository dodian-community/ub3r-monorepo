package net.dodian.uber.game.model.entity.player;

import io.netty.buffer.Unpooled;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.game.GamePacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClientPacketLifecycleTest {

    @Test
    public void sendReleasesMessageWhenClientIsNotWritable() {
        Client client = new Client(null, 1);
        ByteMessage msg = ByteMessage.raw(8);
        msg.put(0x42);

        assertEquals(1, msg.getBuffer().refCnt());
        client.send(msg);
        assertEquals(0, msg.getBuffer().refCnt());
    }

    @Test
    public void queuedInboundPayloadIsReleasedAfterProcessing() {
        Client client = new Client(null, 1);
        GamePacket packet = new GamePacket(250, 1, Unpooled.buffer(1));
        packet.getPayload().writeByte(0x7F);

        assertEquals(1, packet.getPayload().refCnt());
        client.queueInboundPacket(packet);
        client.processQueuedPackets(1);
        assertEquals(0, packet.getPayload().refCnt());
    }
}
