package net.dodian.uber.game.engine.systems.interaction.commands

import java.util.concurrent.atomic.AtomicBoolean
import net.dodian.uber.game.api.plugin.ContentModuleIndex
import net.dodian.uber.game.api.plugin.ContentBootstrap
import org.slf4j.LoggerFactory

object CommandContentRegistry : ContentBootstrap {
    override val id: String = "commands.registry"
    private val logger = LoggerFactory.getLogger(CommandContentRegistry::class.java)
    private val bootstrapped = AtomicBoolean(false)
    private val contents = mutableListOf<CommandContent>()

    @Volatile
    private var byAlias: Map<String, List<CommandDefinition>> = emptyMap()

    override fun bootstrap() {
        if (bootstrapped.get()) {
            return
        }
        synchronized(this) {
            if (bootstrapped.get()) {
                return
            }
            contents += defaultContents()
            rebuildLocked()
            bootstrapped.set(true)
        }
        logStartupSummary()
    }

    // Grouped by the registering CommandContent's package (command.player/.admin/.dev/.beta),
    // which already matches the codebase's own rank/environment convention 1:1 - see StaffCommands
    // (playerRights checks), the command.dev.* files (dev-tooling commands), and BetaOnlyCommands
    // (world > 1 only). Each command still enforces its own rank/env gate at call time; this is a
    // startup-time index of what's registered, not a live per-player availability check.
    private fun logStartupSummary() {
        val tiers = linkedMapOf(
            "player" to sortedSetOf<String>(),
            "staff" to sortedSetOf(),
            "dev" to sortedSetOf(),
            "beta" to sortedSetOf(),
            "other" to sortedSetOf(),
        )
        for (content in contents) {
            val aliases = content.definitions().flatMap { it.aliases }
            tiers.getValue(tierFor(content)) += aliases
        }
        logger.info("commands_ready: {} aliases from {} sources", byAlias.size, contents.size)
        for ((tier, aliases) in tiers) {
            if (aliases.isEmpty()) continue
            logger.info("  {} ({}): {}", tier, aliases.size, aliases.joinToString(", "))
        }
    }

    private fun tierFor(content: CommandContent): String {
        val pkg = content::class.java.packageName
        return when {
            pkg.endsWith(".command.player") -> "player"
            pkg.endsWith(".command.admin") -> "staff"
            pkg.endsWith(".command.dev") -> "dev"
            pkg.endsWith(".command.beta") -> "beta"
            else -> "other"
        }
    }

    fun register(content: CommandContent) {
        synchronized(this) {
            contents += content
            if (bootstrapped.get()) {
                rebuildLocked()
            }
        }
    }

    fun definitionsFor(alias: String): List<CommandDefinition> {
        bootstrap()
        return byAlias[alias].orEmpty()
    }

    internal fun resetForTests(vararg replacement: CommandContent) {
        synchronized(this) {
            contents.clear()
            if (replacement.isEmpty()) {
                contents += defaultContents()
            } else {
                contents += replacement
            }
            rebuildLocked()
            bootstrapped.set(true)
        }
    }

    private fun defaultContents(): List<CommandContent> = ContentModuleIndex.commandContents

    private fun rebuildLocked() {
        val rebuilt = LinkedHashMap<String, MutableList<CommandDefinition>>()
        for (content in contents) {
            for (definition in content.definitions()) {
                for (alias in definition.aliases) {
                    rebuilt.getOrPut(alias) { ArrayList() } += definition
                }
            }
        }
        for ((alias, definitions) in rebuilt) {
            if (definitions.size > 1) {
                logger.debug("Registered {} command handlers for alias {}", definitions.size, alias)
            }
        }
        byAlias = rebuilt
    }
}