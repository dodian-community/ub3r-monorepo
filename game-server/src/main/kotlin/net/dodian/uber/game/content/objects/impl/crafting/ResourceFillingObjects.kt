package net.dodian.uber.game.content.objects.impl.crafting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

object ResourceFillingObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(
        873, 874, 878, 879, 884,
        6232, 6249,
        8689,
        12279,
        14868, 14890,
        20358,
        25929,
    )

    override fun onUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean {
        if (objectId == 879 || objectId == 873 || objectId == 874 || objectId == 6232 ||
            objectId == 12279 || objectId == 14868 || objectId == 20358 || objectId == 25929
        ) {
            client.fillingObj = objectId
            client.filling = true
            return true
        }
        if (objectId == 884 || objectId == 878 || objectId == 6249) {
            client.fillingObj = objectId
            client.filling = true
            return true
        }
        if (objectId == 14890) {
            client.fillingObj = objectId
            client.filling = true
            return true
        }
        if (objectId == 8689 && itemId == 1925) {
            client.setFocus(position.x, position.y)
            client.deleteItem(itemId, 1)
            client.addItem(itemId + 2, 1)
            client.checkItemUpdate()
            return true
        }
        return false
    }
}
