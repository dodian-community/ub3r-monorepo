package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.runtime.interaction.ObjectClickIntent;
import net.dodian.uber.game.runtime.task.GameTaskRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ObjectInteractionListenerTest {

    @AfterEach
    void tearDown() {
        GameTaskRuntime.clear();
        PlayerHandler.playersOnline.clear();
    }

    @Test
    void clickTaskUsesPlayersCurrentPlane() {
        ObjectInteractionListener listener = new ObjectInteractionListener();
        Client client = new Client(new EmbeddedChannel(), 1);
        client.validClient = true;
        client.pLoaded = true;
        client.disconnected = false;
        client.randomed = false;
        client.UsingAgility = false;
        client.getPosition().moveTo(2600, 3100, 2);

        int objectId = 7451;
        int objectX = 2601;
        int objectY = 3100;
        ByteBuf payload = buildFirstClickPayload(objectX, objectId, objectY);
        GamePacket packet = new GamePacket(132, payload.readableBytes(), payload);

        listener.handle(client, packet);

        Assertions.assertNotNull(client.getWalkToTask());
        Assertions.assertEquals(2, client.getWalkToTask().getWalkToPosition().getZ());
        Assertions.assertTrue(client.getPendingInteraction() instanceof ObjectClickIntent);
        ObjectClickIntent intent = (ObjectClickIntent) client.getPendingInteraction();
        Assertions.assertEquals(2, intent.getObjectPosition().getZ());
    }

    private static ByteBuf buildFirstClickPayload(int objectX, int objectId, int objectY) {
        ByteBuf payload = Unpooled.buffer(6);
        writeSignedWordBigEndianA(payload, objectX);
        payload.writeShort(objectId);
        writeUnsignedWordA(payload, objectY);
        return payload;
    }

    private static void writeSignedWordBigEndianA(ByteBuf payload, int value) {
        int low = value & 0xFF;
        int high = (value >> 8) & 0xFF;
        payload.writeByte((low + 128) & 0xFF);
        payload.writeByte(high);
    }

    private static void writeUnsignedWordA(ByteBuf payload, int value) {
        int high = (value >> 8) & 0xFF;
        int low = value & 0xFF;
        payload.writeByte(high);
        payload.writeByte((low + 128) & 0xFF);
    }
}
