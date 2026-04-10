package net.dodian.uber.game.systems.plugin

import net.dodian.uber.game.systems.dispatch.ContentBootstrap
import net.dodian.uber.game.systems.dispatch.ContentModuleIndex
import net.dodian.uber.game.systems.skills.plugin.SkillPlugin
import net.dodian.uber.game.systems.skills.plugin.SkillPluginRegistryEngine
import net.dodian.uber.game.systems.skills.plugin.SkillPluginSnapshot

object PluginRegistry : ContentBootstrap {
    override val id: String = "plugins.registry"

    private val skills = SkillPluginRegistryEngine()

    override fun bootstrap() {
        skills.bootstrap(ContentModuleIndex.skillPlugins)
    }

    fun currentSkills(): SkillPluginSnapshot {
        bootstrap()
        return skills.current()
    }

    fun registerSkill(plugin: SkillPlugin) = skills.register(plugin)

    internal fun clearForTests() = skills.clearForTests()

    fun resetForTests() = skills.resetForTests()
}
