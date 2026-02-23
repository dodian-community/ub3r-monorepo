package net.dodian.uber.game.security;

import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.utilities.DbTables;

import java.sql.Statement;
import java.util.logging.Logger;

import static net.dodian.utilities.DatabaseKt.getDbConnection;
import static net.dodian.utilities.DotEnvKt.getGameWorldId;

/**
 * Saves staff command usage.
 */
public class CommandLog extends LogEntry {

    private static final Logger logger = Logger.getLogger(CommandLog.class.getName());

    public static void recordCommand(Player player, String command) {
        if (getGameWorldId() > 1 || player.playerGroup == 10) {
            return;
        }

        AsyncSqlService.execute("command-log", () -> {
            try (java.sql.Connection conn = getDbConnection();
                 Statement statement = conn.createStatement()) {
                String sanitized = command.replaceAll("'", "`");
                String query = "INSERT INTO " + DbTables.GAME_LOGS_STAFF_COMMANDS +
                        "(userId, name, time, action) VALUES ('" + player.dbId + "','" + player.getPlayerName() + "', '" + getTimeStamp() + "', '::" + sanitized + "')";
                statement.executeUpdate(query);
            } catch (Exception e) {
                logger.severe("Unable to record command!");
                e.printStackTrace();
                YellSystem.alertStaff("Unable to record command logs, please contact an admin.");
            }
        });
    }
}
