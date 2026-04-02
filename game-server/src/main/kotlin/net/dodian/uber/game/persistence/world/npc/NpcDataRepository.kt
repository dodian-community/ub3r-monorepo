package net.dodian.uber.game.persistence.world.npc

import net.dodian.uber.game.model.entity.npc.NpcData
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.repository.DbAsyncRepository

data class NpcDropRow(
    val itemId: Int,
    val amountMin: Int,
    val amountMax: Int,
    val percent: Double,
    val rareShout: Boolean,
)

object NpcDataRepository {
    @JvmStatic
    fun loadDefinitions(): Map<Int, NpcData> =
        DbAsyncRepository.withConnection { connection ->
            connection
                .createStatement()
                .use { statement ->
                    statement.executeQuery("SELECT * FROM ${DbTables.GAME_NPC_DEFINITIONS}").use { results ->
                        val definitions = HashMap<Int, NpcData>()
                        while (results.next()) {
                            definitions[results.getInt("id")] = NpcData(results)
                        }
                        definitions
                    }
                }
        }

    @JvmStatic
    fun loadDefinitionById(id: Int): NpcData? =
        DbAsyncRepository.withConnection { connection ->
            connection
                .prepareStatement("SELECT * FROM ${DbTables.GAME_NPC_DEFINITIONS} WHERE id = ?")
                .use { statement ->
                    statement.setInt(1, id)
                    statement.executeQuery().use { results ->
                        if (results.next()) {
                            NpcData(results)
                        } else {
                            null
                        }
                    }
                }
        }

    @JvmStatic
    fun insertDefaultDefinition(id: Int): Unit =
        DbAsyncRepository.withConnection { connection ->
            connection
                .prepareStatement(
                    "INSERT IGNORE INTO ${DbTables.GAME_NPC_DEFINITIONS}(id, name, examine, size) VALUES(?, ?, 'no_examine', '1')",
                ).use { statement ->
                    statement.setInt(1, id)
                    statement.setString(2, "Unknown_npc_$id")
                    statement.executeUpdate()
                }
        }

    @JvmStatic
    fun updateDefinitionField(id: Int, field: String, value: String): Unit =
        DbAsyncRepository.withConnection { connection ->
            connection
                .createStatement()
                .use { statement ->
                    statement.executeUpdate("UPDATE ${DbTables.GAME_NPC_DEFINITIONS} SET $field = '$value' WHERE id = '$id'")
                }
        }

    @JvmStatic
    fun loadDropsForNpc(id: Int): List<NpcDropRow> =
        DbAsyncRepository.withConnection { connection ->
            connection
                .prepareStatement("SELECT itemid, amt_min, amt_max, percent, rareShout FROM ${DbTables.GAME_NPC_DROPS} WHERE npcid = ?")
                .use { statement ->
                    statement.setInt(1, id)
                    statement.executeQuery().use { results ->
                        val drops = ArrayList<NpcDropRow>()
                        while (results.next()) {
                            drops +=
                                NpcDropRow(
                                    itemId = results.getInt("itemid"),
                                    amountMin = results.getInt("amt_min"),
                                    amountMax = results.getInt("amt_max"),
                                    percent = results.getDouble("percent"),
                                    rareShout = results.getBoolean("rareShout"),
                                )
                        }
                        drops
                    }
                }
        }

    @JvmStatic
    fun loadAllDropsByNpcId(): Map<Int, List<NpcDropRow>> =
        DbAsyncRepository.withConnection { connection ->
            connection
                .createStatement()
                .use { statement ->
                    statement.executeQuery("SELECT npcid, itemid, amt_min, amt_max, percent, rareShout FROM ${DbTables.GAME_NPC_DROPS}")
                        .use { results ->
                            val dropsByNpc = HashMap<Int, MutableList<NpcDropRow>>()
                            while (results.next()) {
                                val npcId = results.getInt("npcid")
                                if (npcId <= 0) {
                                    continue
                                }
                                val list = dropsByNpc.getOrPut(npcId) { ArrayList() }
                                list +=
                                    NpcDropRow(
                                        itemId = results.getInt("itemid"),
                                        amountMin = results.getInt("amt_min"),
                                        amountMax = results.getInt("amt_max"),
                                        percent = results.getDouble("percent"),
                                        rareShout = results.getBoolean("rareShout"),
                                    )
                            }
                            HashMap(dropsByNpc)
                        }
                }
        }
}
