package net.dodian.uber.game.persistence.world.npc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NpcDataRepositoryFieldValidationTest {
    @Test
    fun `parse definition field accepts known aliases`() {
        assertEquals(
            NpcDataRepository.DefinitionField.ATTACK_EMOTE,
            NpcDataRepository.parseDefinitionField("attackemote"),
        )
        assertEquals(
            NpcDataRepository.DefinitionField.HITPOINTS,
            NpcDataRepository.parseDefinitionField("hitpoints"),
        )
        assertEquals(
            NpcDataRepository.DefinitionField.NAME,
            NpcDataRepository.parseDefinitionField("name"),
        )
    }

    @Test
    fun `parse definition field rejects unknown values`() {
        assertThrows(IllegalArgumentException::class.java) {
            NpcDataRepository.parseDefinitionField("drop_table")
        }
    }
}
