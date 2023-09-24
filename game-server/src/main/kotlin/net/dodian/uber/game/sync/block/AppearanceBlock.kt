package net.dodian.uber.game.sync.block

import net.dodian.uber.game.modelkt.entity.player.Appearance
import net.dodian.uber.game.modelkt.inventory.Inventory

class AppearanceBlock(
    val appearance: Appearance,
    val combat: Int,
    val equipment: Inventory,
    val isSkulled: Boolean,
    val name: Long,
    val headIcon: Int,
    val npcId: Int = -1,
    val skill: Int = 0
) : SynchronizationBlock {
    val isAppearingAsNpc: Boolean get() = npcId != -1
}