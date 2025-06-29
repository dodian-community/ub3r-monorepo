package net.dodian.uber.game.networking;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ipfilter.AbstractRemoteAddressFilter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple per-IP connection limiter & blacklist Filter.
 */
@Sharable
public class ConnectionThrottleFilter extends AbstractRemoteAddressFilter<InetSocketAddress> {

    private final int maxPerIp;
    private final ConcurrentMap<String, Integer> counters = new ConcurrentHashMap<>();
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    public ConnectionThrottleFilter(int maxPerIp) {
        this.maxPerIp = Math.max(maxPerIp, 1);
    }

    @Override
    protected boolean accept(ChannelHandlerContext ctx, InetSocketAddress remoteAddress) {
        String ip = toIp(remoteAddress);
        if (blacklist.contains(ip)) {
            return false;
        }
        return counters.getOrDefault(ip, 0) < maxPerIp;
    }

    @Override
    protected void channelAccepted(ChannelHandlerContext ctx, InetSocketAddress remoteAddress) {
        String ip = toIp(remoteAddress);
        counters.merge(ip, 1, Integer::sum);

        ChannelFuture f = ctx.channel().closeFuture();
        f.addListener(cf -> counters.compute(ip, (k, v) -> v == null || v <= 1 ? null : v - 1));
    }

    @Override
    protected ChannelFuture channelRejected(ChannelHandlerContext ctx, InetSocketAddress remoteAddress) {
        return ctx.channel().close();
    }

    /* util */
    private static String toIp(InetSocketAddress addr) {
        InetAddress inet = addr.getAddress();
        return inet != null ? inet.getHostAddress() : addr.getHostString();
    }

    public void addToBlacklist(String ip) {
        blacklist.add(ip);
    }

    public void removeFromBlacklist(String ip) {
        blacklist.remove(ip);
    }
}
