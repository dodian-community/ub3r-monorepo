package net.dodian.uber.game.engine.systems.interaction.commands

import net.dodian.uber.game.model.entity.player.Client

object CommandDispatcher {
    @JvmStatic
    fun dispatch(client: Client, rawCommand: String): Boolean {
        val parts = rawCommand.split(" ").filter { it.isNotEmpty() }
        val context = CommandContext(client, rawCommand, parts)
        if (context.alias.isEmpty()) {
            return false
        }
        for (definition in CommandContentRegistry.definitionsFor(context.alias)) {
            if (definition.handler.handle(context)) {
                return true
            }
        }
        return false
    }
}
