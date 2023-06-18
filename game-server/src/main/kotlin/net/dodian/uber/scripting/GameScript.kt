package net.dodian.uber.scripting

import net.dodian.uber.game.libraries.commands.CommandBuilder
import kotlin.reflect.KFunction
import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Ub3r Script",
    fileExtension = "ub3r.kts",
    compilationConfiguration = GameScriptCompilationConfiguration::class
)
class GameScript {

    fun myShit() = println("Yes")

    fun <A : KFunction<*>, R : Any> command(init: CommandBuilder<A>.() -> Unit) =
        CommandBuilder<A>().apply(init).build<R>()
}