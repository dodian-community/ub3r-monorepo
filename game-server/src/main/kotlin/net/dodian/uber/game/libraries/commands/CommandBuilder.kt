package net.dodian.uber.game.libraries.commands

import com.github.michaelbull.logging.InlineLogger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import net.dodian.uber.commandsLibrary
import net.dodian.uber.game.libraries.commands.helpers.parseArgument
import net.dodian.uber.game.libraries.commands.interfaces.ICommand
import net.dodian.utilities.RightsFlag
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.jvmErasure

private val logger = InlineLogger()

class CommandBuilder<A : KFunction<*>>(
    var parent: ICommand<*>? = null,
    var name: String? = null,
    var aliases: List<String>? = null,
    var description: String? = null,
    var permissions: List<RightsFlag>? = null,
    var permissionMessage: String? = null,
    var onCommand: A? = null
) {

    fun <T : Any> build(): ICommand<T> {
        val parameters = onCommand!!.parameters.associateBy { it.name!! }

        var usage = "::${name}"

        onCommand!!.parameters.subList(1, onCommand!!.parameters.size)
            .associateBy { it.name!! }
            .forEach { (name, param) ->
                val open: String
                val close: String
                when {
                    param.type.isMarkedNullable || param.isOptional -> {
                        open = "["
                        close = "*]"
                    }

                    else -> {
                        open = "["
                        close = "]"
                    }
                }

                usage += " $open$name: ${param.type.jvmErasure.simpleName}$close"
            }

        myLoop@ for (parameter in parameters.values) {
            break@myLoop
        }

        val command = object : CommandBase<T>(
            name = name ?: error("A command needs to have a name"),
            aliases = aliases ?: emptyList(),
            description = description ?: "No description provided...",
            usage = usage,
            permissions = permissions,
            permissionMessage = permissionMessage ?: "You don't have permission to use this command"
        ) {
            override val parameters: Map<String, KParameter> = parameters

            override val defaultArgumentValues: Map<Int, Any> = mapOf()

            override fun execute(vararg arguments: Any): Result<Any, Any> {
                val finalArguments = mutableListOf<Any?>()
                parameters.entries.forEachIndexed { index, (name, param) ->
                    val argument = if (index in arguments.indices) arguments[index] else null

                    val paramType = param.type.jvmErasure
                    val typeName = paramType.simpleName

                    when {
                        argument == null -> finalArguments.add(null)
                        argument::class == paramType -> finalArguments.add(argument)
                        argument is String -> {
                            val finalArgument = argument.parseArgument(paramType)
                                ?: return Err("Failed to parse argument ($typeName: $argument)")

                            finalArguments.add(finalArgument)
                        }
                        else -> return Err(
                            "'${argument::class.simpleName}' is not valid for this argument (${name}: ${typeName})"
                        )
                    }
                }

                println(finalArguments.toList())
                logger.debug { finalArguments.toList() }
                onCommand!!.call(*finalArguments.toTypedArray())
                return Ok(true)
            }
        }

        commandsLibrary.registerCommand(command)
        return command
    }
}