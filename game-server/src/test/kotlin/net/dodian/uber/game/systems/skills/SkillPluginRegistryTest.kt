package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.systems.policy.PolicyPreset
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SkillPluginRegistryTest {
    @Test
    fun `registry resolves migrated skill bindings`() {
        SkillPluginRegistry.resetForTests()
        SkillPluginRegistry.bootstrap()
        val snapshot = SkillPluginRegistry.current()

        assertNotNull(snapshot.objectBinding(option = 1, objectId = 7451), "Expected mining rock object binding")
        assertNotNull(snapshot.objectBinding(option = 1, objectId = 1276), "Expected woodcutting tree object binding")
        assertNotNull(snapshot.npcBinding(option = 1, npcId = 1511), "Expected fishing npc option binding")
        assertNotNull(snapshot.itemBinding(option = 1, itemId = 4155), "Expected slayer gem item option binding")
        assertNotNull(snapshot.itemOnObjectBinding(objectId = 2728, itemId = 317), "Expected cooking range item-on-object binding")
    }

    @Test
    fun `registry rejects duplicate ownership for same binding key`() {
        SkillPluginRegistry.resetForTests()

        SkillPluginRegistry.register(
            object : SkillPlugin {
                override val definition =
                    skillPlugin("TestOne", Skill.MINING) {
                        objectClick(preset = PolicyPreset.GATHERING, option = 1, 999_001) { _, _, _, _ -> true }
                    }
            },
        )

        SkillPluginRegistry.register(
            object : SkillPlugin {
                override val definition =
                    skillPlugin("TestTwo", Skill.WOODCUTTING) {
                        objectClick(preset = PolicyPreset.GATHERING, option = 1, 999_001) { _, _, _, _ -> true }
                    }
            },
        )

        assertThrows(IllegalArgumentException::class.java) {
            SkillPluginRegistry.bootstrap()
        }

        SkillPluginRegistry.resetForTests()
    }

    @Test
    fun `registry supports item on object wildcard binding`() {
        SkillPluginRegistry.resetForTests()
        SkillPluginRegistry.register(
            object : SkillPlugin {
                override val definition =
                    skillPlugin("Wildcard", Skill.COOKING) {
                        itemOnObject(preset = PolicyPreset.PRODUCTION, 888_001, itemIds = intArrayOf(-1)) { _, _, _, _, _, _, _ -> true }
                    }
            },
        )
        SkillPluginRegistry.bootstrap()
        val snapshot = SkillPluginRegistry.current()

        assertNotNull(snapshot.itemOnObjectBinding(objectId = 888_001, itemId = 111))

        SkillPluginRegistry.resetForTests()
    }
}
