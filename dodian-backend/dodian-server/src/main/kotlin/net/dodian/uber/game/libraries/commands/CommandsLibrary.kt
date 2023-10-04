package net.dodian.uber.game.libraries.commands

import com.github.michaelbull.logging.InlineLogger
import com.github.michaelbull.result.onFailure
import net.dodian.uber.game.libraries.commands.interfaces.ICommand
import net.dodian.uber.game.libraries.commands.interfaces.ICommandArgumentParser
import net.dodian.uber.game.libraries.commands.interfaces.ICommandsLibrary
import net.dodian.uber.game.libraries.commands.parsers.GeneralServerParsers
import net.dodian.uber.game.libraries.commands.parsers.PrimitiveArgumentParsers
import net.dodian.uber.game.modelkt.entity.player.Player
import kotlin.reflect.KClass

private val logger = InlineLogger()

class CommandsLibrary(
    override val libraryName: String = CommandsLibrary::class.java.simpleName,
    override val commands: MutableMap<String, ICommand<*>> = mutableMapOf(),
    private val argumentParsers: MutableList<ICommandArgumentParser<*>> = mutableListOf(
        PrimitiveArgumentParsers(),
        GeneralServerParsers()
    )
) : ICommandsLibrary {
    override fun registerCommand(vararg commands: ICommand<*>) {
        commands.forEach {
            logger.debug { "Registered command: ${it.name}" }
            this.commands[it.name] = it
        }
    }

    override fun findCommandForKey(key: String) = commands[key]

    override fun argumentParser(type: KClass<*>): ICommandArgumentParser<*>? =
        argumentParsers.firstOrNull {
            it.destination == type || (it.destinations != null && it.destinations!!.contains(
                type
            ))
        }

    override fun registerArgumentParser(parser: ICommandArgumentParser<*>): Boolean =
        argumentParsers.add(parser)

    override fun processCommand(player: Player, rawCommand: String) {
        val parts = rawCommand.split(" ")
        val name = parts.first()
        val args = parts.subList(1, parts.size)

        val command = findCommandForKey(name)
            ?: return player.sendMessage("Command '$name' couldn't be found.")

        if (command.permissions != null && command.permissions!!.intersect(player.rightsFlags.toSet()).isEmpty())
            return player.sendMessage(command.permissionMessage ?: "You don't have permission to use this command.")

        if (command.requiredParameters.size - 1 > args.size)
            return player.sendMessage(command.usage)

        command.execute(CommandSender(player), *args.toTypedArray())
            .onFailure { logger.error { it } }
    }
}