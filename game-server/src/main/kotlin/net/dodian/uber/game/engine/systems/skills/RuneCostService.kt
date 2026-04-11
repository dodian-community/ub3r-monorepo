package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object RuneCostService {
    @JvmStatic
    fun hasAll(client: Client, runes: IntArray, amounts: IntArray): Boolean {
        for (index in runes.indices) {
            if (!client.playerHasItem(runes[index], amounts[index])) {
                return false
            }
        }
        return true
    }

    @JvmStatic
    fun isMissingAny(client: Client, runes: IntArray, amounts: IntArray): Boolean = !hasAll(client, runes, amounts)

    @JvmStatic
    fun consume(client: Client, runes: IntArray, amounts: IntArray) {
        for (index in runes.indices) {
            client.deleteItem(runes[index], amounts[index])
        }
    }

    @JvmStatic
    fun ensureBloodRune(client: Client): Boolean {
        if (client.playerHasItem(565)) {
            return true
        }
        client.sendMessage("This spell requires 1 blood rune")
        return false
    }
}
