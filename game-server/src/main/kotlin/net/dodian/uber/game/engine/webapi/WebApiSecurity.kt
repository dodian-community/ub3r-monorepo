package net.dodian.uber.game.engine.webapi

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import java.net.InetAddress
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

internal enum class WebRateClass(val limitPerMinute: Int) {
    PUBLIC(60),
    HISCORES(30),
    OPERATIONS(120),
}

internal class WebRequestGuard(
    private val maxSubjects: Int = 10_000,
    private val maxConcurrentPerIp: Int = 8,
) {
    private val states = ConcurrentHashMap<String, State>()

    fun enter(ip: String, rateClass: WebRateClass, now: Long = System.currentTimeMillis()): Decision {
        sweep(now)
        val state = states[ip] ?: run {
            if (states.size >= maxSubjects) return Decision.denied(60)
            states.computeIfAbsent(ip) { State(now) }
        }
        synchronized(state) {
            resetQuietState(state, now)
            state.lastSeenAt = now
            if (now < state.blockedUntil) {
                return Decision.denied(secondsUntil(state.blockedUntil, now))
            }
            if (now - state.windowStartedAt >= 60_000L) {
                state.windowStartedAt = now
                state.counts.fill(0)
            }
            val index = rateClass.ordinal
            if (state.concurrent >= maxConcurrentPerIp || state.counts[index] >= rateClass.limitPerMinute) {
                return rejectLocked(state, now)
            }
            state.concurrent++
            state.counts[index]++
            return Decision.allowed { release(state) }
        }
    }

    fun reject(ip: String, now: Long = System.currentTimeMillis()): Int {
        val state = states.computeIfAbsent(ip) { State(now) }
        synchronized(state) {
            return rejectLocked(state, now).retryAfterSeconds
        }
    }

    private fun rejectLocked(state: State, now: Long): Decision {
        state.lastSeenAt = now
        state.violations++
        if (state.violations >= 5) {
            state.violations = 0
            state.stage = (state.stage + 1).coerceAtMost(3)
            val duration = when (state.stage) {
                1 -> 60_000L
                2 -> 5 * 60_000L
                else -> 24 * 60 * 60_000L
            }
            state.blockedUntil = now + duration
        }
        val retryAt = if (state.blockedUntil > now) state.blockedUntil else state.windowStartedAt + 60_000L
        return Decision.denied(secondsUntil(retryAt, now))
    }

    private fun release(expected: State) {
        synchronized(expected) {
            if (expected.concurrent > 0) expected.concurrent--
        }
    }

    private fun sweep(now: Long) {
        if (states.size < maxSubjects / 2) return
        states.entries.forEach { (ip, state) ->
            synchronized(state) {
                if (state.concurrent == 0 && now >= state.blockedUntil && now - state.lastSeenAt >= QUIET_EXPIRY_MS) {
                    states.remove(ip, state)
                }
            }
        }
    }

    private fun resetQuietState(state: State, now: Long) {
        if (now - state.lastSeenAt >= QUIET_EXPIRY_MS) {
            state.stage = 0
            state.violations = 0
            state.blockedUntil = 0
        }
    }

    internal data class Decision(
        val permitted: Boolean,
        val retryAfterSeconds: Int,
        val release: () -> Unit,
    ) {
        companion object {
            fun allowed(release: () -> Unit) = Decision(true, 0, release)
            fun denied(retryAfterSeconds: Int) = Decision(false, retryAfterSeconds.coerceAtLeast(1)) {}
        }
    }

    private class State(now: Long) {
        var windowStartedAt = now
        val counts = IntArray(WebRateClass.entries.size)
        var concurrent = 0
        var violations = 0
        var stage = 0
        var blockedUntil = 0L
        var lastSeenAt = now
    }

    private companion object {
        const val QUIET_EXPIRY_MS = 7L * 24 * 60 * 60_000L
        fun secondsUntil(until: Long, now: Long): Int =
            ((until - now).coerceAtLeast(1L) / 1_000L).coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}

internal object WebClientAddress {
    fun resolve(call: ApplicationCall, trustedProxyCidrs: List<String>): String {
        val direct = call.request.local.remoteHost
        if (!isTrusted(direct, trustedProxyCidrs)) return direct
        val forwarded = call.request.headers[HttpHeaders.XForwardedFor]
            ?.substringBefore(',')
            ?.trim()
            ?.takeIf { it.length <= 45 && it.isNotEmpty() && it.all { char ->
                char.isDigit() || char in 'a'..'f' || char in 'A'..'F' || char == '.' || char == ':'
            } }
            ?.let { literal ->
                try {
                    InetAddress.getByName(literal).hostAddress
                } catch (_: Exception) {
                    null
                }
            }
        return forwarded ?: direct
    }

    private fun isTrusted(address: String, cidrs: List<String>): Boolean =
        cidrs.any { cidr -> addressInCidr(address, cidr) }

    internal fun addressInCidr(address: String, cidr: String): Boolean {
        return try {
            val parts = cidr.split('/', limit = 2)
            val candidate = InetAddress.getByName(address).address
            val network = InetAddress.getByName(parts[0]).address
            if (candidate.size != network.size) return false
            val prefix = parts.getOrNull(1)?.toIntOrNull() ?: network.size * 8
            if (prefix !in 0..network.size * 8) return false
            var remaining = prefix
            for (index in network.indices) {
                if (remaining <= 0) break
                val bits = remaining.coerceAtMost(8)
                val mask = (0xff shl (8 - bits)) and 0xff
                if ((candidate[index].toInt() and mask) != (network[index].toInt() and mask)) return false
                remaining -= bits
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}

internal fun constantTimeTokenEquals(actualHeader: String?, expected: String): Boolean {
    if (expected.isBlank()) return false
    val actual = actualHeader?.removePrefix("Bearer ") ?: return false
    return MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())
}
