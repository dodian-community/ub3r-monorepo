package net.dodian.uber.game.libraries.commands.helpers

import net.dodian.uber.commandsLibrary
import net.dodian.uber.game.libraries.commands.interfaces.ICommand
import net.dodian.uber.game.libraries.commands.interfaces.ICommandArgumentParser
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

fun ICommand<*>.register() {
    commandsLibrary.registerCommand(this)
}

fun ICommandArgumentParser<*>.register() {
    commandsLibrary.registerArgumentParser(this)
}