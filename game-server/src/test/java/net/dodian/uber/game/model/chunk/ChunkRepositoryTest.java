package net.dodian.uber.game.model.chunk;

import io.netty.channel.embedded.EmbeddedChannel;
import net.dodian.uber.game.model.EntityType;
import net.dodian.uber.game.model.entity.player.Client;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkRepositoryTest {

    @Test
    void startsEmptyWithoutBackingSets() {
        ChunkRepository repository = new ChunkRepository(new Chunk(320, 380));

        assertTrue(repository.isEmpty());
        assertEquals(0, repository.size());
        assertTrue(repository.getAll(EntityType.PLAYER).isEmpty());
        assertTrue(repository.getAll(EntityType.NPC).isEmpty());
    }

    @Test
    void preservesInsertionOrderAndUniquenessForSmallPopulations() {
        ChunkRepository repository = new ChunkRepository(new Chunk(320, 380));
        Client first = newClient(1, 101);
        Client second = newClient(2, 102);

        repository.add(first);
        repository.add(second);
        repository.add(first);

        Set<Client> players = repository.getAll(EntityType.PLAYER);

        assertEquals(2, players.size());
        assertEquals(List.of(first, second), new ArrayList<>(players));
    }

    @Test
    void removesEmptyBucketsAfterLastEntityLeaves() {
        ChunkRepository repository = new ChunkRepository(new Chunk(320, 380));
        Client player = newClient(1, 101);

        repository.add(player);
        repository.remove(player);

        assertTrue(repository.isEmpty());
        assertEquals(0, repository.size());
        assertTrue(repository.getAll(EntityType.PLAYER).isEmpty());
    }

    @Test
    void promotesToLargeSetWithoutLosingOrder() {
        ChunkRepository repository = new ChunkRepository(new Chunk(320, 380));
        List<Client> expected = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Client player = newClient(i + 1, 100 + i);
            expected.add(player);
            repository.add(player);
        }

        List<Client> actual = new ArrayList<>(repository.getAll(EntityType.PLAYER));

        assertEquals(expected, actual);
        assertFalse(repository.isEmpty());
        assertEquals(10, repository.size());
    }

    private static Client newClient(int slot, int dbId) {
        Client client = new Client(new EmbeddedChannel(), slot);
        client.dbId = dbId;
        client.isActive = true;
        client.disconnected = false;
        return client;
    }
}
