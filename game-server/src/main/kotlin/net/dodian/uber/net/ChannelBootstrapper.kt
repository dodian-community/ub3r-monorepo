package net.dodian.uber.net

import com.github.michaelbull.logging.InlineLogger
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import net.dodian.uber.game.session.GameHandler

private val logger = InlineLogger()

const val TIMEOUT_SECONDS = 30L

fun startChannel() {
    // TODO: Se comments on the lines below...
    val acceptGroup = NioEventLoopGroup() // 2?
    val ioGroup = NioEventLoopGroup() // 4?

    val bootstrap = ServerBootstrap()
    bootstrap.group(acceptGroup, ioGroup)
    bootstrap.channel(NioServerSocketChannel::class.java)
    bootstrap.childHandler(ServiceChannelInitializer(GameHandler()))
    bootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT_SECONDS.toInt() * 1_000)
    bootstrap.childOption(ChannelOption.TCP_NODELAY, true)
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true)
    bootstrap.childOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator(false))
    bootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator(false))

    try {
        bootstrap.bind("127.0.0.1", 43594).sync()

        logger.info { "Server is listening on 127.0.0.1:43594..." }
    } catch (exception: Exception) {
        logger.error(exception) { "Failed to bind to port." }
    }
}