package net.dodian.uber.game.npc

import net.dodian.uber.game.api.content.dialogue.DialogueEmote
import net.dodian.uber.game.api.content.dialogue.DialogueOption
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.objects.travel.EssenceMineTravel
import net.dodian.uber.game.shop.ShopId
import net.dodian.utilities.Utils

private val AuburyVarrockProfile = profile("aubury.varrock")
private val AuburyYanilleProfile = profile("aubury.yanille")

internal object Aubury : NpcFamily by npcFamily("Aubury", 11435, block = {
    profiles(
        AuburyVarrockProfile.key,
        AuburyYanilleProfile.key,
    )

    definition {
        examine = "Runes are his passion."


    }

    server {

        deathAnimation = 2304
    }

    options {
        talkTo(handler = ::handleAuburyTalkTo)
        third("trade", ::handleAuburyTrade)
        fourth("teleport", ::handleAuburyTeleport)
    }

    spawns {
        spawn(3253, 3402, walkRadius = 3, profile = AuburyVarrockProfile)
        spawn(2594, 3104, walkRadius = 3, profile = AuburyYanilleProfile)
    }
})

private fun handleAuburyTalkTo(client: Client, npc: Npc): Boolean {
    DialogueService.start(client) {
        npcChat(npc.id, DialogueEmote.EVIL1, "Do you want to buy some runes?")
        options(
            title = "Select an Option",
            DialogueOption("Yes please, show me your shop.") {
                finishThen {
                    it.openUpShopRouted(ShopId.AUBURYS_MAGIC_STORE.id)
                }
            },
            DialogueOption("Can you teleport me to the essence mine?") {
                finishThen {
                    EssenceMineTravel.sendToEssenceMine(it)
                }
            },
            DialogueOption("No thank you, then.") {
                playerChat(DialogueEmote.DEFAULT, "Oh it's a rune shop. No thank you, then.")
                npcChat(
                    npc.id,
                    DialogueEmote.DEFAULT,
                    "Well, if you find someone who does want runes, send them my way.",
                )
            },
        )
    }
    return true
}

@Suppress("UNUSED_PARAMETER")
private fun handleAuburyTrade(client: Client, npc: Npc): Boolean {
    client.openUpShopRouted(ShopId.AUBURYS_MAGIC_STORE.id)
    return true
}

private fun handleAuburyTeleport(client: Client, npc: Npc): Boolean {
    when (npc.interactionProfile) {
        AuburyYanilleProfile.key -> {
            client.triggerTele(
                3086 + Utils.random(2),
                3488 + Utils.random(2),
                0,
                false,
            )
        }
        else -> EssenceMineTravel.sendToEssenceMine(client)
    }
    return true
}
