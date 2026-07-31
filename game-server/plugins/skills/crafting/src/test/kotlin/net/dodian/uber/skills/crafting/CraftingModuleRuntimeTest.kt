package net.dodian.uber.skills.crafting

import net.dodian.uber.game.api.plugin.skills.SkillButtonInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemOnItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillNpcRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CraftingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(CraftingModule.descriptor.id, Skill.CRAFTING)
        assertEquals(CraftingModule.descriptor.id, CraftingModule.contentManifest.id)
    }

    @Test
    fun `toml recipes are loaded`() {
        assertTrue(CraftingModule.gems.isNotEmpty())
        assertTrue(CraftingModule.hides.isNotEmpty())
    }

    @Test
    fun `existing spinning wheels use the Spin option`() {
        setOf(14889, 25824, 4309).forEach { objectId ->
            assertTrue(CraftingModule.definition.objectBindings.any { it.option == 2 && objectId in it.objectIds })
        }
        assertTrue(CraftingModule.definition.objectBindings.none { it.option == 1 && it.objectIds.any { id -> id in setOf(14889, 25824, 4309) } })
    }

    @Test
    fun `real-world Seers Village spinning wheel (4309) is registered`() {
        // Previously orphaned - the plugin only registered 14889/25824, which have no confirmed
        // real-world placement, while the actual live-world wheel at Seers Village
        // (definitions/objects/spawns/seers_village/spinning_wheel_flax.toml) was unregistered
        // anywhere, legacy or plugin.
        assertTrue(CraftingModule.definition.objectBindings.any { it.option == 2 && 4309 in it.objectIds })
    }

    // --- Tanning (npcClick + button) -----------------------------------------------------------

    private fun tannerNpc() = SkillNpcRef(5809, 0, SkillPosition(2711, 3478))

    @Test
    fun `talking to the tanner opens the tanning interface`() {
        val binding = CraftingModule.definition.npcBindings.single { 5809 in it.npcIds }
        val player = FakeSkillPlayer()

        assertTrue(binding.handler(SkillNpcInteraction(player, 1, tannerNpc())))
        assertEquals(listOf(14670), player.openedInterfaces)
        assertEquals("Green", player.strings[14779])
    }

    @Test
    fun `tanning consumes hide and coins clamped by what the player can afford`() {
        val binding = CraftingModule.definition.buttonBindings.single { 57227 in it.rawButtonIds }
        // hideType 2: 1753 -> 1745, 1000gp each. Amount button = 1 (57227), but only enough
        // coins for 1 despite holding 2 hides.
        val player = FakeSkillPlayer(mapOf(1753 to 2, 995 to 1000))

        assertTrue(binding.handler(SkillButtonInteraction(player, 57227, -1, 14670)))
        assertEquals(1, player.amount(1745))
        assertEquals(1, player.amount(1753))
        assertEquals(0, player.amount(995))
    }

    @Test
    fun `tanning without enough coins for one refuses with the legacy message`() {
        val binding = CraftingModule.definition.buttonBindings.single { 57227 in it.rawButtonIds }
        val player = FakeSkillPlayer(mapOf(1753 to 1, 995 to 10))

        binding.handler(SkillButtonInteraction(player, 57227, -1, 14670))
        assertEquals("You need atleast 1000 coins to do this!", player.messages.single())
        assertEquals(1, player.amount(1753))
    }

    @Test
    fun `tanning hideType 1 has no definition and silently no-ops`() {
        // Preserved legacy gap: TanningDefinitions never had an entry for hideType 1.
        val binding = CraftingModule.definition.buttonBindings.single { 57229 in it.rawButtonIds }
        val player = FakeSkillPlayer(mapOf(995 to 100_000))

        assertTrue(binding.handler(SkillButtonInteraction(player, 57229, -1, 14670)))
        assertTrue(player.messages.isEmpty())
    }

    // --- Glass-blowing (itemOnItem + button) ---------------------------------------------------

    @Test
    fun `molten glass and bucket opens the glass picker`() {
        val binding = CraftingModule.definition.itemOnItemBindings.single {
            (it.leftItemId == 1775 && it.rightItemId == 1785) || (it.leftItemId == 1785 && it.rightItemId == 1775)
        }
        val player = FakeSkillPlayer()

        assertTrue(binding.handler(SkillItemOnItemInteraction(player, 1775, 1785)))
        assertEquals(listOf(11462), player.openedInterfaces)
    }

    @Test
    fun `glass button crafts vials repeatedly from molten glass`() {
        val binding = CraftingModule.definition.buttonBindings.single { 44208 in it.rawButtonIds }
        val player = FakeSkillPlayer(mapOf(1775 to 5))

        assertTrue(binding.handler(SkillButtonInteraction(player, 44208, -1, 11462)))
        assertEquals("crafting.glass.229", player.activeActionName())
        player.advanceTicks(16)

        assertEquals(0, player.amount(1775))
        assertEquals(5, player.amount(229))
        assertEquals(5 * 80, player.skills.experience(Skill.CRAFTING))
        assertEquals(null, player.activeActionName())
    }

    @Test
    fun `glass button below the product's level shows the legacy message and does not start`() {
        val binding = CraftingModule.definition.buttonBindings.single { 48105 in it.rawButtonIds }
        val player = FakeSkillPlayer(mapOf(1775 to 5))

        binding.handler(SkillButtonInteraction(player, 48105, -1, 11462))
        assertEquals("You need level 18 crafting to craft a empty cup.", player.messages.single())
        assertEquals(null, player.activeActionName())
    }

    // --- Item-on-item combos --------------------------------------------------------------------

    @Test
    fun `crystal key halves combine at level 60`() {
        val binding = CraftingModule.definition.itemOnItemBindings.single {
            (it.leftItemId == 2382 && it.rightItemId == 2383) || (it.leftItemId == 2383 && it.rightItemId == 2382)
        }
        val player = FakeSkillPlayer(mapOf(2382 to 1, 2383 to 1)).apply { setLevel(Skill.CRAFTING, 60) }

        assertTrue(binding.handler(SkillItemOnItemInteraction(player, 2382, 2383)))
        assertEquals(1, player.amount(989))
        assertEquals(0, player.amount(2382))
        assertEquals(0, player.amount(2383))
    }

    @Test
    fun `crystal key halves refuse below level 60`() {
        val binding = CraftingModule.definition.itemOnItemBindings.single {
            (it.leftItemId == 2382 && it.rightItemId == 2383) || (it.leftItemId == 2383 && it.rightItemId == 2382)
        }
        val player = FakeSkillPlayer(mapOf(2382 to 1, 2383 to 1)).apply { setLevel(Skill.CRAFTING, 1) }

        binding.handler(SkillItemOnItemInteraction(player, 2382, 2383))
        assertEquals("You need 60 crafting to make the crystal key", player.messages.single())
        assertEquals(0, player.amount(989))
        assertEquals(1, player.amount(2382))
    }

    @Test
    fun `chisel and fishbowl make a fishbowl helmet`() {
        val binding = CraftingModule.definition.itemOnItemBindings.single {
            (it.leftItemId == 6667 && it.rightItemId == 1755) || (it.leftItemId == 1755 && it.rightItemId == 6667)
        }
        val player = FakeSkillPlayer(mapOf(6667 to 1, 1755 to 1))

        assertTrue(binding.handler(SkillItemOnItemInteraction(player, 6667, 1755)))
        assertEquals(1, player.amount(7534))
        assertEquals(0, player.amount(6667))
        assertEquals(1, player.amount(1755), "chisel itself is a tool, not consumed")
        assertEquals(60, player.skills.experience(Skill.CRAFTING))
    }

    @Test
    fun `wool strings an unstrung amulet`() {
        val binding = CraftingModule.definition.itemOnItemBindings.single {
            (it.leftItemId == 1759 && it.rightItemId == 1673) || (it.leftItemId == 1673 && it.rightItemId == 1759)
        }
        val player = FakeSkillPlayer(mapOf(1759 to 1, 1673 to 1))

        assertTrue(binding.handler(SkillItemOnItemInteraction(player, 1759, 1673)))
        assertEquals(1, player.amount(1692))
        assertEquals(0, player.amount(1759))
        assertEquals(0, player.amount(1673))
        assertEquals(60, player.skills.experience(Skill.CRAFTING))
    }

    @Test
    fun `all 7 amulet types are registered for wool stringing`() {
        val expected = setOf(1673, 1675, 1677, 1679, 1681, 1683, 6579)
        expected.forEach { amuletId ->
            assertTrue(
                CraftingModule.definition.itemOnItemBindings.any {
                    (it.leftItemId == 1759 && it.rightItemId == amuletId) || (it.leftItemId == amuletId && it.rightItemId == 1759)
                },
                "expected wool+$amuletId to be registered",
            )
        }
    }

    @Test
    fun `dead legacy systems have no live route - jewelry mould interface and standard leather`() {
        // Confirmed dead in the legacy code before this port (no live trigger existed for
        // either); nothing in the plugin should claim these interface ids.
        assertFalse(CraftingModule.definition.buttonBindings.any { it.requiredInterfaceId == 4161 })
        assertFalse(CraftingModule.definition.itemGridBindings.any { it.interfaceId in 4233..4257 })
    }
}
