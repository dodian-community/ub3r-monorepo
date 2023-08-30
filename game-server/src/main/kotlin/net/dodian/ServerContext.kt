package net.dodian

import net.dodian.services.Service

class ServerContext(
    val services: MutableList<Service> = mutableListOf(),
    val handlers: MutableList<Any> = mutableListOf()
) {
    inline fun <reified S : Service> service() = services.single { it is S } as S
    inline fun <reified H : Any> handler() = handlers.single { it is H } as H

    fun registerService(service: Service, start: Boolean = true): Boolean {
        if (services.any { it::class == service::class })
            return false

        service.start()
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
}