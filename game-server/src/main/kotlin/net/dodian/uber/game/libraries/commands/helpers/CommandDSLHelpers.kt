package net.dodian.uber.game.libraries.commands.helpers

import net.dodian.uber.commandsLibrary
import net.dodian.uber.game.libraries.commands.CommandBuilder
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

fun String.parseArgument(type: KClass<*>) = commandsLibrary.argumentParser(type)?.parse(this, type)

fun <A : KFunction<*>, R : Any> command(init: CommandBuilder<A>.() -> Unit) =
    CommandBuilder<A>().apply(init).build<R>()