package net.dodian.uber.game.engine.webapi

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WebApiSecurityTest {
    @Test
    fun `hiscores rate limit and concurrency permits are enforced`() {
        val guard = WebRequestGuard(maxSubjects = 100, maxConcurrentPerIp = 2)
        val ip = "203.0.113.10"
        val first = guard.enter(ip, WebRateClass.HISCORES, 1_000L)
        val second = guard.enter(ip, WebRateClass.HISCORES, 1_000L)
        assertTrue(first.permitted)
        assertTrue(second.permitted)
        assertFalse(guard.enter(ip, WebRateClass.HISCORES, 1_000L).permitted)
        first.release()
        second.release()
    }

    @Test
    fun `five rejected requests trigger a temporary block`() {
        val guard = WebRequestGuard(maxSubjects = 100, maxConcurrentPerIp = 1)
        val ip = "203.0.113.10"
        val held = guard.enter(ip, WebRateClass.PUBLIC, 1_000L)
        repeat(5) { assertFalse(guard.enter(ip, WebRateClass.PUBLIC, 1_000L + it).permitted) }
        held.release()

        val blocked = guard.enter(ip, WebRateClass.PUBLIC, 2_000L)
        assertFalse(blocked.permitted)
        assertTrue(blocked.retryAfterSeconds >= 59)
        assertTrue(guard.enter(ip, WebRateClass.PUBLIC, 62_000L).permitted)
    }

    @Test
    fun `forwarded addresses are trusted only for configured cidrs`() {
        assertTrue(WebClientAddress.addressInCidr("10.10.4.5", "10.10.0.0/16"))
        assertFalse(WebClientAddress.addressInCidr("10.11.4.5", "10.10.0.0/16"))
        assertTrue(WebClientAddress.addressInCidr("2001:db8::1", "2001:db8::/32"))
    }
}
