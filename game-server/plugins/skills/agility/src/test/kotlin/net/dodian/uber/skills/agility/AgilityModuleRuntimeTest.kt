package net.dodian.uber.skills.agility

import net.dodian.uber.game.api.plugin.skills.SkillEquipmentSlot
import net.dodian.uber.game.api.plugin.skills.SkillNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillNpcRef
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AgilityModuleRuntimeTest {
    @Test
    fun `agility constants retain world-entry identifiers`() {
        assertEquals(60, AgilityConstants.WEREWOLF_COURSE_LEVEL)
        assertEquals(536, AgilityConstants.KBD_ENTRANCE_BONE_ID)
        assertEquals(537, AgilityConstants.KBD_ENTRANCE_NOTED_BONE_ID)
        assertEquals(5, AgilityConstants.KBD_ENTRANCE_BONE_AMOUNT)
    }

    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(AgilityModule.descriptor.id, Skill.AGILITY)
        assertEquals(AgilityModule.descriptor.id, AgilityModule.contentManifest.id)
    }

    @Test
    fun `per-course toml obstacles are loaded correctly`() {
        assertTrue(AgilityModule.obstacles.isNotEmpty())

        val courses = AgilityModule.obstacles.map { it.course }.toSet()
        assertTrue(courses.contains("gnome"))
        assertTrue(courses.contains("barbarian"))
        assertTrue(courses.contains("wilderness"))
        assertTrue(courses.contains("werewolf"))
        assertTrue(courses.contains("shortcuts"))
    }

    @Test
    fun `gnome course log balance uses walk movement with balance animation`() {
        val gnomeLog = AgilityModule.obstacles.firstOrNull { it.course == "gnome" && it.name == "log_balance" }
        assertNotNull(gnomeLog)
        assertEquals(AgilityMovementType.WALK, gnomeLog!!.movementType)
        assertEquals(762, gnomeLog.animationId)
        assertEquals(23145, gnomeLog.objectId)
    }

    @Test
    fun `gnome course pipe defines lap completion and agility tickets`() {
        val gnomePipe = AgilityModule.obstacles.firstOrNull { it.course == "gnome" && it.name == "pipe_one" }
        assertNotNull(gnomePipe)
        assertTrue(gnomePipe!!.isLapCompletion)
        assertEquals(1, gnomePipe.ticketBase)
        assertEquals(11, gnomePipe.ticketScaleDivisor)
        assertEquals(1050, gnomePipe.bonusExperience)
        assertEquals("You finished a gnome lap!", gnomePipe.lapMessage)
    }

    @Test
    fun `orange bar blocks crossing without the orange key and allows it with the key`() {
        val binding = AgilityModule.definition.objectBindings.single { it.option == 1 && 23567 in it.objectIds }
        val target = SkillObjectRef(23567, SkillPosition(0, 0, 0))
        val player = FakeSkillPlayer().apply { setLevel(Skill.AGILITY, 99) }

        binding.handler(SkillObjectInteraction(player, 1, target))
        assertTrue(player.messages.any { it.contains("orange key") })
        assertTrue(player.traversals.isEmpty())

        player.messages.clear()
        player.inventory.add(1544, 1)
        binding.handler(SkillObjectInteraction(player, 1, target))
        assertFalse(player.messages.any { it.contains("orange key") })
        assertTrue(player.traversals.isNotEmpty())
    }

    @Test
    fun `yellow ledge blocks crossing without the yellow key`() {
        val binding = AgilityModule.definition.objectBindings.single { it.option == 1 && 23548 in it.objectIds }
        val target = SkillObjectRef(23548, SkillPosition(0, 0, 0))
        val player = FakeSkillPlayer().apply { setLevel(Skill.AGILITY, 99) }

        binding.handler(SkillObjectInteraction(player, 1, target))

        assertTrue(player.messages.any { it.contains("yellow key") })
        assertTrue(player.traversals.isEmpty())
    }

    @Test
    fun `werewolf obstacles grant a 10 percent xp bonus while wearing the ring`() {
        val binding = AgilityModule.definition.objectBindings.single { it.option == 1 && 11638 in it.objectIds }
        val target = SkillObjectRef(11638, SkillPosition(0, 0, 0))

        val withoutRing = FakeSkillPlayer().apply { setLevel(Skill.AGILITY, 99) }
        binding.handler(SkillObjectInteraction(withoutRing, 1, target))
        withoutRing.advanceTicks(10)

        val withRing = FakeSkillPlayer().apply {
            setLevel(Skill.AGILITY, 99)
            equip(SkillEquipmentSlot.RING, 4202)
        }
        binding.handler(SkillObjectInteraction(withRing, 1, target))
        withRing.advanceTicks(10)

        val baselineXp = withoutRing.skills.experience(Skill.AGILITY)
        val bonusXp = withRing.skills.experience(Skill.AGILITY)
        assertEquals((baselineXp * 1.1).toInt(), bonusXp)
    }

    @Test
    fun `hand stick grants xp and consumes the stick when carried`() {
        val binding = AgilityModule.definition.npcBindings.single { it.option == 1 && 5927 in it.npcIds }
        val npc = SkillNpcRef(5927, 0, SkillPosition(0, 0, 0))
        val player = FakeSkillPlayer().apply { inventory.add(4179, 1) }

        val handled = binding.handler(SkillNpcInteraction(player, 1, npc))

        assertTrue(handled)
        assertEquals(4000, player.skills.experience(Skill.AGILITY))
        assertFalse(player.inventory.contains(4179))
    }

    @Test
    fun `hand stick without the item shows the missing-item message`() {
        val binding = AgilityModule.definition.npcBindings.single { it.option == 1 && 5927 in it.npcIds }
        val npc = SkillNpcRef(5927, 0, SkillPosition(0, 0, 0))
        val player = FakeSkillPlayer()

        binding.handler(SkillNpcInteraction(player, 1, npc))

        assertTrue(player.messages.any { it.contains("do not have a stick") })
    }

    @Test
    fun `save progress encoding round-trips through a fresh player`() {
        val player = FakeSkillPlayer()
        player.moduleState.put(AgilityModule.descriptor.id, "stage_barbarian", "4")

        val encoded = AgilityModule.encodeProgressForSave(player)
        assertEquals(14, encoded) // course index 1 (barbarian) * 10 + stage 4

        val restored = FakeSkillPlayer()
        AgilityModule.restoreProgressFromSave(restored, encoded)
        assertEquals("4", restored.moduleState.get(AgilityModule.descriptor.id, "stage_barbarian"))
    }
}
