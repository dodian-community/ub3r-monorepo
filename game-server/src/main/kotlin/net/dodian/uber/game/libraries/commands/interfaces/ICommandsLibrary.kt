package net.dodian.uber.game.libraries.commands.interfaces

import net.dodian.uber.game.libraries.ILibrary
import net.dodian.uber.game.libraries.commands.Player
import kotlin.reflect.KClass

interface ICommandsLibrary : ILibrary {
    fun registerCommand(vararg commands: ICommand<*>)
    fun findCommandForKey(key: String): ICommand<*>?
    fun argumentParser(type: KClass<*>): ICommandArgumentParser<*>?
    fun registerArgumentParser(parser: ICommandArgumentParser<*>): Boolean
    fun processCommand(player: Player, rawCommand: String)
}