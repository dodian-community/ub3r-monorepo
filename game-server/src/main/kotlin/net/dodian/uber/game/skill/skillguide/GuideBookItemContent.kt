package net.dodian.uber.game.skill.skillguide

import net.dodian.uber.game.item.ItemContent
import net.dodian.uber.game.model.entity.player.Client

object GuideBookItemContent : ItemContent {
    override val itemIds: IntArray = intArrayOf(1856)

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        SkillGuide.openBook(client)
        return true
    }
}

