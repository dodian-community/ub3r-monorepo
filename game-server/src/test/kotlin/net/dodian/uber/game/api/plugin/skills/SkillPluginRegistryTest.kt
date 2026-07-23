package net.dodian.uber.game.api.plugin.skills

import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class SkillPluginRegistryTest {
    @Test
    fun `context route binding validates and resolves`() {
        val engine = SkillPluginRegistryEngine()
        val plugin = plugin("context-route") {
            skillPlugin("Context route", Skill.MINING) {
                objectClick(PolicyPreset.GATHERING, 1, 7485) { interaction: SkillObjectInteraction ->
                    interaction.option == 1 && interaction.objectId == 7485
                }
            }
        }

        assertDoesNotThrow { engine.validate(listOf(plugin)) }
    }

    @Test
    fun `duplicate typed routes fail during validation`() {
        val first = plugin("first") {
            skillPlugin("First", Skill.MINING) {
                objectClick(PolicyPreset.GATHERING, 1, 7485) { _: SkillObjectInteraction -> true }
            }
        }
        val duplicate = plugin("duplicate") {
            skillPlugin("Duplicate", Skill.WOODCUTTING) {
                objectClick(PolicyPreset.GATHERING, 1, 7485) { _: SkillObjectInteraction -> true }
            }
        }

        assertThrows<IllegalArgumentException> {
            SkillPluginRegistryEngine().validate(listOf(first, duplicate))
        }
    }

    @Test
    fun `invalid typed route input fails at declaration`() {
        assertThrows<IllegalArgumentException> {
            skillPlugin("Invalid", Skill.MINING) {
                objectClick(PolicyPreset.GATHERING, option = 6, 7485) { _: SkillObjectInteraction -> true }
            }
        }
    }

    @Test
    fun `manifest derives exactly the typed route inventory`() {
        val definition = skillPlugin("Manifested", Skill.MINING) {
            objectClick(PolicyPreset.GATHERING, 1, 7485, 7486) { _: SkillObjectInteraction -> true }
            itemOnItem(PolicyPreset.PRODUCTION, 100, 200) { _: SkillItemOnItemInteraction -> true }
            itemGrid(PolicyPreset.PRODUCTION, 2400) { _: SkillItemGridInteraction -> true }
        }

        val manifest = definition.manifest(
            id = "skill.manifested",
            owner = "test",
            maturity = ContentMaturity.STABLE,
        )

        org.junit.jupiter.api.Assertions.assertEquals(
            setOf("object:1:7485", "object:1:7486", "item-on-item:100:200", "item-grid:2400"),
            manifest.declaredRouteKeys,
        )
    }

    @Test
    fun `duplicate item grid ownership fails during validation`() {
        val first = plugin("first-grid") {
            skillPlugin("First grid", Skill.MINING) {
                itemGrid(PolicyPreset.PRODUCTION, 2400) { _: SkillItemGridInteraction -> true }
            }
        }
        val duplicate = plugin("duplicate-grid") {
            skillPlugin("Duplicate grid", Skill.WOODCUTTING) {
                itemGrid(PolicyPreset.PRODUCTION, 2400) { _: SkillItemGridInteraction -> true }
            }
        }

        assertThrows<IllegalArgumentException> { SkillPluginRegistryEngine().validate(listOf(first, duplicate)) }
    }

    @Test
    fun `support routes resolve without declaring a gameplay skill`() {
        val support = object : SkillSupportModule {
            override val definition = skillSupportModule("skill-guide") {
                itemClick(PolicyPreset.PRODUCTION, 1, 1856) { it.itemId == 1856 }
                button(PolicyPreset.PRODUCTION, requiredInterfaceId = 8714, rawButtonIds = intArrayOf(8654)) { it.rawButtonId == 8654 }
            }
        }

        val registry = SkillSupportRegistryEngine()
        registry.bootstrap(listOf(support))
        org.junit.jupiter.api.Assertions.assertNotNull(registry.current().itemBinding(1, 1856))
        org.junit.jupiter.api.Assertions.assertNotNull(registry.current().buttonBinding(8654, 0, 8714))
    }

    @Test
    fun `duplicate support routes fail during bootstrap`() {
        fun support(name: String) = object : SkillSupportModule {
            override val definition = skillSupportModule(name) {
                itemClick(PolicyPreset.PRODUCTION, 1, 1856) { true }
            }
        }
        assertThrows<IllegalArgumentException> { SkillSupportRegistryEngine().bootstrap(listOf(support("one"), support("two"))) }
    }

    private fun plugin(name: String, definition: () -> SkillPluginDefinition): SkillPlugin =
        object : SkillPlugin {
            override val definition: SkillPluginDefinition = definition()
            override val pluginMetadata = net.dodian.uber.game.api.plugin.PluginModuleMetadata(
                name = name,
                description = name,
                version = "1.0.0",
                owner = "test",
            )
        }
}
