package net.dodian.uber.game.libraries.commands.interfaces

import kotlin.reflect.KClass

interface ICommandArgumentParser<DESTINATION : Any> {
    val destination: KClass<DESTINATION>
    val destinations: List<KClass<*>>?
    fun parse(input: String, type: KClass<*>? = null): DESTINATION?
}