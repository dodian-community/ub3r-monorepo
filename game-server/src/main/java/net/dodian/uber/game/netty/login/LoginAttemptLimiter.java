package net.dodian.uber.game.netty.login;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded, in-memory authentication abuse guard.
 *
 * <p>State intentionally does not survive a restart. Raw Discord authorization
 * codes are never retained; only a SHA-256 fingerprint is used to recognize
 * reuse during the lifetime of this process.</p>
 */
final class LoginAttemptLimiter {
    static final int ONE_MINUTE_CODE = 16;
    static final int FIVE_MINUTE_CODE = 34;
    static final int DAY_CODE = 35;

    private static final long ATTEMPT_WINDOW_MS = 60_000L;
    private static final int MAX_ATTEMPTS_PER_WINDOW = 10;
    private static final int FAILURES_PER_STAGE = 5;
    private static final long QUIET_EXPIRY_MS = 7L * 24L * 60L * 60L * 1_000L;
    private static final long SWEEP_INTERVAL_MS = 60_000L;
    private static final int MAX_TRACKED_SUBJECTS = 20_000;
    private static final long[] BLOCK_DURATIONS_MS = {
            60_000L,
            5L * 60L * 1_000L,
            24L * 60L * 60L * 1_000L
    };

    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();
    private volatile long nextSweepAt;

    Decision preflight(String ip, long now) {
        sweepIfDue(now);
        if (ip == null || ip.isBlank()) return Decision.blocked(ONE_MINUTE_CODE);
        if (isLoopback(ip)) return Decision.allowed();

        String key = ipKey(ip);
        State state = stateFor(key, now);
        if (state == null) return Decision.blocked(ONE_MINUTE_CODE);
        synchronized (state) {
            resetIfQuiet(state, now);
            int blockedCode = blockedCode(state, now);
            if (blockedCode != 0) return Decision.blocked(blockedCode);
            if (now - state.attemptWindowStart >= ATTEMPT_WINDOW_MS) {
                state.attemptWindowStart = now;
                state.attempts = 0;
            }
            if (state.attempts >= MAX_ATTEMPTS_PER_WINDOW) {
                return Decision.blocked(ONE_MINUTE_CODE);
            }
            state.attempts++;
            state.lastSeenAt = now;
            return Decision.allowed();
        }
    }

    Decision checkBlocked(String ip, String username, String discordAuthCode, long now) {
        sweepIfDue(now);
        int code = Math.max(
                subjectBlockedCode(ipKey(ip), now),
                subjectBlockedCode(usernameKey(username), now)
        );
        if (discordAuthCode != null && !discordAuthCode.isBlank()) {
            code = Math.max(code, subjectBlockedCode(codeKey(discordAuthCode), now));
        }
        return code == 0 ? Decision.allowed() : Decision.blocked(code);
    }

    void recordCredentialFailure(String ip, String username, long now) {
        registerFailure(ipKey(ip), now);
        registerFailure(usernameKey(username), now);
    }

    void recordCreationAuthFailure(String ip, String username, String discordAuthCode, long now) {
        registerFailure(ipKey(ip), now);
        registerFailure(usernameKey(username), now);
        if (discordAuthCode != null && !discordAuthCode.isBlank()) {
            registerFailure(codeKey(discordAuthCode), now);
        }
    }

    void recordSuccess(String username) {
        String key = usernameKey(username);
        if (key != null) states.remove(key);
    }

    int trackedSubjectCount() {
        return states.size();
    }

    private int subjectBlockedCode(String key, long now) {
        if (key == null) return 0;
        State state = states.get(key);
        if (state == null) return 0;
        synchronized (state) {
            resetIfQuiet(state, now);
            state.lastSeenAt = now;
            return blockedCode(state, now);
        }
    }

    private void registerFailure(String key, long now) {
        if (key == null) return;
        State state = stateFor(key, now);
        if (state == null) return;
        synchronized (state) {
            resetIfQuiet(state, now);
            state.lastFailureAt = now;
            state.lastSeenAt = now;
            state.failures++;
            if (state.failures < FAILURES_PER_STAGE) return;

            state.failures = 0;
            state.stage = Math.min(state.stage + 1, BLOCK_DURATIONS_MS.length);
            long duration = BLOCK_DURATIONS_MS[state.stage - 1];
            state.blockedUntil = Math.max(state.blockedUntil, now + duration);
        }
    }

    private State stateFor(String key, long now) {
        State existing = states.get(key);
        if (existing != null) return existing;
        if (states.size() >= MAX_TRACKED_SUBJECTS) {
            sweep(now, true);
            if (states.size() >= MAX_TRACKED_SUBJECTS) return null;
        }
        State created = new State(now);
        State raced = states.putIfAbsent(key, created);
        return raced != null ? raced : created;
    }

    private void sweepIfDue(long now) {
        if (now < nextSweepAt) return;
        synchronized (this) {
            if (now < nextSweepAt) return;
            sweep(now, false);
            nextSweepAt = now + SWEEP_INTERVAL_MS;
        }
    }

    private void sweep(long now, boolean pressure) {
        long idleLimit = pressure ? ATTEMPT_WINDOW_MS : QUIET_EXPIRY_MS;
        for (Map.Entry<String, State> entry : states.entrySet()) {
            State state = entry.getValue();
            synchronized (state) {
                boolean evictableUnderPressure = !pressure || (state.stage == 0 && state.failures == 0);
                if (evictableUnderPressure && now >= state.blockedUntil && now - state.lastSeenAt >= idleLimit) {
                    states.remove(entry.getKey(), state);
                }
            }
            if (pressure && states.size() < MAX_TRACKED_SUBJECTS) return;
        }
    }

    private static void resetIfQuiet(State state, long now) {
        if (state.lastFailureAt != 0L && now - state.lastFailureAt >= QUIET_EXPIRY_MS) {
            state.failures = 0;
            state.stage = 0;
            state.blockedUntil = 0L;
            state.lastFailureAt = 0L;
        }
    }

    private static int blockedCode(State state, long now) {
        if (now >= state.blockedUntil || state.stage <= 0) return 0;
        if (state.stage == 1) return ONE_MINUTE_CODE;
        if (state.stage == 2) return FIVE_MINUTE_CODE;
        return DAY_CODE;
    }

    private static String ipKey(String ip) {
        return ip == null || ip.isBlank() ? null : "ip:" + ip.trim().toLowerCase(Locale.ROOT);
    }

    private static String usernameKey(String username) {
        if (username == null || username.isBlank()) return null;
        return "user:" + username.trim().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static String codeKey(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) hex.append(String.format("%02x", value & 0xff));
            return "discord:" + hex;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip)
                || "::1".equals(ip)
                || "localhost".equalsIgnoreCase(ip);
    }

    static final class Decision {
        private static final Decision ALLOWED = new Decision(true, 0);
        private final boolean allowed;
        private final int responseCode;

        private Decision(boolean allowed, int responseCode) {
            this.allowed = allowed;
            this.responseCode = responseCode;
        }

        static Decision allowed() { return ALLOWED; }
        static Decision blocked(int responseCode) { return new Decision(false, responseCode); }
        boolean isAllowed() { return allowed; }
        int getResponseCode() { return responseCode; }
    }

    private static final class State {
        private long attemptWindowStart;
        private int attempts;
        private int failures;
        private int stage;
        private long blockedUntil;
        private long lastFailureAt;
        private long lastSeenAt;

        private State(long now) {
            attemptWindowStart = now;
            lastSeenAt = now;
        }
    }
}
