package net.dodian.uber.game.content.objects.banking

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

/**
 * Banking objects (booths/chests/etc).
 *
 * Matches legacy behavior in ClickObjectListener / ClickObject2Listener:
 * - click1: `openUpBank()` for anything with "bank" in the name, and also sets WanneBank for certain ids.
 * - click2: sets WanneBank for certain ids or names containing "bank booth".
 */
object Banks : ObjectContent {
    override val objectIds: IntArray = intArrayOf(
        2213,
        2214,
        3045,
        5276,
        6084,
        6943, // observed booth id (ClickObject2 log)
        9391, // tzhaar bank object (legacy special-case)
    )

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val objectName = obj?.name?.lowercase() ?: ""

        if (objectId == 9391) {
            client.openUpBank()
        }
        if (objectName.startsWith("bank") || objectName.contains("bank")) {
            client.openUpBank()
        }

        if (objectId == 2213 || objectId == 2214 || objectId == 3045 || objectId == 5276 || objectId == 6084) {
            client.skillX = position.x
            client.setSkillY(position.y)
            client.WanneBank = 1
            client.WanneShop = -1
        }

        return true
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        val objectName = obj?.name?.lowercase() ?: ""

        if (
            objectId == 2213 || objectId == 2214 || objectId == 3045 || objectId == 5276 || objectId == 6084 ||
            objectId == 6943 ||
            objectName.contains("bank booth")
        ) {
            client.skillX = position.x
            client.setSkillY(position.y)
            client.WanneBank = 1
            client.WanneShop = -1
        }

        return true
    }
}
