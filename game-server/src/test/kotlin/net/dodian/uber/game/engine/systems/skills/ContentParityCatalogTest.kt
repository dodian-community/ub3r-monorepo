package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.runtime.parity.LegacyContentParityCatalog
import net.dodian.uber.game.skill.runtime.parity.NpcClickRouteKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentParityCatalogTest {
    @Test
    fun `legacy parity catalog includes deterministic monk and gameplay skill coverage`() {
        val catalog = LegacyContentParityCatalog.default

        assertTrue(catalog.requiredNpcClicks.contains(NpcClickRouteKey(option = 1, npcId = 555)))
        assertTrue(catalog.requiredNpcClicks.contains(NpcClickRouteKey(option = 1, npcId = 557)))
        assertEquals(15, catalog.requiredSkillCoverage.size)
        assertTrue(catalog.requiredSkillCoverage.contains(Skill.MINING))
        assertTrue(catalog.requiredSkillCoverage.contains(Skill.THIEVING))
    }
}
