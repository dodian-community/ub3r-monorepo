/**
 *
 */
package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.content.npcs.spawns.NpcSpawnDef;
import net.dodian.uber.game.content.npcs.spawns.NpcContentRegistry;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.utilities.DbTables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class NpcManager {
    private static final Logger logger = LoggerFactory.getLogger(NpcManager.class);
    private static final Set<Integer> REQUIRED_HARDCODED_NPC_DEFINITIONS =
            new HashSet<>(Arrays.asList(6080, 5924, 5926, 5927, 2267, 2265, 394, 395, 7677));
    private static final Map<Integer, String> REQUIRED_HARDCODED_NPC_NAMES = new HashMap<>();
    static {
        REQUIRED_HARDCODED_NPC_NAMES.put(6080, "Gnome_balloon_pilot");
        REQUIRED_HARDCODED_NPC_NAMES.put(5924, "Agility_werewolf");
        REQUIRED_HARDCODED_NPC_NAMES.put(5926, "Agility_werewolf");
        REQUIRED_HARDCODED_NPC_NAMES.put(5927, "Agility_werewolf_master");
        REQUIRED_HARDCODED_NPC_NAMES.put(2267, "Dagannoth_Rex");
        REQUIRED_HARDCODED_NPC_NAMES.put(2265, "Dagannoth_Supreme");
        REQUIRED_HARDCODED_NPC_NAMES.put(394, "Banker");
        REQUIRED_HARDCODED_NPC_NAMES.put(395, "Banker");
        REQUIRED_HARDCODED_NPC_NAMES.put(7677, "Banker");
    }

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
        // TODO(npc-hard-cutover): Keep legacy SQL loading commented for rollback safety.
//        try (java.sql.Connection conn = getDbConnection();
//             Statement statement = conn.createStatement();
//             ResultSet results = statement.executeQuery("SELECT * FROM " + DbTables.GAME_NPC_SPAWNS)) {
//
//            int amount = 0;
//            while (results.next()) {
//                amount++;
//                createNpc(results.getInt("id"), new Position(results.getInt("x"), results.getInt("y"), results.getInt("height")), results.getInt("face"));
//            }
//            System.out.println("Loaded " + amount + " Npc Spawns");
//
//        } catch (Exception e) {
//            System.out.println("Something went wrong with loading NPC spawns from the database: " + e);
//            e.printStackTrace();
//        }
        logger.info("Skipping database NPC spawn loading");
        repairRequiredHardcodedDefinitions();

        // The rest of the logic for hardcoded/extra spawns remains the same.
        int hardcodedSpawns = 0;
        Map<Integer, Integer> skippedHardcodedMissingData = new TreeMap<>();
        gnomeSpawn = nextIndex;
        for (Position position : gnomePosition) {
            if (getData(6080) == null) {
                skippedHardcodedMissingData.merge(6080, 1, Integer::sum);
                continue;
            }
            createNpc(6080, position, 0);
            hardcodedSpawns++;
        }

        werewolfSpawn = nextIndex;
        for (int i = 0; i < werewolfPosition.length; i++) {
            int npcId = i == 0 ? 5924 : i == werewolfPosition.length - 1 ? 5927 : 5926;
            if (getData(npcId) == null) {
                skippedHardcodedMissingData.merge(npcId, 1, Integer::sum);
                continue;
            }
            createNpc(npcId, werewolfPosition[i], 0);
            hardcodedSpawns++;
        }

        /* Daganoth kings */
        dagaRex = nextIndex;
        if (getData(2267) != null) {
            createNpc(2267, new Position(3248, 2794, 0), 2);
            hardcodedSpawns++;
        } else {
            skippedHardcodedMissingData.merge(2267, 1, Integer::sum);
        }
        dagaSupreme = nextIndex;
        if (getData(2265) != null) {
            createNpc(2265, new Position(3251, 2794, 0), 2);
            hardcodedSpawns++;
        } else {
            skippedHardcodedMissingData.merge(2265, 1, Integer::sum);
        }

        if (!skippedHardcodedMissingData.isEmpty()) {
            logger.warn("Skipped hardcoded NPC spawns with missing definitions: {}", formatMissingNpcCounts(skippedHardcodedMissingData));
        }

        int contentSpawns = loadContentSpawns();
        int totalSpawns = hardcodedSpawns + contentSpawns;

        logger.info(
                "Loaded {} content NPC spawns and {} hardcoded extra NPC spawns from NpcManager (total {}).",
                contentSpawns,
                hardcodedSpawns,
                totalSpawns
        );
    }

    private int loadContentSpawns() {
        List<NpcSpawnDef> contentSpawns = NpcContentRegistry.allSpawns();
        int total = contentSpawns.size();
        int loaded = 0;
        int skipped = 0;
        int skippedDuplicatePosition = 0;
        int skippedMissingData = 0;
        int failed = 0;
        Map<Integer, Integer> missingDataByNpcId = new TreeMap<>();
        Set<String> seen = new HashSet<>(total);

        for (NpcSpawnDef spawn : contentSpawns) {
            try {
                if (getData(spawn.getNpcId()) == null) {
                    skipped++;
                    skippedMissingData++;
                    missingDataByNpcId.merge(spawn.getNpcId(), 1, Integer::sum);
                    continue;
                }
                Position position = new Position(spawn.getX(), spawn.getY(), spawn.getZ());
                String key = spawn.getNpcId() + ":" + spawn.getX() + ":" + spawn.getY() + ":" + spawn.getZ();
                if (!seen.add(key) || hasSpawnAt(spawn.getNpcId(), position)) {
                    skipped++;
                    skippedDuplicatePosition++;
                    continue;
                }
                Npc npc = createNpc(spawn.getNpcId(), position, spawn.getFace());
                if (npc == null) {
                    failed++;
                    continue;
                }
                // Keep base combat stats from MySQL npc definitions unless explicit spawn overrides are provided.
                npc.applySpawnOverrides(
                        spawn.getRespawnTicks(),
                        spawn.getAttack(),
                        spawn.getDefence(),
                        spawn.getStrength(),
                        spawn.getHitpoints(),
                        spawn.getRanged(),
                        spawn.getMagic()
                );
                npc.applySpawnBehaviorOverrides(
                        spawn.getWalkRadius(),
                        spawn.getAttackRange(),
                        spawn.getAlwaysActive(),
                        spawn.getCondition()
                );
                loaded++;
            } catch (Exception e) {
                failed++;
                logger.error(
                        "Failed to create content NPC spawn (id={}, x={}, y={}, z={}).",
                        spawn.getNpcId(),
                        spawn.getX(),
                        spawn.getY(),
                        spawn.getZ(),
                        e
                );
            }
        }

        if (!missingDataByNpcId.isEmpty()) {
            logger.warn(
                    "Skipped {} content NPC spawns with missing NPC definitions: {}",
                    skippedMissingData,
                    formatMissingNpcCounts(missingDataByNpcId)
            );
        }

        logger.info(
                "Loaded {}/{} content NPC spawns (skipped {}, duplicate {}, missing-data {}, failed {}).",
                loaded,
                total,
                skipped,
                skippedDuplicatePosition,
                skippedMissingData,
                failed
        );
        return loaded;
    }

    private void repairRequiredHardcodedDefinitions() {
        if (!readBooleanFlag("npc.hardcoded.definitions.repair.enabled", true)) {
            return;
        }
        List<Integer> missing = REQUIRED_HARDCODED_NPC_DEFINITIONS.stream()
                .filter(id -> getData(id) == null)
                .sorted()
                .collect(Collectors.toList());
        if (missing.isEmpty()) {
            return;
        }
        logger.warn("Missing required hardcoded NPC definitions at startup: {}", missing);
        List<Integer> repaired = new java.util.ArrayList<>();
        List<Integer> unresolved = new java.util.ArrayList<>(missing);

        try (java.sql.Connection conn = getDbConnection()) {
            String placeholders = String.join(",", java.util.Collections.nCopies(missing.size(), "?"));
            String selectSql = "SELECT id FROM " + DbTables.GAME_NPC_DEFINITIONS + " WHERE id IN (" + placeholders + ")";
            Set<Integer> existing = new HashSet<>();
            try (PreparedStatement select = conn.prepareStatement(selectSql)) {
                int idx = 1;
                for (int id : missing) {
                    select.setInt(idx++, id);
                }
                try (ResultSet rs = select.executeQuery()) {
                    while (rs.next()) {
                        existing.add(rs.getInt("id"));
                    }
                }
            }

            String insertSql =
                    "INSERT INTO " + DbTables.GAME_NPC_DEFINITIONS +
                            " (id,name,examine,combat,attackEmote,deathEmote,hitpoints,respawn,size,attack,strength,defence,ranged,magic) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                for (int id : missing) {
                    if (existing.contains(id)) {
                        continue;
                    }
                    insert.setInt(1, id);
                    insert.setString(2, REQUIRED_HARDCODED_NPC_NAMES.getOrDefault(id, "Npc_" + id));
                    insert.setString(3, "Auto repaired startup definition for hardcoded spawn.");
                    insert.setInt(4, 0);
                    insert.setInt(5, 806);
                    insert.setInt(6, 836);
                    insert.setInt(7, 1);
                    insert.setInt(8, 60);
                    insert.setInt(9, 1);
                    insert.setInt(10, 0);
                    insert.setInt(11, 0);
                    insert.setInt(12, 0);
                    insert.setInt(13, 0);
                    insert.setInt(14, 0);
                    insert.addBatch();
                }
                insert.executeBatch();
            }

            for (int id : missing) {
                if (reloadDefinition(conn, id)) {
                    repaired.add(id);
                }
            }
            unresolved.removeAll(repaired);
        } catch (Exception e) {
            logger.warn("Failed to repair required hardcoded NPC definitions: {}", missing, e);
        }

        if (!repaired.isEmpty()) {
            logger.info("Auto-repaired hardcoded NPC definitions: {}", repaired);
        }
        if (!unresolved.isEmpty()) {
            logger.warn("Hardcoded NPC definitions still unresolved after repair attempt: {}", unresolved);
            if (readBooleanFlag("npc.hardcoded.definitions.repair.strict", false)) {
                throw new IllegalStateException("Unresolved required hardcoded NPC definitions: " + unresolved);
            }
        }
    }

    private boolean reloadDefinition(java.sql.Connection conn, int id) {
        String query = "SELECT * FROM " + DbTables.GAME_NPC_DEFINITIONS + " where id=?";
        try (PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                data.put(id, new NpcData(rs));
                return true;
            }
        } catch (Exception e) {
            logger.warn("Failed to reload NPC definition for id={}", id, e);
            return false;
        }
    }

    private String formatMissingNpcCounts(Map<Integer, Integer> missingDataByNpcId) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<Integer, Integer> entry : missingDataByNpcId.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append("x").append(entry.getValue());
            first = false;
        }
        return builder.toString();
    }

    private boolean readBooleanFlag(String property, boolean defaultValue) {
        String prop = System.getProperty(property);
        if (prop != null && !prop.trim().isEmpty()) {
            return "true".equalsIgnoreCase(prop.trim());
        }
        String env = System.getenv(property.toUpperCase().replace('.', '_'));
        if (env != null && !env.trim().isEmpty()) {
            return "true".equalsIgnoreCase(env.trim());
        }
        return defaultValue;
    }

    private boolean hasSpawnAt(int npcId, Position position) {
        for (Npc npc : npcs.values()) {
            if (npc.getId() != npcId) continue;
            Position npcPosition = npc.getPosition();
            if (npcPosition.getX() == position.getX() && npcPosition.getY() == position.getY() && npcPosition.getZ() == position.getZ()) {
                return true;
            }
        }
        return false;
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
            logger.info("Loaded {} Npc Definitions", amount);

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
                        logger.warn("Invalid NPC ID for drop: {}", id);
                    }
                }
            }
            logger.info("Loaded {} Npc Drops", amount);

        } catch (Exception e) {
            System.out.println("Error loading NPC drops: " + e);
            e.printStackTrace(); // Print stack trace for better debugging
        }
    }
    public Npc createNpc(int id, Position position, int face) {
        Npc npc = new Npc(nextIndex, id, position, face);
        npcs.put(nextIndex, npc);
        nextIndex++;
        // Ensure runtime NPC spawns are visible to chunk-based systems.
        // Startup spawns are bootstrapped in Server.main after ChunkManager is constructed.
        if (net.dodian.uber.game.Server.chunkManager != null) {
            npc.syncChunkMembership();
        }
        return npc;
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
