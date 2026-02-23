package net.dodian.uber.game.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class PlayerSaveRepository {

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

    private final ConnectionProvider connectionProvider;

    public PlayerSaveRepository() {
        this(PlayerSaveRepository::defaultConnection);
    }

    public PlayerSaveRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    private static Connection defaultConnection() throws SQLException {
        return getDbConnection();
    }

    public void saveSnapshot(PlayerSaveSnapshot snapshot) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(snapshot.getStatsUpdateSql());

                if (snapshot.getStatsProgressInsertSql() != null) {
                    statement.executeUpdate(snapshot.getStatsProgressInsertSql());
                }

                statement.executeUpdate(snapshot.getCharacterUpdateSql());
                connection.commit();
            } catch (SQLException sqlException) {
                connection.rollback();
                throw sqlException;
            } catch (RuntimeException runtimeException) {
                connection.rollback();
                throw runtimeException;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }
}
