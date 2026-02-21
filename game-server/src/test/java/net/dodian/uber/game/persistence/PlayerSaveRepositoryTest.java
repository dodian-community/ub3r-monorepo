package net.dodian.uber.game.persistence;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerSaveRepositoryTest {

    @Test
    void commitsAllStatementsInSingleTransaction() throws Exception {
        String url = "jdbc:h2:mem:player_save_commit;MODE=MySQL;DB_CLOSE_DELAY=-1";
        setupSchema(url);

        PlayerSaveRepository repository = new PlayerSaveRepository(() -> DriverManager.getConnection(url));

        PlayerSaveSnapshot snapshot = PlayerSaveSnapshot.forSql(
                1L,
                1,
                "tester",
                PlayerSaveReason.PERIODIC,
                true,
                false,
                "UPDATE character_stats SET total=10, combat=5, totalxp=100 WHERE uid=1",
                "INSERT INTO character_stats_progress(uid, updated, total, combat, totalxp) VALUES (1, 'ts', 10, 5, 100)",
                "UPDATE characters SET health=99 WHERE id=1"
        );

        repository.saveSnapshot(snapshot);

        try (Connection connection = DriverManager.getConnection(url);
             var statement = connection.createStatement()) {
            var stats = statement.executeQuery("SELECT total, combat, totalxp FROM character_stats WHERE uid=1");
            stats.next();
            assertEquals(10, stats.getInt("total"));
            assertEquals(5, stats.getInt("combat"));
            assertEquals(100, stats.getInt("totalxp"));

            var chars = statement.executeQuery("SELECT health FROM characters WHERE id=1");
            chars.next();
            assertEquals(99, chars.getInt("health"));

            var progress = statement.executeQuery("SELECT COUNT(*) AS c FROM character_stats_progress WHERE uid=1");
            progress.next();
            assertEquals(1, progress.getInt("c"));
        }
    }

    @Test
    void rollsBackWhenAnyStatementFails() throws Exception {
        String url = "jdbc:h2:mem:player_save_rollback;MODE=MySQL;DB_CLOSE_DELAY=-1";
        setupSchema(url);

        PlayerSaveRepository repository = new PlayerSaveRepository(() -> DriverManager.getConnection(url));

        PlayerSaveSnapshot snapshot = PlayerSaveSnapshot.forSql(
                1L,
                1,
                "tester",
                PlayerSaveReason.PERIODIC,
                true,
                false,
                "UPDATE character_stats SET total=42 WHERE uid=1",
                "INSERT INTO missing_table(value) VALUES (1)",
                "UPDATE characters SET health=12 WHERE id=1"
        );

        assertThrows(SQLException.class, () -> repository.saveSnapshot(snapshot));

        try (Connection connection = DriverManager.getConnection(url);
             var statement = connection.createStatement()) {
            var stats = statement.executeQuery("SELECT total FROM character_stats WHERE uid=1");
            stats.next();
            assertEquals(0, stats.getInt("total"));

            var chars = statement.executeQuery("SELECT health FROM characters WHERE id=1");
            chars.next();
            assertEquals(10, chars.getInt("health"));
        }
    }

    private static void setupSchema(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url);
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE character_stats (uid INT PRIMARY KEY, total INT, combat INT, totalxp INT)");
            statement.execute("CREATE TABLE character_stats_progress (uid INT, updated VARCHAR(255), total INT, combat INT, totalxp INT)");
            statement.execute("CREATE TABLE characters (id INT PRIMARY KEY, health INT)");

            statement.execute("INSERT INTO character_stats(uid, total, combat, totalxp) VALUES (1, 0, 0, 0)");
            statement.execute("INSERT INTO characters(id, health) VALUES (1, 10)");
        }
    }
}
