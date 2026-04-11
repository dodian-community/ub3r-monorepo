@file:Suppress("unused")

package net.dodian.uber.game.command

typealias CommandDefinition = net.dodian.uber.game.engine.systems.interaction.commands.CommandDefinition
typealias CommandContent = net.dodian.uber.game.engine.systems.interaction.commands.CommandContent
typealias CommandContext = net.dodian.uber.game.engine.systems.interaction.commands.CommandContext
typealias CommandDsl = net.dodian.uber.game.engine.systems.interaction.commands.CommandDsl

fun commands(block: CommandDsl.() -> Unit): List<CommandDefinition> =
    net.dodian.uber.game.engine.systems.interaction.commands.commands(block)

