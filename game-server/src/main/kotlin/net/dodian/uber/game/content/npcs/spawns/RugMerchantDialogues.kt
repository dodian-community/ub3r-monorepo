package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueDebug
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.dialogue
import net.dodian.uber.game.model.entity.player.Client

/**
 * Handles:
 * - Dialogue IDs: 17, 18, 19, 20
 * - NPC IDs: 17, 19, 20, 22
 */
object CarpetDialogueModule : DialogueRenderModule {

    private val expectedNpcIds = setOf(17, 19, 20, 22)

    override fun register(builder: net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Carpet.GREETING) { c ->
            DialogueDebug.warnUnexpectedNpc("CarpetDialogueModule", c, expectedNpcIds)
            dialogue(c) {
                npcChat(c.NpcTalkTo, 599, "Hello there Traveler.", "Do you fancy taking a carpet ride?", "It will cost 5000 coins.")
                setNextDiag(DialogueIds.Carpet.PLAYER_QUERY)
            }
        }

        builder.handle(DialogueIds.Carpet.PLAYER_QUERY) { c ->
            dialogue(c) {
                playerChat(615, "Where can your carpet take me to?")
                setNextDiag(DialogueIds.Carpet.DESTINATION_INFO)
            }
        }

        builder.handle(DialogueIds.Carpet.DESTINATION_INFO) { c ->
            dialogue(c) {
                npcChat(c.NpcTalkTo, 598, "These are currently the places.")
                setNextDiag(DialogueIds.Carpet.DESTINATION_OPTIONS)
            }
        }

        builder.handle(DialogueIds.Carpet.DESTINATION_OPTIONS) { c ->
            val carpetBase = arrayOf("Carpet rides cost 5k coins.", "", "", "", "Cancel")
            val carpetOptions = when (c.NpcTalkTo) {
                17 -> arrayOf(carpetBase[0], "Pollnivneach", "Nardah", "Bedabin Camp", carpetBase[4])
                19 -> arrayOf(carpetBase[0], "Pollnivneach", "Nardah", "Sophanem", carpetBase[4])
                20 -> arrayOf(carpetBase[0], "Nardah", "Bedabin Camp", "Sophanem", carpetBase[4])
                22 -> arrayOf(carpetBase[0], "Pollnivneach", "Sophanem", "Bedabin Camp", carpetBase[4])
                else -> null
            }

            dialogue(c) {
                if (carpetOptions != null) {
                    options(*carpetOptions)
                } else {
                    close()
                }
                markSent(carpetOptions != null)
            }
        }
    }
}
