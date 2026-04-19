package net.dodian.uber.game.engine.lifecycle

import net.dodian.uber.game.api.plugin.ContentBootstrap
import net.dodian.uber.game.api.plugin.ContentModuleIndex
import net.dodian.uber.game.api.plugin.PluginRegistry
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.engine.systems.interaction.objects.ObjectContentRegistry
import net.dodian.uber.game.engine.webapi.WebApi
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Single startup entrypoint for plugin/bootstrap initialization.
 */
object EnginePluginBootstrap {
    enum class LifecyclePhase {
        DISCOVER,
        VALIDATE,
        BOOTSTRAP,
        FREEZE,
    }

    private val bootstrapped = AtomicBoolean(false)
    @Volatile
    private var phase: LifecyclePhase = LifecyclePhase.DISCOVER

    private val bootstrapPriority = mapOf(
        PluginRegistry.id to 0,
        "commands.registry" to 10,
        "items.registry" to 20,
        "npcs.registry" to 30,
        "objects.registry" to 40,
        "shops.registry" to 50,
        "skills.doctor" to 90,
    )
    private val legacyBootstrapIds = setOf("skills.registry")

    @JvmStatic
    fun bootstrap() {
        if (bootstrapped.get()) {
            return
        }
        synchronized(this) {
            if (bootstrapped.get()) {
                return
            }
            phase = LifecyclePhase.DISCOVER
            PluginRegistry.discover()

            phase = LifecyclePhase.VALIDATE
            PluginRegistry.validate()

            phase = LifecyclePhase.BOOTSTRAP
            for (bootstrap: ContentBootstrap in orderedContentBootstraps()) {
                bootstrap.bootstrap()
            }
            GameEventBus.bootstrap()
            ObjectContentRegistry.prewarmObjectDefinitions()
            WebApi.start()

            phase = LifecyclePhase.FREEZE
            bootstrapped.set(true)
        }
    }

    @JvmStatic
    fun currentPhase(): LifecyclePhase = phase

    @JvmStatic
    fun orderedContentBootstrapIds(): List<String> = orderedContentBootstraps().map(ContentBootstrap::id)

    private fun orderedContentBootstraps(): List<ContentBootstrap> {
        val byId = linkedMapOf<String, ContentBootstrap>()
        ContentModuleIndex.contentBootstraps
            .sortedWith(
                compareBy<ContentBootstrap>(
                    { bootstrapPriority[it.id] ?: Int.MAX_VALUE },
                    { it.id },
                    { it::class.java.name },
                ),
            )
            .forEach { bootstrap ->
                if (bootstrap.id in legacyBootstrapIds) {
                    return@forEach
                }
                val existing = byId.putIfAbsent(bootstrap.id, bootstrap)
                require(existing == null) {
                    "Duplicate engine bootstrap id=${bootstrap.id} existing=${existing!!::class.java.name} new=${bootstrap::class.java.name}"
                }
            }

        require(byId.containsKey(PluginRegistry.id)) {
            "Engine bootstrap requires ${PluginRegistry.id} in ContentModuleIndex.contentBootstraps"
        }
        return byId.values.toList()
    }
}
