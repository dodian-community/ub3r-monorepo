package net.dodian.uber.game.security;

import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.utilities.DbTables;

import java.sql.Statement;
import java.util.logging.Logger;

import static net.dodian.utilities.DotEnvKt.getGameWorldId;
import static net.dodian.utilities.DatabaseKt.getDbConnection;

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
    public static void recordPublicChat(Player player, String message) {
        if (getGameWorldId() > 1) {
            return;
        }
        try {
            message = message.replaceAll("'", "`").replaceAll("\\u005C", "/"); //letter '\' and ' is not accepted by sql!
            Statement statement = getDbConnection().createStatement();
            String query = "INSERT INTO " + DbTables.GAME_CHAT_LOGS + "(type, sender, receiver, message, timestamp) VALUES ('1', '" + player.dbId + "', '-1', '"+message+"', '" + getTimeStamp() + "')";
            statement.executeUpdate(query);
            statement.close();
        } catch (Exception e) {
            logger.severe("Unable to record public chat!");
            e.printStackTrace();
            YellSystem.alertStaff("Unable to record public chat, please contact an admin.");
        }
    }
    public static void recordYellChat(Player player, String message) {
        if (getGameWorldId() > 1) {
            return;
        }
        try {
            message = message.replaceAll("'", "`").replaceAll("\\u005C", "/"); //letter '\' and ' is not accepted by sql!
            Statement statement = getDbConnection().createStatement();
            String query = "INSERT INTO " + DbTables.GAME_CHAT_LOGS + "(type, sender, receiver, message, timestamp) VALUES ('2', '" + player.dbId + "', '-1', '"+message+"', '" + getTimeStamp() + "')";
            statement.executeUpdate(query);
            statement.close();
        } catch (Exception e) {
            logger.severe("Unable to record yell chat!");
            e.printStackTrace();
            YellSystem.alertStaff("Unable to record yell chat, please contact an admin.");
        }
    }
    public static void recordModChat(Player player, String message) {
        if (getGameWorldId() > 1) {
            return;
        }
        try {
            message = message.replaceAll("'", "`").replaceAll("\\u005C", "/"); //letter '\' and ' is not accepted by sql!
            Statement statement = getDbConnection().createStatement();
            String query = "INSERT INTO " + DbTables.GAME_CHAT_LOGS + "(type, sender, receiver, message, timestamp) VALUES ('4', '" + player.dbId + "', '-1', '"+message+"', '" + getTimeStamp() + "')";
            statement.executeUpdate(query);
            statement.close();
        } catch (Exception e) {
            logger.severe("Unable to record mod chat!");
            e.printStackTrace();
        }
    }
    public static void recordPrivateChat(Player sender, Player receiver, String message) {
        if (getGameWorldId() > 1) {
            return;
        }
        message = message.replaceAll("'", "`").replaceAll("\\u005C", "/"); //letter '\' and ' is not accepted by sql!
        System.out.println("private message = " + message);
        try {
            Statement statement = getDbConnection().createStatement();
            String query = "INSERT INTO " + DbTables.GAME_CHAT_LOGS + "(type, sender, receiver, message, timestamp) VALUES ('3', '" + sender.dbId + "', '" + receiver.dbId + "', '"+message+"', '" + getTimeStamp() + "')";
            statement.executeUpdate(query);
            statement.close();
        } catch (Exception e) {
            logger.severe("Unable to record private chat between " + sender.dbId + " and " + receiver.dbId);
            e.printStackTrace();
            YellSystem.alertStaff("Unable to record private chat of ("+sender.getPlayerName()+") to ("+receiver.getPlayerName()+"), please contact an admin.");
        }
    }
}
