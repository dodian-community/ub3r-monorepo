package net.dodian.uber.game.security;

import net.dodian.Config;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.utilities.Database;

import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Saves information about every player dropped item on the server. Contains
 * (Username, the item, the amount).
 * 
 * @author Stephen
 */
public class PickupLog extends LogEntry {

  /**
   * The logger for the class.
   */
  private final static Logger logger = Logger.getLogger(PickupLog.class.getName());

  /**
   * Adds a drop record to the drop table.
   * 
   * @param player
   *          The player dropping the item.
   * @param item
   *          The item being dropped.
   */
  public static void recordPickup(Player player, int item, int amount, String type, Position pos) {
    try {
  	  if(Config.worldId > 1) {
		  return;
	  }
      Statement statement = Database.conn.createStatement();
      
      String query = "INSERT INTO pickup_log(username, item, amount, type, timestamp, x, y, z) VALUES ('" + player.getPlayerName()
          + "', '" + item + "', '" + amount + "', '" + type.replaceAll("_", " ") + "', '" + getTimeStamp() + "', '" + pos.getX() + "', '" + pos.getY() + "', '" + pos.getZ() + "')";
      statement.executeUpdate(query);
      statement.close();
    } catch (Exception e) {
      logger.severe("Unable to record dropped item!");
      e.printStackTrace();
      YellSystem.alertStaff("Unable to record dropped items, please contact an admin.");
    }
  }

}
