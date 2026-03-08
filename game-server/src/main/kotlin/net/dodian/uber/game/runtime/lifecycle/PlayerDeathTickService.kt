package net.dodian.uber.game.runtime.lifecycle

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.runtime.loop.GameCycleClock
import net.dodian.utilities.Misc

object PlayerDeathTickService {
    private const val RESPAWN_DELAY_TICKS = 3L

    @JvmStatic
    fun process(player: Client, wallClockNow: Long) {
        if (player.deathStage == 0 && player.currentHealth < 1) {
            beginDeath(player, wallClockNow)
            return
        }

        if (player.deathStage == 1 &&
            player.deathStartedCycle > 0L &&
            GameCycleClock.currentCycle() - player.deathStartedCycle >= RESPAWN_DELAY_TICKS
        ) {
            respawn(player)
        }
    }

    private fun beginDeath(player: Client, wallClockNow: Long) {
        player.resetAttack()
        if (player.target is Npc) {
            val npc = Server.npcManager.getNpc(player.target.slot)
            npc.removeEnemy(player)
        } else {
            val other = player.getClient(player.duel_with)
            if (player.duel_with > 0 && player.validClient(player.duel_with) && player.inDuel && player.duelFight) {
                other.duelWin = true
                other.DuelVictory()
            }
        }
        player.requestAnim(836, 5)
        player.currentHealth = 0
        player.deathStage++
        player.deathTimer = wallClockNow
        player.deathStartedCycle = player.currentGameCycle
        player.prayerManager.reset()
        player.send(SendMessage("Oh dear you have died!"))
    }

    private fun respawn(player: Client) {
        player.transport(Position(2604 + Misc.random(6), 3101 + Misc.random(3), player.teleportHeight))
        player.deathStage = 0
        player.deathTimer = 0
        player.deathStartedCycle = 0
        player.combatTimer = 0
        player.lastCombat = 0

        for (i in 0 until player.effects.size) {
            player.addEffectTime(i, if (i == 0 || player.effects[i] == -1) -1 else 0)
        }

        for (i in player.boostedLevel.indices) {
            if (i == 3) {
                player.heal(Skills.getLevelForExperience(player.getExperience(Skill.HITPOINTS)))
            } else if (i == 5) {
                player.currentPrayer = Skills.getLevelForExperience(player.getExperience(Skill.PRAYER))
            } else {
                player.boostedLevel[i] = 0
            }
            player.refreshSkill(Skill.getSkill(i))
        }

        if (player.inWildy()) {
            player.died()
        }
        if (player.skullIcon >= 0) {
            player.skullIcon = -1
        }
        player.GetBonus(true)
        player.requestWeaponAnims()
        player.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
    }
}
