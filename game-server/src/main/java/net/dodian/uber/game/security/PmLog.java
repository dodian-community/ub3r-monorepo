package net.dodian.uber.game.security;

import net.dodian.uber.game.model.YellSystem;
import net.dodian.utilities.DbTables;

import java.sql.Statement;
import java.util.logging.Logger;

import static net.dodian.DotEnvKt.getGameWorldId;
import static net.dodian.utilities.DatabaseKt.getDbConnection;

/**
 * Saves information pertaining to pms between players.
 *
 * @author Stephen
 */
public class PmLog extends LogEntry {

    /**
     * The logger for the class.
     */
    private final static Logger logger = Logger.getLogger(DropLog.class.getName());

    /**
     * Inserts a new entry into 'pm_log'.
     *
     * @param sender   The player sending the message.
     * @param receiver The player receiving the message.
     * @param message  The message being sent.
     */
    public static void recordPm(String sender, String receiver, String message) {
        try {
            if (getGameWorldId() > 1) {
                return;
            }
            Statement statement = getDbConnection().createStatement();
            String query = "INSERT INTO " + DbTables.GAME_LOGS_PLAYER_PRIVATE_CHAT + "(sender, receiver, message, timestamp) VALUES ('" + sender + "', '" + receiver
                    + "', '" + message.replaceAll("'", "") + "', '" + getTimeStamp() + "')";
            statement.executeUpdate(query);
            statement.close();
        } catch (Exception e) {
            logger.severe("Unable to record pm!");
            e.printStackTrace();
            YellSystem.alertStaff("Unable to record private messages, please contact an admin.");
        }
    }
}
