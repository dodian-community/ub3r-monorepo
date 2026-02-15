package net.dodian.uber.game.content.objects.impl.runecrafting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

object RunecraftingObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(14903, 14905, 27978)

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when (objectId) {
            14905 -> {
                client.runecraft(561, 1, 60)
                true
            }
            27978 -> {
                client.runecraft(565, 50, 85)
                true
            }
            14903 -> {
                client.runecraft(564, 75, 120)
                true
            }
            else -> false
        }
    }
}
