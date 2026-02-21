package net.dodian.uber.game.persistence;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncLogoutRelogBarrierTest {

    @Test
    void finalSaveLockBlocksUntilCommitCompletes() throws Exception {
        FinalSaveBlockingRepository repository = new FinalSaveBlockingRepository();
        PlayerSaveCoordinator coordinator = new PlayerSaveCoordinator(repository, 1, 10L, 50L, 3, false);

        PlayerSaveSnapshot finalSnapshot = PlayerSaveSnapshot.forSql(
                1L,
                42,
                "logout_tester",
                PlayerSaveReason.LOGOUT,
                false,
                true,
                "UPDATE character_stats SET total=1 WHERE uid=42",
                null,
                "UPDATE characters SET health=1 WHERE id=42"
        );

        coordinator.requestSave(finalSnapshot);
        assertTrue(repository.started.await(2, TimeUnit.SECONDS));
        assertTrue(coordinator.hasPendingFinalSave(42));

        repository.unblock.countDown();
        assertTrue(coordinator.awaitIdle(Duration.ofSeconds(3)));

        assertFalse(coordinator.hasPendingFinalSave(42));
        coordinator.shutdownAndDrainForTest(Duration.ofSeconds(1));
    }

    private static final class FinalSaveBlockingRepository extends PlayerSaveRepository {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch unblock = new CountDownLatch(1);

        @Override
        public void saveSnapshot(PlayerSaveSnapshot snapshot) throws SQLException {
            started.countDown();
            try {
                unblock.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
