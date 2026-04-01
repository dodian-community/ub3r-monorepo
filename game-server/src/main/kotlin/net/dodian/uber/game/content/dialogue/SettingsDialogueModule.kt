package net.dodian.uber.game.content.dialogue

import net.dodian.uber.game.systems.ui.dialogue.DialogueService
import net.dodian.uber.game.systems.ui.dialogue.core.DialogueIds
import net.dodian.uber.game.systems.ui.dialogue.core.DialogueRenderRegistry
import net.dodian.uber.game.systems.ui.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.netty.listener.out.Frame171
import net.dodian.uber.game.netty.listener.out.SendString

object SettingsDialogueModule : DialogueRenderModule {
    override fun register(builder: DialogueRenderRegistry.Builder) {
        builder.handle(DialogueIds.Classic.TOGGLE_SPECIALS) { c ->
            c.send(Frame171(1, 2465))
            c.send(Frame171(0, 2468))
            c.sendString("What would you like to do?", 2460)
            c.sendString("Enable specials", 2461)
            c.sendString("Disable specials", 2462)
            c.sendChatboxInterface(2459)
            DialogueService.setDialogueSent(c, true)
            true
        }

        builder.handle(DialogueIds.Classic.TOGGLE_BOSS_YELL) { c ->
            c.send(Frame171(1, 2465))
            c.send(Frame171(0, 2468))
            c.sendString("What would you like to do?", 2460)
            c.sendString("Enable boss yell messages", 2461)
            c.sendString("Disable boss yell messages", 2462)
            c.sendChatboxInterface(2459)
            DialogueService.setDialogueSent(c, true)
            true
        }
    }
}
