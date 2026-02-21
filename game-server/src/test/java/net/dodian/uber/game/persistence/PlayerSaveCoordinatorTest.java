package net.dodian.uber.game.persistence;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PlayerSaveCoordinatorTest {

    @Test
    void coalescesPendingSavesPerPlayer() throws Exception {
        BlockingRepository repository = new BlockingRepository();
        repository.blockFirstSave = true;

        PlayerSaveCoordinator coordinator = new PlayerSaveCoordinator(repository, 1, 10L, 50L, 3, false);

        coordinator.requestSave(snapshot(1L, 1, false));
        assertTrue(repository.firstSaveStarted.await(2, TimeUnit.SECONDS));

        coordinator.requestSave(snapshot(2L, 1, false));
        coordinator.requestSave(snapshot(3L, 1, false));

        repository.unblockFirstSave.countDown();

        assertTrue(coordinator.awaitIdle(Duration.ofSeconds(3)));
        coordinator.shutdownAndDrainForTest(Duration.ofSeconds(1));

        assertEquals(2, repository.savedSnapshots.size());
        assertEquals(1L, repository.savedSnapshots.get(0).getSequence());
        assertEquals(3L, repository.savedSnapshots.get(1).getSequence());
    }

    @Test
    void preservesOrderWithInFlightAndPendingSaves() throws Exception {
        BlockingRepository repository = new BlockingRepository();
        repository.blockFirstSave = true;

        PlayerSaveCoordinator coordinator = new PlayerSaveCoordinator(repository, 1, 10L, 50L, 3, false);

        coordinator.requestSave(snapshot(1L, 1, false));
        assertTrue(repository.firstSaveStarted.await(2, TimeUnit.SECONDS));

        coordinator.requestSave(snapshot(2L, 1, false));

        repository.unblockFirstSave.countDown();

        assertTrue(coordinator.awaitIdle(Duration.ofSeconds(3)));
        coordinator.shutdownAndDrainForTest(Duration.ofSeconds(1));

        assertEquals(2, repository.savedSnapshots.size());
        assertEquals(1L, repository.savedSnapshots.get(0).getSequence());
        assertEquals(2L, repository.savedSnapshots.get(1).getSequence());
    }

    private static PlayerSaveSnapshot snapshot(long sequence, int dbId, boolean finalSave) {
        return PlayerSaveSnapshot.forSql(
                sequence,
                dbId,
                "tester",
                PlayerSaveReason.PERIODIC,
                false,
                finalSave,
                "UPDATE character_stats SET total=1 WHERE uid=" + dbId,
                null,
                "UPDATE characters SET health=1 WHERE id=" + dbId
        );
    }

    private static final class BlockingRepository extends PlayerSaveRepository {
        private final List<PlayerSaveSnapshot> savedSnapshots = new ArrayList<>();
        private final CountDownLatch firstSaveStarted = new CountDownLatch(1);
        private final CountDownLatch unblockFirstSave = new CountDownLatch(1);
        private volatile boolean blockFirstSave;

        @Override
        public void saveSnapshot(PlayerSaveSnapshot snapshot) throws SQLException {
            if (blockFirstSave && firstSaveStarted.getCount() > 0) {
                firstSaveStarted.countDown();
                try {
                    unblockFirstSave.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
            savedSnapshots.add(snapshot);
        }
    }
}
