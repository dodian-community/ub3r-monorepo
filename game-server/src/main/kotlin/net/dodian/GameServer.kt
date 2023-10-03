package net.dodian

import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import net.dodian.uber.cache.definition.GameDefinitions
import net.dodian.uber.net.HttpChannelInitializer
import net.dodian.uber.net.JagGrabChannelInitializer
import net.dodian.uber.net.ServiceChannelInitializer
import net.dodian.uber.net.message.GameProtocol
import net.dodian.uber.scripting.KotlinScriptPlugin
import net.dodian.uber.scripting.ScriptPluginLoader
import net.dodian.uber.services.GameService
import net.dodian.uber.services.LoginService
import net.dodian.uber.services.UpdateService
import net.dodian.uber.session.Ub3rHandler
import net.dodian.uber.utils.RsaManager

private val logger = InlineLogger()

const val TIMEOUT_SECONDS = 30L

val context = ServerContext(RsaManager().getPair())
val definitions: GameDefinitions get() = context.definitions
val protocol: GameProtocol get() = context.protocol

fun main() {
    logger.info { "Launching Uber Server 3.0..." }

    val plugins = ScriptPluginLoader.load(KotlinScriptPlugin::class.java)
    logger.info { "Loaded ${plugins.size} plugin script${if (plugins.size == 1) "" else "s"}." }

    val gameService = GameService()
    val loginService = LoginService()
    val updateService = UpdateService()

    context.registerServices(gameService, loginService, updateService)
    gameService.start()
    loginService.start()
    updateService.start()

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