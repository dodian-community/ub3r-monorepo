package net.dodian.uber.skills.firemaking

import net.dodian.uber.game.api.plugin.skills.SkillItemOnItemInteraction
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val TINDERBOX = 590
private const val NORMAL_LOG = 1511

class FiremakingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(FiremakingModule.descriptor.id, Skill.FIREMAKING)
        assertEquals(FiremakingModule.descriptor.id, FiremakingModule.contentManifest.id)
    }

    @Test
    fun `data loads all 6 log tiers`() {
        assertEquals(6, FiremakingModule.logs.size)
        assertTrue(FiremakingModule.logs.any { it.itemId == NORMAL_LOG && it.name == "logs" })
    }

    @Test
    fun `lighting a log consumes it and awards only baseline experience`() {
        val binding = FiremakingModule.definition.itemOnItemBindings.single {
            it.leftItemId == TINDERBOX && it.rightItemId == NORMAL_LOG
        }
        val player = FakeSkillPlayer(mapOf(TINDERBOX to 1, NORMAL_LOG to 1)).apply { setLevel(Skill.FIREMAKING, 1) }

        val handled = binding.handler(SkillItemOnItemInteraction(player, TINDERBOX, NORMAL_LOG))

        assertTrue(handled)
        assertEquals(0, player.amount(NORMAL_LOG))
        assertEquals(160, player.skills.experience(Skill.FIREMAKING))
        assertTrue(player.messages.isEmpty())
        assertTrue(player.spawnedObjects.isEmpty())
    }

    @Test
    fun `insufficient level refuses to light the log`() {
        val magicLog = FiremakingModule.logs.single { it.name == "magic logs" }
        val binding = FiremakingModule.definition.itemOnItemBindings.single {
            it.leftItemId == TINDERBOX && it.rightItemId == magicLog.itemId
        }
        val player = FakeSkillPlayer(mapOf(TINDERBOX to 1, magicLog.itemId to 1)).apply { setLevel(Skill.FIREMAKING, 1) }

        val handled = binding.handler(SkillItemOnItemInteraction(player, TINDERBOX, magicLog.itemId))

        assertTrue(handled)
        assertEquals(1, player.amount(magicLog.itemId))
        assertEquals(
            listOf("You need a firemaking level of ${magicLog.requiredLevel} to burn ${magicLog.name}."),
            player.messages,
        )
        assertTrue(player.spawnedObjects.isEmpty())
    }
}
