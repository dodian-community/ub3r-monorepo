package net.dodian.uber.game.security;

import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.utilities.DbTables;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static net.dodian.uber.utilities.DotEnvKt.getGameWorldId;
import static net.dodian.uber.utilities.DatabaseKt.getDbConnection;

/**
 * Saves all duels that take place.
 *
 * @author Stephen
 */
public class TradeLog extends LogEntry {

    /**
     * The logger for the class.
     */
    private static final Logger logger = Logger.getLogger(TradeLog.class.getName());

    /**
     * Inserts a new entry into 'uber3_logs'.
     *
     * @param p1        The player who initiates the duel.
     * @param p2      The other player involved.
     * @param items   The items the player is trading.
     * @param otherItems The items the other player is trading.
     */

    public static void recordTrade(int p1, int p2, CopyOnWriteArrayList<GameItem> items,
                CopyOnWriteArrayList<GameItem> otherItems, boolean trade) {
        if (getGameWorldId() > 1) {
            return;
        }
        try {
            int type = trade ? 0 : 1;
            Statement statement = getDbConnection().createStatement();
            String query = "INSERT INTO " + DbTables.GAME_LOGS_PLAYER_TRADES + " SET p1=" + p1 + ", p2=" + p2 + ", type='"+type+"', date='" + getTimeStamp() + "'";
            statement.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
            ResultSet inserted = statement.getGeneratedKeys();
            inserted.next();
            int id = inserted.getInt(1);
            for (GameItem item : items) {
                statement.executeUpdate("INSERT INTO " + DbTables.GAME_LOGS_PLAYER + " SET id = " + id + ", pid=" + p1 + ", item="
                        + item.getId() + ", amount=" + item.getAmount());
            }
            for (GameItem item : otherItems) {
                statement.executeUpdate("INSERT INTO " + DbTables.GAME_LOGS_PLAYER + " SET id = " + id + ", pid=" + p2 + ", item="
                        + item.getId() + ", amount=" + item.getAmount());
            }
            inserted.close();
        } catch (Exception e) {
            logger.severe("Unable to record trade!");
            e.printStackTrace();
            YellSystem.alertStaff("Unable to record trade, please contact an admin.");
        }
    }
}
