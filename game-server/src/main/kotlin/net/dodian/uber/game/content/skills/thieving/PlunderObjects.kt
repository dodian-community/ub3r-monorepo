package net.dodian.uber.game.content.skills.thieving

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.systems.ui.dialogue.DialogueService
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.api.content.ContentInteraction
import net.dodian.uber.game.systems.api.content.ContentObjectInteractionPolicy
import net.dodian.uber.game.content.skills.thieving.PyramidPlunderService
import net.dodian.utilities.Misc

object PlunderObjects : ObjectContent {
    override val objectIds: IntArray = ThievingObjectComponents.plunderObjects

    override fun clickInteractionPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ContentObjectInteractionPolicy? {
        if (option != 1 && option != 2) {
            return null
        }
        return ContentInteraction.nearestBoundaryCardinalPolicy()
    }

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when {
            objectId in 26622..26625 -> {
                if (client.getLevel(Skill.THIEVING) < 21 || client.stunTimer > 0) {
                    client.send(
                        SendMessage(
                            if (client.getLevel(Skill.THIEVING) < 21) {
                                "You need level 21 thieving to enter."
                            } else {
                                "You are stunned!"
                            },
                        ),
                    )
                    return true
                }
                if (PyramidPlunderService.isEntryDoor(position)) {
                    val chance = Misc.random(255)
                    if (chance <= (client.getLevel(Skill.THIEVING) * 2.5).toInt()) {
                        client.transport(Position(1934, 4450, 2))
                    } else {
                        client.dealDamage(null, Misc.random(3), Entity.hitType.STANDARD)
                        client.stunTimer = 4
                    }
                } else {
                    client.transport(Position(1968, 4420, 2))
                }
                true
            }
            objectId in 26618..26621 -> {
                if (PyramidPlunderService.roomNumber(client) + 1 == 8) {
                    return true
                }
                if (PyramidPlunderService.canOpenNextRoomDoor(client, objectId)) {
                    PyramidPlunderService.advanceRoom(client)
                } else if (PyramidPlunderService.openDoor(client, objectId)) {
                    client.sendMessage("This tomb door lead nowhere.")
                } else {
                    PyramidPlunderService.toggleObstacle(client, objectId)
                }
                true
            }
            objectId == 20932 -> {
                client.transport(PyramidPlunderService.endPosition(client))
                true
            }
            objectId == 20931 -> {
                DialogueService.setDialogueId(client, 20931)
                DialogueService.setDialogueSent(client, false)
                true
            }
            objectId == 26616 || objectId == 26626 -> {
                PyramidPlunderService.toggleObstacle(client, objectId)
                true
            }
            objectId == 26580 || objectId in 26600..26613 -> {
                PyramidPlunderService.toggleObstacle(client, objectId)
                true
            }
            objectId == 20275 -> {
                client.transport(Position(2799, 5160, 0))
                client.setFocus(2799, 5159)
                true
            }
            objectId == 20277 -> {
                client.transport(Position(3315, 2796, 0))
                client.setFocus(3315, 2797)
                true
            }
            else -> false
        }
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 20931) {
            PyramidPlunderService.reset(client)
            return true
        }
        return false
    }
}
