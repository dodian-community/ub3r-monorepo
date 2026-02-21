package net.dodian.uber.game.security;

import net.dodian.uber.game.model.YellSystem;
import net.dodian.utilities.DbTables;

import java.sql.Statement;
import java.util.logging.Logger;

import static net.dodian.utilities.DatabaseKt.getDbConnection;
import static net.dodian.utilities.DotEnvKt.getGameWorldId;

/**
 * Saves all duels that take place.
 */
public class DuelLog extends LogEntry {

    private static final Logger logger = Logger.getLogger(DuelLog.class.getName());

    public static void recordDuel(String player, String opponent, String playerStake, String opponentStake, String winner) {
        if (getGameWorldId() > 1) {
            return;
        }

        AsyncSqlService.execute("duel-log", () -> {
            try (java.sql.Connection conn = getDbConnection();
                 Statement statement = conn.createStatement()) {
                String query = "INSERT INTO " + DbTables.GAME_LOGS_PLAYER_DUELS +
                        "(player, opponent, playerstake, opponentstake, winner, timestamp) VALUES ('"
                        + player + "', '" + opponent + "', '" + playerStake + "', '" + opponentStake + "', '"
                        + winner + "', '" + getTimeStamp() + "')";
                statement.executeUpdate(query);
            } catch (Exception e) {
                logger.severe("Unable to record duel!");
                e.printStackTrace();
                YellSystem.alertStaff("Unable to record duels, please contact an admin.");
            }
        });
    }
}
