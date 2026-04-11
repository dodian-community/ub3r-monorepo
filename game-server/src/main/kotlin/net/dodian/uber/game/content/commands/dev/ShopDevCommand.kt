package net.dodian.uber.game.content.commands.dev

import net.dodian.uber.game.engine.systems.interaction.commands.CommandDefinition

@Deprecated("Use net.dodian.uber.game.command.dev.ShopDevCommand")
object ShopDevCommand {
    fun definitions(): List<CommandDefinition> = net.dodian.uber.game.command.dev.ShopDevCommand.definitions()

    internal fun renderSummaryForTests(): String = net.dodian.uber.game.command.dev.ShopDevCommand.renderSummaryForTests()
}
