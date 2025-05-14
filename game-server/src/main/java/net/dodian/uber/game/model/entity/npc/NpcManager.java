/**
 *
 */
package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.utilities.DbTables;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static net.dodian.utilities.DatabaseKt.getDbConnection;
import static net.dodian.utilities.DatabaseKt.getDbStatement;

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
        try {
            int amount = 0;
            Statement statement = getDbConnection().createStatement();
            ResultSet results = statement.executeQuery("SELECT * FROM " + DbTables.GAME_NPC_SPAWNS);
            while (results.next()) {
                amount++;
                createNpc(results.getInt("id"), new Position(results.getInt("x"), results.getInt("y"), results.getInt("height")), results.getInt("face"));
            }
            System.out.println("Loaded " + amount + " Npc Spawns");
            results.close();
            statement.close();
        } catch (Exception e) {
            System.out.println("something off with npc spawn!" + e);
        }
        int extraSpawns = 0;
        gnomeSpawn = nextIndex;
        for (Position position : gnomePosition) {
            createNpc(6080, position, 0);
            extraSpawns++;
        }
        werewolfSpawn = nextIndex;
        for(int i = 0; i < werewolfPosition.length; i++) {
            createNpc(i == 0 ? 5924 : i == werewolfPosition.length - 1 ? 5927 : 5926, werewolfPosition[i], 0);
            extraSpawns++;
        }
        /* Daganoth kings */
        dagaRex = nextIndex;
        createNpc(2267, new Position(3329, 3359, 0), 2);
        dagaSupreme = nextIndex;
        createNpc(2265, new Position(3333, 3359, 0), 2);
        extraSpawns += 2;
        System.out.println("Loaded " + extraSpawns + " Extra Npc Spawns!");
    }

    public void reloadDrops(Client c, int id) {
        try {
            if (data.containsKey(id)) {
                data.get(id).getDrops().clear();
                Statement statement = getDbConnection().createStatement();
                ResultSet results = statement.executeQuery("SELECT * FROM " + DbTables.GAME_NPC_DROPS + " where npcid='" + id + "'");
                while (results.next()) {
                    data.get(id).addDrop(results.getInt("itemid"), results.getInt("amt_min"), results.getInt("amt_max"),
                            results.getDouble("percent"), results.getBoolean("rareShout"));
                }
                c.send(new SendMessage("Finished reloading all drops for " + data.get(id).getName()));
                results.close();
                statement.close();
            } else
                c.send(new SendMessage("No npc with id of " + id));
        } catch (Exception e) {
            System.out.println("npc drop wrong during drop reload.." + e);
        }
    }

    public void reloadAllData(Client c, int id) {
        try {
            Statement statement = getDbConnection().createStatement();
            ResultSet results = statement.executeQuery("SELECT * FROM " + DbTables.GAME_NPC_DEFINITIONS + " where id='" + id + "'");
            if (results.next()) {
                data.replace(results.getInt("id"), new NpcData(results));
                /* Reload all npc data! */
                for (Npc n : npcs.values())
                    if (n.getId() == id)
                        n.reloadData();
            }
            reloadDrops(c, id); //Need to set drops!
            c.send(new SendMessage("Finished updating all '" + getData(id).getName() + "' npcs!"));
            results.close();
            statement.close();
        } catch (Exception e) {
            System.out.println("npc drop wrong during reload of data.." + e);
        }
    }

    public void reloadNpcConfig(Client c, int id, String table, String value) {
        if (!data.containsKey(id)) {
            try {
                Statement statement = getDbConnection().createStatement();
                statement.executeUpdate("INSERT INTO " + DbTables.GAME_NPC_DEFINITIONS + "(id, name, examine, size) VALUES("+id+", 'no_name', 'no_examine', '1')");
                ResultSet results = getDbStatement().executeQuery("SELECT * FROM " + DbTables.GAME_NPC_DEFINITIONS + " where id='" + id + "'");
                if (results.next()) {
                    data.put(results.getInt("id"), new NpcData(results));
                    c.send(new SendMessage("Added default config values to the npc!"));
                }
                results.close();
                statement.close();
            } catch (Exception e) {
                System.out.println("error? " + e);
            }
        } else if (!table.equalsIgnoreCase("new npc")) {
            try {
                Statement statement = getDbConnection().createStatement();
                String query = "UPDATE " + DbTables.GAME_NPC_DEFINITIONS + " SET " + table + " = '" + value + "' WHERE id = '" + id + "'";
                statement.executeUpdate(query);
                c.send(new SendMessage("You updated '" + table + "' with value '" + value + "'!"));
                reloadAllData(c, id);
                statement.close();
            } catch (Exception e) {
                if (e.getMessage().contains("Unknown column"))
                    c.send(new SendMessage("row name '" + table + "' do not exist in the database!"));
                else if (e.getMessage().contains("Incorrect integer"))
                    c.send(new SendMessage("row name '" + table + "' need a int value!"));
                else System.out.println("npc drop wrong during config reload.." + e);
            }
        }
    }

    public void loadData() {
        try {
            int amount = 0;
            Statement statement = getDbConnection().createStatement();
            ResultSet results = statement.executeQuery("SELECT * FROM " + DbTables.GAME_NPC_DEFINITIONS);
            while (results.next()) {
                amount++;
                data.put(results.getInt("id"), new NpcData(results));
            }
            System.out.println("Loaded " + amount + " Npc Definitions");
            amount = 0;
            results = getDbStatement().executeQuery("SELECT * FROM " + DbTables.GAME_NPC_DROPS);
            while (results.next()) {
                if (results.getInt("npcid") > 0) {
                    amount++;
                    int id = results.getInt("npcid");
                    if (data.containsKey(id)) {
                        data.get(id).addDrop(results.getInt("itemid"), results.getInt("amt_min"), results.getInt("amt_max"),
                                results.getDouble("percent"), results.getBoolean("rareShout"));
                    } else
                        System.out.println("Invalid key: " + results.getInt("npcid"));
                }
            }
            System.out.println("Loaded " + amount + " Npc Drops");
            statement.close();
            results.close();
        } catch (Exception e) {
            System.out.println("npc drop wrong during load of data.." + e);
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