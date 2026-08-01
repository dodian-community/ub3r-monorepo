package net.dodian.uber.game.engine.systems.interaction.commands

import java.util.concurrent.atomic.AtomicBoolean
import net.dodian.uber.game.api.plugin.ContentModuleIndex
import net.dodian.uber.game.api.plugin.ContentBootstrap
import net.dodian.uber.game.engine.config.gameWorldId
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

private const val PLAYER_SOURCE = "command.player"

object CommandContentRegistry : ContentBootstrap {
    override val id: String = "commands.registry"
    private val logger = LoggerFactory.getLogger(CommandContentRegistry::class.java)
    private val bootstrapped = AtomicBoolean(false)
    private val contents = mutableListOf<CommandContent>()

    @Volatile
    private var byAlias: Map<String, List<CommandDefinition>> = emptyMap()

    /** Which source package(s) (command.player/.admin/.dev/.beta) registered each alias. */
    @Volatile
    private var aliasSources: Map<String, Set<String>> = emptyMap()

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

    /**
     * A command counts as "player-usable" only if EVERY source that registered its alias is
     * command.player.* - an alias also registered by command.admin/.dev/.beta (like "bank": a
     * regular TravelCommands.kt registration that's actually staff-gated on the live world) isn't
     * safe to call player-usable just because one of its handlers lives under command.player.
     */
    private fun logStartupSummary() {
        val playerOnly = aliasSources.filterValues { it == setOf(PLAYER_SOURCE) }.keys.sorted()
        val gated = aliasSources.size - playerOnly.size
        logger.info(
            "commands_ready: {} aliases from {} sources ({} player-usable, {} staff/dev/beta-gated - see source for exact rules)",
            aliasSources.size,
            contents.size,
            playerOnly.size,
            gated,
        )
        logger.info("player-usable commands ({}): {}", playerOnly.size, playerOnly.joinToString(", "))
    }

    /**
     * Called by CommandDispatcher right after a command handler succeeds. Flags the case the
     * source-package grouping can't catch on its own: a non-staff player, off any beta world,
     * successfully running a command whose alias is also registered under an
     * admin/dev/beta-only source - which should be impossible if every handler's own guard is
     * doing its job, so seeing this warning means one of them isn't.
     */
    fun warnIfSuspicious(client: Client, alias: String) {
        val sources = aliasSources[alias] ?: return
        val gatedSources = sources - PLAYER_SOURCE
        if (gatedSources.isEmpty()) return
        if (client.specialRights()) return
        if (gatedSources == setOf("command.beta") && gameWorldId > 1) return
        logger.warn(
            "SUSPICIOUS COMMAND ACCESS: player={} dbId={} alias={} world={} - non-staff player reached " +
                "a command also registered under {} (expected staff-only and/or beta-world-only there)",
            client.playerName,
            client.dbId,
            alias,
            gameWorldId,
            gatedSources,
        )
    }

    private fun Client.specialRights(): Boolean =
        net.dodian.uber.game.engine.config.rankAdminGroupIds.contains(playerGroup)

    private fun sourceFor(content: CommandContent): String {
        val pkg = content::class.java.packageName
        return when {
            pkg.endsWith(".command.player") -> PLAYER_SOURCE
            pkg.endsWith(".command.admin") -> "command.admin"
            pkg.endsWith(".command.dev") -> "command.dev"
            pkg.endsWith(".command.beta") -> "command.beta"
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
        val sources = HashMap<String, MutableSet<String>>()
        for (content in contents) {
            val source = sourceFor(content)
            for (definition in content.definitions()) {
                for (alias in definition.aliases) {
                    rebuilt.getOrPut(alias) { ArrayList() } += definition
                    sources.getOrPut(alias) { mutableSetOf() } += source
                }
            }
        }
        for ((alias, definitions) in rebuilt) {
            if (definitions.size > 1) {
                logger.debug("Registered {} command handlers for alias {}", definitions.size, alias)
            }
        }
        byAlias = rebuilt
        aliasSources = sources
    }
}
