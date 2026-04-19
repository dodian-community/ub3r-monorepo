package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.api.plugin.PluginRegistry
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkillPluginRegistryTest {
    @Test
    fun `plugin registry alias resolves skill bindings`() {
        PluginRegistry.resetForTests()
        PluginRegistry.bootstrap()
        val snapshot = PluginRegistry.currentSkills()

        assertNotNull(snapshot.objectBinding(option = 1, objectId = 7451))
    }

    @Test
    fun `legacy SkillPluginRegistry forwards to PluginRegistry`() {
        @Suppress("DEPRECATION")
        run {
            net.dodian.uber.game.api.plugin.skills.SkillPluginRegistry.resetForTests()
            net.dodian.uber.game.api.plugin.skills.SkillPluginRegistry.bootstrap()
            assertNotNull(net.dodian.uber.game.api.plugin.skills.SkillPluginRegistry.current().itemBinding(option = 1, itemId = 4155))
        }
    }

    @Test
    fun `registry resolves migrated skill bindings`() {
        PluginRegistry.resetForTests()
        PluginRegistry.bootstrap()
        val snapshot = PluginRegistry.currentSkills()

        assertNotNull(snapshot.objectBinding(option = 1, objectId = 7451), "Expected mining rock object binding")
        assertNotNull(snapshot.objectBinding(option = 1, objectId = 1276), "Expected woodcutting tree object binding")
        assertNotNull(snapshot.npcBinding(option = 1, npcId = 1511), "Expected fishing npc option binding")
        assertNotNull(snapshot.itemBinding(option = 1, itemId = 4155), "Expected slayer gem item option binding")
        assertNotNull(snapshot.itemOnObjectBinding(objectId = 2728, itemId = 317), "Expected cooking range item-on-object binding")
        assertNotNull(snapshot.magicOnObjectBinding(objectId = 2151, spellId = 1179), "Expected smithing orb charging magic-on-object binding")
        assertNotNull(snapshot.magicOnObjectBinding(objectId = 2151, spellId = 9999), "Expected smithing wildcard magic-on-object ownership")
        assertNotNull(snapshot.buttonBinding(rawButtonId = 3987, opIndex = -1, activeInterfaceId = 2400), "Expected smithing smelting button binding")
    }

    @Test
    fun `registry rejects duplicate ownership for same binding key`() {
        PluginRegistry.resetForTests()

        PluginRegistry.registerSkill(
            object : SkillPlugin {
                override val definition =
                    skillPlugin("TestOne", Skill.MINING) {
                        objectClick(preset = PolicyPreset.GATHERING, option = 1, 999_001) { _, _, _, _ -> true }
                    }
            },
        )

        PluginRegistry.registerSkill(
            object : SkillPlugin {
                override val definition =
                    skillPlugin("TestTwo", Skill.WOODCUTTING) {
                        objectClick(preset = PolicyPreset.GATHERING, option = 1, 999_001) { _, _, _, _ -> true }
                    }
            },
        )

        assertThrows(IllegalArgumentException::class.java) {
            PluginRegistry.bootstrap()
        }

        PluginRegistry.resetForTests()
    }

    @Test
    fun `registry supports item on object wildcard binding`() {
        PluginRegistry.resetForTests()
        PluginRegistry.registerSkill(
            object : SkillPlugin {
                override val definition =
                    skillPlugin("Wildcard", Skill.COOKING) {
                        itemOnObject(preset = PolicyPreset.PRODUCTION, 888_001, itemIds = intArrayOf(-1)) { _, _, _, _, _, _, _ -> true }
                    }
            },
        )
        PluginRegistry.bootstrap()
        val snapshot = PluginRegistry.currentSkills()

        assertNotNull(snapshot.itemOnObjectBinding(objectId = 888_001, itemId = 111))

        PluginRegistry.resetForTests()
    }

    @Test
    fun `registry supports magic on object wildcard binding`() {
        PluginRegistry.resetForTests()
        PluginRegistry.registerSkill(
            object : SkillPlugin {
                override val definition =
                    skillPlugin("MagicWildcard", Skill.MAGIC) {
                        magicOnObject(PolicyPreset.PRODUCTION, 777_001, spellIds = intArrayOf(-1)) { _, _, _, _, _ -> true }
                    }
            },
        )
        PluginRegistry.bootstrap()
        val snapshot = PluginRegistry.currentSkills()

        assertNotNull(snapshot.magicOnObjectBinding(objectId = 777_001, spellId = 1234))

        PluginRegistry.resetForTests()
    }

    @Test
    fun `registry resolves interface specific skill buttons before wildcard routes`() {
        PluginRegistry.resetForTests()
        PluginRegistry.registerSkill(
            object : SkillPlugin {
                override val definition =
                    skillPlugin("Buttons", Skill.CRAFTING) {
                        button(preset = PolicyPreset.PRODUCTION, requiredInterfaceId = -1, rawButtonIds = intArrayOf(555_001)) { _, _, _ -> true }
                        button(preset = PolicyPreset.PRODUCTION, requiredInterfaceId = 2400, rawButtonIds = intArrayOf(555_001)) { _, _, _ -> true }
                    }
            },
        )
        PluginRegistry.bootstrap()
        val snapshot = PluginRegistry.currentSkills()

        val interfaceScoped = requireNotNull(snapshot.buttonBinding(rawButtonId = 555_001, opIndex = -1, activeInterfaceId = 2400))
        val wildcard = requireNotNull(snapshot.buttonBinding(rawButtonId = 555_001, opIndex = -1, activeInterfaceId = 1234))

        assertTrue(interfaceScoped.requiredInterfaceId == 2400)
        assertTrue(wildcard.requiredInterfaceId == -1)

        PluginRegistry.resetForTests()
    }

    @Test
    fun `plugin registry freezes after bootstrap lifecycle`() {
        PluginRegistry.resetForTests()
        PluginRegistry.discover()
        PluginRegistry.validate()
        PluginRegistry.bootstrap()

        val error = assertThrows(IllegalStateException::class.java) {
            PluginRegistry.registerSkill(
                object : SkillPlugin {
                    override val definition =
                        skillPlugin("LateRegistration", Skill.COOKING) {
                            itemClick(preset = PolicyPreset.PRODUCTION, option = 1, 999_112) { _, _, _, _ -> true }
                        }
                },
            )
        }
        assertTrue(error.message?.contains("frozen") == true)

        PluginRegistry.resetForTests()
    }
}
