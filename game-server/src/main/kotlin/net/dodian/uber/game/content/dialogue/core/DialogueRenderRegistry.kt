package net.dodian.uber.game.content.dialogue.core

import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.dialogue.modules.BrimhavenEntryDialogueModule
import net.dodian.uber.game.content.dialogue.modules.PyramidPlunderDialogueModule
import net.dodian.uber.game.content.dialogue.modules.RockshellDialogueModule
import net.dodian.uber.game.content.dialogue.modules.SettingsDialogueModule
import net.dodian.uber.game.content.npc.DukeHoracio
import net.dodian.uber.game.content.npc.PartyPete
import net.dodian.uber.game.content.npc.UnknownNpc1597
import net.dodian.uber.game.content.npc.Watcher
import net.dodian.uber.game.model.entity.player.Client

object DialogueRenderRegistry {

    class Builder {
        private val handlers = mutableMapOf<Int, DialogueRenderHandler>()

        fun handle(dialogueId: Int, handler: DialogueRenderHandler) {
            require(!handlers.containsKey(dialogueId)) {
                "Duplicate dialogue render handler registered for dialogueId=$dialogueId"
            }
            handlers[dialogueId] = handler
        }

        fun include(module: DialogueRenderModule) {
            module.register(this)
        }

        internal fun build(): Map<Int, DialogueRenderHandler> = handlers.toMap()
    }

    private val handlers: Map<Int, DialogueRenderHandler> = Builder().apply {
        PartyPete.registerDialogueRenders(this)
        include(SettingsDialogueModule)
        UnknownNpc1597.registerDialogueRenders(this)
        Watcher.registerDialogueRenders(this)
        include(BrimhavenEntryDialogueModule)
        DukeHoracio.registerDialogueRenders(this)
        include(RockshellDialogueModule)
        include(PyramidPlunderDialogueModule)
    }.build()

    @JvmStatic
    fun render(client: Client): Boolean {
        val handler = handlers[DialogueService.currentDialogueId(client)] ?: return false
        return handler.render(client)
    }
}
