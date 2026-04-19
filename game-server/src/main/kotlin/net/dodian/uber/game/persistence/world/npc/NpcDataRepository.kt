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
    enum class DefinitionField(
        val column: String,
        val parser: (String) -> Any,
    ) {
        NAME("name", { it }),
        EXAMINE("examine", { it }),
        ATTACK_EMOTE("attackEmote", { it.toInt() }),
        DEATH_EMOTE("deathEmote", { it.toInt() }),
        RESPAWN("respawn", { it.toInt() }),
        COMBAT("combat", { it.toInt() }),
        SIZE("size", { it.toInt() }),
        DEFENCE("defence", { it.toInt() }),
        ATTACK("attack", { it.toInt() }),
        STRENGTH("strength", { it.toInt() }),
        HITPOINTS("hitpoints", { it.toInt() }),
        RANGED("ranged", { it.toInt() }),
        MAGIC("magic", { it.toInt() }),
        ;

        companion object {
            private val byAlias =
                values().associateBy { it.column.lowercase() } +
                    mapOf(
                        "attackemote" to ATTACK_EMOTE,
                        "deathemote" to DEATH_EMOTE,
                        "hitpoints" to HITPOINTS,
                    )

            @JvmStatic
            fun fromInput(input: String): DefinitionField? = byAlias[input.trim().lowercase()]
        }
    }

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
    fun updateDefinitionField(id: Int, field: DefinitionField, value: String): Unit =
        DbAsyncRepository.withConnection { connection ->
            val query = "UPDATE ${DbTables.GAME_NPC_DEFINITIONS} SET ${field.column} = ? WHERE id = ?"
            connection.prepareStatement(query).use { statement ->
                bindValue(statement, index = 1, value = field.parser(value))
                statement.setInt(2, id)
                statement.executeUpdate()
            }
        }

    @JvmStatic
    fun parseDefinitionField(input: String): DefinitionField =
        DefinitionField.fromInput(input)
            ?: throw IllegalArgumentException("Unknown NPC definition field: $input")

    private fun bindValue(statement: java.sql.PreparedStatement, index: Int, value: Any) {
        when (value) {
            is Int -> statement.setInt(index, value)
            is String -> statement.setString(index, value)
            else -> throw IllegalArgumentException("Unsupported NPC definition field type: ${value::class.java.simpleName}")
        }
    }

    @JvmStatic
    fun updateDefinitionField(id: Int, field: String, value: String) {
        val typedField = parseDefinitionField(field)
        updateDefinitionField(id, typedField, value)
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
