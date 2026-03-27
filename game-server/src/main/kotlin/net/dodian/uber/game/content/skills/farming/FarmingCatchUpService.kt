package net.dodian.uber.game.content.skills.farming

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.pulse.GlobalPulseService

object FarmingCatchUpService {
    private const val MAX_CATCH_UP_PULSES = 288

    @JvmStatic
    fun applyLoginCatchUp(client: Client) {
        applyCatchUp(client, System.currentTimeMillis())
    }

    @JvmStatic
    fun applyInteractionCatchUp(client: Client) {
        applyCatchUp(client, System.currentTimeMillis())
    }

    @JvmStatic
    fun applyCatchUp(
        client: Client,
        nowMillis: Long,
    ): Int {
        val state = client.farmingJson
        val lastPulseAt = state.lastGlobalPulseAtMillis
        if (lastPulseAt <= 0L) {
            state.lastGlobalPulseAtMillis = nowMillis
            client.markFarmingDirty()
            return 0
        }

        val elapsed = nowMillis - lastPulseAt
        if (elapsed < GlobalPulseService.FIVE_MINUTE_PULSE_MS) {
            return 0
        }

        val missedPulses = (elapsed / GlobalPulseService.FIVE_MINUTE_PULSE_MS).toInt()
        val pulsesToApply = missedPulses.coerceAtMost(MAX_CATCH_UP_PULSES)
        repeat(pulsesToApply) {
            client.farming.run { client.updateFarming() }
        }
        state.lastGlobalPulseAtMillis =
            if (missedPulses > MAX_CATCH_UP_PULSES) {
                nowMillis
            } else {
                lastPulseAt + (pulsesToApply * GlobalPulseService.FIVE_MINUTE_PULSE_MS)
            }
        client.markFarmingDirty()
        return pulsesToApply
    }
}

