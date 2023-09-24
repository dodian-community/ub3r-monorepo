package net.dodian

import com.github.michaelbull.logging.InlineLogger
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import net.dodian.uber.cache.definition.GameDefinitions
import net.dodian.uber.net.*
import net.dodian.uber.session.Ub3rHandler
import net.dodian.uber.net.codec.handshake.SERVICE_GAME
import net.dodian.uber.net.message.GameProtocol
import net.dodian.uber.services.GameService
import net.dodian.uber.services.LoginService
import net.dodian.uber.utils.RsaManager
import net.dodian.utilities.serverPort
import java.net.InetSocketAddress
import java.net.SocketAddress

private val logger = InlineLogger()

const val TIMEOUT_SECONDS = 30L

val context = ServerContext(RsaManager().getPair())
val definitions: GameDefinitions get() = context.definitions
val protocol: GameProtocol get() = context.protocol

fun main() {
    logger.info { "Launching Uber Server 3.0..." }

    val gameService = GameService()
    val loginService = LoginService()
    context.registerServices(gameService, loginService)
    gameService.start()
    loginService.start()

    val loopGroup = NioEventLoopGroup()
    val handler = Ub3rHandler()

    context.serviceBootstrap.group(loopGroup)
    context.httpBootstrap.group(loopGroup)
    context.jagGrabBootstrap.group(loopGroup)

    val service = ServiceChannelInitializer(handler)
    context.serviceBootstrap.channel(NioServerSocketChannel::class.java)
    context.serviceBootstrap.childHandler(service)
    context.serviceBootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT_SECONDS.toInt() * 1_000)
    context.serviceBootstrap.childOption(ChannelOption.TCP_NODELAY, true)
    context.serviceBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true)
    context.serviceBootstrap.childOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator(false))
    context.serviceBootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator(false))

    val http = HttpChannelInitializer(handler)
    context.httpBootstrap.channel(NioServerSocketChannel::class.java)
    context.httpBootstrap.childHandler(http)

    val jaggrab = JagGrabChannelInitializer(handler)
    context.jagGrabBootstrap.channel(NioServerSocketChannel::class.java)
    context.jagGrabBootstrap.childHandler(jaggrab)

    context.bind()
}