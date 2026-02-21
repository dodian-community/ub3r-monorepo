package net.dodian.uber.game.security;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.utilities.DbTables;

import java.sql.Statement;
import java.util.logging.Logger;

import static net.dodian.utilities.DatabaseKt.getDbConnection;
import static net.dodian.utilities.DotEnvKt.getGameWorldId;

/**
 * Saves information about player item events.
 */
public class ItemLog extends LogEntry {

    private static final Logger logger = Logger.getLogger(ItemLog.class.getName());

    public static void playerPickup(Player player, int userId, int itemId, int itemAmount, Position pos, boolean npc) {
        if (getGameWorldId() > 1) {
            return;
        }

        AsyncSqlService.execute("item-log-pickup", () -> {
            try (java.sql.Connection conn = getDbConnection();
                 Statement statement = conn.createStatement()) {
                String query = "INSERT INTO " + DbTables.GAME_LOGS_ITEMS + "(receiver, type, from_id, item_id, item_amount, timestamp, x, y, z, reason) VALUES(" +
                        "'" + player.dbId + "', '1', '" + userId + "', '" + itemId + "', '" + itemAmount + "', '" + getTimeStamp() + "', '" + pos.getX() + "', '" + pos.getY() + "', '" + pos.getZ() + "', '" + (npc ? "npc" : "player") + "')";
                statement.executeUpdate(query);
            } catch (Exception e) {
                logger.severe("Unable to record player picking up items!");
                e.printStackTrace();
                YellSystem.alertStaff("Unable to record player pickup of a item, please contact an admin.");
            }
        });
    }

    public static void playerDrop(Player player, int itemId, int itemAmount, Position pos, String reason) {
        if (getGameWorldId() > 1) {
            return;
        }

        AsyncSqlService.execute("item-log-drop", () -> {
            try (java.sql.Connection conn = getDbConnection();
                 Statement statement = conn.createStatement()) {
                String query = "INSERT INTO " + DbTables.GAME_LOGS_ITEMS + "(receiver, type, from_id, item_id, item_amount, timestamp, x, y, z, reason) VALUES(" +
                        "'" + player.dbId + "', '2', '-1', '" + itemId + "', '" + itemAmount + "', '" + getTimeStamp() + "', '" + pos.getX() + "', '" + pos.getY() + "', '" + pos.getZ() + "', '" + (reason.isEmpty() ? "player" : reason) + "')";
                statement.executeUpdate(query);
            } catch (Exception e) {
                logger.severe("Unable to record player dropping items!");
                e.printStackTrace();
                YellSystem.alertStaff("Unable to record player drop of a item, please contact an admin.");
            }
        });
    }

    public static void npcDrop(Player player, int npcId, int itemId, int itemAmount, Position pos) {
        if (getGameWorldId() > 1) {
            return;
        }

        AsyncSqlService.execute("item-log-npc-drop", () -> {
            try (java.sql.Connection conn = getDbConnection();
                 Statement statement = conn.createStatement()) {
                String query = "INSERT INTO " + DbTables.GAME_LOGS_ITEMS + "(receiver, type, from_id, item_id, item_amount, timestamp, x, y, z, reason) VALUES(" +
                        "'" + player.dbId + "', '2', '" + npcId + "', '" + itemId + "', '" + itemAmount + "', '" + getTimeStamp() + "', '" + pos.getX() + "', '" + pos.getY() + "', '" + pos.getZ() + "', 'npc')";
                statement.executeUpdate(query);
            } catch (Exception e) {
                logger.severe("Unable to record npc dropping items!");
                e.printStackTrace();
                YellSystem.alertStaff("Unable to record npc drop of a item, please contact an admin.");
            }
        });
    }

    public static void playerGathering(Player player, int itemId, int itemAmount, Position pos, String reason) {
        if (getGameWorldId() > 1) {
            return;
        }

        AsyncSqlService.execute("item-log-gathering", () -> {
            try (java.sql.Connection conn = getDbConnection();
                 Statement statement = conn.createStatement()) {
                String query = "INSERT INTO " + DbTables.GAME_LOGS_ITEMS + "(receiver, type, from_id, item_id, item_amount, timestamp, x, y, z, reason) VALUES(" +
                        "'" + player.dbId + "', '3', '-1', '" + itemId + "', '" + itemAmount + "', '" + getTimeStamp() + "', '" + pos.getX() + "', '" + pos.getY() + "', '" + pos.getZ() + "', '" + reason + "')";
                statement.executeUpdate(query);
            } catch (Exception e) {
                logger.severe("Unable to record player gathering items!");
                e.printStackTrace();
                YellSystem.alertStaff("Unable to record player pickup of a item, please contact an admin.");
            }
        });
    }
}
