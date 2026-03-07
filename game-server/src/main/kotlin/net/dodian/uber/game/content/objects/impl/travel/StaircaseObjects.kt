package net.dodian.uber.game.content.objects.impl.travel

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.skills.core.VerticalTravelDslObjectContent
import net.dodian.uber.game.skills.core.verticalTravelActions

private fun startStairLegacy(
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
        VerticalTravelStyles.STAIRS,
    )

object StaircaseObjects : VerticalTravelDslObjectContent(
    verticalTravelActions {
        firstClick(1725) { client, _, position, _ ->
            if (position.x != 2732 || position.y != 3377) {
                return@firstClick false
            }
            if (!client.premium) {
                return@firstClick true
            }
            VerticalTravel.start(
                client,
                VerticalTravelCompletion.QueuedDestination(Position(2732, 3380, 1)),
                VerticalTravelStyles.STAIRS,
            )
        }
        firstClick(1726) { client, _, position, _ ->
            if (position.x != 2732 || position.y != 3378) {
                return@firstClick false
            }
            if (!client.premium) {
                return@firstClick true
            }
            VerticalTravel.start(
                client,
                VerticalTravelCompletion.QueuedDestination(Position(2732, 3376, 0)),
                VerticalTravelStyles.STAIRS,
            )
        }
        firstClick(16664) { client, _, position, _ ->
            val completion =
                when {
                    position.x == 2724 && position.y == 3374 -> {
                        if (!client.premium) {
                            client.resetPos()
                        }
                        VerticalTravelCompletion.QueuedDestination(Position(2727, 9774, 0))
                    }
                    position.x == 2603 && position.y == 3078 -> {
                        if (!client.checkItem(1543)) {
                            client.send(SendMessage("You need a red key to go down these stairs!"))
                            return@firstClick true
                        }
                        VerticalTravelCompletion.QueuedDestination(Position(2602, 9479, 0))
                    }
                    position.x == 2569 && position.y == 3122 -> {
                        if (!client.checkItem(1545)) {
                            client.send(SendMessage("You need a yellow key to use this staircase!"))
                            return@firstClick true
                        }
                        VerticalTravelCompletion.QueuedDestination(Position(2570, 9525, 0))
                    }
                    else -> return@firstClick false
                }
            VerticalTravel.start(client, completion, VerticalTravelStyles.STAIRS)
        }
        firstClick(1992) { client, _, position, _ ->
            if (position.x != 2558 || position.y != 3444) {
                return@firstClick false
            }
            VerticalTravel.start(
                client,
                VerticalTravelCompletion.QueuedDestination(Position(2717, 9820, 0)),
                VerticalTravelStyles.STAIRS,
            )
        }
        firstClick(881) { client, _, position, _ -> startStairLegacy(client, position, 2, 1) }
        firstClick(54) { client, _, position, _ -> startStairLegacy(client, position, 14, 3, 1) }
        firstClick(55) { client, _, position, _ -> startStairLegacy(client, position, 15, 3, 1) }
        firstClick(56) { client, _, position, _ -> startStairLegacy(client, position, 14, 3) }
        firstClick(57) { client, _, position, _ -> startStairLegacy(client, position, 15, 3) }
        firstClick(1568, 1570) { client, _, position, _ ->
            if (position.x == 2594 && position.y == 3085) {
                return@firstClick false
            }
            startStairLegacy(client, position, 3, 1)
        }
        firstClick(1723) { client, _, position, _ -> startStairLegacy(client, position, 22, 2, 2) }
    },
)
