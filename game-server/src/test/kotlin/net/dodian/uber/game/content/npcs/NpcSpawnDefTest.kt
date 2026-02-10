package net.dodian.uber.game.content.npcs

import net.dodian.uber.game.content.npcs.spawns.NpcSpawnDef
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcSpawnDefTest {

    @Test
    fun `defaults include hard-cutover spawn behavior values`() {
        val def = NpcSpawnDef(npcId = 1, x = 10, y = 20)

        assertEquals(0, def.walkRadius)
        assertEquals(6, def.attackRange)
        assertFalse(def.alwaysActive)
        assertNull(def.preset)
        assertEquals(-1, def.respawnTicks)
        assertEquals(-1, def.attack)
        assertEquals(-1, def.defence)
        assertEquals(-1, def.strength)
        assertEquals(-1, def.hitpoints)
        assertEquals(-1, def.ranged)
        assertEquals(-1, def.magic)
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
}
