package net.dodian.uber.game.model.chunk;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChunkPlayerComparatorTest {

    private Player createPlayer(int slot, int x, int y, int z) {
        Client player = new Client(null, slot);
        player.moveTo(x, y, z);
        return player;
    }

    @Test
    public void testCloserPlayerHasHigherPriority() {
        Player viewer = createPlayer(0, 100, 100, 0);
        Player close = createPlayer(1, 102, 102, 0); // 2 tiles away
        Player far = createPlayer(2, 110, 110, 0);   // 10 tiles away

        ChunkPlayerComparator comparator = new ChunkPlayerComparator(viewer);

        assertTrue(comparator.compare(close, far) < 0);
        assertTrue(comparator.compare(far, close) > 0);
    }

    @Test
    public void testSortingByDistance() {
        Player viewer = createPlayer(3, 100, 100, 0);
        Player p1 = createPlayer(4, 115, 100, 0); // 15 tiles
        Player p2 = createPlayer(5, 105, 100, 0); // 5 tiles
        Player p3 = createPlayer(6, 110, 100, 0); // 10 tiles

        List<Player> players = new ArrayList<>(Arrays.asList(p1, p2, p3));
        players.sort(new ChunkPlayerComparator(viewer));

        // Should be sorted: p2 (5), p3 (10), p1 (15)
        assertEquals(p2, players.get(0));
        assertEquals(p3, players.get(1));
        assertEquals(p1, players.get(2));
    }
}
