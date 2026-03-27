package net.dodian.uber.game.content.skills.crafting.objects

import net.dodian.uber.game.model.entity.player.Client

object ResourceFillingService {
    private data class FillEntry(val emptyItemId: Int, val filledItemId: Int, val emote: Int)

    private val waterSourceEntries =
        arrayOf(
            FillEntry(229, 227, 832),
            FillEntry(1980, 4458, 832),
            FillEntry(1935, 1937, 832),
            FillEntry(1825, 1823, 832),
            FillEntry(1827, 1823, 832),
            FillEntry(1829, 1823, 832),
            FillEntry(1831, 1823, 832),
            FillEntry(1925, 1929, 832),
            FillEntry(1923, 1921, 832),
        )

    private val cookingPotEntries = arrayOf(FillEntry(1925, 1783, 895))
    private val sinkEntries = arrayOf(FillEntry(1925, 1929, 832))

    @JvmStatic
    fun handleObjectUse(client: Client, objectId: Int): Boolean {
        val entries =
            when (objectId) {
                879, 873, 874, 6232, 12279, 14868, 20358, 25929 -> waterSourceEntries
                14890 -> cookingPotEntries
                884, 878, 6249 -> sinkEntries
                else -> return false
            }

        for (entry in entries) {
            if (!client.playerHasItem(entry.emptyItemId)) {
                continue
            }
            client.deleteItem(entry.emptyItemId, 1)
            client.addItem(entry.filledItemId, 1)
            client.checkItemUpdate()
            client.requestAnim(entry.emote, 0)
            return true
        }
        return true
    }
}
