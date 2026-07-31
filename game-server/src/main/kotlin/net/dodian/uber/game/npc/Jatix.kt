package net.dodian.uber.game.npc

import net.dodian.uber.game.api.content.dialogue.DialogueEmote
import net.dodian.uber.game.api.content.dialogue.DialogueOption
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.shop.ShopId
import net.dodian.uber.skills.herblore.HerbloreModule

internal object Jatix : NpcFamily by npcFamily("Jatix", 8532, block = {
    definition {
        name = "Jatix"
        examine = "He knows about herblore"
    }

    server {
        deathAnimation = 2304
    }

    options {
        talkTo(handler = ::handleJatixTalkTo)
        third("trade", ::handleJatixTrade)
    }

    spawns {
        spawn(2897, 3428)
    }
})

private fun handleJatixTalkTo(client: Client, npc: Npc): Boolean {
    DialogueService.start(client) {
        npcChat(
            npc.id,
            DialogueEmote.DEFAULT,
            "Hello there adventurer ${client.playerName},",
            "is there anything you are looking for?",
        )
        playerChat(DialogueEmote.DEFAULT, "I heard you know alot about herblore.")
        npcChat(
            npc.id,
            DialogueEmote.DEFAULT,
            "For you ${client.playerName} I know the art of decanting.",
            "I can offer you to decant any noted potions",
            "that you will bring me, free of charge.",
            "I also got a herblore store if you wish to take a look.",
        )
        options(
            title = "What do you wish to do?",
            DialogueOption("Visit store") {
                finishThen {
                    it.openUpShopRouted(ShopId.JATIX_HERBLORE_STORE.id)
                }
            },
            DialogueOption("Decant potions") {
                finishThen {
                    HerbloreModule.openDecantMenu(it.asSkillPlayer())
                }
            },
            DialogueOption("Nevermind") {
                playerChat(DialogueEmote.DEFAULT, "Nevermind, I do not need anything.")
                finish()
            },
        )
    }
    return true
}

@Suppress("UNUSED_PARAMETER")
private fun handleJatixTrade(client: Client, npc: Npc): Boolean {
    client.openUpShopRouted(ShopId.JATIX_HERBLORE_STORE.id)
    return true
}
