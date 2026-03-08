package net.dodian.uber.game.content.objects.travel

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.skills.core.VerticalTravelDslObjectContent
import net.dodian.uber.game.skills.core.verticalTravelActions

private fun Position.offset(dx: Int = 0, dy: Int = 0, dz: Int = 0): Position = Position(x + dx, y + dy, z + dz)

object LadderObjects : VerticalTravelDslObjectContent(
    verticalTravelActions {
        ladderUp(1747) { _, _, position, _ -> position.offset(dz = 1) }
        ladderUp(1738) { _, _, position, _ -> position.offset(dz = 1) }
        ladderUp(1734) { _, _, position, _ -> Position(position.x + 4, position.y - 6400, 0) }
        ladderUp(1755, 5946, 1757) { _, _, position, _ -> position.offset(dy = -6400) }
        ladderUp(1764) { _, _, position, _ -> Position(2857, 3167, position.z) }
        ladderUp(2148) { _, _, position, _ -> Position(3105, 3162, position.z) }
        ladderUp(2408) { _, _, position, _ -> Position(2828, 9772, position.z) }
        ladderUp(5055) { _, _, position, _ -> Position(3477, 9845, position.z) }
        ladderUp(5131) { _, _, position, _ -> Position(3549, 9865, position.z) }
        ladderUp(9359) { _, _, position, _ -> Position(2862, 9572, position.z) }
        firstClick(2406) { client, _, position, _ ->
            if (client.equipment[Equipment.Slot.WEAPON.id] != 772) {
                return@firstClick false
            }
            VerticalTravel.start(client, Position(2453, 4468, position.z), VerticalTravelStyles.LADDER)
        }
        ladderDown(1746, 1749, 1740) { _, _, position, _ -> position.offset(dz = -1) }
        ladderDown(5947, 6434) { _, _, position, _ -> position.offset(dy = 6400) }
        firstClick(2113) { client, _, position, _ ->
            if (client.getLevel(Skill.MINING) < 60) {
                client.send(SendMessage("You need 60 mining to enter the mining guild."))
                return@firstClick true
            }
            VerticalTravel.start(client, position.offset(dy = 6400), VerticalTravelStyles.LADDER)
        }
        ladderDown(492) { _, _, position, _ -> Position(2856, 9570, position.z) }
        ladderDown(2147) { _, _, position, _ -> Position(3104, 9576, position.z) }
        ladderDown(5054) { _, _, position, _ -> Position(3494, 3465, position.z) }
        ladderDown(5130) { _, _, position, _ -> Position(3543, 3463, position.z) }
        ladderDown(9358) { _, _, position, _ -> Position(2480, 5175, position.z) }
        ladderDown(5488) { _, _, position, _ -> Position(3201, 3169, position.z) }
        firstClick(9294) { _, _, position, _ ->
            position.x == 2879 && position.y == 9813
        }
        ladderDown(1739, option = 3) { _, _, position, _ -> position.offset(dz = -1) }
    },
)
