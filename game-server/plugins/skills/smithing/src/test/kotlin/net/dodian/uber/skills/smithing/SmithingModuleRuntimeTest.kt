package net.dodian.uber.skills.smithing

import net.dodian.uber.game.api.plugin.skills.SkillItemOnNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillMagicOnItemInteraction
import net.dodian.uber.game.api.plugin.skills.SkillNpcRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SmithingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(SmithingModule.descriptor.id, Skill.SMITHING)
        assertEquals(SmithingModule.descriptor.id, SmithingModule.contentManifest.id)
    }

    @Test
    fun `toml smelting recipes are loaded`() {
        assertTrue(SmithingModule.smeltingRecipes.isNotEmpty())
        assertTrue(SmithingModule.smeltingRecipes.any { it.barId == 2349 })
    }

    @Test
    fun `plugin smelting registry preserves legacy iron chance and furnace mappings`() {
        val iron = requireNotNull(SmeltingRegistry.findRecipe(2351))
        assertEquals(54, iron.successChancePercent)
        assertEquals("You fail to refine the iron", iron.failureMessage)
        assertEquals(8, SmeltingRegistry.recipes.size)
        assertEquals(FurnaceButtonMapping(15147, 2349, 1), SmeltingRegistry.mapping(15147))
        assertEquals(FurnaceButtonMapping(2414, 2349, 0), SmeltingRegistry.mapping(2414))
        assertTrue(SmeltingRegistry.isFurnaceFrame(2405))
        assertTrue(SmeltingRegistry.isFurnaceButton(2414))
    }

    @Test
    fun `furnace routes use only verified current cache furnaces`() {
        assertEquals(setOf(2030, 3994, 16469), SmithingModule.furnaceObjectIds.toSet())
        assertFalse(SmithingModule.furnaceObjectIds.any { it in setOf(2150, 2151, 2152, 2153, 11666, 29662) })
        SmithingModule.furnaceObjectIds.forEach { objectId ->
            assertTrue(SmithingModule.definition.itemOnObjectBindings.any { objectId in it.objectIds })
        }
        setOf(2030, 16469).forEach { objectId ->
            assertTrue(SmithingModule.definition.objectBindings.any { it.option == 2 && objectId in it.objectIds })
        }
        assertFalse(SmithingModule.definition.objectBindings.any { it.option == 1 && it.objectIds.any { id -> id in SmithingModule.furnaceObjectIds } })
        assertFalse(SmithingModule.definition.objectBindings.any { 3994 in it.objectIds })
    }

    @Test
    fun `anvil routes exclude revision 218 table id`() {
        assertEquals(setOf(2097), SmithingModule.anvilObjectIds.toSet())
        assertFalse(SmithingModule.definition.itemOnObjectBindings.any { 2783 in it.objectIds })
    }

    @Test
    fun `plugin owns repeated furnace action and clears it on completion`() {
        val player = net.dodian.uber.skills.testkit.FakeSkillPlayer(
            mapOf(436 to 2, 438 to 2),
        ).apply { setLevel(Skill.SMITHING, 99) }

        assertTrue(SmithingSmeltingService.start(player, 2349, 2))
        assertEquals("smithing.smelt", player.activeActionName())
        player.advanceTicks(4)

        assertEquals(0, player.amount(436))
        assertEquals(0, player.amount(438))
        assertEquals(2, player.amount(2349))
        assertEquals(2 * requireNotNull(SmeltingRegistry.findRecipe(2349)).experience, player.skills.experience(Skill.SMITHING))
        assertEquals(2, player.randomEventChecks)
        assertEquals(null, player.activeActionName())
    }

    @Test
    fun `furnace action preserves legacy missing ore message and cancellation`() {
        val missingCoal = net.dodian.uber.skills.testkit.FakeSkillPlayer(
            mapOf(440 to 1, 453 to 1),
        ).apply { setLevel(Skill.SMITHING, 99) }

        assertTrue(SmithingSmeltingService.start(missingCoal, 2353, 1))
        missingCoal.advanceTicks()
        assertEquals("You need a iron ore and 2 coal to do this!", missingCoal.messages.single())
        assertEquals(1, missingCoal.amount(440))
        assertEquals(1, missingCoal.amount(453))
        assertEquals(0, missingCoal.amount(2353))

        val cancelled = net.dodian.uber.skills.testkit.FakeSkillPlayer(
            mapOf(436 to 1, 438 to 1),
        ).apply { setLevel(Skill.SMITHING, 99) }
        assertTrue(SmithingSmeltingService.start(cancelled, 2349, 1))
        SmithingSmeltingService.stopAction(cancelled)
        cancelled.advanceTicks(4)
        assertEquals(1, cancelled.amount(436))
        assertEquals(1, cancelled.amount(438))
        assertEquals(0, cancelled.amount(2349))
    }

    @Test
    fun `pending furnace selection is plugin runtime state`() {
        val player = net.dodian.uber.skills.testkit.FakeSkillPlayer()

        assertTrue(SmithingSmeltingService.selectPending(player, 2351))
        assertEquals(2351, SmithingSmeltingService.pendingBar(player))
        SmithingSmeltingService.clearPending(player)
        assertEquals(-1, SmithingSmeltingService.pendingBar(player))
        assertFalse(SmithingSmeltingService.selectPending(player, 9999))
    }

    @Test
    fun `plugin owns anvil registry interface and transient selection`() {
        val player = net.dodian.uber.skills.testkit.FakeSkillPlayer(
            mapOf(2347 to 1, 2349 to 5),
        ).apply { setLevel(Skill.SMITHING, 99) }

        assertTrue(SmithingAnvilService.openForBar(player, 2349, 3201, 3202))
        assertEquals(listOf(994), player.openedInterfaces)
        assertEquals(setOf(1119, 1120, 1121, 1122, 1123), player.itemGrids.keys)
        assertEquals(1, SmithingAnvilService.activeSelection(player)?.tierId)
        assertEquals(3201, SmithingAnvilService.activeSelection(player)?.anvilX)

        val request = SmithingAnvilService.resolveRequest(
            player = player,
            interfaceId = 1119,
            itemId = 1205,
            slot = 0,
            amount = 3,
            fallbackAnchorX = -1,
            fallbackAnchorY = -1,
        )
        assertEquals(1205, request?.product?.itemId)
        assertEquals(3, request?.amount)
        assertEquals(2349, request?.barId)

        SmithingAnvilService.clear(player)
        assertEquals(null, SmithingAnvilService.activeSelection(player))
    }

    @Test
    fun `anvil interface rejects missing hammer without creating state`() {
        val player = net.dodian.uber.skills.testkit.FakeSkillPlayer(mapOf(2349 to 1))

        assertFalse(SmithingAnvilService.openForBar(player, 2349, 3201, 3202))
        assertEquals("You need a item 2347 to hammer bars.", player.messages.single())
        assertEquals(null, SmithingAnvilService.activeSelection(player))
        assertTrue(player.openedInterfaces.isEmpty())
    }

    @Test
    fun `plugin owns delayed repeated anvil action and cleanup`() {
        val player = net.dodian.uber.skills.testkit.FakeSkillPlayer(
            mapOf(2347 to 1, 2349 to 2),
        ).apply {
            setLevel(Skill.SMITHING, 99)
            positionValue = net.dodian.uber.game.api.plugin.skills.SkillPosition(3201, 3202)
        }
        assertTrue(SmithingAnvilService.openForBar(player, 2349, 3201, 3202))
        val request = requireNotNull(
            SmithingAnvilService.resolveRequest(
                player, 1119, 1205, 0, 2, -1, -1,
            ),
        )

        assertTrue(SmithingAnvilService.start(player, request))
        assertEquals("smithing.anvil", player.activeActionName())
        player.advanceTicks(3)
        assertEquals(0, player.amount(1205), "the initial hammer delay must be preserved")
        player.advanceTicks(4)

        assertEquals(0, player.amount(2349))
        assertEquals(2, player.amount(1205))
        assertEquals(780, player.skills.experience(Skill.SMITHING))
        assertEquals(2, player.randomEventChecks)
        assertEquals(null, player.activeActionName())
        assertEquals(null, SmithingAnvilService.activeSelection(player))
    }

    // --- Dragonfire shield (itemOnNpc) --------------------------------------------------------

    @Test
    fun `dragonfire shield assembles from either item and has no smithing level gate`() {
        val binding = SmithingModule.definition.itemOnNpcBindings.single { 535 in it.npcIds }
        val npc = SkillNpcRef(535, 0, SkillPosition(3200, 3200))

        // Preserved legacy quirk (confirmed with the user during this port): no Smithing
        // level check exists here, unlike the OSRS canon requirement of 90.
        val player = FakeSkillPlayer(mapOf(1540 to 1, 11286 to 1, 995 to 1_500_000)).apply { setLevel(Skill.SMITHING, 1) }
        assertTrue(binding.handler(SkillItemOnNpcInteraction(player, npc, 1540, 0)))
        assertEquals(0, player.amount(1540))
        assertEquals(0, player.amount(11286))
        assertEquals(0, player.amount(995))
        assertEquals(1, player.amount(11284))
        assertEquals("Here you go. Your shield is done.", player.messages.single())
    }

    @Test
    fun `dragonfire shield assembly also triggers from the anti-dragon shield item`() {
        val binding = SmithingModule.definition.itemOnNpcBindings.single { 535 in it.npcIds }
        val npc = SkillNpcRef(535, 0, SkillPosition(3200, 3200))
        val player = FakeSkillPlayer(mapOf(1540 to 1, 11286 to 1, 995 to 1_500_000))

        assertTrue(binding.handler(SkillItemOnNpcInteraction(player, npc, 11286, 0)))
        assertEquals(1, player.amount(11284))
    }

    @Test
    fun `dragonfire shield refuses without the paired item or coins`() {
        val binding = SmithingModule.definition.itemOnNpcBindings.single { 535 in it.npcIds }
        val npc = SkillNpcRef(535, 0, SkillPosition(3200, 3200))

        val noShield = FakeSkillPlayer(mapOf(1540 to 1))
        binding.handler(SkillItemOnNpcInteraction(noShield, npc, 1540, 0))
        assertEquals("You need a anti-dragon shield!", noShield.messages.single())
        assertEquals(0, noShield.amount(11284))

        val noVisage = FakeSkillPlayer(mapOf(11286 to 1))
        binding.handler(SkillItemOnNpcInteraction(noVisage, npc, 11286, 0))
        assertEquals("You need a draconic visage!", noVisage.messages.single())

        val noCoins = FakeSkillPlayer(mapOf(1540 to 1, 11286 to 1))
        binding.handler(SkillItemOnNpcInteraction(noCoins, npc, 1540, 0))
        assertEquals("You need 1.5 million coins!", noCoins.messages.single())
        assertEquals(0, noCoins.amount(11284))
    }

    // --- Rockshell armour (playerOptionMenu) --------------------------------------------------

    @Test
    fun `rockshell menu guard blocks below level 60 or without a hammer`() {
        val binding = SmithingModule.definition.playerOptionMenuBindings.single()

        val lowLevel = FakeSkillPlayer(mapOf(2347 to 1)).apply { setLevel(Skill.SMITHING, 59) }
        assertEquals("You need level 60 smithing to do this.", binding.guard?.invoke(lowLevel))

        val noHammer = FakeSkillPlayer().apply { setLevel(Skill.SMITHING, 60) }
        assertEquals("You need a hammer to handle this material.", binding.guard?.invoke(noHammer))

        val ready = FakeSkillPlayer(mapOf(2347 to 1)).apply { setLevel(Skill.SMITHING, 60) }
        assertEquals(null, binding.guard?.invoke(ready))
    }

    @Test
    fun `rockshell crafts each of the 5 pieces and consumes the right materials`() {
        val binding = SmithingModule.definition.playerOptionMenuBindings.single()
        val player = FakeSkillPlayer(mapOf(6157 to 2, 6159 to 4, 6161 to 5))

        binding.handler(player, 1)
        assertEquals(1, player.amount(6128), "head")
        binding.handler(player, 2)
        assertEquals(1, player.amount(6129), "body")
        binding.handler(player, 3)
        assertEquals(1, player.amount(6130), "legs")
        binding.handler(player, 4)
        assertEquals(1, player.amount(6145), "boots")
        binding.handler(player, 5)
        assertEquals(1, player.amount(6151), "gloves")
        assertEquals(0, player.amount(6157))
        assertEquals(0, player.amount(6159))
        assertEquals(0, player.amount(6161))
    }

    @Test
    fun `rockshell missing materials shows the itemized message without consuming anything`() {
        val binding = SmithingModule.definition.playerOptionMenuBindings.single()
        val player = FakeSkillPlayer(mapOf(6159 to 1))

        binding.handler(player, 1)
        assertTrue(player.messages.single().startsWith("I need the following items:"))
        assertEquals(1, player.amount(6159))
        assertEquals(0, player.amount(6128))
    }

    @Test
    fun `all 10 rockshell item pairs route to the same menu-opening trigger`() {
        val ids = listOf(6157, 6158, 6159, 6160, 6161)
        val pairs = ids.indices.flatMap { i -> (i + 1 until ids.size).map { j -> ids[i] to ids[j] } }
        assertEquals(10, pairs.size)
        pairs.forEach { (a, b) ->
            assertTrue(
                SmithingModule.definition.itemOnItemBindings.any {
                    (it.leftItemId == a && it.rightItemId == b) || (it.leftItemId == b && it.rightItemId == a)
                },
                "expected a trigger for pair $a/$b",
            )
        }
        val binding = SmithingModule.definition.itemOnItemBindings.first {
            (it.leftItemId == 6157 && it.rightItemId == 6158) || (it.leftItemId == 6158 && it.rightItemId == 6157)
        }
        val player = FakeSkillPlayer()
        binding.handler(net.dodian.uber.game.api.plugin.skills.SkillItemOnItemInteraction(player, 6157, 6158))
        // DialogueIds.Misc.ROCKSHELL_MENU
        assertEquals(listOf(10000), player.openedDialogueIds)
    }

    // --- Superheat spell (magicOnItem) --------------------------------------------------------

    @Test
    fun `superheat smelts a bronze bar and grants smithing and magic xp`() {
        val binding = SmithingModule.definition.magicOnItemBindings.single()
        val player = FakeSkillPlayer(mapOf(436 to 1, 438 to 1, 561 to 1)).apply { setLevel(Skill.SMITHING, 99) }

        assertTrue(binding.handler(SkillMagicOnItemInteraction(player, 436, 0, 1173)))
        assertEquals(0, player.amount(436))
        assertEquals(0, player.amount(438))
        assertEquals(0, player.amount(561))
        assertEquals(1, player.amount(2349))
        assertEquals(requireNotNull(SmeltingRegistry.findRecipe(2349)).experience, player.skills.experience(Skill.SMITHING))
        assertEquals(500, player.skills.experience(Skill.MAGIC))
        assertTrue(player.magicCastCycleMarked)
        assertEquals(listOf(148 to 100), player.castGraphicsPlayed)
    }

    @Test
    fun `superheat item 440 dispatches to steel with coal or iron without`() {
        val binding = SmithingModule.definition.magicOnItemBindings.single()

        val withCoal = FakeSkillPlayer(mapOf(440 to 1, 453 to 2, 561 to 1)).apply { setLevel(Skill.SMITHING, 99) }
        binding.handler(SkillMagicOnItemInteraction(withCoal, 440, 0, 1173))
        assertEquals(1, withCoal.amount(2353))

        val withoutCoal = FakeSkillPlayer(mapOf(440 to 1, 561 to 1)).apply { setLevel(Skill.SMITHING, 99) }
        binding.handler(SkillMagicOnItemInteraction(withoutCoal, 440, 0, 1173))
        assertEquals(1, withoutCoal.amount(2351))
    }

    @Test
    fun `superheat converts sand and ash to molten glass with crafting and magic xp`() {
        val binding = SmithingModule.definition.magicOnItemBindings.single()
        val player = FakeSkillPlayer(mapOf(1783 to 2, 1781 to 2, 561 to 3))

        assertTrue(binding.handler(SkillMagicOnItemInteraction(player, 1783, 0, 1173)))
        assertEquals(0, player.amount(1783))
        assertEquals(0, player.amount(1781))
        assertEquals(0, player.amount(561))
        // FakeSkillPlayer.random.chance always succeeds for a positive numerator, so both
        // conversions get the legacy 30%-bonus roll: moltenCount = count * 2.
        assertEquals(4, player.amount(1775))
        assertEquals(80, player.skills.experience(Skill.CRAFTING))
        assertEquals(500, player.skills.experience(Skill.MAGIC))
        assertEquals(0, player.skills.experience(Skill.SMITHING))
    }

    @Test
    fun `superheat glass also triggers from the alternate ash item`() {
        val binding = SmithingModule.definition.magicOnItemBindings.single()
        val player = FakeSkillPlayer(mapOf(1783 to 1, 401 to 1, 561 to 3))

        assertTrue(binding.handler(SkillMagicOnItemInteraction(player, 401, 0, 1173)))
        assertEquals(0, player.amount(1783))
        assertEquals(0, player.amount(401))
        assertTrue(player.amount(1775) > 0)
    }

    @Test
    fun `superheat shows the legacy nature rune and ore missing messages`() {
        val binding = SmithingModule.definition.magicOnItemBindings.single()

        val noRune = FakeSkillPlayer(mapOf(436 to 1, 438 to 1)).apply { setLevel(Skill.SMITHING, 99) }
        binding.handler(SkillMagicOnItemInteraction(noRune, 436, 0, 1173))
        assertEquals("You need 1 nature runes to cast this spell!", noRune.messages.single())
        assertEquals(0, noRune.amount(2349))

        val noOre = FakeSkillPlayer(mapOf(561 to 1)).apply { setLevel(Skill.SMITHING, 99) }
        binding.handler(SkillMagicOnItemInteraction(noOre, 436, 0, 1173))
        assertEquals("You need a tin and copper to do this!", noOre.messages.single())

        val noRuneGlass = FakeSkillPlayer(mapOf(1783 to 1, 1781 to 1))
        binding.handler(SkillMagicOnItemInteraction(noRuneGlass, 1783, 0, 1173))
        assertEquals("Need 3 nature runes to cast this spell on glass material!", noRuneGlass.messages.single())
    }

    @Test
    fun `superheat refuses below the recipe's smithing level`() {
        val binding = SmithingModule.definition.magicOnItemBindings.single()
        val player = FakeSkillPlayer(mapOf(451 to 1, 561 to 1)).apply { setLevel(Skill.SMITHING, 1) }

        binding.handler(SkillMagicOnItemInteraction(player, 451, 0, 1173))
        assertTrue(player.messages.single().startsWith("You need level"))
        assertEquals(0, player.amount(2363))
    }
}
