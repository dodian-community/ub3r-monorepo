package net.dodian.uber.game.engine.lifecycle
import net.dodian.uber.game.api.content.ContentActions

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerCore.positions
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.engine.util.Misc
import net.dodian.uber.game.engine.systems.action.PlayerActionCancellationService
import net.dodian.uber.game.engine.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import net.dodian.uber.game.skill.runtime.action.SkillingRandomEventService
import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.skills.thieving.ThievingModule

object PlayerLifecycleTickService {
    data class TimerSnapshot(
        val lastCombat: Int,
        val combatTimer: Int,
        val stunTimer: Int,
        val snareTimer: Int,
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
            ContentActions.cancel(player, PlayerActionCancelReason.DISCONNECTED, false, false, false, true)
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
        tickPlayerPoison(player)
        if (player.genieCombatFlag && !player.isInCombat) {
            player.genieCombatFlag = false
            SkillingRandomEventService.show(player)
        }

        ThievingModule.tick(player.asSkillPlayer())

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
        if (!shouldPersistActiveEffects(player.effects, player.contentRuntimeState.getEffectsPeriodicDirtyAtMillis(), wallClockNow)) {
            return
        }
        player.markSaveDirty(PlayerSaveSegment.EFFECTS.mask)
        player.contentRuntimeState.setEffectsPeriodicDirtyAtMillis(wallClockNow)
    }

    private fun handlePrayerDrain(player: Client) {
        net.dodian.uber.skills.prayer.PrayerModule.tickDrain(player.asSkillPlayer())
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

    private fun tickPlayerPoison(player: Client) {
        if (player.poisonDamage <= 0) {
            return
        }
        if (player.poisonTimer > 0) {
            player.poisonTimer--
            return
        }
        player.poisonTimer = 30
        player.dealDamage(null, player.poisonDamage, Entity.hitType.POISON)
        player.poisonDamage--
        if (player.poisonDamage <= 0) {
            player.sendMessage("You have been cured of the poison.")
        }
    }
}
