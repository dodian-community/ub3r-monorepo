package net.dodian.uber.game.content.commands

import java.util.concurrent.atomic.AtomicBoolean
import net.dodian.uber.game.content.commands.boss.BossCommands
import net.dodian.uber.game.content.commands.dev.BetaOnlyCommands
import net.dodian.uber.game.content.commands.dev.DevAccountAndStateCommands
import net.dodian.uber.game.content.commands.dev.DevDebugCommands
import net.dodian.uber.game.content.commands.dev.DevNpcContentCommands
import net.dodian.uber.game.content.commands.dev.DevSpawnAndNpcCommands
import net.dodian.uber.game.content.commands.dev.DevVisualCommands
import net.dodian.uber.game.content.commands.player.PlayerUtilityCommands
import net.dodian.uber.game.content.commands.social.SocialCommands
import net.dodian.uber.game.content.commands.staff.StaffModerationCommands
import net.dodian.uber.game.content.commands.staff.StaffWorldControlCommands
import net.dodian.uber.game.content.commands.travel.PositionCommands
import org.slf4j.LoggerFactory

object CommandContentRegistry {
    private val logger = LoggerFactory.getLogger(CommandContentRegistry::class.java)
    private val bootstrapped = AtomicBoolean(false)
    private val contents = mutableListOf<CommandContent>()

    @Volatile
    private var byAlias: Map<String, List<CommandDefinition>> = emptyMap()

    @JvmStatic
    fun bootstrap() {
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

    private fun defaultContents(): List<CommandContent> =
        listOf(
            DevDebugCommands,
            DevNpcContentCommands,
            DevVisualCommands,
            DevSpawnAndNpcCommands,
            DevAccountAndStateCommands,
            StaffModerationCommands,
            StaffWorldControlCommands,
            PlayerUtilityCommands,
            SocialCommands,
            BossCommands,
            PositionCommands,
            BetaOnlyCommands,
        )

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
