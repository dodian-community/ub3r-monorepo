package net.dodian.uber.game.content.skills.slayer.items

import net.dodian.uber.game.content.entities.items.ItemContent
import net.dodian.uber.game.content.entities.npcs.spawns.SlayerMasterDialogue
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.quests.QuestSend

object SlayerGemItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(4155)

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        if (client.inTrade || client.inDuel) {
            return true
        }
        SlayerMasterDialogue.showCurrentTask(client)
        return true
    }

    override fun onSecondClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        SlayerMasterDialogue.showResetCountPrompt(client)
        return true
    }

    override fun onThirdClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        QuestSend.showMonsterLog(client)
        return true
    }
}
