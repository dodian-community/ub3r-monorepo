package net.dodian.uber.game.api.plugin

import net.dodian.uber.game.api.plugin.ContentBootstrap
import net.dodian.uber.game.api.plugin.ContentModuleIndex
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginRegistryEngine
import net.dodian.uber.game.api.plugin.skills.SkillPluginSnapshot

object PluginRegistry : ContentBootstrap {
    override val id: String = "plugins.registry"

    private enum class Lifecycle {
        DISCOVER,
        VALIDATE,
        BOOTSTRAP,
        FROZEN,
    }

    private val skills = SkillPluginRegistryEngine()
    private val lock = Any()
    @Volatile
    private var lifecycle: Lifecycle = Lifecycle.DISCOVER
    @Volatile
    private var discoveredSkillPlugins: List<SkillPlugin> = emptyList()

    fun discover() {
        if (lifecycle != Lifecycle.DISCOVER) return
        synchronized(lock) {
            if (lifecycle != Lifecycle.DISCOVER) return
            val discoveredFromIndex = ContentModuleIndex.skillPlugins
            discoveredSkillPlugins =
                (discoveredSkillPlugins + discoveredFromIndex)
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
            lifecycle = Lifecycle.BOOTSTRAP
        }
    }

    override fun bootstrap() {
        if (lifecycle == Lifecycle.FROZEN) return
        validate()
        synchronized(lock) {
            if (lifecycle == Lifecycle.FROZEN) return
            skills.bootstrap(discoveredSkillPlugins)
            skills.freeze()
            lifecycle = Lifecycle.FROZEN
        }
    }

    fun currentSkills(): SkillPluginSnapshot {
        bootstrap()
        return skills.current()
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

    internal fun clearForTests() {
        synchronized(lock) {
            lifecycle = Lifecycle.FROZEN
            discoveredSkillPlugins = emptyList()
            skills.clearForTests()
        }
    }

    fun resetForTests() {
        synchronized(lock) {
            lifecycle = Lifecycle.DISCOVER
            discoveredSkillPlugins = emptyList()
            skills.resetForTests()
        }
    }
}
