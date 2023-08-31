package net.dodian.server.scripting

import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Dodian Script",
    fileExtension = "plugin.kts"
)
abstract class KotlinScriptPlugin : ScriptPlugin()