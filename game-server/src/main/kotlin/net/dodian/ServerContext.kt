package net.dodian

import com.github.michaelbull.logging.InlineLogger
import io.netty.bootstrap.ServerBootstrap
import net.dodian.uber.cache.definition.GameDefinitions
import net.dodian.uber.game.libraries.commands.CommandsLibrary
import net.dodian.uber.game.modelkt.World
import net.dodian.uber.net.NetworkPorts
import net.dodian.uber.net.message.GameProtocol
import net.dodian.uber.services.Service
import net.dodian.uber.utils.RsaKeyPair
import java.net.InetSocketAddress
import java.net.SocketAddress

private val logger = InlineLogger()

@Suppress("MemberVisibilityCanBePrivate")
class ServerContext(
    val rsaKeys: RsaKeyPair,
    val serviceBootstrap: ServerBootstrap = ServerBootstrap(),
    val httpBootstrap: ServerBootstrap = ServerBootstrap(),
    val jagGrabBootstrap: ServerBootstrap = ServerBootstrap(),
    val services: MutableList<Service> = mutableListOf(),
    val world: World = World(),
    val protocol: GameProtocol = GameProtocol(),
    val definitions: GameDefinitions = GameDefinitions(),
    val commandsLibrary: CommandsLibrary = CommandsLibrary()
) {
    inline fun <reified S : Service> service() = services.single { it is S } as S

    fun registerService(service: Service): Boolean {
        if (services.any { it::class == service::class })
            return false

        return services.add(service)
    }

    fun registerServices(vararg services: Service) {
        services.forEach { registerService(it) }
    }

    fun bind() {
        val ports = NetworkPorts()

        val service = InetSocketAddress(ports.service)
        //val http = InetSocketAddress(ports.http)
        //val jagGrab = InetSocketAddress(ports.jaggrab)

        bind(serviceBootstrap, service)
        //bind(httpBootstrap, http)
        //bind(jagGrabBootstrap, jagGrab)

        logger.info { "Game is ready for connections..." }
    }

    private fun bind(bootstrap: ServerBootstrap, address: SocketAddress) {
        bootstrap.bind(address).sync()
    }
}