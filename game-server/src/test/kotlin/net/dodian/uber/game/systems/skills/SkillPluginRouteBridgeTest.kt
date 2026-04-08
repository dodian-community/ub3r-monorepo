package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.systems.plugin.PluginRegistry
import net.dodian.uber.game.systems.action.PolicyPreset
import net.dodian.uber.game.systems.skills.plugin.bindItemContentClick
import net.dodian.uber.game.systems.skills.plugin.bindObjectContentUseItem
import net.dodian.uber.game.systems.skills.plugin.skillPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SkillPluginRouteBridgeTest {
    @Test
    fun `dispatcher resolves via PluginRegistry`() {
        PluginRegistry.resetForTests()
        PluginRegistry.bootstrap()
        val binding = PluginRegistry.currentSkills().objectBinding(option = 1, objectId = 1276)
        assertNotNull(binding)
    }

    @Test
    fun `bind item content click emits item bindings`() {
        val plugin =
            skillPlugin(name = "BridgeItem", skill = Skill.HERBLORE) {
                bindItemContentClick(
                    preset = PolicyPreset.PRODUCTION,
                    option = 1,
                    content =
                        object : ItemContent {
                            override val itemIds: IntArray = intArrayOf(100, 101)
                        },
                )
            }

        assertEquals(1, plugin.itemBindings.size)
        val binding = plugin.itemBindings.first()
        assertEquals(PolicyPreset.PRODUCTION, binding.preset)
        assertEquals(1, binding.option)
        assertEquals(setOf(100, 101), binding.itemIds.toSet())
    }

    @Test
    fun `bind object content use item emits item on object bindings`() {
        val plugin =
            skillPlugin(name = "BridgeObject", skill = Skill.COOKING) {
                bindObjectContentUseItem(
                    preset = PolicyPreset.PRODUCTION,
                    content =
                        object : ObjectContent {
                            override val objectIds: IntArray = intArrayOf(200, 201)
                        },
                )
            }

        assertEquals(1, plugin.itemOnObjectBindings.size)
        val binding = plugin.itemOnObjectBindings.first()
        assertEquals(PolicyPreset.PRODUCTION, binding.preset)
        assertEquals(setOf(200, 201), binding.objectIds.toSet())
        assertEquals(setOf(-1), binding.itemIds.toSet())
        assertNotNull(binding.handler)
    }
}
