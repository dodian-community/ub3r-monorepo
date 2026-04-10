package net.dodian.uber.game.systems.ui.dialogue.core

import net.dodian.uber.game.systems.ui.dialogue.DialogueService
import net.dodian.uber.game.content.dialogue.BrimhavenEntryDialogueModule
import net.dodian.uber.game.content.dialogue.PyramidPlunderDialogueModule
import net.dodian.uber.game.content.dialogue.RockshellDialogueModule
import net.dodian.uber.game.content.dialogue.SettingsDialogueModule
import net.dodian.uber.game.content.npcs.DukeHoracio
import net.dodian.uber.game.content.npcs.PartyPete
import net.dodian.uber.game.content.npcs.unknown.UnknownNpc1597
import net.dodian.uber.game.content.npcs.Watcher
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
        PartyPete.registerLegacyDialogues(DialogueRegistry.Builder(this))
        include(SettingsDialogueModule)
        UnknownNpc1597.registerLegacyDialogues(DialogueRegistry.Builder(this))
        Watcher.registerLegacyDialogues(DialogueRegistry.Builder(this))
        include(BrimhavenEntryDialogueModule)
        DukeHoracio.registerLegacyDialogues(DialogueRegistry.Builder(this))
        include(RockshellDialogueModule)
        include(PyramidPlunderDialogueModule)
    }.build()

    @JvmStatic
    fun render(client: Client): Boolean {
        val handler = handlers[DialogueService.currentDialogueId(client)] ?: return false
        return handler.render(client)
    }
}
