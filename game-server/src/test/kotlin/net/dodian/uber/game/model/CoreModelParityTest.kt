package net.dodian.uber.game.model

import net.dodian.uber.game.model.entity.Entity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoreModelParityTest {
    @Test
    fun `position copy move and equality remain packed-value based`() {
        val pos = Position(3200, 3200, 0)
        val copy = pos.copy()

        assertEquals(pos, copy)
        assertEquals(pos.hashCode(), copy.hashCode())

        copy.moveTo(3201, 3202, 1)
        assertNotEquals(pos, copy)
        assertEquals(3201, copy.x)
        assertEquals(3202, copy.y)
        assertEquals(1, copy.z)

        val delta = Position.delta(pos, copy)
        assertEquals(1, delta.x)
        assertEquals(2, delta.y)
    }

    @Test
    fun `update flags masks remain protocol compatible for player and npc`() {
        val flags = UpdateFlags()
        flags.setRequired(UpdateFlag.APPEARANCE, true)
        flags.setRequired(UpdateFlag.HIT, true)

        assertTrue(flags.isUpdateRequired)
        assertTrue(flags.get(UpdateFlag.APPEARANCE))
        assertFalse(flags.get(UpdateFlag.CHAT))

        assertEquals(0x30, flags.getMask(Entity.Type.PLAYER))
        assertEquals(0x42, flags.getMask(Entity.Type.NPC))
    }

    @Test
    fun `walk to task equality is type-safe and preserves identity semantics for position`() {
        val shared = Position(3000, 3000, 0)
        val taskA = WalkToTask(WalkToTask.Action.ATTACK_NPC, 5, shared)
        val taskB = WalkToTask(WalkToTask.Action.ATTACK_NPC, 5, shared)
        val taskC = WalkToTask(WalkToTask.Action.ATTACK_NPC, 5, Position(3000, 3000, 0))

        assertTrue(taskA == taskB)
        assertFalse(taskA == taskC)
        assertFalse(taskA.equals("not-a-task"))
    }
}
