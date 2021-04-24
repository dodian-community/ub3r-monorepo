package net.dodian.uber.game.security;

import net.dodian.Config;
import net.dodian.uber.game.model.YellSystem;
import net.dodian.utilities.Database;

import java.sql.Statement;
import java.util.logging.Logger;

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
   * @param sender
   *          The player sending the message.
   * @param receiver
   *          The player receiving the message.
   * @param message
   *          The message being sent.
   */
  public static void recordPm(String sender, String receiver, String message) {
    try {
  	  if(Config.worldId > 1) {
		  return;
	  }
      Statement statement = Database.conn.createStatement();
      String query = "INSERT INTO pm_log(sender, receiver, message, timestamp) VALUES ('" + sender + "', '" + receiver
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
