package net.dodian.uber.net

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import net.dodian.uber.net.channel.ServerChannelHandler
import net.dodian.uber.net.channel.ServerChannelInitializer

fun buildServerChannelInitializer(): ServerChannelInitializer {
    val handler = ServerChannelHandler()

    val initializer = ServerChannelInitializer(
        handler,
    )

    return initializer
}

fun startChannel() {
    // TODO: Se comments on the lines below...
    val acceptGroup = NioEventLoopGroup() // 2?
    val ioGroup = NioEventLoopGroup() // 4?

    val bootstrap = ServerBootstrap()
    bootstrap.group(acceptGroup, ioGroup)
    bootstrap.channel(NioServerSocketChannel::class.java)
    bootstrap.childHandler(buildServerChannelInitializer())
    bootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30_000)
    bootstrap.childOption(ChannelOption.TCP_NODELAY, true)
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true)
    bootstrap.childOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator(false))
    bootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator(false))
}