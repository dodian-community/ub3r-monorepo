package net.dodian.uber.game.content.dialogue.modules

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.netty.listener.out.Frame171
import net.dodian.uber.game.netty.listener.out.SendString

object SettingsDialogueModule : DialogueRenderModule {
    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Legacy.TOGGLE_SPECIALS) { c ->
            c.send(Frame171(1, 2465))
            c.send(Frame171(0, 2468))
            c.send(SendString("What would you like to do?", 2460))
            c.send(SendString("Enable specials", 2461))
            c.send(SendString("Disable specials", 2462))
            c.sendFrame164(2459)
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Legacy.TOGGLE_BOSS_YELL) { c ->
            c.send(Frame171(1, 2465))
            c.send(Frame171(0, 2468))
            c.send(SendString("What would you like to do?", 2460))
            c.send(SendString("Enable boss yell messages", 2461))
            c.send(SendString("Disable boss yell messages", 2462))
            c.sendFrame164(2459)
            c.NpcDialogueSend = true
            true
        }
    }
}
