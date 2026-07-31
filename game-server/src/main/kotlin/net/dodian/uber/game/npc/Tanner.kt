package net.dodian.uber.game.npc

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.shop.ShopId

internal object Tanner : NpcFamily by npcFamily("Tanner", 5809, block = {

    server {
        deathAnimation = 2304
    }

    // talkTo's handler below is dead code in practice - CraftingModule's own
    // npcClick(option=1, 5809) binding is tried first and always wins (see CraftingModule.kt) -
    // but the registration itself must stay: it's what puts "talk-to" in the client's right-click
    // menu at all (NpcFamilyDsl's optionLabels), independent of which handler actually runs.
    options {
        talkTo(handler = ::handleTannerTan)
        third("trade", ::handleTannerTrade)
    }

    spawns {
        spawn(2711, 3478)
    }
})

// Unreachable in practice - CraftingModule's npcClick(option=1, 5809) binding always wins first
// (SkillInteractionDispatcher is tried before NpcContentRegistry). Kept only so talkTo() above
// has a handler to register, which is what makes the "talk-to" menu option exist for the client.
@Suppress("UNUSED_PARAMETER")
private fun handleTannerTan(client: Client, npc: Npc): Boolean = true

@Suppress("UNUSED_PARAMETER")
private fun handleTannerTrade(client: Client, npc: Npc): Boolean {
    client.openUpShopRouted(ShopId.CRAFTING_STORE.id)
    return true
}
