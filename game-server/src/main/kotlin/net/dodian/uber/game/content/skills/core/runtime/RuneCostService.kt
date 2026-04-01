package net.dodian.uber.game.content.skills.core.runtime

import net.dodian.uber.game.model.entity.player.Client

object RuneCostService {
    @JvmStatic
    fun hasAll(client: Client, runes: IntArray, amounts: IntArray): Boolean =
        net.dodian.uber.game.systems.skills.RuneCostService.hasAll(client, runes, amounts)

    @JvmStatic
    fun isMissingAny(client: Client, runes: IntArray, amounts: IntArray): Boolean =
        net.dodian.uber.game.systems.skills.RuneCostService.isMissingAny(client, runes, amounts)

    @JvmStatic
    fun consume(client: Client, runes: IntArray, amounts: IntArray) =
        net.dodian.uber.game.systems.skills.RuneCostService.consume(client, runes, amounts)

    @JvmStatic
    fun ensureBloodRune(client: Client): Boolean =
        net.dodian.uber.game.systems.skills.RuneCostService.ensureBloodRune(client)
}
