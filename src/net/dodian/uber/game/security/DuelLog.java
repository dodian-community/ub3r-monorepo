package net.dodian.uber.game.security;

import net.dodian.Config;
import net.dodian.uber.game.model.YellSystem;
import net.dodian.utilities.Database;

import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Saves all duels that take place.
 * 
 * @author Stephen
 */
public class DuelLog extends LogEntry {

  /**
   * The logger for the class.
   */
  private static final Logger logger = Logger.getLogger(DuelLog.class.getName());

  /**
   * Inserts a new entry into 'duel_log'.
   * 
   * @param player
   *          The player who initiates the duel.
   * @param opponent
   *          The other player involved.
   * @param playerStake
   *          The items the player is staking.
   * @param opponentStake
   *          The items the other player is staking.
   * @param winner
   *          The winner of the duel.
   */
  public static void recordDuel(String player, String opponent, String playerStake, String opponentStake,
      String winner) {
	  if(Config.worldId > 1) {
		  return;
	  }
    try {
      Statement statement = Database.conn.createStatement();
      String query = "INSERT INTO duel_log(player, opponent, playerstake, opponentstake, winner, timestamp) VALUES ('"
          + player + "', '" + opponent + "', '" + playerStake + "', '" + opponentStake + "', '" + winner + "', '"
          + getTimeStamp() + "')";
      statement.executeUpdate(query);
      statement.close();
    } catch (Exception e) {
      logger.severe("Unable to record duel!");
      e.printStackTrace();
      YellSystem.alertStaff("Unable to record duels, please contact an admin.");
    }
  }
}
