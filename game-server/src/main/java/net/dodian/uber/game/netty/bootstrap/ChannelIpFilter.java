package net.dodian.uber.game.netty.bootstrap;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.dodian.uber.game.engine.config.DotEnvKt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class ChannelIpFilter extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ChannelIpFilter.class);
    private static final AttributeKey<Boolean> IP_LIMIT_OWNED = AttributeKey.valueOf("ipLimitOwned");
    private final ConcurrentHashMap<String, AtomicInteger> ipCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastLogTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AttemptWindow> attemptWindows = new ConcurrentHashMap<>();
    private static final int MAX_CONNECTION_ATTEMPTS_PER_MINUTE = 20;
    private static final int MAX_TRACKED_IPS = 10_000;

    private static final AtomicLong currentConnections = new AtomicLong(0);
    private static final AtomicLong rejectedConnections = new AtomicLong(0);

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        String host = hostFrom(ctx);
        if (host == null) {
            ctx.close();
            return;
        }
        if (!allowConnectionAttempt(host, System.currentTimeMillis())) {
            rejectedConnections.incrementAndGet();
            logRateLimited(host, "Connection-attempt rate exceeded for {}");
            ctx.close();
            return;
        }

        int limit = DotEnvKt.getGameConnectionsPerIp();
        AtomicInteger counter = ipCounters.computeIfAbsent(host, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        if (count > limit) {
            counter.decrementAndGet();
            rejectedConnections.incrementAndGet();
            long now = System.currentTimeMillis();
            lastLogTime.compute(host, (k, v) -> {
                if (v == null || now - v > 5000) {
                    logger.warn("Connection rejected from {} (exceeds limit of {})", host, limit);
                    return now;
                }
                return v;
            });
            ctx.close();
            return;
        }

        ctx.channel().attr(IP_LIMIT_OWNED).set(true);
        currentConnections.incrementAndGet();
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        releaseSlot(ctx);
        ctx.fireChannelUnregistered();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.debug("Exception in IP filter for channel: {}", ctx.channel().remoteAddress(), cause);
        releaseSlot(ctx);
        ctx.fireExceptionCaught(cause);
    }

    private void releaseSlot(ChannelHandlerContext ctx) {
        Boolean owned = ctx.channel().attr(IP_LIMIT_OWNED).getAndSet(false);
        if (Boolean.TRUE.equals(owned)) {
            String host = hostFrom(ctx);
            if (host != null) {
                AtomicInteger counter = ipCounters.get(host);
                if (counter != null) {
                    int remaining = counter.decrementAndGet();
                    if (remaining <= 0) {
                        // Must compare against the same AtomicInteger reference: remove(key, value)
                        // removes only when the current mapped value .equals(value), and AtomicInteger
                        // doesn't override equals(), so comparing against a boxed int here would never
                        // match and this entry would never actually be cleaned up.
                        ipCounters.remove(host, counter);
                    }
                }
            }
            currentConnections.decrementAndGet();
        }
    }

    private boolean allowConnectionAttempt(String host, long now) {
        if (isLoopback(host)) return true;
        if (attemptWindows.size() >= MAX_TRACKED_IPS && !attemptWindows.containsKey(host)) {
            attemptWindows.entrySet().removeIf(entry -> {
                boolean expired = now - entry.getValue().lastSeenAt > 60_000L;
                if (expired) lastLogTime.remove(entry.getKey());
                return expired;
            });
            if (attemptWindows.size() >= MAX_TRACKED_IPS) return false;
        }
        AttemptWindow window = attemptWindows.computeIfAbsent(host, ignored -> new AttemptWindow(now));
        synchronized (window) {
            if (now - window.startedAt >= 60_000L) {
                window.startedAt = now;
                window.attempts = 0;
            }
            window.lastSeenAt = now;
            if (window.attempts >= MAX_CONNECTION_ATTEMPTS_PER_MINUTE) return false;
            window.attempts++;
            return true;
        }
    }

    private void logRateLimited(String host, String message) {
        long now = System.currentTimeMillis();
        lastLogTime.compute(host, (key, previous) -> {
            if (previous == null || now - previous > 5_000L) {
                logger.warn(message, host);
                return now;
            }
            return previous;
        });
    }

    private static boolean isLoopback(String host) {
        return "127.0.0.1".equals(host) || "::1".equals(host)
                || "0:0:0:0:0:0:0:1".equals(host);
    }

    private static final class AttemptWindow {
        private long startedAt;
        private long lastSeenAt;
        private int attempts;

        private AttemptWindow(long now) {
            startedAt = now;
            lastSeenAt = now;
        }
    }

    private static String hostFrom(ChannelHandlerContext ctx) {
        try {
            InetSocketAddress addr = (InetSocketAddress) ctx.channel().remoteAddress();
            if (addr == null) return null;
            java.net.InetAddress inetAddr = addr.getAddress();
            if (inetAddr == null) return null;

            if (inetAddr instanceof java.net.Inet6Address) {
                java.net.Inet6Address ipv6 = (java.net.Inet6Address) inetAddr;
                if (ipv6.isIPv4CompatibleAddress()) {
                    byte[] bytes = ipv6.getAddress();
                    byte[] ipv4Bytes = new byte[] { bytes[12], bytes[13], bytes[14], bytes[15] };
                    return java.net.InetAddress.getByAddress(ipv4Bytes).getHostAddress();
                }
                byte[] bytes = ipv6.getAddress();
                if (isIPv4Mapped(bytes)) {
                    byte[] ipv4Bytes = new byte[] { bytes[12], bytes[13], bytes[14], bytes[15] };
                    return java.net.InetAddress.getByAddress(ipv4Bytes).getHostAddress();
                }
            }
            return inetAddr.getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isIPv4Mapped(byte[] bytes) {
        if (bytes.length != 16) return false;
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) return false;
        }
        return bytes[10] == (byte)0xFF && bytes[11] == (byte)0xFF;
    }
}
