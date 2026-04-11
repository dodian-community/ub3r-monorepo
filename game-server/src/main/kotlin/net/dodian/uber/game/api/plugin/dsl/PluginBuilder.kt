package net.dodian.uber.game.api.plugin.dsl

/**
 * Top-level DSL function to define plugin metadata.
 *
 * Usage (typically in a plugin's top-level file):
 * ```kotlin
 * val metadata = plugin {
 *     name = "Woodcutting"
 *     description = "Handles all woodcutting interactions."
 *     version = "1.0.0"
 *     authors += "Dodian Team"
 * }
 * ```
 */
fun plugin(block: PluginMetadata.() -> Unit): PluginMetadata {
    val metadata = PluginMetadata()
    block(metadata)
    metadata.validate()
    return metadata
}

