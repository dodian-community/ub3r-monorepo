package net.dodian.uber.game.libraries

import net.dodian.uber.game.libraries.commands.interfaces.ICommand

interface ILibrary {
    val libraryName: String
    val commands: MutableMap<String, ICommand<*>>
}