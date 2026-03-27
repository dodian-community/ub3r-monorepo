package net.dodian.uber.game.runtime.lifecycle

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player.positions
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason
import net.dodian.uber.game.skills.core.runtime.SkillingRandomEventService
import net.dodian.uber.game.skills.thieving.plunder.PyramidPlunderService
import net.dodian.utilities.Misc

object PlayerLifecycleTickService {

    @JvmStatic
    fun processBeforeCombat(player: Client) {
        if (player.disconnected) {
            PlayerActionCancellationService.cancel(player, PlayerActionCancelReason.DISCONNECTED, false, false, false, true)
        }
        player.lastCombat = maxOf(player.lastCombat - 1, 0)
        player.combatTimer = maxOf(player.combatTimer - 1, 0)
        player.stunTimer = maxOf(player.stunTimer - 1, 0)
        player.snareTimer = maxOf(player.snareTimer - 1, 0)

        player.changeEffectTime()
        if (player.genieCombatFlag && !player.isInCombat()) {
            player.genieCombatFlag = false
            SkillingRandomEventService.show(player)
        }

        player.actionTimer = if (player.actionTimer > 0) player.actionTimer - 1 else 0
        PyramidPlunderService.tick(player)

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
        if (player.isKicked) {
            player.disconnected = true
        } else if (Server.updateRunning && wallClockNow - Server.updateStartTime > Server.updateSeconds * 1000L) {
            player.logout()
        }
    }

    private fun handlePrayerDrain(player: Client) {
        val prayers = player.prayerManager
        if (prayers.getCurrentDrainRate() <= 0.0 && prayers.getDrainRate() <= 0.0) {
            prayers.setCurrentDrainRate(0.0)
            return
        }

        if (prayers.getDrainRate() > 0.0) {
            prayers.setCurrentDrainRate(prayers.getCurrentDrainRate() + 0.6)
            if (prayers.getCurrentDrainRate() >= prayers.getDrainRate()) {
                player.drainPrayer(1)
                prayers.setCurrentDrainRate(0.0)
            }
        } else {
            prayers.setCurrentDrainRate(0.0)
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
                player.send(SendMessage("The strange aura from the dungeon makes you vulnerable!"))
                val damage = 5 + Misc.random(15)
                player.dealDamage(null, damage, if (damage >= 15) Entity.hitType.CRIT else Entity.hitType.STANDARD)
                player.iconTimer = 6
            }
            else -> player.iconTimer = 6
        }
    }
}
