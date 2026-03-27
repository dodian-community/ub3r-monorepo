package net.dodian.uber.game.model.entity.npc

import java.sql.ResultSet

data class NpcDropData(
    val id: Int,
    val minAmount: Int,
    val maxAmount: Int,
    val percent: Double,
    val rareShout: Boolean,
)

data class NpcDefinitionData(
    val id: Int,
    val name: String,
    val examine: String?,
    val attackEmote: Int,
    val deathEmote: Int,
    val respawn: Int,
    val combat: Int,
    val size: Int,
    val defence: Int,
    val attack: Int,
    val strength: Int,
    val hitpoints: Int,
    val ranged: Int,
    val magic: Int,
)

object NpcDataInterop {
    @JvmStatic
    fun fromResultSet(row: ResultSet): NpcDefinitionData =
        NpcDefinitionData(
            id = row.getInt("id"),
            name = row.getString("name") ?: "",
            examine = row.getString("examine"),
            attackEmote = row.getInt("attackEmote"),
            deathEmote = row.getInt("deathEmote"),
            respawn = row.getInt("respawn"),
            combat = row.getInt("combat"),
            size = row.getInt("size"),
            defence = row.getInt("defence"),
            attack = row.getInt("attack"),
            strength = row.getInt("strength"),
            hitpoints = row.getInt("hitpoints"),
            ranged = row.getInt("ranged"),
            magic = row.getInt("magic"),
        )

    @JvmStatic
    fun toLegacy(data: NpcDefinitionData): NpcData =
        NpcData(
            data.name,
            data.examine,
            data.attackEmote,
            data.deathEmote,
            data.respawn,
            data.combat,
            data.size,
            intArrayOf(
                data.defence,
                data.attack,
                data.strength,
                data.hitpoints,
                data.ranged,
                0,
                data.magic,
            ),
        )
}

