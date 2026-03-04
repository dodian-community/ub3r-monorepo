package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WalkingListenerTest {

    @Test
    void opcode248UsesEffectiveSizeAndMatchesOpcode164PathDecode() {
        WalkingListener listener = new WalkingListener();
        Client clickWalkClient = walkReadyClient(1);
        Client minimapWalkClient = walkReadyClient(2);

        int firstStepXAbs = (clickWalkClient.mapRegionX * 8) + 50;
        int firstStepYAbs = (clickWalkClient.mapRegionY * 8) + 52;
        byte[] stepDeltas = new byte[] {1, -1};

        ByteBuf clickPayload = buildWalkPayload(firstStepXAbs, firstStepYAbs, true, stepDeltas, 0);
        GamePacket clickPacket = new GamePacket(164, clickPayload.readableBytes(), clickPayload);
        listener.handle(clickWalkClient, clickPacket);

        ByteBuf minimapPayload = buildWalkPayload(firstStepXAbs, firstStepYAbs, true, stepDeltas, 0);
        GamePacket minimapPacket = new GamePacket(248, minimapPayload.readableBytes(), minimapPayload);
        listener.handle(minimapWalkClient, minimapPacket);

        Assertions.assertEquals(clickWalkClient.newWalkCmdSteps, minimapWalkClient.newWalkCmdSteps);
        Assertions.assertEquals(clickWalkClient.newWalkCmdIsRunning, minimapWalkClient.newWalkCmdIsRunning);
        for (int i = 0; i < clickWalkClient.newWalkCmdSteps; i++) {
            Assertions.assertEquals(clickWalkClient.newWalkCmdX[i], minimapWalkClient.newWalkCmdX[i], "newWalkCmdX mismatch at " + i);
            Assertions.assertEquals(clickWalkClient.newWalkCmdY[i], minimapWalkClient.newWalkCmdY[i], "newWalkCmdY mismatch at " + i);
        }
    }

    @Test
    void malformedOpcode248ResetsWalkingQueueWithoutApplyingCommand() {
        WalkingListener listener = new WalkingListener();
        Client client = walkReadyClient(3);
        client.addToWalkingQueue(12, 12);
        client.newWalkCmdSteps = 5;

        ByteBuf malformedPayload = Unpooled.buffer(10);
        malformedPayload.writeZero(10);
        GamePacket malformed = new GamePacket(248, malformedPayload.readableBytes(), malformedPayload);

        listener.handle(client, malformed);

        Assertions.assertEquals(0, client.newWalkCmdSteps);
        Assertions.assertEquals(client.wQueueReadPtr, client.wQueueWritePtr);
    }

    @Test
    void resolveEffectiveSizeDefaultsToRawAndSupportsLegacySuffixMode() {
        Assertions.assertEquals(7, WalkingListener.resolveEffectiveSize(164, 7, false));
        Assertions.assertEquals(21, WalkingListener.resolveEffectiveSize(248, 21, false));
        Assertions.assertEquals(7, WalkingListener.resolveEffectiveSize(248, 21, true));
    }

    private static Client walkReadyClient(int slot) {
        Client client = new Client(new EmbeddedChannel(), slot);
        client.validClient = true;
        client.pLoaded = true;
        client.disconnected = false;
        client.randomed = false;
        client.mapRegionX = 320;
        client.mapRegionY = 320;
        client.setCurrentHealth(99);
        client.getPosition().moveTo(client.mapRegionX * 8 + 49, client.mapRegionY * 8 + 52, 0);
        return client;
    }

    private static ByteBuf buildWalkPayload(
            int firstStepXAbs,
            int firstStepYAbs,
            boolean running,
            byte[] stepDeltas,
            int trailingBytes
    ) {
        int extraSteps = stepDeltas.length / 2;
        int effectiveSize = 5 + (extraSteps * 2);
        ByteBuf payload = Unpooled.buffer(effectiveSize + trailingBytes);

        writeFirstStepX(payload, firstStepXAbs);
        for (int i = 0; i < stepDeltas.length; i += 2) {
            payload.writeByte(stepDeltas[i]);
            payload.writeByte(stepDeltas[i + 1]);
        }
        writeFirstStepY(payload, firstStepYAbs);
        payload.writeByte(running ? -1 : 0);
        if (trailingBytes > 0) {
            payload.writeZero(trailingBytes);
        }
        return payload;
    }

    private static void writeFirstStepX(ByteBuf payload, int value) {
        int low = value & 0xFF;
        int high = (value >> 8) & 0xFF;
        payload.writeByte((low + 128) & 0xFF);
        payload.writeByte(high);
    }

    private static void writeFirstStepY(ByteBuf payload, int value) {
        int low = value & 0xFF;
        int high = (value >> 8) & 0xFF;
        payload.writeByte(low);
        payload.writeByte(high);
    }
}
