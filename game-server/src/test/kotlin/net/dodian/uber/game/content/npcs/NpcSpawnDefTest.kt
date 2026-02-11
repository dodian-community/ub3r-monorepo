package net.dodian.uber.game.content.npcs

import net.dodian.uber.game.content.npcs.spawns.MYSQL_DEFAULT_STAT
import net.dodian.uber.game.content.npcs.spawns.NpcSpawnDef
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcSpawnDefTest {

    @Test
    fun `defaults include hard-cutover spawn behavior values`() {
        val def = NpcSpawnDef(npcId = 1, x = 10, y = 20)

        assertEquals(0, def.walkRadius)
        assertEquals(6, def.attackRange)
        assertFalse(def.alwaysActive)
        assertEquals(MYSQL_DEFAULT_STAT, def.respawnTicks)
        assertEquals(MYSQL_DEFAULT_STAT, def.attack)
        assertEquals(MYSQL_DEFAULT_STAT, def.defence)
        assertEquals(MYSQL_DEFAULT_STAT, def.strength)
        assertEquals(MYSQL_DEFAULT_STAT, def.hitpoints)
        assertEquals(MYSQL_DEFAULT_STAT, def.ranged)
        assertEquals(MYSQL_DEFAULT_STAT, def.magic)
        assertNotNull(def.condition)
    }

    @Test
    fun `explicit spawn behavior overrides are preserved`() {
        val condition: (Client) -> Boolean = { it.playerRights > 0 }

        val def = NpcSpawnDef(
            npcId = 2,
            x = 30,
            y = 40,
            walkRadius = 4,
            attackRange = 9,
            alwaysActive = true,
            condition = condition,
        )

        assertEquals(4, def.walkRadius)
        assertEquals(9, def.attackRange)
        assertTrue(def.alwaysActive)
        assertSame(condition, def.condition)
    }

    @Test
    fun `withStatOverrides applies only provided values`() {
        val def = NpcSpawnDef(npcId = 2, x = 30, y = 40).withStatOverrides(attack = 45, hitpoints = 90)

        assertEquals(45, def.attack)
        assertEquals(90, def.hitpoints)
        assertEquals(MYSQL_DEFAULT_STAT, def.defence)
        assertEquals(MYSQL_DEFAULT_STAT, def.strength)
        assertEquals(MYSQL_DEFAULT_STAT, def.ranged)
        assertEquals(MYSQL_DEFAULT_STAT, def.magic)
        assertEquals(MYSQL_DEFAULT_STAT, def.respawnTicks)
    }
}
