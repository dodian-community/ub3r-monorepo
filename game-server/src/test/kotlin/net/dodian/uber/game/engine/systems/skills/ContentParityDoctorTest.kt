package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.npc.NO_CLICK_HANDLER
import net.dodian.uber.game.npc.NpcContentDefinition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.content.skills.runtime.parity.ContentParityCatalog
import net.dodian.uber.game.content.skills.runtime.parity.ContentParityDoctor
import net.dodian.uber.game.content.skills.runtime.parity.NpcClickRouteKey
import net.dodian.uber.game.api.plugin.skills.SkillPluginSnapshot
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentParityDoctorTest {
    @Test
    fun `doctor fails when required npc route is missing`() {
        val catalog =
            ContentParityCatalog(
                requiredNpcClicks = setOf(NpcClickRouteKey(option = 1, npcId = 555)),
                bannedNpcClicks = emptySet(),
                requiredObjectClicks = emptySet(),
                bannedObjectClicks = emptySet(),
                requiredItemOnItem = emptySet(),
                bannedItemOnItem = emptySet(),
                requiredSkillCoverage = setOf(Skill.MINING),
                requiredSkillRouteTypes = emptyMap(),
            )

        val findings =
            ContentParityDoctor.scan(
                catalog = catalog,
                skillSnapshot = SkillPluginSnapshot.empty(),
                npcLookup = { null },
                discoveredSkills = setOf(Skill.MINING),
            )

        assertTrue(findings.any { it.code == "parity-missing-npc-route" })
    }

    @Test
    fun `doctor fails when banned npc route exists`() {
        val catalog =
            ContentParityCatalog(
                requiredNpcClicks = emptySet(),
                bannedNpcClicks = setOf(NpcClickRouteKey(option = 1, npcId = 555)),
                requiredObjectClicks = emptySet(),
                bannedObjectClicks = emptySet(),
                requiredItemOnItem = emptySet(),
                bannedItemOnItem = emptySet(),
                requiredSkillCoverage = emptySet(),
                requiredSkillRouteTypes = emptyMap(),
            )

        val definition =
            NpcContentDefinition(
                name = "Monk",
                npcIds = intArrayOf(555),
                onFirstClick = { _, _ -> true },
                onSecondClick = NO_CLICK_HANDLER,
                onThirdClick = NO_CLICK_HANDLER,
                onFourthClick = NO_CLICK_HANDLER,
                onAttack = NO_CLICK_HANDLER,
            )

        val findings =
            ContentParityDoctor.scan(
                catalog = catalog,
                skillSnapshot = SkillPluginSnapshot.empty(),
                npcLookup = { npcId -> if (npcId == 555) definition else null },
                discoveredSkills = emptySet(),
            )

        assertTrue(findings.any { it.code == "parity-banned-npc-route" })
    }
}
