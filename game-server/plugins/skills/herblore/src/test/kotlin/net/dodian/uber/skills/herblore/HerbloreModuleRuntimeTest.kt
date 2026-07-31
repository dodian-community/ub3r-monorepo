package net.dodian.uber.skills.herblore

import net.dodian.uber.game.api.plugin.skills.SkillItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillItemOnItemInteraction
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillMultiSelection
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HerbloreModuleRuntimeTest {
    @Test
    fun `grimy herbs and potion recipes are plugin owned`() {
        assertEquals(HerbloreModule.descriptor.id, HerbloreModule.contentManifest.id)
        val player = FakeSkillPlayer(mapOf(199 to 1, 249 to 1, 227 to 1)).apply { setLevel(Skill.HERBLORE, 99) }
        val clean = HerbloreModule.definition.itemBindings.single { 199 in it.itemIds }
        assertTrue(clean.handler(SkillItemInteraction(player, 1, 199, 0, -1)))
        assertEquals(2, player.amount(249))

        val mix = HerbloreModule.definition.itemOnItemBindings.first { setOf(it.leftItemId, it.rightItemId) == setOf(249, 227) }
        assertTrue(mix.handler(SkillItemOnItemInteraction(player, 249, 227)))
        val menu = player.production.pending()!!
        assertTrue(player.production.select(SkillMultiSelection(menu.key, menu.entries.single().recipe.key, 1)))
        player.advanceTicks(2)
        assertEquals(1, player.amount(91))
    }

    @Test
    fun `premium recipes refuse non premium players`() {
        val player = FakeSkillPlayer(mapOf(259 to 1, 227 to 1)).apply { setLevel(Skill.HERBLORE, 99) }
        val binding = HerbloreModule.definition.itemOnItemBindings.first { setOf(it.leftItemId, it.rightItemId) == setOf(259, 227) }
        assertTrue(binding.handler(SkillItemOnItemInteraction(player, 259, 227)))
        val menu = player.production.pending()!!
        assertTrue(player.production.select(SkillMultiSelection(menu.key, menu.entries.single().recipe.key, 1)))
        player.advanceTicks(1)
        assertEquals(1, player.amount(259))
        assertEquals(1, player.amount(227))
    }

    // --- Dose-mixing (itemOnItem, instant) ------------------------------------------------------
    // Dose def used: PotionDoseDefinition(oneDoseId=119, twoDoseId=117, threeDoseId=115, fourDoseId=113)

    private fun doseBinding(a: Int, b: Int) = HerbloreModule.definition.itemOnItemBindings.single { setOf(it.leftItemId, it.rightItemId) == setOf(a, b) }

    @Test
    fun `four dose plus empty vial yields two 2-dose potions`() {
        val binding = doseBinding(113, 229)
        val player = FakeSkillPlayer(mapOf(113 to 1, 229 to 1))

        assertTrue(binding.handler(SkillItemOnItemInteraction(player, 113, 229)))
        assertEquals(0, player.amount(113))
        assertEquals(0, player.amount(229))
        assertEquals(2, player.amount(117))
    }

    @Test
    fun `three dose plus two dose yields a 4-dose and a 1-dose`() {
        val binding = doseBinding(115, 117)
        val player = FakeSkillPlayer(mapOf(115 to 1, 117 to 1))

        assertTrue(binding.handler(SkillItemOnItemInteraction(player, 115, 117)))
        assertEquals(0, player.amount(115))
        assertEquals(0, player.amount(117))
        assertEquals(1, player.amount(113))
        assertEquals(1, player.amount(119))
    }

    @Test
    fun `two identical one-dose potions merge into a 2-dose plus empty vial`() {
        val binding = doseBinding(119, 119)
        val player = FakeSkillPlayer(mapOf(119 to 2))

        assertTrue(binding.handler(SkillItemOnItemInteraction(player, 119, 119)))
        assertEquals(0, player.amount(119))
        assertEquals(1, player.amount(117))
        assertEquals(1, player.amount(229))
    }

    @Test
    fun `all 8 dose pair-shapes are registered for every potion type`() {
        HerbloreModule.doses.forEach { dose ->
            val expectedPairs = listOf(
                dose.fourDoseId to 229,
                dose.threeDoseId to dose.threeDoseId,
                dose.threeDoseId to dose.twoDoseId,
                dose.twoDoseId to 229,
                dose.twoDoseId to dose.twoDoseId,
                dose.oneDoseId to dose.oneDoseId,
                dose.oneDoseId to dose.twoDoseId,
                dose.oneDoseId to dose.threeDoseId,
            )
            expectedPairs.forEach { (a, b) ->
                assertTrue(
                    HerbloreModule.definition.itemOnItemBindings.any { setOf(it.leftItemId, it.rightItemId) == setOf(a, b) },
                    "expected a dose-mixing trigger for pair $a/$b",
                )
            }
        }
    }

    // --- Super combat potion (nested-pairs itemOnItem) ------------------------------------------

    private fun superCombatBinding(a: Int, b: Int) = HerbloreModule.definition.itemOnItemBindings.single { setOf(it.leftItemId, it.rightItemId) == setOf(a, b) }

    @Test
    fun `super combat mixes from any of the 10 trigger pairs and prefers torstol herb over unfinished`() {
        val binding = superCombatBinding(269, 2436)
        val player = FakeSkillPlayer(mapOf(269 to 1, 111 to 1, 2436 to 1, 2440 to 1, 2442 to 1)).apply { setLevel(Skill.HERBLORE, 88) }

        assertTrue(binding.handler(SkillItemOnItemInteraction(player, 269, 2436)))
        assertEquals(1, player.amount(12695))
        assertEquals(0, player.amount(269), "expected torstol herb to be preferred/consumed over the unfinished potion")
        assertEquals(1, player.amount(111), "expected the unfinished torstol potion to be untouched")
        assertEquals(0, player.amount(2436))
        assertEquals(0, player.amount(2440))
        assertEquals(0, player.amount(2442))
        assertEquals(600, player.skills.experience(Skill.HERBLORE))
    }

    @Test
    fun `super combat falls back to the unfinished torstol potion when no torstol herb is held`() {
        val binding = superCombatBinding(111, 2436)
        val player = FakeSkillPlayer(mapOf(111 to 1, 2436 to 1, 2440 to 1, 2442 to 1)).apply { setLevel(Skill.HERBLORE, 88) }

        assertTrue(binding.handler(SkillItemOnItemInteraction(player, 111, 2436)))
        assertEquals(1, player.amount(12695))
        assertEquals(0, player.amount(111))
    }

    @Test
    fun `super combat refuses missing ingredients and below level 88`() {
        val missing = superCombatBinding(2440, 2442)
        val missingPlayer = FakeSkillPlayer(mapOf(2440 to 1, 2442 to 1)).apply { setLevel(Skill.HERBLORE, 99) }
        missing.handler(SkillItemOnItemInteraction(missingPlayer, 2440, 2442))
        assertEquals("You need a torstol herb or (unf) potion, super attack, strength and defence potion!", missingPlayer.messages.single())
        assertEquals(0, missingPlayer.amount(12695))

        val lowLevel = FakeSkillPlayer(mapOf(269 to 1, 2436 to 1, 2440 to 1, 2442 to 1)).apply { setLevel(Skill.HERBLORE, 1) }
        superCombatBinding(2440, 2442).handler(SkillItemOnItemInteraction(lowLevel, 2440, 2442))
        assertEquals("You need level 88 herblore to mix a super combat potion!", lowLevel.messages.single())
    }

    @Test
    fun `all 10 super combat trigger pairs are registered`() {
        val ids = listOf(269, 111, 2436, 2440, 2442)
        val expectedPairs = ids.indices.flatMap { i -> (i + 1 until ids.size).map { j -> ids[i] to ids[j] } }
        assertEquals(10, expectedPairs.size)
        expectedPairs.forEach { (a, b) ->
            assertTrue(
                HerbloreModule.definition.itemOnItemBindings.any { setOf(it.leftItemId, it.rightItemId) == setOf(a, b) },
                "expected a super combat trigger for pair $a/$b",
            )
        }
    }

    // --- Overload potion (nested-pairs itemOnItem) ----------------------------------------------

    @Test
    fun `overload mixes from any of the 3 trigger pairs`() {
        val binding = HerbloreModule.definition.itemOnItemBindings.single { setOf(it.leftItemId, it.rightItemId) == setOf(12695, 2444) }
        val player = FakeSkillPlayer(mapOf(12695 to 1, 2444 to 1, 5978 to 1)).apply { setLevel(Skill.HERBLORE, 93) }

        assertTrue(binding.handler(SkillItemOnItemInteraction(player, 12695, 2444)))
        assertEquals(1, player.amount(11730))
        assertEquals(0, player.amount(12695))
        assertEquals(0, player.amount(2444))
        assertEquals(0, player.amount(5978))
        assertEquals(800, player.skills.experience(Skill.HERBLORE))
    }

    @Test
    fun `overload refuses missing ingredients and below level 93`() {
        val binding = HerbloreModule.definition.itemOnItemBindings.single { setOf(it.leftItemId, it.rightItemId) == setOf(12695, 2444) }

        val missingPlayer = FakeSkillPlayer(mapOf(12695 to 1, 2444 to 1)).apply { setLevel(Skill.HERBLORE, 99) }
        binding.handler(SkillItemOnItemInteraction(missingPlayer, 12695, 2444))
        assertEquals("You need a coconut, super combat potion and a ranging potion!", missingPlayer.messages.single())
        assertEquals(0, missingPlayer.amount(11730))

        val lowLevel = FakeSkillPlayer(mapOf(12695 to 1, 2444 to 1, 5978 to 1)).apply { setLevel(Skill.HERBLORE, 1) }
        binding.handler(SkillItemOnItemInteraction(lowLevel, 12695, 2444))
        assertEquals("You need level 93 herblore to mix an overload potion!", lowLevel.messages.single())
    }

    // --- Zahur: herb-cleaner / unfinished-potion-maker batch service ---------------------------
    // Note: FakeSkillPlayer.notedItemId(id) is the identity function - it doesn't model a
    // distinct noted/unnoted item pair the way the real game does. That's fine here: these
    // tests exercise the plugin's own cost/clamp/consume math, not the engine's noted-item
    // resolution (which is trusted, pre-existing engine behavior).

    @Test
    fun `herb cleaner menu lists only noted grimy herbs the player holds`() {
        val player = FakeSkillPlayer(mapOf(199 to 1))
        HerbloreModule.openHerbCleanerMenu(player)
        assertEquals(listOf(199), player.pendingItemListMenuItems())
    }

    @Test
    fun `herb cleaner menu shows a message when the player holds no herbs`() {
        val player = FakeSkillPlayer()
        HerbloreModule.openHerbCleanerMenu(player)
        assertEquals("You got no herbs for me to clean!", player.messages.single())
    }

    @Test
    fun `selecting a herb and submitting an amount grinds it into clean herbs`() {
        val player = FakeSkillPlayer(mapOf(199 to 5, 995 to 1000))
        HerbloreModule.openHerbCleanerMenu(player)

        assertTrue(player.selectItemListOption(199))
        assertTrue(player.submitAmount(3))

        assertEquals(2, player.amount(199))
        assertEquals(3, player.amount(249))
        assertEquals(1000 - 3 * 200, player.amount(995))
    }

    @Test
    fun `grinding clamps to whatever coins or herbs are actually available`() {
        val player = FakeSkillPlayer(mapOf(199 to 5, 995 to 400))
        HerbloreModule.openHerbCleanerMenu(player)

        player.selectItemListOption(199)
        player.submitAmount(10)

        assertEquals(3, player.amount(199))
        assertEquals(2, player.amount(249))
        assertEquals(0, player.amount(995))
    }

    @Test
    fun `grinding with insufficient coins shows the legacy message`() {
        val player = FakeSkillPlayer(mapOf(199 to 5, 995 to 100))
        HerbloreModule.openHerbCleanerMenu(player)

        player.selectItemListOption(199)
        player.submitAmount(1)

        assertEquals("You need 1 herb and 200 coins for me to grind it for you.", player.messages.single())
    }

    @Test
    fun `unfinished potion menu lists the unfinished-potion id for held noted clean herbs`() {
        val player = FakeSkillPlayer(mapOf(249 to 1))
        HerbloreModule.openUnfinishedPotionMakerMenu(player)
        assertEquals(listOf(91), player.pendingItemListMenuItems())
    }

    @Test
    fun `making unfinished potions consumes clean herb, vial of water and coins`() {
        val player = FakeSkillPlayer(mapOf(249 to 5, 228 to 5, 995 to 5000))
        HerbloreModule.openUnfinishedPotionMakerMenu(player)

        player.selectItemListOption(91)
        player.submitAmount(3)

        assertEquals(2, player.amount(249))
        assertEquals(2, player.amount(228))
        assertEquals(3, player.amount(91))
        assertEquals(5000 - 3 * 1000, player.amount(995))
    }

    @Test
    fun `making unfinished potions without a vial of water shows the legacy message`() {
        val player = FakeSkillPlayer(mapOf(249 to 5, 995 to 5000))
        HerbloreModule.openUnfinishedPotionMakerMenu(player)

        player.selectItemListOption(91)
        player.submitAmount(1)

        assertEquals("You need noted vial of water for me to do that!", player.messages.single())
    }

    @Test
    fun `zahur direct decant and clean options are registered`() {
        assertTrue(HerbloreModule.definition.npcBindings.any { it.option == 3 && 4753 in it.npcIds })
        assertTrue(HerbloreModule.definition.npcBindings.any { it.option == 4 && 4753 in it.npcIds })
    }

    // --- Zahur/Jatix: bulk potion decanting -----------------------------------------------------

    @Test
    fun `opening the decant menu opens the registered dialogue`() {
        val player = FakeSkillPlayer()
        HerbloreModule.openDecantMenu(player)
        // DialogueIds are game-server-only; the plugin picks its own opaque id (20932).
        assertEquals(listOf(20932), player.openedDialogueIds)
    }

    @Test
    fun `decanting two 1-dose potions into 2-dose yields one potion and a noted empty vial`() {
        val player = FakeSkillPlayer(mapOf(119 to 2))
        val binding = HerbloreModule.definition.playerOptionMenuBindings.single()

        binding.handler(player, 3) // "Two dose"

        assertEquals(0, player.amount(119))
        assertEquals(1, player.amount(117))
        assertEquals(1, player.amount(230))
    }

    @Test
    fun `decanting mixed doses into 4-dose produces a leftover partial potion`() {
        // 3 one-dose potions -> 3 total doses; converting to 4-dose: 0 whole potions, 3 leftover
        // (a 3-dose leftover item), consuming all 3 vials with none left over as empty vials.
        val player = FakeSkillPlayer(mapOf(119 to 3))
        val binding = HerbloreModule.definition.playerOptionMenuBindings.single()

        binding.handler(player, 1) // "Four dose"

        assertEquals(0, player.amount(119))
        assertEquals(0, player.amount(113), "not enough doses for a whole 4-dose potion")
        assertEquals(1, player.amount(115), "expected a 3-dose leftover potion")
    }

    @Test
    fun `nevermind shows the legacy flavor message and decants nothing`() {
        val player = FakeSkillPlayer(mapOf(119 to 2))
        val binding = HerbloreModule.definition.playerOptionMenuBindings.single()

        binding.handler(player, 5)

        assertEquals("Nevermind, I do not need anything.", player.messages.single())
        assertEquals(2, player.amount(119))
    }
}
