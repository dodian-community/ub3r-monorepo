package net.dodian.uber.game.runtime.combat

import java.util.ArrayList
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object CombatHitQueueService {
    private data class PendingHit(
        val resolveCycle: Long,
        val attacker: Client,
        val target: Entity,
        val damage: Int,
        val hitType: Entity.hitType,
        val damageType: Entity.damageType?,
    )

    private val pendingHits = ArrayList<PendingHit>(64)

    @JvmStatic
    fun enqueue(
        resolveCycle: Long,
        attacker: Client,
        target: Entity,
        damage: Int,
        hitType: Entity.hitType,
        damageType: Entity.damageType?,
    ) {
        pendingHits.add(PendingHit(resolveCycle, attacker, target, damage, hitType, damageType))
    }

    @JvmStatic
    fun process(currentCycle: Long) {
        if (pendingHits.isEmpty()) {
            return
        }
        var index = 0
        while (index < pendingHits.size) {
            val pending = pendingHits[index]
            if (pending.resolveCycle > currentCycle) {
                index++
                continue
            }
            pendingHits.removeAt(index)
            apply(pending)
        }
    }

    @JvmStatic
    fun clearFor(player: Client) {
        if (pendingHits.isEmpty()) {
            return
        }
        pendingHits.removeIf { pending -> pending.attacker === player || pending.target === player }
    }

    @JvmStatic
    fun clearFor(npc: Npc) {
        if (pendingHits.isEmpty()) {
            return
        }
        pendingHits.removeIf { pending -> pending.target === npc }
    }

    private fun apply(hit: PendingHit) {
        if (hit.attacker.disconnected || hit.attacker.isDeathSequenceActive()) {
            return
        }
        when (val target = hit.target) {
            is Npc -> {
                if (!target.alive || target.currentHealth <= 0) {
                    return
                }
                target.dealDamage(hit.attacker, hit.damage, hit.hitType)
            }
            is Client -> {
                if (target.disconnected || target.isDeathSequenceActive() || target.currentHealth <= 0) {
                    return
                }
                val damageType = hit.damageType
                if (damageType != null) {
                    target.dealDamage(hit.damage, hit.hitType, hit.attacker, damageType)
                } else {
                    target.dealDamage(hit.attacker, hit.damage, hit.hitType)
                }
            }
        }
    }
}
