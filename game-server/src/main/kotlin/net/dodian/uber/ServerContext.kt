package net.dodian.uber

import net.dodian.server.scripting.ScriptPlugin
import net.dodian.uber.protocol.game.GamePacketMaps
import net.dodian.uber.services.Service
import net.dodian.uber.services.GameService

@Suppress("MemberVisibilityCanBePrivate")
class ServerContext(
    val packetMap: GamePacketMaps = GamePacketMaps(),
    val services: MutableList<Service> = mutableListOf(),
    val handlers: MutableList<Any> = mutableListOf(),
    val plugins: MutableList<ScriptPlugin> = mutableListOf()
) {
    inline fun <reified S : Service> service() = services.single { it is S } as S
    inline fun <reified H : Any> handler() = handlers.single { it is H } as H

    fun registerService(service: Service, start: Boolean = true): Boolean {
        if (services.any { it::class == service::class })
            return false

        service.startUp()
        return services.add(service)
    }

    fun registerServices(vararg services: Service) {
        services.forEach { registerService(it) }
    }

    fun registerHandler(handler: Any): Boolean {
        if (handlers.any { it::class == handler::class })
            return false

        return handlers.add(handler)
    }

    fun registerHandlers(vararg handlers: Any) {
        handlers.forEach { registerHandler(it) }
    }

    fun registerPlugin(plugin: ScriptPlugin): Boolean {
        if (plugins.any { it::class == plugin::class })
            return false

        return plugins.add(plugin)
    }

    fun registerPlugins(vararg plugins: ScriptPlugin) {
        plugins.forEach { registerPlugin(it) }
    }
}