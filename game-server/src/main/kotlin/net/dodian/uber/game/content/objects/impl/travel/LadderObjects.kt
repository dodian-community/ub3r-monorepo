package net.dodian.uber.game.content.objects.impl.travel

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.skills.core.VerticalTravelDslObjectContent
import net.dodian.uber.game.skills.core.verticalTravelActions

private fun startLadderLegacy(
    client: Client,
    position: Position,
    stairs: Int,
    distance: Int,
    distanceAdd: Int = 0,
): Boolean =
    VerticalTravel.start(
        client,
        VerticalTravelCompletion.LegacyStair(
            stairs = stairs,
            skillX = position.x,
            skillY = position.y,
            stairDistance = distance,
            stairDistanceAdd = distanceAdd,
        ),
        VerticalTravelStyles.LADDER,
    )

object LadderObjects : VerticalTravelDslObjectContent(
    verticalTravelActions {
        firstClick(1747) { client, _, position, _ -> startLadderLegacy(client, position, 1, 1) }
        firstClick(1738) { client, _, position, _ -> startLadderLegacy(client, position, 1, 2) }
        firstClick(1734) { client, _, position, _ -> startLadderLegacy(client, position, 10, 3, 1) }
        firstClick(1755, 5946, 1757) { client, _, position, _ -> startLadderLegacy(client, position, 4, 2) }
        firstClick(1764) { client, _, position, _ -> startLadderLegacy(client, position, 12, 1) }
        firstClick(2148) { client, _, position, _ -> startLadderLegacy(client, position, 8, 1) }
        firstClick(2408) { client, _, position, _ -> startLadderLegacy(client, position, 16, 1) }
        firstClick(5055) { client, _, position, _ -> startLadderLegacy(client, position, 18, 1) }
        firstClick(5131) { client, _, position, _ -> startLadderLegacy(client, position, 20, 1) }
        firstClick(9359) { client, _, position, _ -> startLadderLegacy(client, position, 24, 1) }
        firstClick(2406) { client, _, position, _ ->
            if (client.equipment[Equipment.Slot.WEAPON.id] != 772) {
                return@firstClick false
            }
            startLadderLegacy(client, position, 27, 1)
        }
        firstClick(1746, 1749, 1740) { client, _, position, _ -> startLadderLegacy(client, position, 2, 1) }
        firstClick(5947, 6434) { client, _, position, _ -> startLadderLegacy(client, position, 3, 1) }
        firstClick(2113) { client, _, position, _ ->
            if (client.getLevel(Skill.MINING) < 60) {
                client.send(SendMessage("You need 60 mining to enter the mining guild."))
                return@firstClick true
            }
            startLadderLegacy(client, position, 3, 1)
        }
        firstClick(492) { client, _, position, _ -> startLadderLegacy(client, position, 11, 2) }
        firstClick(2147) { client, _, position, _ -> startLadderLegacy(client, position, 7, 1) }
        firstClick(5054) { client, _, position, _ -> startLadderLegacy(client, position, 17, 1) }
        firstClick(5130) { client, _, position, _ -> startLadderLegacy(client, position, 19, 1) }
        firstClick(9358) { client, _, position, _ -> startLadderLegacy(client, position, 23, 1) }
        firstClick(5488) { client, _, position, _ -> startLadderLegacy(client, position, 28, 1) }
        firstClick(9294) { _, _, position, _ ->
            position.x == 2879 && position.y == 9813
        }
        thirdClick(1739) { client, _, position, _ -> startLadderLegacy(client, position, 2, 1) }
    },
)
