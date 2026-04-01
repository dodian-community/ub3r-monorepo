package net.dodian.uber.game.content.skills.crafting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.skills.crafting.CraftingPlugin

object SpinningWheelObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(14889, 14896, 14909, 25824)

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when (objectId) {
            14889, 25824 -> {
                client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
                CraftingPlugin.startSpinning(client)
                true
            }
            14896, 14909 -> {
                client.addItem(1779, 1)
                client.checkItemUpdate()
                true
            }
            else -> false
        }
    }
}
