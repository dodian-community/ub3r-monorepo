package net.dodian.uber.game.api.interaction

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.objects.ObjectContent
import net.dodian.uber.game.objects.ObjectContentBuilder
import net.dodian.uber.game.objects.objectContent
import net.dodian.uber.game.npc.npcFamily
import net.dodian.uber.game.npc.NpcContentDefinition
import net.dodian.uber.game.npc.handleFirstClick
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.systems.world.npc.NpcManager
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.testutil.RequiresCache

class InteractionContextTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupAll() {
            RequiresCache.assume()
            Server.npcManager = NpcManager()
            val coins = net.dodian.uber.game.model.item.Item(
                id = 995,
                name = "Coins",
                slot = -1,
                standAnim = 0,
                walkAnim = 0,
                runAnim = 0,
                attackAnim = 0,
                shopSellValue = 0,
                shopBuyValue = 0,
                bonuses = IntArray(12),
                stackable = true,
                noteable = false,
                tradeable = true,
                twoHanded = false,
                full = false,
                mask = false,
                premium = false,
                examine = "Lovely coins",
                alchemy = 0
            )
            Server.itemManager = ItemManager({ mapOf(995 to coins) }, {})
        }
    }

    @Test
    fun `object DSL context callback executes successfully`() {
        var called = false
        val content = objectContent {
            bind(100)
            onFirstClick { ctx ->
                assertEquals(100, ctx.objectId)
                called = true
                true
            }
        }
        val client = Client(null, 1)
        val context = ObjectInteractionContext(
            player = client,
            option = InteractionOption.FIRST,
            objectId = 100,
            position = Position(10, 10, 0),
            definition = null
        )
        val result = content.onFirstClick(context)
        assertTrue(called)
        assertTrue(result)
    }

    @Test
    fun `npc DSL context callback executes successfully`() {
        var called = false
        val family = npcFamily("Giant", 55) {
            options {
                first("talk-to") { ctx ->
                    assertEquals(55, ctx.npc.id)
                    called = true
                    true
                }
            }
        }
        val npc = Npc(1, 55, Position(0, 0, 0), 0)
        val client = Client(null, 1)
        val context = NpcInteractionContext(client, InteractionOption.FIRST, npc)
        val result = family.definition.handleFirstClick(context)
        assertTrue(called)
        assertTrue(result)
    }
}
