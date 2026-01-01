/**
 *
 */
package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.utilities.DbTables;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class NpcManager {
    Map<Integer, Npc> npcs = new HashMap<>();
    Map<Integer, NpcData> data = new HashMap<>();
    public int gnomeSpawn = -1, werewolfSpawn = -1;
    Position[] gnomePosition = {  new Position(2475, 3428, 0), new Position(2476, 3423, 1), new Position(2475, 3419, 2), new Position(2485, 3421, 2), new Position(2487, 3423, 0), new Position(2486, 3430, 0) };
    Position[] werewolfPosition = {  new Position(3540, 9873, 0), new Position(3540, 9890, 0), new Position(3537, 9903, 0), new Position(3533, 9908, 0), new Position(3528, 9913, 0), new Position(3527, 9865, 0) };
    public int dagaRex = -1, dagaSupreme = -1;
    int nextIndex = 1;

    public NpcManager() {
        loadData();
    }

    public Collection<Npc> getNpcs() {
        return npcs.values();
    }
    public Collection<NpcData> getNpcData() {
        return data.values();
    }

    public void loadSpawns() {
        // The database loading part is handled first.
        try (java.sql.Connection conn = getDbConnection();
             Statement statement = conn.createStatement();
             ResultSet results = statement.executeQuery("SELECT * FROM " + DbTables.GAME_NPC_SPAWNS)) {

            int amount = 0;
            while (results.next()) {
                amount++;
                createNpc(results.getInt("id"), new Position(results.getInt("x"), results.getInt("y"), results.getInt("height")), results.getInt("face"));
            }
            System.out.println("Loaded " + amount + " Npc Spawns");

        } catch (Exception e) {
            // The catch block is simpler and handles exceptions from the query or connection.
            System.out.println("Something went wrong with loading NPC spawns from the database: " + e);
            e.printStackTrace(); // It's often good practice to print the stack trace for debugging.
        }
        // No 'finally' block is needed for closing resources.

        // The rest of the logic for hardcoded/extra spawns remains the same.
        int extraSpawns = 0;
        gnomeSpawn = nextIndex;
        for (Position position : gnomePosition) {
            createNpc(6080, position, 0);
            extraSpawns++;
        }

        werewolfSpawn = nextIndex;
        for (int i = 0; i < werewolfPosition.length; i++) {
            createNpc(i == 0 ? 5924 : i == werewolfPosition.length - 1 ? 5927 : 5926, werewolfPosition[i], 0);
            extraSpawns++;
        }

        /* Daganoth kings */
        dagaRex = nextIndex;
        createNpc(2267, new Position(3248, 2794, 0), 2);
        dagaSupreme = nextIndex;
        createNpc(2265, new Position(3251, 2794, 0), 2);
        extraSpawns += 2;

        System.out.println("Loaded " + extraSpawns + " Extra Npc Spawns!");
    }

    public void reloadDrops(Client c, int id) {
        Statement statement = null;
        ResultSet results = null;
        try {
            if (data.containsKey(id)) {
                data.get(id).getDrops().clear();
                statement = getDbConnection().createStatement();
                results = statement.executeQuery("SELECT * FROM " + DbTables.GAME_NPC_DROPS + " where npcid='" + id + "'");
                while (results.next()) {
                    data.get(id).addDrop(results.getInt("itemid"), results.getInt("amt_min"), results.getInt("amt_max"),
                            results.getDouble("percent"), results.getBoolean("rareShout"));
                }
                c.send(new SendMessage("Finished reloading all drops for " + data.get(id).getName()));
            } else
                c.send(new SendMessage("No npc with id of " + id));
        } catch (Exception e) {
            System.out.println("npc drop wrong during drop reload.." + e);
        } finally {
            try {
                if (results != null) results.close();
                if (statement != null) statement.close();
            } catch (Exception e) {
                System.out.println("Error closing resources in reloadDrops: " + e);
            }
        }
    }

    public void reloadAllData(Client c, int id) {
        Statement statement = null;
        ResultSet results = null;
        try {
            statement = getDbConnection().createStatement();
            results = statement.executeQuery("SELECT * FROM " + DbTables.GAME_NPC_DEFINITIONS + " where id='" + id + "'");
            if (results.next()) {
                data.replace(results.getInt("id"), new NpcData(results));
                /* Reload all npc data! */
                for (Npc n : npcs.values())
                    if (n.getId() == id)
                        n.reloadData();
            }
            reloadDrops(c, id); //Need to set drops!
            c.send(new SendMessage("Finished updating all '" + getData(id).getName() + "' npcs!"));
        } catch (Exception e) {
            System.out.println("npc drop wrong during reload of data.." + e);
        } finally {
            try {
                if (results != null) results.close();
                if (statement != null) statement.close();
            } catch (Exception e) {
                System.out.println("Error closing resources in reloadAllData: " + e);
            }
        }
    }

    public void reloadNpcConfig(Client c, int id, String table, String value) {
        if (!data.containsKey(id)) {
            Statement statement1 = null;
            Statement statement2 = null;
            ResultSet results = null;
            try {
                statement1 = getDbConnection().createStatement();
                statement1.executeUpdate("INSERT INTO " + DbTables.GAME_NPC_DEFINITIONS + "(id, name, examine, size) VALUES("+id+", 'no_name', 'no_examine', '1')");
                statement2 = getDbConnection().createStatement();
                results = statement2.executeQuery("SELECT * FROM " + DbTables.GAME_NPC_DEFINITIONS + " where id='" + id + "'");
                if (results.next()) {
                    data.put(results.getInt("id"), new NpcData(results));
                    c.send(new SendMessage("Added default config values to the npc!"));
                }
            } catch (Exception e) {
                System.out.println("error? " + e);
            } finally {
                try {
                    if (results != null) results.close();
                    if (statement1 != null) statement1.close();
                    if (statement2 != null) statement2.close();
                } catch (Exception e) {
                    System.out.println("Error closing resources in reloadNpcConfig: " + e);
                }
            }
        } else if (!table.equalsIgnoreCase("new npc")) {
            Statement statement = null;
            try {
                statement = getDbConnection().createStatement();
                String query = "UPDATE " + DbTables.GAME_NPC_DEFINITIONS + " SET " + table + " = '" + value + "' WHERE id = '" + id + "'";
                statement.executeUpdate(query);
                c.send(new SendMessage("You updated '" + table + "' with value '" + value + "'!"));
                reloadAllData(c, id);
            } catch (Exception e) {
                if (e.getMessage().contains("Unknown column"))
                    c.send(new SendMessage("row name '" + table + "' do not exist in the database!"));
                else if (e.getMessage().contains("Incorrect integer"))
                    c.send(new SendMessage("row name '" + table + "' need a int value!"));
                else System.out.println("npc drop wrong during config reload.." + e);
            } finally {
                try {
                    if (statement != null) statement.close();
                } catch (Exception e) {
                    System.out.println("Error closing resources in reloadNpcConfig: " + e);
                }
            }
        }
    }

    public void loadData() {
        // First try-with-resources block for loading NPC definitions
        try (java.sql.Connection conn1 = getDbConnection();
             Statement statement1 = conn1.createStatement();
             ResultSet results1 = statement1.executeQuery("SELECT * FROM " + DbTables.GAME_NPC_DEFINITIONS)) {

            int amount = 0;
            while (results1.next()) {
                amount++;
                data.put(results1.getInt("id"), new NpcData(results1));
            }
            System.out.println("Loaded " + amount + " Npc Definitions");

        } catch (Exception e) {
            // It's good practice to log the specific operation that failed.
            System.out.println("Error loading NPC definitions: " + e);
            e.printStackTrace(); // Print stack trace for better debugging
        }

        // Second try-with-resources block for loading NPC drops
        try (java.sql.Connection conn2 = getDbConnection();
             Statement statement2 = conn2.createStatement();
             ResultSet results2 = statement2.executeQuery("SELECT * FROM " + DbTables.GAME_NPC_DROPS)) {

            int amount = 0;
            while (results2.next()) {
                if (results2.getInt("npcid") > 0) {
                    amount++;
                    int id = results2.getInt("npcid");
                    if (data.containsKey(id)) {
                        data.get(id).addDrop(results2.getInt("itemid"), results2.getInt("amt_min"), results2.getInt("amt_max"),
                                results2.getDouble("percent"), results2.getBoolean("rareShout"));
                    } else {
                        System.out.println("Invalid NPC ID for drop: " + id);
                    }
                }
            }
            System.out.println("Loaded " + amount + " Npc Drops");

        } catch (Exception e) {
            System.out.println("Error loading NPC drops: " + e);
            e.printStackTrace(); // Print stack trace for better debugging
        }
    }
    public void createNpc(int id, Position position, int face) {
        npcs.put(nextIndex, new Npc(nextIndex, id, position, face));
        nextIndex++;
    }

    public Npc getNpc(int index) {
        if (index > 0 && index < nextIndex && npcs.get(index) != null) {
            return npcs.get(index);
        } else {
            return null;
        }
    }

    public String getName(int id) {
        return data.get(id) == null ? "NO NPC NAME YET!" : data.get(id).getName();
    }

    public NpcData getData(int id) {
        return data.get(id);
    }
}