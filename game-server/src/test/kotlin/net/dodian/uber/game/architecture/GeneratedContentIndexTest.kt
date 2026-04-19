package net.dodian.uber.game.architecture

import net.dodian.uber.game.api.plugin.ContentModuleIndex
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GeneratedContentIndexTest {
    @Test
    fun `generated command contents include expected module groups`() {
        val moduleNames = ContentModuleIndex.commandContents.map { it::class.java.name }.toSet()

        assertTrue(moduleNames.any { it.endsWith("game.command.dev.DevDebugCommands") }, "Expected dev command module in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.command.player.TravelCommands") }, "Expected canonical travel command module in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.command.admin.BossCommands") }, "Expected canonical boss command module in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.command.dev.ShopDevCommand") }, "Expected canonical shop dev command module in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.command.beta.BetaOnlyCommands") }, "Expected beta command module in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.command.player.PlayerCommands") }, "Expected player command module in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.command.admin.StaffCommands") }, "Expected admin command module in generated index")
    }

    @Test
    fun `generated interface button contents include canonical ui modules`() {
        val moduleNames = ContentModuleIndex.interfaceButtons.map { it::class.java.name }.toSet()

        assertTrue(moduleNames.any { it.endsWith("game.ui.UiInterface") }, "Expected canonical ui interface module in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.ui.SettingsInterface") }, "Expected canonical settings interface module in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.ui.QuestInterface") }, "Expected canonical quest interface module in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.ui.AppearanceInterface") }, "Expected canonical appearance interface module in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.ui.EmoteInterface") }, "Expected canonical emote interface module in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.ui.MagicInterface") }, "Expected canonical magic interface module in generated index")
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

        assertTrue(moduleNames.any { it.endsWith("game.skill.agility.AgilitySkillPlugin") }, "Expected agility skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.cooking.CookingSkillPlugin") }, "Expected cooking skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.crafting.CraftingSkillPlugin") }, "Expected crafting skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.farming.FarmingSkillPlugin") }, "Expected farming skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.firemaking.FiremakingSkillPlugin") }, "Expected firemaking skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.fishing.FishingSkillPlugin") }, "Expected fishing skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.fletching.FletchingSkillPlugin") }, "Expected fletching skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.herblore.HerbloreSkillPlugin") }, "Expected herblore skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.mining.MiningSkillPlugin") }, "Expected mining skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.prayer.PrayerSkillPlugin") }, "Expected prayer skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.runecrafting.RunecraftingSkillPlugin") }, "Expected runecrafting skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.slayer.SlayerSkillPlugin") }, "Expected slayer skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.smithing.SmithingSkillPlugin") }, "Expected smithing skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.thieving.ThievingSkillPlugin") }, "Expected thieving skill plugin in generated index")
        assertTrue(moduleNames.any { it.endsWith("game.skill.woodcutting.WoodcuttingSkillPlugin") }, "Expected woodcutting skill plugin in generated index")
    }

    @Test
    fun `plugin catalog metadata and ordering are deterministic`() {
        val catalog = ContentModuleIndex.pluginCatalog
        assertFalse(catalog.isEmpty(), "expected plugin catalog entries")
        assertEquals(catalog.map { it.moduleClass }.sorted(), catalog.map { it.moduleClass }, "plugin catalog must be sorted by class")
        catalog.forEach { entry ->
            assertTrue(entry.metadata.name.isNotBlank(), "plugin metadata name must be set for ${entry.moduleClass}")
            assertTrue(entry.metadata.description.isNotBlank(), "plugin metadata description must be set for ${entry.moduleClass}")
            assertTrue(entry.metadata.version.isNotBlank(), "plugin metadata version must be set for ${entry.moduleClass}")
            assertTrue(entry.metadata.owner.isNotBlank(), "plugin metadata owner must be set for ${entry.moduleClass}")
        }
    }

    @Test
    fun `plugin scanning roots use canonical domains only`() {
        val canonical = ContentModuleIndex.canonicalScanPackages.toSet()

        val expectedCanonical = setOf(
            "net.dodian.uber.game.api.plugin",
            "net.dodian.uber.game.skill",
            "net.dodian.uber.game.npc",
            "net.dodian.uber.game.item",
            "net.dodian.uber.game.objects",
            "net.dodian.uber.game.combat",
            "net.dodian.uber.game.social",
            "net.dodian.uber.game.ui",
            "net.dodian.uber.game.command",
            "net.dodian.uber.game.activity",
            "net.dodian.uber.game.shop",
            "net.dodian.uber.game.world",
            "net.dodian.uber.game.player",
            "net.dodian.uber.game.engine.event.bootstrap",
        )

        assertTrue(canonical.containsAll(expectedCanonical), "canonical scan roots missing expected domains")
        assertFalse(canonical.contains("net.dodian.uber.game.content"), "broad legacy content root must not be canonical")
        assertTrue(
            canonical.none { it.startsWith("net.dodian.uber.game.content.") },
            "canonical roots must not include legacy content package roots",
        )
    }
}
