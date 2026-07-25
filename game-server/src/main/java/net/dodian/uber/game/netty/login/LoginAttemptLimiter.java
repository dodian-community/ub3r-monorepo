package net.dodian.uber.game.netty.login;

import java.util.concurrent.ConcurrentHashMap;

/** Bounded, in-memory pre-auth throttle for direct client peer addresses. */
final class LoginAttemptLimiter {
    private static final long WINDOW_MS = 60_000L;
    private static final long MAX_BACKOFF_MS = 30_000L;
    private static final int MAX_ATTEMPTS_PER_WINDOW = 10;
    private static final int MAX_TRACKED_IPS = 10_000;

    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

    boolean tryAcquire(String ip, long now) {
        if (ip == null || ip.isBlank()) return false;
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("localhost")) {
            return true;
        }
        if (states.size() >= MAX_TRACKED_IPS && !states.containsKey(ip)) return false;
        State state = states.computeIfAbsent(ip, ignored -> new State(now));
        synchronized (state) {
            if (now - state.windowStart >= WINDOW_MS) {
                state.windowStart = now;
                state.attempts = 0;
            }
            if (now < state.blockedUntil || state.attempts >= MAX_ATTEMPTS_PER_WINDOW) return false;
            state.attempts++;
            return true;
        }
    }

    void recordFailure(String ip, long now) {
        State state = states.get(ip);
        if (state == null) return;
        synchronized (state) {
            state.failures = Math.min(state.failures + 1, 16);
            long backoff = Math.min(MAX_BACKOFF_MS, 1_000L << Math.min(state.failures - 1, 5));
            state.blockedUntil = Math.max(state.blockedUntil, now + backoff);
        }
    }

    void recordSuccess(String ip) {
        if (ip != null) states.remove(ip);
    }

    private static final class State {
        private long windowStart;
        private int attempts;
        private int failures;
        private long blockedUntil;
        private State(long now) { windowStart = now; }
    }
}
