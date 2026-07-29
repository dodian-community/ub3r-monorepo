package net.dodian.uber.game.persistence.account

import java.util.concurrent.ConcurrentHashMap

/** Short local barrier after an accepted player-requested logout. */
object LogoutReLoginGuard {
    private const val COOLDOWN_MS = 5_000L
    private val untilByDbId = ConcurrentHashMap<Int, Long>()

    @JvmStatic @JvmOverloads fun recordLogout(dbId: Int, now: Long = System.currentTimeMillis()) {
        if (dbId > 0) untilByDbId[dbId] = now + COOLDOWN_MS
    }

    @JvmStatic @JvmOverloads fun remainingMillis(dbId: Int, now: Long = System.currentTimeMillis()): Long {
        val until = untilByDbId[dbId] ?: return 0L
        val remaining = until - now
        if (remaining <= 0L) untilByDbId.remove(dbId, until)
        return remaining.coerceAtLeast(0L)
    }

    @JvmStatic fun clearForTests() = untilByDbId.clear()
}
