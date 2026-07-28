package net.dodian.uber.game.netty.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {
    @Test
    void caps_raw_attempts_per_peer_within_a_window() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        long now = 1_000L;

        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.preflight("203.0.113.10", now).isAllowed());
        }
        assertFalse(limiter.preflight("203.0.113.10", now).isAllowed());
        assertTrue(limiter.preflight("203.0.113.11", now).isAllowed());
    }

    @Test
    void five_failures_escalate_from_one_minute_to_five_minutes_and_one_day() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        String ip = "203.0.113.10";
        String username = "Neosback";
        long now = 1_000L;

        recordFiveFailures(limiter, ip, username, now);
        assertEquals(16, limiter.checkBlocked(ip, username, "", now).getResponseCode());

        now += 60_001L;
        recordFiveFailures(limiter, ip, username, now);
        assertEquals(34, limiter.checkBlocked(ip, username, "", now).getResponseCode());

        now += 5 * 60_000L + 1L;
        recordFiveFailures(limiter, ip, username, now);
        assertEquals(35, limiter.checkBlocked(ip, username, "", now).getResponseCode());
    }

    @Test
    void successful_login_clears_the_account_subject_but_not_ip_reputation() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        long now = 1_000L;
        recordFiveFailures(limiter, "203.0.113.10", "Neosback", now);

        limiter.recordSuccess("Neosback");

        assertTrue(limiter.checkBlocked("203.0.113.11", "Neosback", "", now).isAllowed());
        assertFalse(limiter.checkBlocked("203.0.113.10", "Other", "", now).isAllowed());
    }

    @Test
    void escalation_expires_after_seven_quiet_days() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        long now = 1_000L;
        recordFiveFailures(limiter, "203.0.113.10", "Neosback", now);

        long afterQuietPeriod = now + 7L * 24L * 60L * 60L * 1_000L + 1L;
        assertTrue(limiter.checkBlocked("203.0.113.10", "Neosback", "", afterQuietPeriod).isAllowed());
    }

    private static void recordFiveFailures(
            LoginAttemptLimiter limiter,
            String ip,
            String username,
            long now
    ) {
        for (int i = 0; i < 5; i++) {
            limiter.recordCredentialFailure(ip, username, now + i);
        }
    }
}
