package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.player.Client

const val MYSQL_DEFAULT_STAT = -1
const val NORTH = 0
const val EAST = 2
const val SOUTH = 4
const val WEST = 6
const val north = NORTH
const val east = EAST
const val south = SOUTH
const val west = WEST

data class NpcSpawnDef(
    val npcId: Int,
    val x: Int,
    val y: Int,
    val z: Int = 0,
    val face: Int = 0,
    val walkRadius: Int = 0,
    val attackRange: Int = 6,
    val alwaysActive: Boolean = false,
    val condition: (Client) -> Boolean = { true },
    val respawnTicks: Int = MYSQL_DEFAULT_STAT,
    val attack: Int = MYSQL_DEFAULT_STAT,
    val defence: Int = MYSQL_DEFAULT_STAT,
    val strength: Int = MYSQL_DEFAULT_STAT,
    val hitpoints: Int = MYSQL_DEFAULT_STAT,
    val ranged: Int = MYSQL_DEFAULT_STAT,
    val magic: Int = MYSQL_DEFAULT_STAT,
) {
    fun withStatOverrides(
        respawnTicks: Int? = null,
        attack: Int? = null,
        defence: Int? = null,
        strength: Int? = null,
        hitpoints: Int? = null,
        ranged: Int? = null,
        magic: Int? = null,
    ): NpcSpawnDef = copy(
        respawnTicks = respawnTicks ?: this.respawnTicks,
        attack = attack ?: this.attack,
        defence = defence ?: this.defence,
        strength = strength ?: this.strength,
        hitpoints = hitpoints ?: this.hitpoints,
        ranged = ranged ?: this.ranged,
        magic = magic ?: this.magic,
    )
}
