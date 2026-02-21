package net.dodian.uber.game.security;

import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.utilities.DbTables;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static net.dodian.utilities.DatabaseKt.getDbConnection;
import static net.dodian.utilities.DotEnvKt.getGameWorldId;

/**
 * Saves all trades that take place.
 */
public class TradeLog extends LogEntry {

    private static final Logger logger = Logger.getLogger(TradeLog.class.getName());

    public static void recordTrade(int p1,
                                   int p2,
                                   CopyOnWriteArrayList<GameItem> items,
                                   CopyOnWriteArrayList<GameItem> otherItems,
                                   boolean trade) {
        if (getGameWorldId() > 1) {
            return;
        }

        AsyncSqlService.execute("trade-log", () -> {
            try (java.sql.Connection conn = getDbConnection();
                 Statement statement = conn.createStatement()) {
                int type = trade ? 0 : 1;
                String query = "INSERT INTO " + DbTables.GAME_LOGS_PLAYER_TRADES +
                        " SET p1=" + p1 + ", p2=" + p2 + ", type='" + type + "', date='" + getTimeStamp() + "'";
                statement.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);

                try (ResultSet inserted = statement.getGeneratedKeys()) {
                    inserted.next();
                    int id = inserted.getInt(1);

                    for (GameItem item : items) {
                        statement.executeUpdate("INSERT INTO " + DbTables.GAME_LOGS_PLAYER +
                                " SET id = " + id + ", pid=" + p1 + ", item=" + item.getId() + ", amount=" + item.getAmount());
                    }
                    for (GameItem item : otherItems) {
                        statement.executeUpdate("INSERT INTO " + DbTables.GAME_LOGS_PLAYER +
                                " SET id = " + id + ", pid=" + p2 + ", item=" + item.getId() + ", amount=" + item.getAmount());
                    }
                }
            } catch (Exception e) {
                logger.severe("Unable to record trade!");
                e.printStackTrace();
                YellSystem.alertStaff("Unable to record trade, please contact an admin.");
            }
        });
    }
}
