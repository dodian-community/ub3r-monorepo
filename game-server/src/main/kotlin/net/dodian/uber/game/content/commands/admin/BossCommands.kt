package net.dodian.uber.game.content.commands.admin

import net.dodian.uber.game.engine.systems.interaction.commands.CommandDefinition

@Deprecated("Use net.dodian.uber.game.command.admin.BossCommands")
object BossCommands {
    fun definitions(): List<CommandDefinition> = net.dodian.uber.game.command.admin.BossCommands.definitions()
}
