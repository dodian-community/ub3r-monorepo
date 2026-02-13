package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueDebug
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.dialogue
import net.dodian.uber.game.netty.listener.out.SetTabInterface

/**
 * Handles:
 * - Dialogue IDs: 21, 22, 23, 24, 25
 * - NPC IDs: 1306, 1307
 */
object AppearanceDialogueModule : DialogueRenderModule {

    private val expectedNpcIds = setOf(1306, 1307)

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Appearance.MAKEOVER_GREETING) { c ->
            DialogueDebug.warnUnexpectedNpc("AppearanceDialogueModule", c, expectedNpcIds)
            dialogue(c) {
                npcChat(
                    if (c.gender == 0) 1306 else 1307,
                    588,
                    "Hello there, would you like to change your looks?",
                    "If so, it will be free of charge"
                )
            }
        }

        builder.handle(DialogueIds.Appearance.MAKEOVER_OPTIONS) { c ->
            dialogue(c) {
                options("Would you like to change your looks?", "Sure", "No thanks")
            }
        }

        builder.handle(DialogueIds.Appearance.MAKEOVER_ACCEPT) { c ->
            dialogue(c) {
                playerChat(614, "I would love that.")
            }
        }

        builder.handle(DialogueIds.Appearance.MAKEOVER_DECLINE) { c ->
            dialogue(c) {
                playerChat(614, "Not at the moment.")
            }
        }

        builder.handle(DialogueIds.Appearance.MAKEOVER_OPEN_INTERFACE) { c ->
            c.send(SetTabInterface(3559, 3213))
            c.NpcDialogue = 0
            c.NpcDialogueSend = false
            true
        }
    }
}
