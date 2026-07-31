package net.dodian.uber.skills.prayer

import net.dodian.uber.game.api.plugin.skills.SkillItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.api.plugin.skills.SkillPrayer
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrayerModuleRuntimeTest {
    @Test
    fun `burying a bone consumes it and grants xp`() {
        val player = FakeSkillPlayer(mapOf(526 to 1))
        val binding = PrayerModule.definition.itemBindings.single { 526 in it.itemIds }
        assertTrue(binding.handler(SkillItemInteraction(player, 1, 526, 0, -1)))
        assertEquals(0, player.amount(526))
        assertEquals(45, player.skills.experience(Skill.PRAYER))
    }

    @Test
    fun `altar restores prayer through the public vitals contract`() {
        val player = FakeSkillPlayer().apply { currentPrayerValue = 10; maximumPrayerValue = 50 }
        val binding = PrayerModule.definition.objectBindings.single { 409 in it.objectIds }
        assertTrue(binding.handler(SkillObjectInteraction(player, 1, SkillObjectRef(409, SkillPosition(3200, 3200)))))
        assertEquals(50, player.currentPrayerValue)
    }

    @Test
    fun `tarnish only chaos altar is not registered as local content`() {
        assertTrue(PrayerModule.definition.objectBindings.none { 411 in it.objectIds })
        assertTrue(PrayerModule.definition.itemOnObjectBindings.none { 411 in it.objectIds })
    }

    @Test
    fun `prayer tab buttons are owned by the prayer plugin`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.PRAYER, 80); currentPrayerValue = 10 }
        val binding = PrayerModule.definition.buttonBindings.single { SkillPrayer.PIETY.buttonId in it.rawButtonIds }

        assertTrue(binding.handler(net.dodian.uber.game.api.plugin.skills.SkillButtonInteraction(player, SkillPrayer.PIETY.buttonId, -1, 5608)))
        assertTrue(SkillPrayer.PIETY in player.activePrayers)
    }

    // Regression tests for the toggle-validation/cascade/drain port: this logic used to live
    // entirely in the legacy PrayerManager.togglePrayer/checkExtraPrayers/getDrainRate; it's now
    // ported into PrayerModule using PrayerDef (prayer/prayers.toml) plus the new SkillVitals
    // inDuel/isDead/prayerBonus fields.
    private fun pietyBinding() = PrayerModule.definition.buttonBindings.single { SkillPrayer.PIETY.buttonId in it.rawButtonIds }
    private fun toggle(player: FakeSkillPlayer, prayer: SkillPrayer) {
        val binding = PrayerModule.definition.buttonBindings.single { prayer.buttonId in it.rawButtonIds }
        binding.handler(net.dodian.uber.game.api.plugin.skills.SkillButtonInteraction(player, prayer.buttonId, -1, 5608))
    }

    @Test
    fun `insufficient level refuses the toggle`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.PRAYER, 1); currentPrayerValue = 10 }

        assertTrue(pietyBinding().handler(net.dodian.uber.game.api.plugin.skills.SkillButtonInteraction(player, SkillPrayer.PIETY.buttonId, -1, 5608)))
        assertTrue(player.activePrayers.isEmpty(), "expected Piety (level 80) to be refused at level 1")
    }

    @Test
    fun `no prayer points resets all active prayers instead of toggling`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.PRAYER, 80); currentPrayerValue = 1 }
        toggle(player, SkillPrayer.THICK_SKIN)
        assertTrue(SkillPrayer.THICK_SKIN in player.activePrayers)
        player.currentPrayerValue = 0

        toggle(player, SkillPrayer.PIETY)

        assertTrue(player.activePrayers.isEmpty(), "expected 0 prayer points to reset everything, not just refuse the click")
    }

    @Test
    fun `being in a duel resets all active prayers instead of toggling`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.PRAYER, 80); currentPrayerValue = 10 }
        toggle(player, SkillPrayer.THICK_SKIN)
        assertTrue(SkillPrayer.THICK_SKIN in player.activePrayers)
        player.inDuelValue = true

        toggle(player, SkillPrayer.PIETY)

        assertTrue(player.activePrayers.isEmpty(), "expected duelFight to reset everything, matching legacy PrayerManager")
    }

    @Test
    fun `toggling a no-level-requirement prayer resets all active prayers - preserved legacy quirk`() {
        // Confirmed intentional-as-is with the user during this port: legacy PrayerManager's
        // `prayer.prayerLevel == -1` guard resets everything rather than toggling for the six
        // no-level-requirement prayers (Protect Item, Smite, Retribution, Redemption, Rapid
        // Restore, Rapid Heal), even outside a duel/death. Looks like a bug, preserved exactly.
        val player = FakeSkillPlayer().apply { setLevel(Skill.PRAYER, 80); currentPrayerValue = 10 }
        toggle(player, SkillPrayer.THICK_SKIN)
        assertTrue(SkillPrayer.THICK_SKIN in player.activePrayers)

        toggle(player, SkillPrayer.PROTECT_ITEM)

        assertTrue(player.activePrayers.isEmpty())
    }

    @Test
    fun `activating a conflicting prayer deactivates the other via the mutual-exclusion cascade`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.PRAYER, 80); currentPrayerValue = 10 }
        toggle(player, SkillPrayer.BURST_OF_STRENGTH) // strength/magic/range category

        toggle(player, SkillPrayer.SUPERHUMAN_STRENGTH) // same category, higher tier

        assertTrue(SkillPrayer.SUPERHUMAN_STRENGTH in player.activePrayers)
        assertTrue(SkillPrayer.BURST_OF_STRENGTH !in player.activePrayers, "expected the conflicting lower-tier strength prayer to be deactivated")
    }

    @Test
    fun `activating a non-conflicting prayer leaves the other active`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.PRAYER, 80); currentPrayerValue = 10 }
        toggle(player, SkillPrayer.THICK_SKIN) // defence category

        toggle(player, SkillPrayer.CLARITY_OF_THOUGHT) // attack category, no overlap

        assertTrue(SkillPrayer.THICK_SKIN in player.activePrayers)
        assertTrue(SkillPrayer.CLARITY_OF_THOUGHT in player.activePrayers)
    }

    @Test
    fun `tickDrain does nothing with no active prayers`() {
        val player = FakeSkillPlayer().apply { currentPrayerValue = 10 }

        PrayerModule.tickDrain(player)

        assertEquals(10, player.currentPrayerValue)
    }

    @Test
    fun `tickDrain eventually drains a point and resets all prayers when it hits zero`() {
        // Thick Skin: drainEffect=3, no equipment bonus -> drainResistance=60, rate=20 ticks/point.
        val player = FakeSkillPlayer().apply { setLevel(Skill.PRAYER, 80); currentPrayerValue = 1 }
        toggle(player, SkillPrayer.THICK_SKIN)
        assertTrue(SkillPrayer.THICK_SKIN in player.activePrayers)

        repeat(34) { PrayerModule.tickDrain(player) } // ceil(20 / 0.6) = 34 ticks to cross the threshold

        assertEquals(0, player.currentPrayerValue)
        assertTrue(player.activePrayers.isEmpty(), "expected hitting 0 prayer points to reset all active prayers")
    }
}
