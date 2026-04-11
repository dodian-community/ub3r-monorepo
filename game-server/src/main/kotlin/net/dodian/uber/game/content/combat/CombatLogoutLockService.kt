package net.dodian.uber.game.content.combat

import kotlin.math.ceil
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.loop.GameCycleClock

object CombatLogoutLockService {
    private val logoutLockTicks = GameCycleClock.ticksForDurationMs(10_000L).toLong()

    @JvmStatic
    fun refresh(player: Client, cycleNow: Long = GameCycleClock.currentCycle()) {
        val nextExpiry = cycleNow + logoutLockTicks
        if (nextExpiry > player.combatLogoutLockUntilCycle) {
            player.combatLogoutLockUntilCycle = nextExpiry
        }
    }

    @JvmStatic
    fun refreshInteraction(attacker: Entity?, defender: Entity?) {
        refreshInteraction(attacker, defender, GameCycleClock.currentCycle())
    }

    @JvmStatic
    fun refreshInteraction(attacker: Entity?, defender: Entity?, cycleNow: Long = GameCycleClock.currentCycle()) {
        if (attacker is Client) {
            refresh(attacker, cycleNow)
        }
        if (defender is Client) {
            refresh(defender, cycleNow)
        }
        if (attacker is Client && defender is Client) {
            val now = System.currentTimeMillis()
            attacker.lastPlayerCombat = now
            defender.lastPlayerCombat = now
        }
    }

    @JvmStatic
    fun isLocked(player: Client): Boolean = isLocked(player, GameCycleClock.currentCycle())

    @JvmStatic
    fun isLocked(player: Client, cycleNow: Long = GameCycleClock.currentCycle()): Boolean =
        player.combatLogoutLockUntilCycle > cycleNow

    @JvmStatic
    fun remainingSeconds(player: Client, cycleNow: Long = GameCycleClock.currentCycle()): Int {
        val remainingCycles = player.combatLogoutLockUntilCycle - cycleNow
        if (remainingCycles <= 0L) {
            return 0
        }
        return ceil((remainingCycles * Server.TICK.toDouble()) / 1000.0).toInt().coerceAtLeast(1)
    }

    @JvmStatic
    fun clear(player: Client) {
        player.combatLogoutLockUntilCycle = 0L
    }
}
