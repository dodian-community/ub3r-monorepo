package net.dodian.uber.game.libraries.commands.helpers

import net.dodian.context
import net.dodian.uber.game.libraries.commands.interfaces.ICommand
import net.dodian.uber.game.libraries.commands.interfaces.ICommandArgumentParser

fun ICommand<*>.register() {
    context.commandsLibrary.registerCommand(this)
}

fun ICommandArgumentParser<*>.register() {
    context.commandsLibrary.registerArgumentParser(this)
}