package net.dodian.uber.game.security;

import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.utilities.DbTables;

import java.sql.Statement;
import java.util.logging.Logger;

import static net.dodian.uber.utilities.DotEnvKt.getGameWorldId;
import static net.dodian.uber.utilities.DatabaseKt.getDbConnection;

/**
 * Saves all the chat logs to the 'chat_log' database.
 *
 * @author Stephen
 */
public class ChatLog extends LogEntry {

    /**
     * The logger for the class.
     */
    private static final Logger logger = Logger.getLogger(ChatLog.class.getName());

    /**
     * Inserts a new entry into the 'chat_log' table.
     *
     * @param player  The player sending the message.
     * @param message The message sent.
     */
    public static void recordChat(String player, String message) {
        if (getGameWorldId() > 1) {
            return;
        }
        try {
            Statement statement = getDbConnection().createStatement();
            String query = "INSERT INTO " + DbTables.GAME_LOGS_PLAYER_PUBLIC_CHAT + "(username, message, timestamp) VALUES ('" + player + "', '" + message.replaceAll("'", "") + "', '"
                    + getTimeStamp() + "')";
            statement.executeUpdate(query);
            statement.close();
        } catch (Exception e) {
            logger.severe("Unable to record chat!");
            e.printStackTrace();
            YellSystem.alertStaff("Unable to record chat logs, please contact an admin.");
        }
    }
}
