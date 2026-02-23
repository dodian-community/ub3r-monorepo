package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.netty.codec.ByteMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NpcUpdatingDirectionRegressionTest {

    private NpcManager originalNpcManager;

    private static final class TestNpcManager extends NpcManager {
        @Override
        public void loadData() {
            // Prevent DB access in tests.
        }
    }

    @BeforeEach
    public void setUp() {
        originalNpcManager = Server.npcManager;
        Server.npcManager = new TestNpcManager();
    }

    @AfterEach
    public void tearDown() {
        Server.npcManager = originalNpcManager;
    }

    @Test
    public void movementDirectionIsStableAcrossMultipleViewers() {
        Npc npc = new Npc(1, 1, new Position(3200, 3200, 0), 0);
        npc.setDirection(3);

        byte[] first = encodeMovement(npc);
        byte[] second = encodeMovement(npc);

        assertEquals(3, npc.getDirection());
        assertArrayEquals(first, second);
    }

    private static byte[] encodeMovement(Npc npc) {
        ByteMessage message = ByteMessage.raw(8);
        try {
            message.startBitAccess();
            NpcUpdating.getInstance().updateNPCMovement(npc, message);
            message.endBitAccess();

            byte[] data = new byte[message.getBuffer().writerIndex()];
            message.getBuffer().getBytes(0, data);
            return data;
        } finally {
            message.releaseAll();
        }
    }
}
