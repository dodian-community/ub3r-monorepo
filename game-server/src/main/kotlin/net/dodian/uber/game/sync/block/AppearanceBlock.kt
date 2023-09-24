package net.dodian.uber.game.sync.block

import net.dodian.uber.game.modelkt.entity.player.Appearance
import net.dodian.uber.game.modelkt.entity.player.Player
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

    companion object {

        fun createFrom(player: Player): AppearanceBlock {
            // TODO:
            val combat = 126
            val id = -1

            return AppearanceBlock(
                name = player.encodedName,
                appearance = player.appearance,
                combat = combat,
                skill = 0,
                equipment = player.equipment,
                headIcon = -1,
                npcId = id,
                isSkulled = false
            )
        }
    }
}