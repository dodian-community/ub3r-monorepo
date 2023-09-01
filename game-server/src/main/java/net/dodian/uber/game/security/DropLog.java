package net.dodian.uber.game.security;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.utilities.DbTables;

import java.sql.Statement;
import java.util.logging.Logger;

import static net.dodian.uber.utilities.DotEnvKt.getGameWorldId;
import static net.dodian.uber.utilities.DatabaseKt.getDbConnection;

/**
 * Saves information about every player dropped item on the server. Contains
 * (Username, the item, the amount).
 *
 * @author Stephen
 */
public class DropLog extends LogEntry {

    /**
     * The logger for the class.
     */
    private final static Logger logger = Logger.getLogger(DropLog.class.getName());

    /**
     * Adds a drop record to the drop table.
     *
     * @param player The player dropping the item.
     * @param id   The item being dropped.
     */
    public static void recordDrop(Player player, int id, int amount, String type, Position pos, String reason) {
        if (getGameWorldId() > 1) {
            return;
        }
        try {
            Statement statement = getDbConnection().createStatement();
            String query = "INSERT INTO " + DbTables.GAME_LOGS_ITEM_DROPS + "(username, item, amount, type, timestamp, x, y, z, reason) VALUES ('" + player.getPlayerName()
                    + "', '" + id + "', '" + amount + "', '" + type.replaceAll("_", " ") + "', '" + getTimeStamp() + "', '" + pos.getX() + "', '" + pos.getY() + "', '" + pos.getZ() + "', '"+reason+"')";
            statement.executeUpdate(query);
            statement.close();
        } catch (Exception e) {
            logger.severe("Unable to record dropped item!");
            e.printStackTrace();
            YellSystem.alertStaff("Unable to record dropped items, please contact an admin.");
        }
    }

}
