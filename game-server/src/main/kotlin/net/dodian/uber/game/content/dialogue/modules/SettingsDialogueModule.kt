package net.dodian.uber.game.content.dialogue.modules

import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.dialogue.core.DialogueIds
import net.dodian.uber.game.content.dialogue.core.DialogueRenderRegistry
import net.dodian.uber.game.content.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.netty.listener.out.Frame171
import net.dodian.uber.game.netty.listener.out.SendString

object SettingsDialogueModule : DialogueRenderModule {
    override fun register(builder: DialogueRenderRegistry.Builder) {
        builder.handle(DialogueIds.Classic.TOGGLE_SPECIALS) { c ->
            c.send(Frame171(1, 2465))
            c.send(Frame171(0, 2468))
            c.send(SendString("What would you like to do?", 2460))
            c.send(SendString("Enable specials", 2461))
            c.send(SendString("Disable specials", 2462))
            c.sendFrame164(2459)
            DialogueService.setDialogueSent(c, true)
            true
        }

        builder.handle(DialogueIds.Classic.TOGGLE_BOSS_YELL) { c ->
            c.send(Frame171(1, 2465))
            c.send(Frame171(0, 2468))
            c.send(SendString("What would you like to do?", 2460))
            c.send(SendString("Enable boss yell messages", 2461))
            c.send(SendString("Disable boss yell messages", 2462))
            c.sendFrame164(2459)
            DialogueService.setDialogueSent(c, true)
            true
        }
    }
}
