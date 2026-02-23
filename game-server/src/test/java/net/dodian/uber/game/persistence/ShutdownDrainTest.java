package net.dodian.uber.game.persistence;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShutdownDrainTest {

    @Test
    void drainsPendingSaveBeforeShutdown() throws Exception {
        DrainRepository repository = new DrainRepository();
        PlayerSaveCoordinator coordinator = new PlayerSaveCoordinator(repository, 1, 10L, 50L, 3, false);

        coordinator.requestSave(PlayerSaveSnapshot.forSql(
                1L,
                7,
                "shutdown_tester",
                PlayerSaveReason.PERIODIC,
                false,
                false,
                "UPDATE character_stats SET total=1 WHERE uid=7",
                null,
                "UPDATE characters SET health=1 WHERE id=7"
        ));

        assertTrue(repository.started.await(2, TimeUnit.SECONDS));

        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
            repository.unblock.countDown();
        }).start();

        coordinator.shutdownAndDrainForTest(Duration.ofSeconds(2));

        assertTrue(repository.saved.await(2, TimeUnit.SECONDS));
    }

    private static final class DrainRepository extends PlayerSaveRepository {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch unblock = new CountDownLatch(1);
        private final CountDownLatch saved = new CountDownLatch(1);

        @Override
        public void saveSnapshot(PlayerSaveSnapshot snapshot) throws SQLException {
            started.countDown();
            try {
                unblock.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
            saved.countDown();
        }
    }
}
