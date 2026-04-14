package net.dodian.uber.game.npc

import net.dodian.uber.game.api.content.dialogue.DialogueEmote
import net.dodian.uber.game.api.content.dialogue.DialogueOption
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object Zahur : NpcModule {
    // Stats: 4753: r=0 a=0 d=0 s=0 hp=0 rg=0 mg=0

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 4753, x = 3424, y = 2908, z = 0, face = 0),
    )

    val npcIds: IntArray = npcIdsFromEntries(entries)


    override val definition = legacyNpcDefinition(
        name = "Zahur",
        entries = entries,
        onFirstClick = ::onFirstClick,
        onSecondClick = ::onSecondClick,
        onThirdClick = ::onThirdClick,
        onFourthClick = ::onFourthClick,
    )

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        DialogueService.start(client) {
            npcChat(npc.id, DialogueEmote.DEFAULT, "Hello ${if (client.gender == 1) "miss" else "mr"} adventurer.", "What can I help you with today?")
            options(
                title = "Select an option",
                DialogueOption("Visit the store") {
                    action { c -> c.WanneShop = 22 }
                    finish()
                },
                DialogueOption("Clean herbs") {
                    action { c -> HerbloreNpcDialogue.openHerbCleaner(c, npc.id) }
                    finish(closeInterfaces = false)
                },
                DialogueOption("Make unfinish potions") {
                    action { c -> HerbloreNpcDialogue.openUnfinishedPotionMaker(c, npc.id) }
                    finish(closeInterfaces = false)
                },
            )
        }
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSecondClick(client: Client, npc: Npc): Boolean {
        client.WanneShop = 39
        return true
    }

    fun onThirdClick(client: Client, npc: Npc): Boolean {
        HerbloreNpcDialogue.openHerbCleaner(client, npc.id)
        return true
    }

    fun onFourthClick(client: Client, npc: Npc): Boolean {
        HerbloreNpcDialogue.openUnfinishedPotionMaker(client, npc.id)
        return true
    }
}
