package net.dodian.uber.game.architecture

import net.dodian.uber.game.systems.plugin.ContentModuleIndex
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GeneratedContentIndexTest {
    @Test
    fun `generated command contents include expected module groups`() {
        val moduleNames = ContentModuleIndex.commandContents.map { it::class.java.name }.toSet()

        assertTrue(moduleNames.any { it.endsWith("content.commands.dev.DevDebugCommands") }, "Expected dev command module in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.commands.beta.BetaOnlyCommands") }, "Expected beta command module in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.commands.player.PlayerCommands") }, "Expected player command module in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.commands.admin.StaffCommands") }, "Expected admin command module in generated index")
    }

    @Test
    fun `generated content bootstraps include core registries and are unique`() {
        val bootstraps = ContentModuleIndex.contentBootstraps
        val ids = bootstraps.map { it.id }

        assertTrue(ids.contains("commands.registry"), "commands registry bootstrap missing")
        assertTrue(ids.contains("items.registry"), "items registry bootstrap missing")
        assertTrue(ids.contains("npcs.registry"), "npcs registry bootstrap missing")
        assertTrue(ids.contains("objects.registry"), "objects registry bootstrap missing")
        assertTrue(ids.contains("skills.registry"), "skills registry bootstrap missing")
        assertEquals(ids.size, ids.toSet().size, "duplicate content bootstrap ids found")

        bootstraps.forEach { it.bootstrap() }
    }

    @Test
    fun `generated skill plugins include all gameplay skills`() {
        val moduleNames = ContentModuleIndex.skillPlugins.map { it::class.java.name }.toSet()
        val skills = ContentModuleIndex.skillPlugins.map { it.definition.skill }.toSet()

        val expectedSkills = setOf(
            Skill.MINING,
            Skill.WOODCUTTING,
            Skill.FISHING,
            Skill.AGILITY,
            Skill.COOKING,
            Skill.CRAFTING,
            Skill.FARMING,
            Skill.FIREMAKING,
            Skill.FLETCHING,
            Skill.HERBLORE,
            Skill.PRAYER,
            Skill.RUNECRAFTING,
            Skill.SLAYER,
            Skill.SMITHING,
            Skill.THIEVING,
        )

        assertEquals(expectedSkills, skills, "Expected full gameplay skill plugin coverage")

        assertTrue(moduleNames.any { it.endsWith("content.skills.agility.AgilitySkillPlugin") }, "Expected agility skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.cooking.CookingSkillPlugin") }, "Expected cooking skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.crafting.CraftingSkillPlugin") }, "Expected crafting skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.farming.FarmingSkillPlugin") }, "Expected farming skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.firemaking.FiremakingSkillPlugin") }, "Expected firemaking skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.fishing.FishingSkillPlugin") }, "Expected fishing skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.fletching.FletchingSkillPlugin") }, "Expected fletching skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.herblore.HerbloreSkillPlugin") }, "Expected herblore skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.mining.MiningSkillPlugin") }, "Expected mining skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.prayer.PrayerSkillPlugin") }, "Expected prayer skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.runecrafting.RunecraftingSkillPlugin") }, "Expected runecrafting skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.slayer.SlayerSkillPlugin") }, "Expected slayer skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.smithing.SmithingSkillPlugin") }, "Expected smithing skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.thieving.ThievingSkillPlugin") }, "Expected thieving skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.skills.woodcutting.WoodcuttingSkillPlugin") }, "Expected woodcutting skill plugin in generated index")
    }
}
