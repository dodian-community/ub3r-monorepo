package net.dodian.uber.game.libraries.commands.parsers

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.libraries.commands.interfaces.ICommandArgumentParser
import kotlin.reflect.KClass

class PrimitiveArgumentParsers(
    override val destination: KClass<Any> = Any::class,
    override val destinations: List<KClass<*>> = listOf(
        Int::class,
        Double::class,
        Float::class,
        Boolean::class,
        Long::class
    )
) : ICommandArgumentParser<Any> {

    private val logger = InlineLogger()

    override fun parse(input: String, type: KClass<*>?): Any? = when (type) {
        Int::class -> input.toIntOrNull()
        Double::class -> input.toDoubleOrNull()
        Float::class -> input.toFloatOrNull()
        Long::class -> input.toLongOrNull()
        Boolean::class -> {
            input.toBooleanStrictOrNull() ?: when (input.lowercase()) {
                "yes", "1" -> true
                "no", "0" -> false
                else -> null
            }
        }

        else -> {
            logger.warn {
                "No parser found for the given input and type (Input=$input, Type=${type?.qualifiedName}).\r\n" +
                        "Valid types are: ${destinations.map { it.qualifiedName }.joinToString(", ")}"
            }
            null
        }
    }
}