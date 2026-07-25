package net.dodian.uber.game.api.plugin

import net.dodian.uber.game.api.plugin.ContentBootstrap
import net.dodian.uber.game.api.plugin.ContentModuleIndex
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginRegistryEngine
import net.dodian.uber.game.api.plugin.skills.SkillPluginSnapshot
import net.dodian.uber.game.api.plugin.skills.SkillSupportModule
import net.dodian.uber.game.api.plugin.skills.SkillSupportRegistryEngine
import net.dodian.uber.game.api.plugin.skills.SkillSupportSnapshot
import net.dodian.uber.game.api.plugin.skills.routeKeys
import net.dodian.uber.game.api.plugin.quests.QuestPlugin
import net.dodian.uber.game.api.plugin.quests.QuestPluginRegistryEngine
import net.dodian.uber.game.api.plugin.quests.QuestPluginSnapshot

object PluginRegistry : ContentBootstrap {
    override val id: String = "plugins.registry"

    private enum class Lifecycle {
        DISCOVER,
        VALIDATE,
        BOOTSTRAP,
        FROZEN,
    }

    private val skills = SkillPluginRegistryEngine()
    private val support = SkillSupportRegistryEngine()
    private val quests = QuestPluginRegistryEngine()
    private val lock = Any()
    @Volatile
    private var lifecycle: Lifecycle = Lifecycle.DISCOVER
    @Volatile
    private var discoveredSkillPlugins: List<SkillPlugin> = emptyList()
    @Volatile
    private var discoveredSkillSupportModules: List<SkillSupportModule> = emptyList()
    @Volatile
    private var discoveredQuestPlugins: List<QuestPlugin> = emptyList()

    fun discover() {
        if (lifecycle != Lifecycle.DISCOVER) return
        synchronized(lock) {
            if (lifecycle != Lifecycle.DISCOVER) return
            val discoveredFromIndex = ContentModuleIndex.skillPlugins
            discoveredSkillPlugins =
                (discoveredSkillPlugins + discoveredFromIndex)
                    .distinctBy { it::class.java.name }
                    .sortedBy { it::class.java.name }
            discoveredSkillSupportModules =
                (discoveredSkillSupportModules + ContentModuleIndex.skillSupportModules)
                    .distinctBy { it::class.java.name }
                    .sortedBy { it::class.java.name }
            discoveredQuestPlugins =
                (discoveredQuestPlugins + ContentModuleIndex.questPlugins)
                    .distinctBy { it::class.java.name }
                    .sortedBy { it::class.java.name }
            lifecycle = Lifecycle.VALIDATE
        }
    }

    fun validate() {
        if (lifecycle == Lifecycle.FROZEN || lifecycle == Lifecycle.BOOTSTRAP) return
        discover()
        synchronized(lock) {
            if (lifecycle == Lifecycle.FROZEN || lifecycle == Lifecycle.BOOTSTRAP) return
            skills.validate(discoveredSkillPlugins)
            val gameplayRoutes = discoveredSkillPlugins.flatMap { it.definition.routeKeys() }.toSet()
            val supportRoutes = discoveredSkillSupportModules.flatMap { it.definition.routeKeys() }
            require(supportRoutes.none { it in gameplayRoutes }) { "A skill support route is also owned by gameplay: ${supportRoutes.first { it in gameplayRoutes }}" }
            quests.validate(discoveredQuestPlugins)
            lifecycle = Lifecycle.BOOTSTRAP
        }
    }

    override fun bootstrap() {
        if (lifecycle == Lifecycle.FROZEN) return
        validate()
        synchronized(lock) {
            if (lifecycle == Lifecycle.FROZEN) return
            skills.bootstrap(discoveredSkillPlugins)
            support.bootstrap(discoveredSkillSupportModules)
            quests.bootstrap(discoveredQuestPlugins)
            skills.freeze()
            quests.freeze()
            lifecycle = Lifecycle.FROZEN
        }
    }

    fun currentSkills(): SkillPluginSnapshot {
        bootstrap()
        return skills.current()
    }

    fun currentSkillSupport(): SkillSupportSnapshot {
        bootstrap()
        return support.current()
    }

    fun currentQuests(): QuestPluginSnapshot {
        bootstrap()
        return quests.current()
    }

    fun registerSkill(plugin: SkillPlugin) {
        synchronized(lock) {
            check(lifecycle != Lifecycle.FROZEN) { "Plugin registry is frozen; cannot register ${plugin::class.java.name}" }
            if (lifecycle == Lifecycle.BOOTSTRAP) {
                skills.register(plugin)
                return
            }
            discoveredSkillPlugins = discoveredSkillPlugins + plugin
        }
    }

    fun registerQuest(plugin: QuestPlugin) {
        synchronized(lock) {
            check(lifecycle != Lifecycle.FROZEN) { "Plugin registry is frozen; cannot register ${plugin::class.java.name}" }
            if (lifecycle == Lifecycle.BOOTSTRAP) {
                quests.register(plugin)
                return
            }
            discoveredQuestPlugins = discoveredQuestPlugins + plugin
        }
    }

    internal fun clearForTests() {
        synchronized(lock) {
            lifecycle = Lifecycle.FROZEN
            discoveredSkillPlugins = emptyList()
            discoveredSkillSupportModules = emptyList()
            discoveredQuestPlugins = emptyList()
            skills.clearForTests()
            support.resetForTests()
            quests.clearForTests()
        }
    }

    fun resetForTests() {
        synchronized(lock) {
            lifecycle = Lifecycle.DISCOVER
            discoveredSkillPlugins = emptyList()
            discoveredSkillSupportModules = emptyList()
            discoveredQuestPlugins = emptyList()
            skills.resetForTests()
            support.resetForTests()
            quests.resetForTests()
        }
    }
}
