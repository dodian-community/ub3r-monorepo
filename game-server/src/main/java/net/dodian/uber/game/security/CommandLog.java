package net.dodian.uber.game.security;

import net.dodian.Config;
import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.utilities.Database;

import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Saves all the chat logs to the 'chat_log' database.
 *
 * @author Stephen
 */
public class CommandLog extends LogEntry {

    /**
     * The logger for the class.
     */
    private static final Logger logger = Logger.getLogger(CommandLog.class.getName());

    /**
     * Inserts a new entry into the 'chat_log' table.
     *
     * @param player  The player sending the message.
     * @param command The command typed.
     */
    public static void recordCommand(Player player, String command) {
        if (Config.getWorldId() > 1) {
            return;
        }
        try {
            Statement statement = Database.conn.createStatement();
            String query = "INSERT INTO uber3_command_log(userId, name, time, action) VALUES ('" + player.dbId + "','" + player.getPlayerName() + "', '" + getTimeStamp() + "', '::" + command.replaceAll("'", "") + "')";
            statement.executeUpdate(query);
            statement.close();
        } catch (Exception e) {
            logger.severe("Unable to record chat!");
            e.printStackTrace();
            YellSystem.alertStaff("Unable to record chat logs, please contact an admin.");
        }
    }
}
