package net.dodian.uber.game.content.events.partyroom

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.systems.api.content.ContentTiming
import kotlin.math.max
import kotlin.random.Random

class DropParty(private var minutes: Int) : Runnable {
    private val startTime = System.currentTimeMillis() + (minutes * 60_000L)
    private var started = false
    private val dropItems: MutableList<GameItem> = DropPartyItems.getDropPartyItems().toMutableList()

    init {
        Client.publicyell("<col=FF0000>[S] There will be a drop party @ Ardougne in aproximately $minutes minutes.")
    }

    override fun run() {
        if (dropItems.isEmpty()) {
            return
        }

        val startDelayMs = max(0L, startTime - System.currentTimeMillis()).toInt()
        ContentTiming.runLaterMs(startDelayMs) {
            if (!started) {
                Client.publicyell("<col=FF0000>[S] The drop party in Ardougne has now started!")
                started = true
            }
            beginDropLoop()
        }
    }

    fun getMinutes(): Int = minutes

    fun setMinutes(minutes: Int) {
        this.minutes = minutes
    }

    private fun beginDropLoop() {
        ContentTiming.runRepeatingMs(3000) {
            if (dropItems.isEmpty()) {
                ContentTiming.runLaterMs(180_000) {
                    Client.publicyell("<col=FF0000>[S] The drop party has now ended.")
                }
                return@runRepeatingMs false
            }
            dropItems.removeAt(Random.nextInt(dropItems.size))
            true
        }
    }

    private enum class DropPartyItems(val id: Int, val amount: Int, val occurrences: Int) {
        FANCY_BOOTS(9005, 1, 1),
        DRAGON_MED_HELM(1149, 1, 3),
        NATURE_RUNES(561, 250, 3),
        BLOOD_RUNE(565, 200, 2),
        MAGIC_LOGS(1514, 100, 1),
        RUNE_FULL_HELM(1164, 2, 2),
        UNCUT_DIAMONDS(1618, 25, 2),
        AMULET_OF_POWER(1731, 1, 8),
        RUNE_SCIMITAR(1333, 1, 4),
        COAL(454, 100, 5),
        ADAMANT_BOOTS(4129, 1, 5),
        DRAGON_BONES(537, 45, 2),
        IRON_BARS(2352, 50, 4),
        CASH(995, 175_000, 20);

        companion object {
            fun getDropPartyItems(): ArrayList<GameItem> {
                val items = ArrayList<GameItem>()
                values().forEach { item ->
                    repeat(item.occurrences) {
                        items.add(GameItem(item.id, item.amount))
                    }
                }
                return items
            }
        }
    }
}
