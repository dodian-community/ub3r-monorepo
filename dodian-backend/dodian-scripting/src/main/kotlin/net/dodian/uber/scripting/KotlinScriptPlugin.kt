package net.dodian.uber.scripting

import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Dodian Content Script",
    fileExtension = "content.kts"
)
abstract class KotlinScriptPlugin : ScriptPlugin()