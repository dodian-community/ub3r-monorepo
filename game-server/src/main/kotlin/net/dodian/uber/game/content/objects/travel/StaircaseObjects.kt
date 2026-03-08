package net.dodian.uber.game.content.objects.travel

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.skills.core.VerticalTravelDslObjectContent
import net.dodian.uber.game.skills.core.verticalTravelActions

private fun Position.stairsOffset(dx: Int = 0, dy: Int = 0, dz: Int = 0): Position = Position(x + dx, y + dy, z + dz)

object StaircaseObjects : VerticalTravelDslObjectContent(
    verticalTravelActions {
        firstClick(1725) { client, _, position, _ ->
            if (position.x != 2732 || position.y != 3377) {
                return@firstClick false
            }
            if (!client.premium) {
                return@firstClick true
            }
            VerticalTravel.start(client, Position(2732, 3380, 1), VerticalTravelStyles.STAIRS)
        }
        firstClick(1726) { client, _, position, _ ->
            if (position.x != 2732 || position.y != 3378) {
                return@firstClick false
            }
            if (!client.premium) {
                return@firstClick true
            }
            VerticalTravel.start(client, Position(2732, 3376, 0), VerticalTravelStyles.STAIRS)
        }
        firstClick(16664) { client, _, position, _ ->
            val destination =
                when {
                    position.x == 2724 && position.y == 3374 -> {
                        if (!client.premium) {
                            client.resetPos()
                        }
                        Position(2727, 9774, 0)
                    }
                    position.x == 2603 && position.y == 3078 -> {
                        if (!client.checkItem(1543)) {
                            client.send(SendMessage("You need a red key to go down these stairs!"))
                            return@firstClick true
                        }
                        Position(2602, 9479, 0)
                    }
                    position.x == 2569 && position.y == 3122 -> {
                        if (!client.checkItem(1545)) {
                            client.send(SendMessage("You need a yellow key to use this staircase!"))
                            return@firstClick true
                        }
                        Position(2570, 9525, 0)
                    }
                    else -> return@firstClick false
                }
            VerticalTravel.start(client, destination, VerticalTravelStyles.STAIRS)
        }
        firstClick(1992) { client, _, position, _ ->
            if (position.x != 2558 || position.y != 3444) {
                return@firstClick false
            }
            VerticalTravel.start(client, Position(2717, 9820, 0), VerticalTravelStyles.STAIRS)
        }
        stairsDown(881) { _, _, position, _ -> position.stairsOffset(dz = -1) }
        stairsDown(54) { _, _, position, _ -> position.stairsOffset(dy = -6396) }
        stairsUp(55) { _, _, position, _ -> position.stairsOffset(dy = 6396) }
        stairsDown(56) { _, _, position, _ -> position.stairsOffset(dy = -6397) }
        stairsUp(57) { _, _, position, _ -> position.stairsOffset(dy = 6397) }
        stairsDown(1568, 1570) { _, _, position, _ ->
            if (position.x == 2594 && position.y == 3085) {
                return@stairsDown null
            }
            position.stairsOffset(dy = 6400)
        }
        stairsDown(1723) { _, _, position, _ -> position.stairsOffset(dy = -4, dz = -1) }
    },
)
