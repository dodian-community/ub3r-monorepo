package net.dodian.uber.game.content.npc

import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object Banker {
    val definition = npcPlugin("Banker") {
        spawns(BankerGenerated.entries)
        ids(395, 7677)
        options {
            first("talk-to") { client, npc ->
                DialogueService.start(client) {
                    npcChat(npc.id, DialogueEmote.DEFAULT, "Good day, how can I help you?")
                    options(
                        title = "What would you like to say?",
                        DialogueOption("I'd like to access my bank account, please.") {
                            action { c -> c.openUpBank() }
                            finish(closeInterfaces = false)
                        },
                        DialogueOption("I'd like to check my PIN settings.") {
                            npcChat(npc.id, DialogueEmote.DEFAULT, "Pins have not been implemented yet.")
                            finish()
                        },
                    )
                }
                true
            }
            second("bank") { client, _ ->
                client.openUpBankRouted()
                true
            }
        }
    }

    // Compatibility surface for legacy reflection path if needed during staged migration.
    val entries: List<NpcSpawnDef> = definition.entries
    val npcIds: IntArray = definition.npcIds

    fun onFirstClick(client: Client, npc: Npc): Boolean = definition.optionBindings
        .firstOrNull { it.slot == NpcOptionSlot.FIRST }
        ?.handler
        ?.invoke(client, npc) == true

    fun onSecondClick(client: Client, npc: Npc): Boolean = definition.optionBindings
        .firstOrNull { it.slot == NpcOptionSlot.SECOND }
        ?.handler
        ?.invoke(client, npc) == true
}
