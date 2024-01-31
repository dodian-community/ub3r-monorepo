package net.dodian.uber.game

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.ChannelOption
import io.netty.channel.socket.nio.NioServerSocketChannel
import net.dodian.uber.net.ServiceChannelInitializer
import net.dodian.uber.net.session.Ub3rHandler
import net.dodian.utilities.serverPort
import java.net.InetSocketAddress

const val TIMEOUT_SECONDS = 30L

fun runGameThread() {
}

fun startClientListener() {
    val bootstrap = ServerBootstrap()

    val service = ServiceChannelInitializer(Ub3rHandler())

    bootstrap.channel(NioServerSocketChannel::class.java)
    bootstrap.childHandler(service)
    bootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT_SECONDS.toInt() * 1_000)
    bootstrap.childOption(ChannelOption.TCP_NODELAY, true)
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true)
    bootstrap.childOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator(false))
    bootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator(false))

    bootstrap.bind(InetSocketAddress(serverPort))
}