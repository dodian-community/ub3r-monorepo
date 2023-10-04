package net.dodian.uber.game.libraries.commands.helpers

import net.dodian.context
import net.dodian.uber.game.libraries.commands.CommandBuilder
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

fun String.parseArgument(type: KClass<*>) = context.commandsLibrary.argumentParser(type)?.parse(this, type)

fun <A : KFunction<*>, R : Any> command(init: CommandBuilder<A>.() -> Unit) =
    CommandBuilder<A>().apply(init).build<R>()