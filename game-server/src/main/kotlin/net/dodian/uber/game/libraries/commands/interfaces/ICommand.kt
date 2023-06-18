package net.dodian.uber.game.libraries.commands.interfaces

import com.github.michaelbull.result.Result
import kotlin.reflect.KParameter

interface ICommand<R : Any> : ICommandDefinition<String> {
    val parameters: Map<String, KParameter>
    fun execute(vararg arguments: Any): Result<Any, Any>
    val requiredParameters get() = parameters.entries.filterNot { (name, param) -> param.isOptional || param.type.isMarkedNullable }
}