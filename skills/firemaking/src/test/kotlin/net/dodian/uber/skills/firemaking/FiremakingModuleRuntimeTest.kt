package net.dodian.uber.skills.firemaking

import net.dodian.uber.game.api.plugin.skills.SkillItemOnItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemOnObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val TINDERBOX = 590
private const val FIRE_OBJECT_ID = 5249
private const val ASHES_ITEM_ID = 592
private const val NORMAL_LOG = 1511

private fun fireRef() = SkillObjectRef(FIRE_OBJECT_ID, SkillPosition(3200, 3200, 0))

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
    fun `lighting a log consumes it, animates and spawns a temporary fire`() {
        val log = FiremakingModule.logs.single { it.itemId == NORMAL_LOG }
        val binding = FiremakingModule.definition.itemOnItemBindings.single {
            it.leftItemId == TINDERBOX && it.rightItemId == NORMAL_LOG
        }
        val player = FakeSkillPlayer(mapOf(TINDERBOX to 1, NORMAL_LOG to 1)).apply { setLevel(Skill.FIREMAKING, 1) }

        val handled = binding.handler(SkillItemOnItemInteraction(player, TINDERBOX, NORMAL_LOG))

        assertTrue(handled)
        assertEquals(0, player.amount(NORMAL_LOG))
        assertEquals(listOf("You light the logs."), player.messages)
        assertEquals(listOf(FIRE_OBJECT_ID to log.durationTicks), player.spawnedObjects)
    }

    @Test
    fun `fire burns out and drops ashes after its duration`() {
        val log = FiremakingModule.logs.single { it.itemId == NORMAL_LOG }
        val binding = FiremakingModule.definition.itemOnItemBindings.single {
            it.leftItemId == TINDERBOX && it.rightItemId == NORMAL_LOG
        }
        val player = FakeSkillPlayer(mapOf(TINDERBOX to 1, NORMAL_LOG to 1)).apply { setLevel(Skill.FIREMAKING, 1) }

        binding.handler(SkillItemOnItemInteraction(player, TINDERBOX, NORMAL_LOG))
        assertTrue(player.groundItems.isEmpty())

        player.advanceTicks(log.durationTicks)

        assertEquals(listOf(ASHES_ITEM_ID to 1), player.groundItems)
        assertEquals("The fire has burnt out.", player.messages.last())
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

    @Test
    fun `adding a log to an existing fire grants xp without spawning another fire`() {
        val binding = FiremakingModule.definition.itemOnObjectBindings.single { FIRE_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(NORMAL_LOG to 1)).apply { setLevel(Skill.FIREMAKING, 1) }

        val handled = binding.handler(SkillItemOnObjectInteraction(player, fireRef(), NORMAL_LOG, 0, -1))

        assertTrue(handled)
        assertEquals(0, player.amount(NORMAL_LOG))
        assertEquals(160, player.skills.experience(Skill.FIREMAKING))
        assertEquals(listOf("You add the logs to the fire."), player.messages)
        assertTrue(player.spawnedObjects.isEmpty())
    }

    @Test
    fun `adding a log to a fire below the required level refuses`() {
        val magicLog = FiremakingModule.logs.single { it.name == "magic logs" }
        val binding = FiremakingModule.definition.itemOnObjectBindings.single { FIRE_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(magicLog.itemId to 1)).apply { setLevel(Skill.FIREMAKING, 1) }

        binding.handler(SkillItemOnObjectInteraction(player, fireRef(), magicLog.itemId, 0, -1))

        assertEquals(1, player.amount(magicLog.itemId))
        assertEquals(0, player.skills.experience(Skill.FIREMAKING))
        assertFalse(player.messages.isEmpty())
    }
}
