package net.dodian.uber.game.runtime.world.npc

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Misc

/**
 * Canonical NPC lifecycle helpers (death floor -> invisible, loot messaging).
 *
 * Centralizing this allows schedulers to advance NPC state offscreen without
 * requiring full world scans.
 */
object NpcLifecycle {
    @JvmStatic
    fun processDeathFloor(npc: Npc) {
        npc.setVisible(false)
        npc.drop()
        val player = npc.getTarget(false)
        npc.removeEnemy(player)

        if (isJadNpc(npc)) {
            handleJadLoot(npc, player)
        } else if (isNewBossNpc(npc)) {
            handleNewBossLoot(npc, player)
        }
    }

    private fun isJadNpc(npc: Npc): Boolean = npc.id == 3127

    private fun isNewBossNpc(npc: Npc): Boolean =
        npc.id == 4303 || npc.id == 4304 || npc.id == 6610

    private fun handleJadLoot(
        npc: Npc,
        initialTarget: Client?,
    ) {
        for (i in 1..4) {
            if (npc.damage.isEmpty()) {
                break
            }
            val target = if (i == 1) initialTarget ?: npc.getTarget(false) else npc.getTarget(false)
            if (target != null) {
                handleLootRoll(npc, target)
            }
            npc.removeEnemy(target)
        }
    }

    private fun handleNewBossLoot(
        npc: Npc,
        initialTarget: Client?,
    ) {
        val target = npc.getSecondTarget(initialTarget, false)
        if (target != null) {
            handleLootRoll(npc, target)
        }
        npc.removeEnemy(target)
    }

    private fun handleLootRoll(npc: Npc, player: Client) {
        val damageMap = npc.damage
        val dealt = damageMap[player] ?: return
        val chance = (0.1 + (dealt / npc.maxHealth.toDouble())) * 100
        val rate = Misc.chance(100000) / 1000.0
        if (chance - 10 >= 5 && rate <= chance) {
            npc.drop()
            player.send(SendMessage("You managed to roll for the loot!"))
        } else if (chance - 10 < 5) {
            player.send(SendMessage("You were not eligible for the drop!"))
        } else {
            player.send(SendMessage("Unlucky! Better luck next time."))
        }
    }
}

