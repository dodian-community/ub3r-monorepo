package net.dodian.uber.game.content.npcs.spawns

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NpcKotlinDslTest {
    @Test
    fun `spawn entries helper expands grouped points`() {
        val entries =
            spawnEntries(
                npcId = 637,
                point(2594, 3104),
                point(3253, 3402, face = 2),
            )

        assertEquals(2, entries.size)
        assertEquals(637, entries[0].npcId)
        assertEquals(2594, entries[0].x)
        assertEquals(3402, entries[1].y)
        assertEquals(2, entries[1].face)
    }

    @Test
    fun `dialogue flow requires two to five choice options`() {
        assertThrows(IllegalArgumentException::class.java) {
            buildSequentialHandler {
                choice("Select an Option") {
                    "Only one option" { }
                }
            }
        }
    }

    @Test
    fun `dialogue flow rejects duplicate labels`() {
        assertThrows(IllegalArgumentException::class.java) {
            npcDialogue {
                npc("start", "Hello.")
                npc("start", "Duplicate.")
            }
        }
    }

    @Test
    fun `aubury module owns slots one through three only`() {
        val labels = Aubury.definition.toContentDefinition("", false).optionLabels
        assertEquals("talk-to", labels[1])
        assertEquals("trade", labels[2])
        assertEquals("teleport", labels[3])
        assertFalse(labels.containsKey(4))
    }

    @Test
    fun `banker and shopkeeper expose second-click handlers`() {
        val banker = Banker.definition.toContentDefinition("", false)
        val shopKeeper = ShopKeeper.definition.toContentDefinition("", false)
        assertTrue(banker.onSecondClick !== NO_CLICK_HANDLER)
        assertTrue(shopKeeper.onSecondClick !== NO_CLICK_HANDLER)
    }

    @Test
    fun `rsps aliases map to expected option slots`() {
        val plugin =
            npcPlugin("AliasTest") {
                ids(9999)
                options {
                    talkTo { npc("hello") }
                    trade { openShop(3) }
                    teleportOption { teleport(3200, 3200, 0, random = 2) }
                }
            }
        val definition = plugin.toContentDefinition("", false)
        assertEquals("talk-to", definition.optionLabels[1])
        assertEquals("trade", definition.optionLabels[2])
        assertEquals("teleport", definition.optionLabels[3])
    }

    @Test
    fun `duplicate option ownership in one module is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            npcPlugin("DupOption") {
                ids(10000)
                options {
                    talkTo { npc("one") }
                    first("talk-to") { _, _ -> true }
                }
            }
        }
    }

    @Test
    fun `condition helper compiles with balloons predicate`() {
        val handler =
            buildSequentialHandler {
                whenCondition({ balloonsEventActive() }, thenBlock = {
                    sendMessage("active")
                }) otherwise {
                    sendMessage("inactive")
                }
            }
        assertTrue(handler !== NO_CLICK_HANDLER)
    }

    @Test
    fun `banker and shopkeeper use rsps alias slot labels with no slot four`() {
        val bankerLabels = Banker.definition.toContentDefinition("", false).optionLabels
        val shopKeeperLabels = ShopKeeper.definition.toContentDefinition("", false).optionLabels

        assertEquals("talk-to", bankerLabels[1])
        assertEquals("bank", bankerLabels[2])
        assertFalse(bankerLabels.containsKey(4))

        assertEquals("talk-to", shopKeeperLabels[1])
        assertEquals("trade", shopKeeperLabels[2])
        assertFalse(shopKeeperLabels.containsKey(4))
    }
}
