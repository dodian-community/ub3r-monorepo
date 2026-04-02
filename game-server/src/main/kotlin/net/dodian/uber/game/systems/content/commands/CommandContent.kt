package net.dodian.uber.game.systems.content.commands

fun interface CommandHandler {
    fun handle(context: CommandContext): Boolean
}

data class CommandDefinition(
    val aliases: Set<String> = emptySet(),
    val handler: CommandHandler,
)

interface CommandContent {
    fun definitions(): List<CommandDefinition>
}

class CommandDsl {
    private val definitions = ArrayList<CommandDefinition>()

    fun command(vararg aliases: String, handler: CommandContext.() -> Boolean) {
        definitions += CommandDefinition(
            aliases = aliases.map { it.lowercase() }.toSet(),
            handler = CommandHandler { context -> context.handler() },
        )
    }

    fun build(): List<CommandDefinition> = definitions
}

fun commands(block: CommandDsl.() -> Unit): List<CommandDefinition> {
    val dsl = CommandDsl()
    dsl.block()
    return dsl.build()
}
