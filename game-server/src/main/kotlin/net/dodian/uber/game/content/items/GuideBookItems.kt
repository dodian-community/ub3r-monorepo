package net.dodian.uber.game.content.items

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.skills.guide.SkillGuidePlugin

object GuideBookItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(1856)

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        SkillGuidePlugin.openBook(client)
        return true
    }
}
