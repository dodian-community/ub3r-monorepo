package net.dodian.uber.game.engine.lifecycle

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player.positions
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.action.PlayerActionCancellationService
import net.dodian.uber.game.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import net.dodian.uber.game.systems.skills.action.SkillingRandomEventService
import net.dodian.uber.game.content.skills.thieving.PyramidPlunder
import net.dodian.utilities.Misc

object PlayerLifecycleTickService {
    data class TimerSnapshot(
        val lastCombat: Int,
        val combatTimer: Int,
        val stunTimer: Int,
        val snareTimer: Int,
    )

    data class PrayerDrainStep(
        val nextCurrentDrainRate: Double,
        val shouldDrainPrayer: Boolean,
    )

    data class PostCombatOutcome(
        val disconnectPlayer: Boolean,
        val logoutPlayer: Boolean,
    )

    internal fun decrementTimers(
        lastCombat: Int,
        combatTimer: Int,
        stunTimer: Int,
        snareTimer: Int,
    ): TimerSnapshot =
        TimerSnapshot(
            lastCombat = maxOf(lastCombat - 1, 0),
            combatTimer = maxOf(combatTimer - 1, 0),
            stunTimer = maxOf(stunTimer - 1, 0),
            snareTimer = maxOf(snareTimer - 1, 0),
        )

    internal fun nextPrayerDrainStep(
        currentDrainRate: Double,
        drainRate: Double,
    ): PrayerDrainStep {
        if (currentDrainRate <= 0.0 && drainRate <= 0.0) {
            return PrayerDrainStep(0.0, false)
        }
        if (drainRate <= 0.0) {
            return PrayerDrainStep(0.0, false)
        }
        val updated = currentDrainRate + 0.6
        return if (updated >= drainRate) {
            PrayerDrainStep(0.0, true)
        } else {
            PrayerDrainStep(updated, false)
        }
    }

    internal fun shouldPersistActiveEffects(
        effects: List<Int?>,
        lastEffectsPeriodicDirtyAtMs: Long,
        wallClockNow: Long,
    ): Boolean {
        if (effects.isEmpty()) {
            return false
        }
        var hasActiveEffect = false
        for (i in effects.indices) {
            val effect = effects[i]
            if (effect != null && effect > 0) {
                hasActiveEffect = true
                break
            }
        }
        if (!hasActiveEffect) {
            return false
        }
        return wallClockNow - lastEffectsPeriodicDirtyAtMs >= 10_000L
    }

    internal fun evaluatePostCombatOutcome(
        isKicked: Boolean,
        updateRunning: Boolean,
        wallClockNow: Long,
        updateStartTime: Long,
        updateSeconds: Int,
    ): PostCombatOutcome {
        if (isKicked) {
            return PostCombatOutcome(disconnectPlayer = true, logoutPlayer = false)
        }
        val shouldLogoutForUpdate =
            updateRunning && wallClockNow - updateStartTime > updateSeconds * 1000L
        return PostCombatOutcome(disconnectPlayer = false, logoutPlayer = shouldLogoutForUpdate)
    }

    @JvmStatic
    fun processBeforeCombat(player: Client) {
        if (player.disconnected) {
            PlayerActionCancellationService.cancel(player, PlayerActionCancelReason.DISCONNECTED, false, false, false, true)
        }
        val decremented =
            decrementTimers(
                lastCombat = player.lastCombat,
                combatTimer = player.combatTimer,
                stunTimer = player.stunTimer,
                snareTimer = player.snareTimer,
            )
        player.lastCombat = decremented.lastCombat
        player.combatTimer = decremented.combatTimer
        player.stunTimer = decremented.stunTimer
        player.snareTimer = decremented.snareTimer

        player.changeEffectTime()
        if (player.genieCombatFlag && !player.isInCombat()) {
            player.genieCombatFlag = false
            SkillingRandomEventService.show(player)
        }

        PyramidPlunder.tick(player)

        if (player.getPositionName(player.position) == positions.DESERT && !player.effects.isEmpty() && player.effects[0] == -1) {
            player.addEffectTime(0, 30 + Misc.random(40))
        }

        handleBrimhavenDungeon(player)
        handlePrayerDrain(player)
        PlayerRecoveryTickService.process(player)

        if (player.reloadHp) {
            player.heal(player.maxHealth)
            player.currentPrayer = player.maxPrayer
            player.pray(0)
        }

    }

    @JvmStatic
    fun processAfterCombat(player: Client, wallClockNow: Long) {
        PlayerDeathTickService.process(player, wallClockNow)
        player.incrementTimeOutCounter()
        val outcome =
            evaluatePostCombatOutcome(
                isKicked = player.isKicked,
                updateRunning = Server.updateRunning,
                wallClockNow = wallClockNow,
                updateStartTime = Server.updateStartTime,
                updateSeconds = Server.updateSeconds,
            )
        if (outcome.disconnectPlayer) {
            player.disconnected = true
        } else if (outcome.logoutPlayer) {
            player.logout()
        }
    }

    @JvmStatic
    fun processEffectsPeriodicPersistence(player: Client, wallClockNow: Long) {
        if (!shouldPersistActiveEffects(player.effects, player.getLastEffectsPeriodicDirtyAtMs(), wallClockNow)) {
            return
        }
        player.markSaveDirty(PlayerSaveSegment.EFFECTS.mask)
        player.setLastEffectsPeriodicDirtyAtMs(wallClockNow)
    }

    private fun handlePrayerDrain(player: Client) {
        val prayers = player.prayerManager
        val step = nextPrayerDrainStep(prayers.getCurrentDrainRate(), prayers.getDrainRate())
        prayers.setCurrentDrainRate(step.nextCurrentDrainRate)
        if (step.shouldDrainPrayer) {
            player.drainPrayer(1)
        }
    }

    private fun handleBrimhavenDungeon(player: Client) {
        if (player.getPositionName(player.position) != positions.BRIMHAVEN_DUNGEON) {
            player.iconTimer = 6
            return
        }

        val gotIcon =
            player.equipment[Equipment.Slot.NECK.id] == 8923 ||
                player.gotSlayerHelmet(player)
        when {
            player.iconTimer > 0 && !gotIcon -> player.iconTimer--
            player.iconTimer == 0 && !gotIcon -> {
                player.sendMessage("The strange aura from the dungeon makes you vulnerable!")
                val damage = 5 + Misc.random(15)
                player.dealDamage(null, damage, if (damage >= 15) Entity.hitType.CRIT else Entity.hitType.STANDARD)
                player.iconTimer = 6
            }
            else -> player.iconTimer = 6
        }
    }
}
