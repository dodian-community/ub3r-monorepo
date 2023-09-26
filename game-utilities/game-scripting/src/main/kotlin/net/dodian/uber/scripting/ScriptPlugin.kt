package net.dodian.uber.scripting

open class ScriptPlugin(
    pluginName: String? = null
) {
    open val name: String = pluginName ?: (this::class.simpleName ?: error("Couldn't set plugin name"))
}