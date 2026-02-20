package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.netty.codec.ByteMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlayerUpdatingAddLocalAppearanceTest {

    @Test
    public void addLocalAlwaysForcesAppearanceAndSkipsCachedBlock() {
        Client target = new Client(null, 2);
        target.playerName = "target";
        target.isNpc = true;
        target.setPlayerNpc(1);

        target.cacheUpdateBlock(new byte[]{0x55, 0x66}, 2);

        ByteMessage updateBlock = ByteMessage.raw(256);
        try {
            PlayerUpdating.getInstance().appendAddLocalBlockUpdate(target, updateBlock);

            int length = updateBlock.getBuffer().writerIndex();
            assertTrue(length > 2, "Expected forced appearance block data.");

            byte[] encoded = new byte[length];
            updateBlock.getBuffer().getBytes(0, encoded);

            assertEquals(UpdateFlag.APPEARANCE.getMask(target.getType()), encoded[0] & 0xFF);
            assertFalse(encoded[0] == 0x55 && encoded[1] == 0x66, "Unexpected cached block reuse in add-local phase.");
        } finally {
            updateBlock.releaseAll();
        }
    }
}
