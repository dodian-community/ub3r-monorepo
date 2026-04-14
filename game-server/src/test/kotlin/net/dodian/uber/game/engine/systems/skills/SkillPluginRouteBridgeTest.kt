package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.item.ItemContent
import net.dodian.uber.game.objects.ObjectContent
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.api.plugin.PluginRegistry
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.api.plugin.skills.bindItemContentClick
import net.dodian.uber.game.api.plugin.skills.bindObjectContentMagic
import net.dodian.uber.game.api.plugin.skills.bindObjectContentUseItem
import net.dodian.uber.game.api.plugin.skills.skillPlugin
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

    @Test
    fun `bind object content magic emits magic on object bindings`() {
        val plugin =
            skillPlugin(name = "BridgeMagic", skill = Skill.MAGIC) {
                bindObjectContentMagic(
                    preset = PolicyPreset.PRODUCTION,
                    content =
                        object : ObjectContent {
                            override val objectIds: IntArray = intArrayOf(300, 301)
                        },
                    spellIds = intArrayOf(1179, 1182),
                )
            }

        assertEquals(1, plugin.magicOnObjectBindings.size)
        val binding = plugin.magicOnObjectBindings.first()
        assertEquals(PolicyPreset.PRODUCTION, binding.preset)
        assertEquals(setOf(300, 301), binding.objectIds.toSet())
        assertEquals(setOf(1179, 1182), binding.spellIds.toSet())
        assertNotNull(binding.handler)
    }
}
