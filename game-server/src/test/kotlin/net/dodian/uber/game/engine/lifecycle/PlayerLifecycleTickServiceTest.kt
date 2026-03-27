package net.dodian.uber.game.engine.lifecycle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerLifecycleTickServiceTest {
    @Test
    fun `timer decrement clamps at zero`() {
        val timers =
            PlayerLifecycleTickService.decrementTimers(
                lastCombat = 1,
                combatTimer = 0,
                stunTimer = 2,
                snareTimer = 0,
                actionTimer = 1,
            )

        assertEquals(0, timers.lastCombat)
        assertEquals(0, timers.combatTimer)
        assertEquals(1, timers.stunTimer)
        assertEquals(0, timers.snareTimer)
        assertEquals(0, timers.actionTimer)
    }

    @Test
    fun `prayer drain step drains prayer and resets accumulator at threshold`() {
        val step =
            PlayerLifecycleTickService.nextPrayerDrainStep(
                currentDrainRate = 1.8,
                drainRate = 2.0,
            )

        assertTrue(step.shouldDrainPrayer)
        assertEquals(0.0, step.nextCurrentDrainRate)
    }

    @Test
    fun `post combat outcome disconnects kicked player and logs out after update timer`() {
        val kickedOutcome =
            PlayerLifecycleTickService.evaluatePostCombatOutcome(
                isKicked = true,
                updateRunning = true,
                wallClockNow = 5_000L,
                updateStartTime = 0L,
                updateSeconds = 1,
            )
        assertTrue(kickedOutcome.disconnectPlayer)
        assertFalse(kickedOutcome.logoutPlayer)

        val updateOutcome =
            PlayerLifecycleTickService.evaluatePostCombatOutcome(
                isKicked = false,
                updateRunning = true,
                wallClockNow = 10_001L,
                updateStartTime = 0L,
                updateSeconds = 10,
            )
        assertFalse(updateOutcome.disconnectPlayer)
        assertTrue(updateOutcome.logoutPlayer)
    }

    @Test
    fun `effects periodic persistence only triggers when active and interval elapsed`() {
        assertFalse(
            PlayerLifecycleTickService.shouldPersistActiveEffects(
                effects = listOf(-1, 0, null),
                lastEffectsPeriodicDirtyAtMs = 0L,
                wallClockNow = 50_000L,
            ),
        )

        assertFalse(
            PlayerLifecycleTickService.shouldPersistActiveEffects(
                effects = listOf(10, -1),
                lastEffectsPeriodicDirtyAtMs = 45_001L,
                wallClockNow = 50_000L,
            ),
        )

        assertTrue(
            PlayerLifecycleTickService.shouldPersistActiveEffects(
                effects = listOf(10, -1),
                lastEffectsPeriodicDirtyAtMs = 39_999L,
                wallClockNow = 50_000L,
            ),
        )
    }
}
