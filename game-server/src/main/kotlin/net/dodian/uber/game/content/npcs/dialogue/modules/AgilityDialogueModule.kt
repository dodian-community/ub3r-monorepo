package net.dodian.uber.game.content.npcs.dialogue.modules

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.dialogue

/**
 * Handles:
 * - Dialogue IDs: 162, 163, 164
 * - NPC IDs: 6080
 */
object AgilityDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Agility.TICKET_GREETING) { c ->
            dialogue(c) {
                npcChat(c.NpcTalkTo, 591, "Fancy meeting you here maggot.", "If you have any agility ticket,", "I would gladly take them from you.")
                setNextDiag(DialogueIds.Agility.TICKET_OPTIONS)
            }
        }

        builder.handle(DialogueIds.Agility.TICKET_OPTIONS) { c ->
            c.send(net.dodian.uber.game.netty.listener.out.Frame171(1, 2465))
            c.send(net.dodian.uber.game.netty.listener.out.Frame171(0, 2468))
            c.send(net.dodian.uber.game.netty.listener.out.SendString("Trade in tickets or teleport to agility course?", 2460))
            c.send(net.dodian.uber.game.netty.listener.out.SendString("Trade in tickets.", 2461))
            c.send(net.dodian.uber.game.netty.listener.out.SendString("Another course, please.", 2462))
            c.sendFrame164(2459)
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Agility.COURSE_TRAVEL) { c ->
            val type = if (c.skillX == 3002 && c.skillY == 3931) 3 else if (c.skillX == 2547 && c.skillY == 3554) 2 else 1
            val typeGnome = arrayOf("Which course do you wish to be taken to?", "Barbarian", "Wilderness", "Stay here")
            val typeBarbarian = arrayOf("Which course do you wish to be taken to?", "Gnome", "Wilderness", "Stay here")
            val typeWilderness = arrayOf("Which course do you wish to be taken to?", "Gnome", "Barbarian", "Stay here")
            dialogue(c) {
                options(*(if (type == 3) typeWilderness else if (type == 2) typeBarbarian else typeGnome))
            }
        }
    }
}
