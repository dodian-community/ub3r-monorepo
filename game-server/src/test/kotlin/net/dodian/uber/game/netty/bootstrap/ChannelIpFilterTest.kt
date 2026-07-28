package net.dodian.uber.game.netty.bootstrap

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.util.AttributeKey
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * ChannelIpFilter.releaseSlot() used to call `ipCounters.remove(host, 0)` on a
 * ConcurrentHashMap<String, AtomicInteger>. Map.remove(key, value) only removes when
 * the mapped value .equals(value) - but AtomicInteger doesn't override equals(), so
 * comparing it against a boxed int literal can never succeed. That meant the per-IP
 * connection counter was never actually cleaned up: every distinct source IP that ever
 * connected left a permanent entry in this map for the life of the process, an
 * unbounded leak in the exact rate limiter meant to protect a public login port on a
 * server that's supposed to stay up for weeks/months. This test drives the real
 * release path (via the public channelUnregistered override) and asserts the entry is
 * now actually removed once the counter reaches zero.
 */
class ChannelIpFilterTest {
    /** EmbeddedChannel reports an EmbeddedSocketAddress by default; override with a
     *  real InetSocketAddress so ChannelIpFilter's hostFrom() can resolve a host. */
    private class FixedRemoteAddressChannel(
        private val remote: SocketAddress,
    ) : EmbeddedChannel(ChannelInboundHandlerAdapter()) {
        override fun remoteAddress0(): SocketAddress = remote
    }

    @Suppress("UNCHECKED_CAST")
    private fun ipCountersOf(filter: ChannelIpFilter): ConcurrentHashMap<String, AtomicInteger> {
        val field = ChannelIpFilter::class.java.getDeclaredField("ipCounters")
        field.isAccessible = true
        return field.get(filter) as ConcurrentHashMap<String, AtomicInteger>
    }

    @Test
    fun `releasing the last connection for a host removes its counter entry`() {
        val filter = ChannelIpFilter()
        val ipCounters = ipCountersOf(filter)

        val host = "203.0.113.42"
        ipCounters[host] = AtomicInteger(1) // simulate one previously-registered connection

        val remote = InetSocketAddress(InetAddress.getByName(host), 43594)
        val channel = FixedRemoteAddressChannel(remote)
        val ctx: ChannelHandlerContext = channel.pipeline().firstContext()
        ctx.channel().attr(AttributeKey.valueOf<Boolean>("ipLimitOwned")).set(true)

        filter.channelUnregistered(ctx)

        assertFalse(
            ipCounters.containsKey(host),
            "releaseSlot should remove the counter entry once it reaches zero, not leak it forever",
        )
    }

    @Test
    fun `releasing one of several connections for a host keeps the counter until it reaches zero`() {
        val filter = ChannelIpFilter()
        val ipCounters = ipCountersOf(filter)

        val host = "203.0.113.99"
        ipCounters[host] = AtomicInteger(2) // two connections currently open from this host

        val remote = InetSocketAddress(InetAddress.getByName(host), 43595)
        val channel = FixedRemoteAddressChannel(remote)
        val ctx: ChannelHandlerContext = channel.pipeline().firstContext()
        ctx.channel().attr(AttributeKey.valueOf<Boolean>("ipLimitOwned")).set(true)

        filter.channelUnregistered(ctx)

        val counter = ipCounters[host]
        checkNotNull(counter) { "counter should still be present while a connection from this host remains open" }
        org.junit.jupiter.api.Assertions.assertEquals(1, counter.get())
    }
}
