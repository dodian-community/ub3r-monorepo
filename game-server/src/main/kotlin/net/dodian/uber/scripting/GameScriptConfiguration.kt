package net.dodian.uber.scripting

import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm

class GameScriptCompilationConfiguration : ScriptCompilationConfiguration({
    defaultImports(
        "net.dodian.utilities.RightsFlag",
        "net.dodian.uber.game.libraries.commands.helpers.command"
    )
    jvm {
        dependenciesFromClassContext(GameScriptCompilationConfiguration::class, wholeClasspath = true)
        compilerOptions(
            "-Xopt-in=kotlin.time.ExperimentalTime,kotlin.ExperimentalStdlibApi",
            "-jvm-target", "11"
        )
    }
    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
})