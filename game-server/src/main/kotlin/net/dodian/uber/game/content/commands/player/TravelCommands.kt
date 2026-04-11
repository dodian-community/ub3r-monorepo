package net.dodian.uber.game.content.commands.player

import net.dodian.uber.game.engine.systems.interaction.commands.CommandDefinition

@Deprecated("Use net.dodian.uber.game.command.player.TravelCommands")
object TravelCommands {
    fun definitions(): List<CommandDefinition> = net.dodian.uber.game.command.player.TravelCommands.definitions()
}
