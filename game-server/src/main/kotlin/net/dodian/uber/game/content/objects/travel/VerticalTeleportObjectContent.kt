package net.dodian.uber.game.content.objects.travel

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.systems.interaction.VerticalTravelDslObjectContent
import net.dodian.uber.game.systems.interaction.verticalTravelActions
import net.dodian.uber.game.content.objects.travel.VerticalTravel
import net.dodian.uber.game.content.objects.travel.VerticalTravelStyles

object VerticalTeleportObjectContent : VerticalTravelDslObjectContent(
    verticalTravelActions {
        firstClick(16675) { client, _, position, _ ->
            val destination =
                when {
                    position.x == 2488 && position.y == 3407 -> Position(2489, 3409, 1)
                    position.x == 2485 && position.y == 3402 -> Position(2485, 3401, 1)
                    position.x == 2445 && position.y == 3434 -> Position(2445, 3433, 1)
                    position.x == 2444 && position.y == 3414 -> Position(2445, 3416, 1)
                    else -> return@firstClick false
                }
            VerticalTravel.start(client, destination, VerticalTravelStyles.STAIRS)
        }
        firstClick(16677) { client, _, position, _ ->
            val destination =
                when {
                    position.x == 2489 && position.y == 3408 -> Position(2488, 3406, 0)
                    position.x == 2485 && position.y == 3402 -> Position(2485, 3404, 0)
                    position.x == 2445 && position.y == 3434 -> Position(2446, 3436, 0)
                    position.x == 2445 && position.y == 3415 -> Position(2444, 3413, 0)
                    else -> return@firstClick false
                }
            VerticalTravel.start(client, destination, VerticalTravelStyles.STAIRS)
        }
        firstClick(16665) { client, _, position, _ ->
            val destination =
                when {
                    position.x == 2724 && position.y == 9774 -> {
                        if (!client.premium) {
                            client.resetPos()
                        }
                        Position(2723, 3375, 0)
                    }
                    position.x == 2603 && position.y == 9478 -> Position(2606, 3079, 0)
                    position.x == 2569 && position.y == 9522 -> Position(2570, 3121, 0)
                    else -> return@firstClick false
                }
            VerticalTravel.start(client, destination, VerticalTravelStyles.STAIRS)
        }
        firstClick(16683) { client, _, position, _ ->
            if (position.x != 2597 || position.y != 3107) {
                return@firstClick false
            }
            VerticalTravel.start(
                client,
                Position(2597, 3106, 1),
                VerticalTravelStyles.STAIRS,
            )
        }
        firstClick(16681) { client, _, position, _ ->
            if (position.x != 2597 || position.y != 3107) {
                return@firstClick false
            }
            VerticalTravel.start(
                client,
                Position(2597, 3106, 0),
                VerticalTravelStyles.STAIRS,
            )
        }
        firstClick(25938) { client, _, position, _ ->
            if (position.x != 2715 || position.y != 3470) {
                return@firstClick false
            }
            VerticalTravel.start(
                client,
                Position(2714, 3470, 1),
                VerticalTravelStyles.STAIRS,
            )
        }
        firstClick(25939) { client, _, position, _ ->
            if (position.x != 2715 || position.y != 3470) {
                return@firstClick false
            }
            VerticalTravel.start(
                client,
                Position(2715, 3471, 0),
                VerticalTravelStyles.STAIRS,
            )
        }
        firstClick(2833) { client, _, position, _ ->
            if (position.x != 2544 || position.y != 3111) {
                return@firstClick false
            }
            VerticalTravel.start(
                client,
                Position(2544, 3112, 1),
                VerticalTravelStyles.STAIRS,
            )
        }
        firstClick(17122) { client, _, position, _ ->
            if (position.x != 2544 || position.y != 3111) {
                return@firstClick false
            }
            VerticalTravel.start(
                client,
                Position(2544, 3112, 0),
                VerticalTravelStyles.STAIRS,
            )
        }
        firstClick(2796) { client, _, position, _ ->
            if (position.x != 2549 || position.y != 3111) {
                return@firstClick false
            }
            VerticalTravel.start(
                client,
                Position(2549, 3112, 2),
                VerticalTravelStyles.STAIRS,
            )
        }
        firstClick(2797) { client, _, position, _ ->
            if (position.x != 2549 || position.y != 3111) {
                return@firstClick false
            }
            VerticalTravel.start(
                client,
                Position(2549, 3112, 1),
                VerticalTravelStyles.STAIRS,
            )
        }
    },
)
